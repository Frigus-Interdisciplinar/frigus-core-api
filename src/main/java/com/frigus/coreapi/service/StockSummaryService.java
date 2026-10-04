package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.stock.StockSummaryDto;
import com.frigus.coreapi.enums.*;
import com.frigus.coreapi.mapper.StockProductMapper;
import com.frigus.coreapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StockSummaryService {
 private final GroupContextService context;
 private final StockRepository stocks;
 private final StockProductRepository items;
 private final StockMovementRepository movements;
 private final ShoppingListProductRepository shopping;
 private final StockProductMapper mapper;
 @Value("${app.time-zone:America/Sao_Paulo}") private String timeZone="America/Sao_Paulo";
 @Value("${notifications.expiration.warning-days:3}") private int warningDays;
 public StockSummaryDto get(UUID groupId){return summarize(context.resolveGroup(groupId).getId());}
 StockSummaryDto summarize(UUID groupId){
  ZoneId zone=ZoneId.of(timeZone);LocalDate today=LocalDate.now(zone);
  LocalDate week=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  var products=items.findActiveByGroup(groupId);
  Map<UnitOfMeasure,Long> quantities=new EnumMap<>(UnitOfMeasure.class);
  Map<StoragePlace,Long> places=new EnumMap<>(StoragePlace.class);
  Map<UnitOfMeasure,Long> consumption=new EnumMap<>(UnitOfMeasure.class);
  BigDecimal value=BigDecimal.ZERO;
  for(var item:products){
   quantities.merge(item.getProduct().getUnitOfMeasure(),item.getQuantity().longValue(),Long::sum);
   places.merge(item.getProduct().getStoragePlace(),1L,Long::sum);
   value=value.add(item.getProduct().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
  }
  for(Object[] row:movements.consumedByUnit(groupId,week.atStartOfDay(zone).toInstant(),today.plusDays(1).atStartOfDay(zone).toInstant()))
   consumption.put((UnitOfMeasure)row[0],((Number)row[1]).longValue());
  var expiring=products.stream().filter(i -> i.getQuantity()>0 && !i.getExpireDate().isBefore(today) && !i.getExpireDate().isAfter(today.plusDays(warningDays)))
   .sorted(Comparator.comparing(com.frigus.coreapi.model.StockProduct::getExpireDate)).map(mapper::toDto).toList();
  return new StockSummaryDto(groupId,stocks.findByGroupIdAndDeletedAtIsNull(groupId).size(),products.stream().map(i -> i.getProduct().getId()).distinct().count(),products.size(),value,
   shopping.countPendingForGroup(groupId),quantities,places,consumption,week,expiring);
 }
}
