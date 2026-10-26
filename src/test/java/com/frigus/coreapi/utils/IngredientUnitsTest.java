package com.frigus.coreapi.utils;
import com.frigus.coreapi.enums.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class IngredientUnitsTest {
 @Test void convertsOnlyCompatibleMeasures(){
  assertThat(IngredientUnits.convert(2,UnitOfMeasure.KILOGRAM,"g")).isEqualByComparingTo("2000");
  assertThat(IngredientUnits.convert(500,UnitOfMeasure.MILLILITER,"LITER")).isEqualByComparingTo("0.5");
  assertThat(IngredientUnits.convert(1,UnitOfMeasure.DOZEN,"UNIT")).isEqualByComparingTo("12");
  assertThat(IngredientUnits.convert(2,UnitOfMeasure.KILOGRAM,"LITER")).isNull();
  assertThat(IngredientUnits.convert(1,UnitOfMeasure.PACKAGE,"UNIT")).isNull();
 }
}
