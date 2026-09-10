package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.DiscardRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DiscardService extends BaseService<Discard, Integer, DiscardCreateRequestDto, DiscardResponseDto, DiscardMapper, DiscardRepository> {

    private final StockProductRepository stockProductRepository;

    public DiscardService(
            DiscardRepository repository,
            DiscardMapper mapper,
            StockProductRepository stockProductRepository) {
        super(repository, mapper);
        this.stockProductRepository = stockProductRepository;
    }

    @Transactional
    public DiscardResponseDto create(DiscardCreateRequestDto dto) {
        StockProduct stockProduct = stockProductRepository.findById(dto.getStockProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Produto do estoque não encontrado",
                        "Nenhum produto do estoque foi encontrado com o ID informado"));

        Discard discard = mapper.toEntity(dto);
        discard.setStockProduct(stockProduct);
        discard.setDate(Instant.now());

        return mapper.toDto(repository.save(discard));
    }

}
