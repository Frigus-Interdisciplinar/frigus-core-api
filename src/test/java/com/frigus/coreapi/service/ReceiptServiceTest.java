package com.frigus.coreapi.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceiptServiceTest {
    @Test
    void recognizesSignatureMismatchInSefazPageScripts() {
        String response = "$('#spnErroProsseguirMaster').html('<li>QR Code inválido. Erro(s): - Assinatura do documento (Digest Value) inconsistente</li>');";
        assertTrue(ReceiptService.hasQrValidationFailure(response));
    }

    @Test
    void doesNotFlagRegularInvoicePageAsQrFailure() {
        assertFalse(ReceiptService.hasQrValidationFailure("Consulta NFC-e QR Code - itens e valor total"));
    }

    @Test
    void extractsTypedProductListForMandatoryAiEnrichment() {
        String html = """
                <div class="txtTopo">MERCADO TESTE LTDA.</div>
                <table id="tabResult">
                  <tr id="Item + 1"><td><span class="txtTit">LEITE INTEGRAL 1L</span>
                    <span class="Rqtd"><strong>Qtde.:</strong>2</span>
                    <span class="RUN"><strong>UN: </strong>UN</span>
                    <span class="RvlUnit"><strong>Vl. Unit.:</strong> 5,00</span></td>
                    <td><span class="valor">10,00</span></td></tr>
                </table>
                <div id="totalNota"><div id="linhaTotal"><label>Valor a pagar R$:</label>
                  <span class="totalNumb">9,50</span></div></div>
                <div>Emissão: 24/10/2026 12:30:00</div>
                """;

        var result = ReceiptService.parseStructuredInvoice(html);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LEITE INTEGRAL 1L", result.get(0).description());
        assertEquals(1, result.get(0).lineNumber());
        assertEquals(2.0, result.get(0).quantity());
        assertEquals("UN", result.get(0).unit());
        assertEquals(5.0, result.get(0).unitPrice());
        assertEquals(10.0, result.get(0).totalPrice());
        assertTrue(result.get(0).evidence().contains("LEITE INTEGRAL 1L"));
    }
}
