package com.frigus.coreapi.service;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GroupContextService {
 private final GroupAccessService access;
 private final GroupRepository groups;
 private final StockRepository stocks;
 public Group resolveGroup(UUID groupId) {
  if(groupId==null) {
   var mine=groups.findGroupsByUserId(access.requireCurrentUser().getId());
   if(mine.isEmpty()) throw new NotFoundException("Nenhum grupo ativo","Crie ou entre em um grupo");
   if(mine.size()!=1) throw new ConflictException("Grupo ambíguo","Informe o groupId");
   groupId=mine.get(0).getId();
  }
  access.requireGroupAccess(groupId);
  return groups.findByIdAndDeletedAtIsNull(groupId).orElseThrow(NotFoundException::new);
 }
 public Stock requireStock(Integer stockId,boolean write) {
  Stock stock=stocks.findByIdAndDeletedAtIsNull(stockId).orElseThrow(NotFoundException::new);
  resolveGroup(stock.getGroup().getId());
  if(write) access.requireGroupWriteAccess(stock.getGroup().getId());
  return stock;
 }
 public void requireProductForGroup(Product product,UUID groupId) {
  if(product.getOwnerGroup()!=null && !product.getOwnerGroup().getId().equals(groupId))
   throw new ForbiddenException("Produto de outro grupo","Este produto não está disponível neste grupo");
 }
}
