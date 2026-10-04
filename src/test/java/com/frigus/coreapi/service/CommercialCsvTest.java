package com.frigus.coreapi.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CommercialCsvTest {
 @Test void escapesCsvQuotesAndSpreadsheetFormulas(){
  assertThat(CommercialService.cell("Shop, \"A\"")).isEqualTo("\"Shop, \"\"A\"\"\"");
  assertThat(CommercialService.cell("  =1+1")).startsWith("\"'  =1+1");
  assertThat(CommercialService.cell(null)).isEmpty();
 }
}
