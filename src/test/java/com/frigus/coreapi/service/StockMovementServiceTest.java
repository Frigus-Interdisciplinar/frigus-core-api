package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto;
import com.frigus.coreapi.dto.stockmovement.StockMovementResponseDto;
import com.frigus.coreapi.enums.MovementType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.mapper.StockMovementMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockMovement;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.StockMovementRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {
    @Mock private StockMovementRepository movementRepository;
    @Mock private StockProductRepository stockProductRepository;
    @Mock private StockMovementMapper movementMapper;
    @Mock private GroupAccessService groupAccessService;

    @Test
    void shouldIncreaseBalanceAndPersistAuditData() {
        StockProduct stockProduct = StockProduct.builder()
                .id(9)
                .stock(Stock.builder().id(3).group(Group.builder().id(UUID.randomUUID()).build()).build())
                .quantity(5)
                .build();
        StockMovementCreateRequestDto request = StockMovementCreateRequestDto.builder()
                .movementType(MovementType.IN)
                .quantity(3)
                .build();
        StockMovement movement = StockMovement.builder().build();
        StockMovementResponseDto response = StockMovementResponseDto.builder().balanceAfter(8).build();
        User user = User.builder().build();
        StockMovementService service = new StockMovementService(
                movementRepository, movementMapper, stockProductRepository, groupAccessService);

        when(stockProductRepository.findByIdForUpdate(9)).thenReturn(Optional.of(stockProduct));
        when(groupAccessService.requireCurrentUser()).thenReturn(user);
        when(movementMapper.toEntity(request)).thenReturn(movement);
        when(movementRepository.save(movement)).thenReturn(movement);
        when(movementMapper.toDto(movement)).thenReturn(response);

        StockMovementResponseDto result = service.create(9, request);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(result.getBalanceAfter()).isEqualTo(8);
        assertThat(stockProduct.getQuantity()).isEqualTo(8);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(8);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        verify(stockProductRepository).save(stockProduct);
    }

    @Test
    void shouldRejectOutputThatWouldMakeBalanceNegative() {
        StockProduct stockProduct = StockProduct.builder()
                .id(9)
                .stock(Stock.builder().id(3).group(Group.builder().id(UUID.randomUUID()).build()).build())
                .quantity(2)
                .build();
        StockMovementCreateRequestDto request = StockMovementCreateRequestDto.builder()
                .movementType(MovementType.OUT)
                .quantity(3)
                .build();
        StockMovementService service = new StockMovementService(
                movementRepository, movementMapper, stockProductRepository, groupAccessService);

        when(stockProductRepository.findByIdForUpdate(9)).thenReturn(Optional.of(stockProduct));

        assertThatThrownBy(() -> service.create(9, request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldAllowAdjustmentToZero() {
        StockProduct stockProduct = StockProduct.builder()
                .id(9)
                .stock(Stock.builder().id(3).group(Group.builder().id(UUID.randomUUID()).build()).build())
                .quantity(5)
                .build();
        StockMovementCreateRequestDto request = StockMovementCreateRequestDto.builder()
                .movementType(MovementType.ADJUSTMENT)
                .quantity(0)
                .build();
        StockMovement movement = StockMovement.builder().build();
        StockMovementService service = new StockMovementService(
                movementRepository, movementMapper, stockProductRepository, groupAccessService);

        when(stockProductRepository.findByIdForUpdate(9)).thenReturn(Optional.of(stockProduct));
        when(groupAccessService.requireCurrentUser()).thenReturn(User.builder().build());
        when(movementMapper.toEntity(request)).thenReturn(movement);
        when(movementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(movementMapper.toDto(movement)).thenReturn(StockMovementResponseDto.builder().balanceAfter(0).build());

        service.create(9, request);

        assertThat(stockProduct.getQuantity()).isZero();
    }
}
