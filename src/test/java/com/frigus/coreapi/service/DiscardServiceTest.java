package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.discard.*;
import com.frigus.coreapi.dto.stockmovement.*;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class DiscardServiceTest {
 @Mock DiscardRepository repository;
 @Mock StockProductRepository stockProducts;
 @Mock StockMovementRepository movements;
 @Mock StockMovementService movementService;
 @Mock GroupContextService context;
 DiscardService service;
 @BeforeEach void setup(){service=new DiscardService(repository,new DiscardMapper(),stockProducts,movements,movementService,context);}
 @Test void discardLinksTheRecordedQuantityToExactlyOneOutMovement(){
  var dto=DiscardCreateRequestDto.builder().stockProductId(12).quantity(2).reason("Expired").build();
  when(movementService.createDiscard(eq(12),any())).thenReturn(StockMovementResponseDto.builder().id(7).balanceAfter(3).build());
  when(stockProducts.findById(12)).thenReturn(Optional.of(StockProduct.builder().id(12).build()));
  when(movements.findById(7)).thenReturn(Optional.of(StockMovement.builder().id(7).build()));
  when(repository.save(any())).thenAnswer(i -> {Discard d=i.getArgument(0);d.setId(3);return d;});
  var response=service.create(dto);
  assertThat(response.getQuantity()).isEqualTo(2);assertThat(response.getMovementId()).isEqualTo(7);
  ArgumentCaptor<StockMovementCreateRequestDto> movement=ArgumentCaptor.forClass(StockMovementCreateRequestDto.class);
  verify(movementService).createDiscard(eq(12),movement.capture());assertThat(movement.getValue().getObservation()).isEqualTo("Expired");
  verify(stockProducts,never()).save(any());
 }
 @Test void invalidStockDoesNotPersistDiscard(){
  when(movementService.createDiscard(eq(999),any())).thenThrow(new NotFoundException());
  assertThatThrownBy(() -> service.create(DiscardCreateRequestDto.builder().stockProductId(999).quantity(1).build())).isInstanceOf(NotFoundException.class);
  verifyNoInteractions(repository,stockProducts,movements);
 }
}
