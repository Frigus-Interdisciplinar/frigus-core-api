package com.frigus.coreapi.service;

import com.frigus.coreapi.client.FrigusAiClient;
import com.frigus.coreapi.dto.receipt.*;
import com.frigus.coreapi.enums.ReceiptStatus;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

@Service
public class ReceiptService {
 private static final Set<String> SP_HOSTS=Set.of("www.nfce.fazenda.sp.gov.br","nfce.fazenda.sp.gov.br");
 private static final Pattern ACCESS_KEY=Pattern.compile("(?<!\\d)(\\d{44})(?!\\d)");
 private final ReceiptDraftRepository receipts; private final ReceiptDraftLineRepository lines;
 private final GroupContextService context; private final ProductRepository products; private final StockProductService stockProducts;
 private final FrigusAiClient ai;
 public ReceiptService(ReceiptDraftRepository receipts,ReceiptDraftLineRepository lines,GroupContextService context,ProductRepository products,StockProductService stockProducts,FrigusAiClient ai){this.receipts=receipts;this.lines=lines;this.context=context;this.products=products;this.stockProducts=stockProducts;this.ai=ai;}

 @Transactional
 public ReceiptResponse scan(ReceiptScanRequest request){
  Stock stock=requireStock(request.stockId(),false);
  URI uri=validateQr(request.qrPayload());
  FetchedPage page=fetch(uri);
  if(hasQrValidationFailure(page.html())) throw error(400,"INVALID_QR_PAYLOAD","A SEFAZ-SP rejeitou este QR Code: a assinatura digital não corresponde à NFC-e.");
  if(page.visibleText().length()<30) throw error(422,"UNSUPPORTED_SEFAZ_PAGE","Página fiscal sem conteúdo legível.");
  String qrKey=accessKeyFromQr(uri);
  List<FrigusAiClient.InvoiceProductInput> productInputs=parseStructuredInvoice(page.html());
  if(productInputs==null||productInputs.isEmpty())throw error(422,"UNSUPPORTED_SEFAZ_PAGE","Não foi possível identificar a lista estruturada de produtos na página fiscal.");
  FrigusAiClient.InvoiceExtraction aiResult;
  try { aiResult=ai.extractInvoice(productInputs); }
  catch(ServiceUnavailableException e){throw error(503,"AI_UNAVAILABLE","A classificação dos produtos por IA está temporariamente indisponível.");}
  var metadata=parseInvoiceMetadata(page.html());
  FrigusAiClient.InvoiceExtraction extraction=new FrigusAiClient.InvoiceExtraction(qrKey,metadata.issuerName(),metadata.issuedAt(),metadata.totalAmount(),aiResult.items(),aiResult.warnings());
  if(extraction.accessKey()==null || !ACCESS_KEY.matcher(extraction.accessKey()).matches() || !qrKey.equals(extraction.accessKey()) || extraction.items()==null || extraction.items().isEmpty())
   throw error(422,"EXTRACTION_FAILED","Não foi possível identificar itens com evidência na nota.");
  validateAiItems(productInputs,extraction.items());
  if(!validMoney(extraction.totalAmount()) || (extraction.issuedAt()!=null && parseDate(extraction.issuedAt())==null))
   throw error(422,"EXTRACTION_FAILED","A resposta da extração contém valores fiscais inválidos.");
  var already=receipts.findByAccessKeyAndStockId(extraction.accessKey(),stock.getId());
  if(already.isPresent() && already.get().getStatus()==ReceiptStatus.IMPORTED) throw error(409,"ALREADY_IMPORTED","Esta nota já foi importada neste estoque.");
  if(already.isPresent()) {
   if(!already.get().getCreatedBy().getId().equals(context.currentUser().getId()))
    throw error(409,"ALREADY_IMPORTED","Esta nota já foi registrada neste estoque.");
   return toResponse(already.get());
  }
  var extractionWarnings=new ArrayList<String>();
  if(extraction.warnings()!=null) extraction.warnings().stream().filter(Objects::nonNull).map(w->clean(w,500)).limit(20).forEach(extractionWarnings::add);
  var receipt=ReceiptDraft.builder().stock(stock).createdBy(context.currentUser()).status(ReceiptStatus.REVIEW_REQUIRED)
    .accessKey(extraction.accessKey()).issuerName(clean(extraction.issuerName(),255)).issuedAt(parseDate(extraction.issuedAt()))
    .totalAmount(money(extraction.totalAmount())).warnings(extractionWarnings).build();
  receipt=receipts.save(receipt);
  var groupId=stock.getGroup().getId();
  for(var item:extraction.items()){
   if(item==null || item.description()==null || item.description().isBlank() || item.evidence()==null || item.evidence().isBlank() || item.quantity()==null || item.quantity()<=0 || item.quantity()>Integer.MAX_VALUE) continue;
   if(!validMoney(item.unitPrice()) || !validMoney(item.totalPrice())) throw error(422,"EXTRACTION_FAILED","A resposta da extração contém preço inválido.");
   if(item.quantity()%1!=0){if(extractionWarnings.size()<20) extractionWarnings.add("Um item com quantidade fracionária foi omitido porque o estoque aceita somente unidades inteiras.");continue;}
   String description=clean(item.description(),500); String normalized=clean(item.productName(),255).trim();
   Product match=products.findFirstByOwnerGroupIdAndNameIgnoreCase(groupId,normalized).orElseGet(()->products.findFirstByNameIgnoreCaseAndOwnerGroupIsNull(normalized).orElse(null));
   lines.save(ReceiptDraftLine.builder().receipt(receipt).description(description).normalizedProductName(normalized).quantity(item.quantity().intValue()).unit(clean(item.unit(),30))
    .unitPrice(money(item.unitPrice())).totalPrice(money(item.totalPrice())).confidence(confidence(item.confidence())).evidence(clean(item.evidence(),1000)).matchedProduct(match)
    .brand(clean(item.brand(),120)).category(com.frigus.coreapi.enums.Category.valueOf(item.category())).storagePlace(com.frigus.coreapi.enums.StoragePlace.valueOf(item.storagePlace())).unitOfMeasure(com.frigus.coreapi.enums.UnitOfMeasure.valueOf(item.unitOfMeasure())).build());
  }
  receipt.setWarnings(extractionWarnings); receipts.save(receipt);
  if(lines.findByReceiptIdOrderById(receipt.getId()).isEmpty()) throw error(422,"EXTRACTION_FAILED","A nota não contém itens com quantidade inteira e evidência verificável.");
  return toResponse(receipt);
 }

 @Transactional(readOnly=true)
 public ReceiptResponse get(UUID id){ ReceiptDraft receipt=owned(id); requireStock(receipt.getStock().getId(),false); return toResponse(receipt); }

 @Transactional
 public ReceiptResponse.ConfirmationResponse confirm(UUID id,ReceiptConfirmRequest request){
  ReceiptDraft receipt=owned(id); requireStock(receipt.getStock().getId(),true);
  if(receipt.getStatus()==ReceiptStatus.IMPORTED) throw error(409,"ALREADY_IMPORTED","Esta leitura já foi confirmada.");
  var imported=new ArrayList<ReceiptResponse.ImportedItem>(); var ignored=new ArrayList<UUID>();
  Set<UUID> seen=new HashSet<>();
  for(var selection:request.items()){
   if(!seen.add(selection.lineId())) throw new BadRequestException("Linha duplicada","Uma linha foi informada mais de uma vez.");
   var line=lines.findByIdAndReceiptId(selection.lineId(),receipt.getId()).orElseThrow(()->new BadRequestException("Linha inválida","A linha não pertence a esta leitura."));
   if("IGNORE".equals(selection.action())){ignored.add(line.getId());continue;}
   if(!"IMPORT".equals(selection.action()) || selection.productId()==null || selection.quantity()==null || selection.quantity()<=0 || selection.expireDate()==null)
    throw new BadRequestException("Confirmação inválida","Informe produto, quantidade inteira e validade para importar.");
   var created=stockProducts.create(StockProductCreateRequestDto.builder().productId(selection.productId()).stockId(receipt.getStock().getId())
    .quantity(selection.quantity()).expireDate(selection.expireDate()).batch("NFCE-"+receipt.getId()+"-"+line.getId()).build());
   imported.add(new ReceiptResponse.ImportedItem(line.getId(),created.getId(),selection.productId(),selection.quantity()));
   line.setMatchedProduct(products.findById(selection.productId()).orElse(null)); line.setQuantity(selection.quantity()); line.setExpireDate(selection.expireDate());
  }
  if(seen.size()!=lines.findByReceiptIdOrderById(receipt.getId()).size())
   throw new BadRequestException("Revisão incompleta","Revise cada linha da nota antes de confirmar.");
  receipt.setStatus(ReceiptStatus.IMPORTED); receipt.setImportedAt(Instant.now()); receipts.save(receipt); lines.saveAll(lines.findByReceiptIdOrderById(receipt.getId()));
  return new ReceiptResponse.ConfirmationResponse(receipt.getId(),"IMPORTED",imported,ignored);
 }

 private ReceiptDraft owned(UUID id){return receipts.findByIdAndCreatedById(id,context.currentUser().getId()).orElseThrow(NotFoundException::new);}
 private Stock requireStock(Integer id,boolean write){try{return context.requireStock(id,write);}catch(ForbiddenException e){throw error(403,"STOCK_ACCESS_DENIED","Você não tem acesso a este estoque.");}}
 private ReceiptResponse toResponse(ReceiptDraft r){var items=lines.findByReceiptIdOrderById(r.getId()).stream().map(l->new ReceiptResponse.Item(l.getId(),l.getDescription(),l.getNormalizedProductName(),l.getBrand(),l.getCategory(),l.getStoragePlace(),l.getUnitOfMeasure(),l.getQuantity(),l.getUnit(),l.getUnitPrice(),l.getTotalPrice(),l.getConfidence(),l.getEvidence(),l.getMatchedProduct()==null?null:new ReceiptResponse.MatchedProduct(l.getMatchedProduct().getId(),l.getMatchedProduct().getName()),l.getExpireDate())).toList();
  return new ReceiptResponse(r.getId(),r.getStatus().name(),new ReceiptResponse.Invoice("NFCE",r.getAccessKey(),r.getIssuerName(),r.getIssuedAt(),r.getTotalAmount()),items,r.getWarnings()==null?List.of():r.getWarnings());}
 private URI validateQr(String raw){try{URI u=URI.create(raw.trim().replace("|","%7C"));String host=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||host==null||!SP_HOSTS.contains(host.toLowerCase(Locale.ROOT))||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null)throw new IllegalArgumentException();
   String path=u.getPath();
   if(!("/qrcode".equalsIgnoreCase(path)||"/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx".equalsIgnoreCase(path)) || u.getRawQuery()==null || !u.getRawQuery().matches("(?i).*(^|&)p=[^&]+.*"))throw new IllegalArgumentException();
   String qrParam=Arrays.stream(u.getRawQuery().split("&")).filter(q->q.regionMatches(true,0,"p=",0,2)).findFirst().orElse("");
   if(!ACCESS_KEY.matcher(URLDecoder.decode(qrParam.substring(2),java.nio.charset.StandardCharsets.UTF_8)).find())throw new IllegalArgumentException();
   InetAddress[] addresses=InetAddress.getAllByName(host);if(addresses.length==0||Arrays.stream(addresses).anyMatch(a->a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress()))throw new IllegalArgumentException();
   return u;
  }catch(Exception e){throw error(400,"INVALID_QR_PAYLOAD","O QR Code deve apontar para uma página HTTPS oficial da SEFAZ-SP.");}}
 private FetchedPage fetch(URI uri){try{Connection.Response response=Jsoup.connect(uri.toString()).userAgent("Frigus-Demo/1.0").timeout(10_000).maxBodySize(1_000_000).followRedirects(false).ignoreHttpErrors(true).execute();
   if(response.statusCode()>=300&&response.statusCode()<400)throw new IllegalStateException("Redirecionamento não permitido");
   if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Portal indisponível");
   var document=response.parse();
   return new FetchedPage(document.body().text(),document.html());
  }catch(Exception e){throw error(502,"SEFAZ_UNAVAILABLE","Não foi possível consultar a página da SEFAZ-SP agora.");}}
 static boolean hasQrValidationFailure(String html){return html!=null&&Pattern.compile("(?is)(qr\\s*code.{0,100}inv[aá]lido|digest\\s+value.{0,180}(inconsistente|inv[aá]lido))").matcher(html).find();}
 static List<FrigusAiClient.InvoiceProductInput> parseStructuredInvoice(String html){
  var document=Jsoup.parse(html);
  var rows=document.select("#tabResult tr[id^=Item]");
  if(rows.isEmpty())return List.of();
  var items=new ArrayList<FrigusAiClient.InvoiceProductInput>();
  int lineNumber=0;
  for(var row:rows){
   lineNumber++;
   var title=row.selectFirst("td:first-child > span.txtTit");
   var quantity=row.selectFirst(".Rqtd");var unit=row.selectFirst(".RUN");
   var unitPrice=row.selectFirst(".RvlUnit");var totalPrice=row.selectFirst(".valor");
   if(title==null||quantity==null||unit==null||unitPrice==null||totalPrice==null)return null;
   Double qty=parseBrazilianNumber(afterLabel(quantity.text()));
   Double unitValue=parseBrazilianNumber(afterLabel(unitPrice.text()));
   Double totalValue=parseBrazilianNumber(totalPrice.text());
   if(qty==null||unitValue==null||totalValue==null)return null;
   String description=title.text().trim();String evidence=row.text().trim();
   if(description.isBlank()||evidence.isBlank())return null;
   items.add(new FrigusAiClient.InvoiceProductInput(lineNumber,description,qty,afterLabel(unit.text()).trim(),unitValue,totalValue,evidence));
  }
  return items;
 }
 private static void validateAiItems(List<FrigusAiClient.InvoiceProductInput> source,List<FrigusAiClient.InvoiceItem> result){
  if(result.size()!=source.size())throw error(422,"EXTRACTION_FAILED","A IA não classificou todos os produtos da nota.");
  var expected=new HashMap<Integer,FrigusAiClient.InvoiceProductInput>();
  source.forEach(p->expected.put(p.lineNumber(),p));
  var seen=new HashSet<Integer>();
  for(var item:result){
   if(item==null)throw error(422,"EXTRACTION_FAILED","A IA retornou um produto inválido.");
   var original=expected.get(item.lineNumber());
   if(original==null||!seen.add(item.lineNumber())||item.description()==null||item.description().isBlank()||item.productName()==null||item.productName().isBlank()||item.evidence()==null||item.evidence().isBlank()
     ||!Objects.equals(original.description(),item.description())||!Objects.equals(original.quantity(),item.quantity())||!Objects.equals(original.unit(),item.unit())||!Objects.equals(original.unitPrice(),item.unitPrice())||!Objects.equals(original.totalPrice(),item.totalPrice())||!Objects.equals(original.evidence(),item.evidence()))
    throw error(422,"EXTRACTION_FAILED","A resposta da IA não preservou os dados fiscais de um produto.");
   try { com.frigus.coreapi.enums.Category.valueOf(item.category()); com.frigus.coreapi.enums.StoragePlace.valueOf(item.storagePlace()); com.frigus.coreapi.enums.UnitOfMeasure.valueOf(item.unitOfMeasure()); }
   catch(Exception invalid){throw error(422,"EXTRACTION_FAILED","A IA retornou uma classificação de produto inválida.");}
  }
 }
 private static InvoiceMetadata parseInvoiceMetadata(String html){
  var document=Jsoup.parse(html);
  String issuerText=Optional.ofNullable(document.selectFirst(".txtTopo")).map(org.jsoup.nodes.Element::text).orElse(null);
  String issuedAt=null;
  Matcher issued=Pattern.compile("(?i)Emissão:\\s*(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(document.text());
  if(issued.find())try{issuedAt=LocalDateTime.parse(issued.group(1),java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")).atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toString();}catch(Exception ignored){}
  Double totalAmount=null;
  for(var total:document.select("#totalNota #linhaTotal")){
   var label=total.selectFirst("label");var value=total.selectFirst(".totalNumb");
   if(label!=null&&value!=null&&label.text().toLowerCase(Locale.ROOT).contains("valor a pagar")){totalAmount=parseBrazilianNumber(value.text());break;}
  }
  if(totalAmount==null)for(var total:document.select("#totalNota #linhaTotal")){
   var label=total.selectFirst("label");var value=total.selectFirst(".totalNumb");
   if(label!=null&&value!=null&&label.text().toLowerCase(Locale.ROOT).contains("valor total")){totalAmount=parseBrazilianNumber(value.text());break;}
  }
  return new InvoiceMetadata(issuerText,issuedAt,totalAmount);
 }
 private record InvoiceMetadata(String issuerName,String issuedAt,Double totalAmount){}
 private static String afterLabel(String text){int colon=text.indexOf(':');return colon<0?text:text.substring(colon+1).trim();}
 private static Double parseBrazilianNumber(String raw){
  if(raw==null)return null;String value=raw.replaceAll("[^0-9,.-]","");
  if(value.isBlank())return null;
  if(value.contains(","))value=value.replace(".","").replace(',','.');
  try{double parsed=Double.parseDouble(value);return Double.isFinite(parsed)&&parsed>=0?parsed:null;}catch(NumberFormatException e){return null;}
 }
 private record FetchedPage(String visibleText,String html){}
 private static BigDecimal money(Double v){if(v==null||!Double.isFinite(v)||v<0||v>9999999999d)return null;return BigDecimal.valueOf(v).setScale(2,java.math.RoundingMode.HALF_UP);}
 private static boolean validMoney(Double v){return v==null||(Double.isFinite(v)&&v>=0&&v<=9999999999d);}
 private static Double confidence(Double v){return v!=null&&Double.isFinite(v)&&v>=0&&v<=1?v:null;}
 private static String clean(String s,int max){if(s==null)return null;String x=s.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]","").trim();return x.length()>max?x.substring(0,max):x;}
 private static Instant parseDate(String s){if(s==null)return null;try{return OffsetDateTime.parse(s).toInstant();}catch(Exception ignored){try{return Instant.parse(s);}catch(Exception ignoredAgain){return null;}}}
 private static String accessKeyFromQr(URI uri){String p=Arrays.stream(uri.getRawQuery().split("&")).filter(q->q.regionMatches(true,0,"p=",0,2)).findFirst().orElseThrow();Matcher m=ACCESS_KEY.matcher(URLDecoder.decode(p.substring(2),java.nio.charset.StandardCharsets.UTF_8));if(!m.find())throw error(400,"INVALID_QR_PAYLOAD","A chave de acesso não foi encontrada no QR Code.");return m.group(1);}
 private static AppException error(int status,String code,String message){return new ReceiptException(status,code,message,message);}
}
