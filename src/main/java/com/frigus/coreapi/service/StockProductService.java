package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import com.frigus.coreapi.dto.stockproduct.StockProductResponseDto;
import com.frigus.coreapi.dto.stockproduct.StockProductUpdateRequestDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.StockProductMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
public class StockProductService extends BaseService<StockProduct, Integer, StockProductCreateRequestDto, StockProductResponseDto, StockProductMapper, StockProductRepository> {
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final GroupAccessService groupAccessService;
    private final StockProductStatusResolver statusResolver;

    public StockProductService(
            StockProductRepository repository,
            StockProductMapper mapper,
            StockRepository stockRepository,
            ProductRepository productRepository,
            GroupAccessService groupAccessService,
            StockProductStatusResolver statusResolver) {
        super(repository, mapper);
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.groupAccessService = groupAccessService;
        this.statusResolver = statusResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockProductResponseDto> findAll(Pageable pageable, MultiValueMap<String, String> filters) {
        Integer stockId = requiredIntegerFilter(filters, "stockId");
        return findByStockId(stockId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockProductResponseDto> findByStockId(Integer stockId, Pageable pageable) {
        Stock stock = getRequiredStock(stockId);
        groupAccessService.requireGroupAccess(stock.getGroup().getId());
        return repository.findByStockId(stockId, pageable).map(this::mapToDto);
    }

    @Override
    protected void authorizeRead(StockProduct stockProduct) {
        groupAccessService.requireGroupAccess(stockProduct.getStock().getGroup().getId());
    }

    @Override
    protected boolean requiresAuthorizedDelete() {
        return true;
    }

    @Override
    protected StockProductResponseDto mapToDto(StockProduct stockProduct) {
        stockProduct.setProductStatus(statusResolver.resolve(stockProduct.getExpireDate()));
        return super.mapToDto(stockProduct);
    }

    @Transactional
    public StockProductResponseDto create(StockProductCreateRequestDto dto) {
        Stock stock = getRequiredStock(dto.getStockId());
        groupAccessService.requireGroupAccess(stock.getGroup().getId());
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado", "O produto informado não existe"));

        if (repository.existsByProductIdAndStockIdAndExpireDate(product.getId(), stock.getId(), dto.getExpireDate())) {
            throw new ConflictException(
                    "Produto já cadastrado neste estoque para a validade informada",
                    "Já existe um item com este produto, estoque e validade");
        }

        StockProduct stockProduct = mapper.toEntity(dto);
        stockProduct.setProduct(product);
        stockProduct.setStock(stock);
        stockProduct.setCategory(product.getCategory());
        stockProduct.setProductStatus(statusResolver.resolve(dto.getExpireDate()));
        return mapToDto(repository.save(stockProduct));
    }

    @Transactional
    public StockProductResponseDto update(Integer id, StockProductUpdateRequestDto dto) {
        StockProduct stockProduct = getRequiredEntity(id);
        authorizeRead(stockProduct);
        stockProduct.setMinimalQuantity(dto.getMinimalQuantity());
        stockProduct.setExpireDate(dto.getExpireDate());
        stockProduct.setProductStatus(statusResolver.resolve(dto.getExpireDate()));
        return mapToDto(repository.save(stockProduct));
    }

    private Stock getRequiredStock(Integer stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new NotFoundException("Estoque não encontrado", "O estoque informado não existe"));
    }

    private Integer requiredIntegerFilter(MultiValueMap<String, String> filters, String name) {
        String rawValue = filters.getFirst(name);
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Filtro obrigatório ausente", "O filtro " + name + " é obrigatório");
        }
        try {
            Integer value = Integer.valueOf(rawValue);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Filtro inválido", "O filtro " + name + " é inválido");
        }
    }
}
