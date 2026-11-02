package com.frigus.coreapi.utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import com.frigus.coreapi.enums.UnitOfMeasure;
public final class IngredientUnits {
 private IngredientUnits(){}
 private record Unit(String dimension,BigDecimal factor){}
 private static Unit parse(String value){
  if(value==null) return null;
  return switch(value.trim().toUpperCase(Locale.ROOT)){
   case "GRAM","G","GRAMA","GRAMAS" -> new Unit("MASS",BigDecimal.ONE);
   case "KILOGRAM","KG","QUILOGRAMA","QUILOGRAMAS" -> new Unit("MASS",BigDecimal.valueOf(1000));
   case "MILLILITER","ML","MILILITRO","MILILITROS" -> new Unit("VOLUME",BigDecimal.ONE);
   case "LITER","L","LITRO","LITROS" -> new Unit("VOLUME",BigDecimal.valueOf(1000));
   case "UNIT","UN","UNIDADE","UNIDADES" -> new Unit("COUNT",BigDecimal.ONE);
   case "DOZEN","DUZIA","DÚZIA" -> new Unit("COUNT",BigDecimal.valueOf(12));
   case "PACKAGE","PACOTE","PACOTES" -> new Unit("PACKAGE",BigDecimal.ONE);
   default -> null;
  };
 }
 public static BigDecimal convert(int quantity,UnitOfMeasure source,String target){
  Unit from=parse(source.name()),to=parse(target);
  if(from==null || to==null || !from.dimension().equals(to.dimension())) return null;
  return BigDecimal.valueOf(quantity).multiply(from.factor()).divide(to.factor(),6,RoundingMode.HALF_UP);
 }
}
