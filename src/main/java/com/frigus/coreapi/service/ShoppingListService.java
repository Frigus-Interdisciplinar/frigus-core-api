package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.shopping.*;
import com.frigus.coreapi.enums.*;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ShoppingListService {
 @org.springframework.beans.factory.annotation.Value("${app.time-zone:America/Sao_Paulo}") private String timeZone="America/Sao_Paulo";
 private final ShoppingListRepository lists;
 private final ShoppingListProductRepository items;
 private final ProductRepository products;
 private final StockProductRepository inventory;
 private final BusinessExpenseRepository expenses;
 private final GroupContextService context;
 private final GroupAccessService access;
 private final ProductMapper productMapper;
 public Page<ShoppingListResponseDto> list(Integer stockId,ListStatus status,Pageable pageable){
  context.requireStock(stockId,false);
  return (status==null ? lists.findByStockId(stockId,pageable) : lists.findByStockIdAndStatus(stockId,status,pageable)).map(this::toDto);
 }
 public ShoppingListResponseDto get(UUID id){return toDto(requireList(id,false));}
 @Transactional public ShoppingListResponseDto create(ShoppingListRequestDto dto){
  var stock=context.requireStock(dto.stockId(),true);
  var list=ShoppingList.builder().stock(stock).name(dto.name().trim()).date(dto.date()==null ? LocalDate.now(ZoneId.of(timeZone)) : dto.date()).status(ListStatus.OPEN).createdAt(Instant.now()).build();
  return toDto(lists.save(list));
 }
 @Transactional public ShoppingListResponseDto update(UUID id,ShoppingListRequestDto dto){
  var list=requireList(id,true);requireOpen(list);
  if(!list.getStock().getId().equals(dto.stockId())) throw new BadRequestException("Estoque imutável","Crie uma nova lista para outro estoque");
  list.setName(dto.name().trim());if(dto.date()!=null) list.setDate(dto.date());return toDto(lists.save(list));
 }
 @Transactional public void cancel(UUID id){var list=requireList(id,true);requireOpen(list);list.setStatus(ListStatus.CANCELED);lists.save(list);}
 @Transactional public ShoppingItemResponseDto add(UUID listId,ShoppingItemRequestDto dto){
  var list=requireList(listId,true);requireOpen(list);
  var product=products.findById(dto.productId()).orElseThrow(NotFoundException::new);
  context.requireProductForGroup(product,list.getStock().getGroup().getId());
  var existing=items.findByListIdAndProductId(listId,dto.productId());
  if(existing.isPresent() && existing.get().getStatus()!=ProductListStatus.REMOVED) throw new ConflictException("Item já incluído","Edite a quantidade do item existente");
  var item=existing.orElseGet(() -> ShoppingListProduct.builder().list(list).product(product).createdAt(Instant.now()).build());
  item.setQuantity(dto.quantity());item.setStatus(ProductListStatus.PENDING);item.setPurchasedUnitPrice(null);
  return toItemDto(items.save(item));
 }
 @Transactional public ShoppingItemResponseDto updateItem(UUID listId,Integer itemId,ShoppingItemUpdateDto dto){
  var list=requireList(listId,true);requireOpen(list);var item=requireItem(listId,itemId);
  if(dto.quantity()!=null) item.setQuantity(dto.quantity());
  if(dto.status()!=null) item.setStatus(dto.status());
  if(dto.purchasedUnitPrice()!=null) item.setPurchasedUnitPrice(dto.purchasedUnitPrice());
  if(item.getStatus()==ProductListStatus.PURCHASED && item.getPurchasedUnitPrice()==null) item.setPurchasedUnitPrice(item.getProduct().getUnitPrice());
  return toItemDto(items.save(item));
 }
 @Transactional public void removeItem(UUID listId,Integer itemId){
  requireOpen(requireList(listId,true));var item=requireItem(listId,itemId);item.setStatus(ProductListStatus.REMOVED);items.save(item);
 }
 @Transactional public void clearPurchased(UUID id){
  requireOpen(requireList(id,true));
  for(var item:items.findByListId(id)) if(item.getStatus()==ProductListStatus.PURCHASED){item.setStatus(ProductListStatus.REMOVED);items.save(item);}
 }
 @Transactional public ShoppingListResponseDto complete(UUID id,CompleteShoppingListDto dto){
  var list=requireList(id,true);
  if(list.getStatus()==ListStatus.COMPLETED) return toDto(list);
  requireOpen(list);var all=items.findByListId(id);
  if(all.stream().anyMatch(i -> i.getStatus()==ProductListStatus.PENDING)) throw new BadRequestException("Há itens pendentes","Compre ou remova os itens pendentes antes de concluir");
  var purchased=all.stream().filter(i -> i.getStatus()==ProductListStatus.PURCHASED).toList();
  if(purchased.isEmpty()) throw new BadRequestException("Lista sem compras","Compre ao menos um item antes de concluir");
  BigDecimal total=BigDecimal.ZERO;
  for(var item:purchased){
   if(item.getPurchasedUnitPrice()==null) item.setPurchasedUnitPrice(item.getProduct().getUnitPrice());
   total=total.add(item.getPurchasedUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));items.save(item);
  }
  list.setStatus(ListStatus.COMPLETED);list.setSupplier(dto.supplier());list.setTotal(total);list.setCompletedAt(Instant.now());lists.save(list);
  var group=list.getStock().getGroup();
  if(group.getOwner().getAccountType()==AccountType.COMMERCIAL && total.signum()>0)
   expenses.save(BusinessExpense.builder().group(group).createdBy(access.requireCurrentUser()).description("Compra: "+list.getName()).category("PURCHASE").amount(total).expenseDate(LocalDate.now(ZoneId.of(timeZone))).supplier(dto.supplier()).shoppingList(list).build());
  return toDto(list);
 }
 public List<ShoppingSuggestionDto> suggestions(Integer stockId){
  context.requireStock(stockId,false);
  Map<Integer,Integer> quantities=new HashMap<>(),minimums=new HashMap<>();Map<Integer,Product> productMap=new LinkedHashMap<>();
  for(var item:inventory.findActiveByStock(stockId)){
   productMap.put(item.getProduct().getId(),item.getProduct());
   if(!item.getExpireDate().isBefore(LocalDate.now(ZoneId.of(timeZone)))) quantities.merge(item.getProduct().getId(),item.getQuantity(),Math::addExact);
   if(item.getMinimalQuantity()!=null) minimums.merge(item.getProduct().getId(),item.getMinimalQuantity(),Math::max);
  }
  List<ShoppingSuggestionDto> result=new ArrayList<>();
  for(var entry:productMap.entrySet()){
   int quantity=quantities.getOrDefault(entry.getKey(),0),minimum=minimums.getOrDefault(entry.getKey(),0);
   if(quantity<=minimum) result.add(new ShoppingSuggestionDto(productMapper.toDto(entry.getValue()),quantity,Math.max(1,minimum+1-quantity),quantity==0 ? "OUT_OF_STOCK" : "LOW_STOCK"));
  }
  return result;
 }
 private ShoppingList requireList(UUID id,boolean write){
  var list=(write ? lists.lockById(id) : lists.findById(id)).orElseThrow(NotFoundException::new);
  context.requireStock(list.getStock().getId(),write);return list;
 }
 private ShoppingListProduct requireItem(UUID id,Integer itemId){var item=items.findById(itemId).orElseThrow(NotFoundException::new);if(!item.getList().getId().equals(id)) throw new NotFoundException();return item;}
 private void requireOpen(ShoppingList list){if(list.getStatus()!=ListStatus.OPEN) throw new ConflictException("Lista encerrada","Crie uma nova lista para continuar");}
 private ShoppingItemResponseDto toItemDto(ShoppingListProduct item){return new ShoppingItemResponseDto(item.getId(),productMapper.toDto(item.getProduct()),item.getQuantity(),item.getStatus(),item.getPurchasedUnitPrice());}
 private ShoppingListResponseDto toDto(ShoppingList list){return new ShoppingListResponseDto(list.getId(),list.getStock().getId(),list.getName(),list.getDate(),list.getStatus(),list.getSupplier(),list.getTotal(),list.getCompletedAt(),list.getCreatedAt(),items.findByListIdOrderByCreatedAtAsc(list.getId()).stream().filter(i -> i.getStatus()!=ProductListStatus.REMOVED).map(this::toItemDto).toList());}
}
