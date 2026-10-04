package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.product.*;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GroupProductService {
 private final ProductRepository products;
 private final GroupRepository groups;
 private final GroupContextService context;
 private final GroupAccessService access;
 private final PlanLimitsResolverService limits;
 private final ProductMapper mapper;
 public List<ProductResponseDto> list(UUID groupId){context.resolveGroup(groupId);return products.findByOwnerGroupId(groupId).stream().map(mapper::toDto).toList();}
 @Transactional public ProductResponseDto create(UUID groupId,ProductCreateRequestDto dto){
  var group=requireWrite(groupId);unique(groupId,dto.getName(),null);
  var product=mapper.toEntity(dto);product.setOwnerGroup(group);product.setCreatedAt(Instant.now());return mapper.toDto(products.save(product));
 }
 @Transactional public ProductResponseDto update(UUID groupId,Integer id,ProductUpdateRequestDto dto){
  requireWrite(groupId);var product=require(groupId,id);unique(groupId,dto.getName(),id);
  product.setName(dto.getName().trim());product.setCategory(dto.getCategory());product.setStoragePlace(dto.getStoragePlace());product.setUnitPrice(dto.getUnitPrice());product.setUnitOfMeasure(dto.getUnitOfMeasure());product.setBrand(dto.getBrand());product.setImageUrl(dto.getImageUrl());
  return mapper.toDto(products.save(product));
 }
 @Transactional public void delete(UUID groupId,Integer id){
  requireWrite(groupId);var product=require(groupId,id);
  if(products.isReferenced(id)) throw new ConflictException("Produto possui histórico","Remova o item do estoque; o catálogo com vínculos será preservado");
  products.delete(product);
 }
 private Group requireWrite(UUID groupId){
  var group=context.resolveGroup(groupId);access.requireGroupWriteAccess(groupId);
  if(group.getOwner().getAccountType()!=AccountType.COMMERCIAL) throw new ForbiddenException("Catálogo próprio comercial","Este recurso exige um grupo comercial");
  if(!limits.getLimitsForUser(group.getOwner().getId()).isAllowOwnProducts()) throw new ForbiddenException("Plano não permite produtos próprios","Escolha um plano com catálogo próprio");
  return groups.findByIdForUpdate(groupId).orElseThrow(NotFoundException::new);
 }
 private Product require(UUID groupId,Integer id){var product=products.findById(id).orElseThrow(NotFoundException::new);if(product.getOwnerGroup()==null || !product.getOwnerGroup().getId().equals(groupId)) throw new NotFoundException();return product;}
 private void unique(UUID groupId,String name,Integer id){var duplicate=products.findFirstByOwnerGroupIdAndNameIgnoreCase(groupId,name.trim());if(duplicate.isPresent() && !duplicate.get().getId().equals(id)) throw new ConflictException("Nome já cadastrado","Já existe um produto com este nome no grupo");}
}
