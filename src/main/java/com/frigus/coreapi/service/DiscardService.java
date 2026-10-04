package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.discard.*;
import com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto;
import com.frigus.coreapi.enums.MovementType;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.Instant;
@Service
public class DiscardService extends BaseService<Discard,Integer,DiscardCreateRequestDto,DiscardResponseDto,DiscardMapper,DiscardRepository> {
 private final StockProductRepository stockProducts;
 private final StockMovementRepository movements;
 private final StockMovementService movementService;
 private final GroupContextService context;
 public DiscardService(DiscardRepository repository,DiscardMapper mapper,StockProductRepository stockProducts,StockMovementRepository movements,StockMovementService movementService,GroupContextService context){super(repository,mapper);this.stockProducts=stockProducts;this.movements=movements;this.movementService=movementService;this.context=context;}
 @Override @Transactional(readOnly=true) public Page<DiscardResponseDto> findAll(Pageable pageable){return repository.findByStockProductStockGroupId(context.resolveGroup(null).getId(),pageable).map(mapper::toDto);}
 @Override protected void authorizeRead(Discard d){context.resolveGroup(d.getStockProduct().getStock().getGroup().getId());}
 @Override public void delete(Integer id){throw new BadRequestException("Histórico imutável","Registre um ajuste de estoque para corrigir o saldo");}
 @Transactional public DiscardResponseDto create(DiscardCreateRequestDto dto){
  var moved=movementService.createDiscard(dto.getStockProductId(),StockMovementCreateRequestDto.builder().movementType(MovementType.OUT).quantity(dto.getQuantity()).observation(dto.getReason()).build());
  var discard=mapper.toEntity(dto);discard.setStockProduct(stockProducts.findById(dto.getStockProductId()).orElseThrow(NotFoundException::new));
  discard.setMovement(movements.findById(moved.getId()).orElseThrow(NotFoundException::new));discard.setDate(Instant.now());
  return mapper.toDto(repository.save(discard));
 }
}
