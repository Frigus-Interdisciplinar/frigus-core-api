package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.dto.ingredient.IngredientUpdateRequestDto;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.IngredientMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.model.RecipeIngredient;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.RecipeIngredientRepository;
import com.frigus.coreapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class IngredientService extends BaseService<RecipeIngredient, Integer, IngredientRequestDto, IngredientResponseDto, IngredientMapper, RecipeIngredientRepository> {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;

    public IngredientService(
            RecipeIngredientRepository repository,
            IngredientMapper mapper,
            RecipeRepository recipeRepository,
            ProductRepository productRepository) {
        super(repository, mapper);
        this.recipeRepository = recipeRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public IngredientResponseDto create(IngredientRequestDto dto) {
        Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                .orElseThrow(() -> new NotFoundException("Receita não encontrada", "Nenhuma receita foi encontrada com o ID informado"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado", "Nenhum produto foi encontrado com o ID informado"));

        if (repository.existsByRecipeIdAndProductId(recipe.getId(), product.getId())) {
            throw new ConflictException("Ingrediente já cadastrado", "Este produto já foi adicionado como ingrediente desta receita");
        }

        String unit = (dto.getUnit() != null && !dto.getUnit().isBlank())
                ? dto.getUnit().trim()
                : (product.getUnitOfMeasure() != null ? product.getUnitOfMeasure().name() : null);

        RecipeIngredient ingredient = mapper.toEntity(dto);
        ingredient.setRecipe(recipe);
        ingredient.setProduct(product);
        ingredient.setUnit(unit);

        return mapper.toDto(repository.save(ingredient));
    }

    @Transactional
    public IngredientResponseDto update(Integer id, IngredientUpdateRequestDto dto) {
        RecipeIngredient ingredient = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingrediente não encontrado", "Nenhum ingrediente foi encontrado com o ID informado"));

        if (dto.getProductId() != null && !dto.getProductId().equals(ingredient.getProduct().getId())) {
            Product newProduct = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produto não encontrado", "Nenhum produto foi encontrado com o ID informado"));

            if (repository.existsByRecipeIdAndProductId(ingredient.getRecipe().getId(), newProduct.getId())) {
                throw new ConflictException("Ingrediente já cadastrado", "Este produto já foi adicionado como ingrediente desta receita");
            }
            ingredient.setProduct(newProduct);
        }

        if (dto.getQuantity() != null) {
            ingredient.setQuantity(dto.getQuantity());
        }

        if (dto.getUnit() != null && !dto.getUnit().isBlank()) {
            ingredient.setUnit(dto.getUnit().trim());
        }

        if (dto.getRequired() != null) {
            ingredient.setRequired(dto.getRequired());
        }

        return mapper.toDto(repository.save(ingredient));
    }

    @Transactional
    public IngredientResponseDto update(Integer id, IngredientRequestDto dto) {
        RecipeIngredient ingredient = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingrediente não encontrado", "Nenhum ingrediente foi encontrado com o ID informado"));

        boolean targetRecipeChanged = dto.getRecipeId() != null && !dto.getRecipeId().equals(ingredient.getRecipe().getId());
        boolean targetProductChanged = dto.getProductId() != null && !dto.getProductId().equals(ingredient.getProduct().getId());

        Recipe targetRecipe = ingredient.getRecipe();
        if (targetRecipeChanged) {
            targetRecipe = recipeRepository.findById(dto.getRecipeId())
                    .orElseThrow(() -> new NotFoundException("Receita não encontrada", "Nenhuma receita foi encontrada com o ID informado"));
        }

        Product targetProduct = ingredient.getProduct();
        if (targetProductChanged) {
            targetProduct = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produto não encontrado", "Nenhum produto foi encontrado com o ID informado"));
        }

        if ((targetRecipeChanged || targetProductChanged)
                && repository.existsByRecipeIdAndProductId(targetRecipe.getId(), targetProduct.getId())) {
            throw new ConflictException("Ingrediente já cadastrado", "Este produto já foi adicionado como ingrediente desta receita");
        }

        ingredient.setRecipe(targetRecipe);
        ingredient.setProduct(targetProduct);
        if (dto.getQuantity() != null) {
            ingredient.setQuantity(dto.getQuantity());
        }
        if (dto.getUnit() != null && !dto.getUnit().isBlank()) {
            ingredient.setUnit(dto.getUnit().trim());
        } else if (targetProductChanged) {
            ingredient.setUnit(targetProduct.getUnitOfMeasure() != null ? targetProduct.getUnitOfMeasure().name() : null);
        }
        if (dto.getRequired() != null) {
            ingredient.setRequired(dto.getRequired());
        }

        return mapper.toDto(repository.save(ingredient));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        RecipeIngredient ingredient = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingrediente não encontrado", "Nenhum ingrediente foi encontrado com o ID informado"));
        repository.delete(ingredient);
    }

    @Transactional(readOnly = true)
    public List<IngredientResponseDto> findByRecipeId(Integer recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new NotFoundException("Receita não encontrada", "Nenhuma receita foi encontrada com o ID informado");
        }
        return repository.findByRecipeId(recipeId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<IngredientResponseDto> findByProductId(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Produto não encontrado", "Nenhum produto foi encontrado com o ID informado");
        }
        return repository.findByProductId(productId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public void deleteByRecipeId(Integer recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new NotFoundException("Receita não encontrada", "Nenhuma receita foi encontrada com o ID informado");
        }
        repository.deleteByRecipeId(recipeId);
    }

    @Transactional
    public void replaceAll(Recipe recipe, List<IngredientRequestDto> items) {
        repository.deleteByRecipeId(recipe.getId());
        if (items == null || items.isEmpty()) {
            return;
        }
        for (IngredientRequestDto item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produto não encontrado", "Nenhum produto foi encontrado com o ID informado"));

            String unit = (item.getUnit() != null && !item.getUnit().isBlank())
                    ? item.getUnit().trim()
                    : (product.getUnitOfMeasure() != null ? product.getUnitOfMeasure().name() : null);

            RecipeIngredient ingredient = RecipeIngredient.builder()
                    .recipe(recipe)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unit(unit)
                    .required(item.getRequired() == null || item.getRequired())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            repository.save(ingredient);
        }
    }
}
