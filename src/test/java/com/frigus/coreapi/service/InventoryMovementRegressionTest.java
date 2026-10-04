package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.stockmovement.*;
import com.frigus.coreapi.enums.*;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.mapper.StockMovementMapper;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class InventoryMovementRegressionTest {
 @Mock StockMovementRepository movements;
 @Mock StockProductRepository products;
 @Mock GroupAccessService access;
 StockMovementService service;StockProduct item;UUID groupId;
 @BeforeEach void setup(){groupId=UUID.randomUUID();item=StockProduct.builder().id(1).quantity(5).stock(Stock.builder().group(Group.builder().id(groupId).build()).build()).build();service=new StockMovementService(movements,new StockMovementMapper(),products,access);when(products.findByIdForUpdate(1)).thenReturn(Optional.of(item));}
 @Test void partialConsumptionPreservesObservationAndBalance(){
  when(access.requireCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).build());
  when(movements.save(any())).thenAnswer(i -> {StockMovement m=i.getArgument(0);m.setId(8);return m;});
  var result=service.create(1,StockMovementCreateRequestDto.builder().movementType(MovementType.OUT).quantity(2).observation("Dinner").build());
  assertThat(item.getQuantity()).isEqualTo(3);assertThat(result.getBalanceAfter()).isEqualTo(3);assertThat(result.getPurpose()).isEqualTo("CONSUMPTION");assertThat(result.getObservation()).isEqualTo("Dinner");
  verify(access).requireGroupWriteAccess(groupId);verify(products,times(1)).save(item);
 }
 @Test void excessiveOutflowDoesNotWriteAnyBalanceOrMovement(){
  assertThatThrownBy(() -> service.create(1,StockMovementCreateRequestDto.builder().movementType(MovementType.OUT).quantity(6).build())).isInstanceOf(BadRequestException.class);
  assertThat(item.getQuantity()).isEqualTo(5);verify(products,never()).save(any());verifyNoInteractions(movements);
 }
 @Test void viewerCannotAdjustTheBalance(){
  doThrow(new ForbiddenException()).when(access).requireGroupWriteAccess(groupId);
  assertThatThrownBy(() -> service.create(1,StockMovementCreateRequestDto.builder().movementType(MovementType.ADJUSTMENT).quantity(0).build())).isInstanceOf(ForbiddenException.class);
  assertThat(item.getQuantity()).isEqualTo(5);verify(products,never()).save(any());verifyNoInteractions(movements);
 }
}
