package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.stock.*;
import com.frigus.coreapi.service.*;
import com.frigus.coreapi.repository.StockRepository;
import com.frigus.coreapi.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/stocks") @RequiredArgsConstructor
public class StockSummaryController {
 private final StockSummaryService summary;
 private final GroupContextService context;
 private final StockRepository stocks;
 private final StockMapper mapper;
 public record ActiveContext(UUID groupId,List<StockResponseDto> stocks){}
 @GetMapping("/my-summary") public StockSummaryDto get(@RequestParam(required=false) UUID groupId){return summary.get(groupId);}
 @GetMapping("/active-context") public ActiveContext context(@RequestParam(required=false) UUID groupId){
  var group=context.resolveGroup(groupId);return new ActiveContext(group.getId(),stocks.findByGroupIdAndDeletedAtIsNull(group.getId()).stream().map(mapper::toDto).toList());
 }
}
