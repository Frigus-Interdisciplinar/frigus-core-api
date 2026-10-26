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
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;

@Service
public class StockProductService extends BaseService<StockProduct, Integer, StockProductCreateRequestDto, StockProductResponseDto, StockProductMapper, StockProductRepository> {
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final GroupAccessService groupAccessService;
    private final StockProductStatusResolver statusResolver;
    private final StockMovementService stockMovementService;

    public StockProductService(
            StockProductRepository repository,
            StockProductMapper mapper,
            StockRepository stockRepository,
            ProductRepository productRepository,
            GroupAccessService groupAccessService,
            StockProductStatusResolver statusResolver,
            StockMovementService stockMovementService) {
        super(repository, mapper);
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.groupAccessService = groupAccessService;
        this.statusResolver = statusResolver;
        this.stockMovementService = stockMovementService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockProductResponseDto> findAll(Pageable pageable, MultiValueMap<String, String> filters) {
        Integer stockId = requiredIntegerFilter(filters, "stockId");
        return findByStockId(stockId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockProductResponseDto> findByStockId(Integer stockId, Pageable pageable) {
        return findByStockId(stockId, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockProductResponseDto> findByStockId(Integer stockId, Category category,
                                                       StoragePlace storagePlace, String query, Pageable pageable) {
        Stock stock = getRequiredStock(stockId);
        groupAccessService.requireGroupAccess(stock.getGroup().getId());
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Specification<StockProduct> specification = (root, criteria, builder) -> {
            var product = root.join("product");
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("stock").get("id"), stockId));
            predicates.add(builder.isNull(root.get("deletedAt")));
            predicates.add(builder.isNull(root.get("stock").get("deletedAt")));
            if (category != null) predicates.add(builder.equal(root.get("category"), category));
            if (storagePlace != null) predicates.add(builder.equal(product.get("storagePlace"), storagePlace));
            if (normalizedQuery != null) {
                String pattern = "%" + normalizedQuery.toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(product.get("name")), pattern),
                        builder.like(builder.lower(builder.coalesce(product.get("brand"), "")), pattern),
                        builder.like(builder.lower(builder.coalesce(root.get("batch"), "")), pattern)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(specification, pageable);
        return result.map(this::mapToDto);
    }

    @Override
    protected void authorizeRead(StockProduct stockProduct) {
        if(stockProduct.getDeletedAt()!=null || stockProduct.getStock().getDeletedAt()!=null) throw new NotFoundException();
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
        groupAccessService.requireGroupWriteAccess(stock.getGroup().getId());
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Produto nÃ£o encontrado", "O produto informado nÃ£o existe"));

        if (repository.existsByProductIdAndStockIdAndExpireDateAndBatchAndDeletedAtIsNull(product.getId(), stock.getId(), dto.getExpireDate(),dto.getBatch()==null || dto.getBatch().isBlank() ? null : dto.getBatch().trim())) {
            throw new ConflictException(
                    "Produto jÃ¡ cadastrado neste estoque para a validade informada",
                    "JÃ¡ existe um item com este produto, estoque e validade");
        }

        if(product.getOwnerGroup()!=null && !product.getOwnerGroup().getId().equals(stock.getGroup().getId()))
            throw new com.frigus.coreapi.exception.ForbiddenException("Produto de outro grupo","Produto indisponÃ­vel");
        StockProduct stockProduct = mapper.toEntity(dto);
        stockProduct.setProduct(product);
        stockProduct.setStock(stock);
        stockProduct.setInitialQuantity(dto.getQuantity());
        stockProduct.setCategory(product.getCategory());
        stockProduct.setProductStatus(statusResolver.resolve(dto.getExpireDate()));
        return mapToDto(repository.save(stockProduct));
    }

    @Transactional
    public StockProductResponseDto update(Integer id, StockProductUpdateRequestDto dto) {
        StockProduct stockProduct = repository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        authorizeRead(stockProduct);
        groupAccessService.requireGroupWriteAccess(stockProduct.getStock().getGroup().getId());
        if (dto.isEmpty()) {
            throw new BadRequestException("Atualizacao vazia", "Informe ao menos um campo para atualizar");
        }

        if (dto.getName() != null || dto.getCategory() != null || dto.getStoragePlace() != null) {
            Product product = stockProduct.getProduct();
            java.util.UUID groupId = stockProduct.getStock().getGroup().getId();
            if (product.getOwnerGroup() == null || !product.getOwnerGroup().getId().equals(groupId)) {
                product = Product.builder()
                        .name(product.getName()).brand(product.getBrand()).imageUrl(product.getImageUrl())
                        .category(product.getCategory()).storagePlace(product.getStoragePlace())
                        .unitPrice(product.getUnitPrice()).unitOfMeasure(product.getUnitOfMeasure())
                        .createdAt(java.time.Instant.now()).ownerGroup(stockProduct.getStock().getGroup())
                        .build();
            }
            if (dto.getName() != null) product.setName(dto.getName().trim());
            if (dto.getCategory() != null) product.setCategory(dto.getCategory());
            if (dto.getStoragePlace() != null) product.setStoragePlace(dto.getStoragePlace());
            stockProduct.setProduct(productRepository.save(product));
            stockProduct.setCategory(product.getCategory());
        }

        if (dto.getMinimalQuantity() != null) stockProduct.setMinimalQuantity(dto.getMinimalQuantity());
        if (dto.getInitialQuantity() != null) stockProduct.setInitialQuantity(dto.getInitialQuantity());
        if (dto.getPurchaseUnitPrice() != null) stockProduct.setPurchaseUnitPrice(dto.getPurchaseUnitPrice());
        if (dto.getExpireDate() != null) stockProduct.setExpireDate(dto.getExpireDate());
        if (dto.getBatch() != null) stockProduct.setBatch(dto.getBatch().trim());
        stockProduct.setProductStatus(statusResolver.resolve(stockProduct.getExpireDate()));
        repository.save(stockProduct);

        if (dto.getQuantity() != null && !dto.getQuantity().equals(stockProduct.getQuantity())) {
            stockMovementService.create(id, com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto.builder()
                    .movementType(com.frigus.coreapi.enums.MovementType.ADJUSTMENT)
                    .quantity(dto.getQuantity())
                    .observation("Ajuste de saldo na edicao do alimento")
                    .build());
        }
        return mapToDto(repository.findById(id).orElseThrow(NotFoundException::new));
    }

    private Stock getRequiredStock(Integer stockId) {
        return stockRepository.findById(stockId).filter(stock -> stock.getDeletedAt()==null)
                .orElseThrow(() -> new NotFoundException("Estoque nÃ£o encontrado", "O estoque informado nÃ£o existe"));
    }

    private Integer requiredIntegerFilter(MultiValueMap<String, String> filters, String name) {
        String rawValue = filters.getFirst(name);
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Filtro obrigatÃ³rio ausente", "O filtro " + name + " Ã© obrigatÃ³rio");
        }
        try {
            Integer value = Integer.valueOf(rawValue);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Filtro invÃ¡lido", "O filtro " + name + " Ã© invÃ¡lido");
        }
    }

    @Override @Transactional
    public void delete(Integer id) {
        StockProduct item=repository.findByIdForUpdate(id).orElseThrow(NotFoundException::new);
        groupAccessService.requireGroupWriteAccess(item.getStock().getGroup().getId());
        item.setDeletedAt(java.time.Instant.now());repository.save(item);
    }
    @Override protected void authorizeDelete(StockProduct item) {groupAccessService.requireGroupWriteAccess(item.getStock().getGroup().getId());}
}
