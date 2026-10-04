package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.commercial.*;
import com.frigus.coreapi.dto.group.*;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CommercialService {
 private final BusinessExpenseRepository expenses;
 private final ShoppingListRepository lists;
 private final GroupContextService context;
 private final GroupAccessService access;
 private final GroupService groups;
 private final GroupInvitationService invitations;
 @Value("${app.time-zone:America/Sao_Paulo}") private String timeZone="America/Sao_Paulo";
 public Group requireCommercial(UUID groupId,boolean write){
  var group=context.resolveGroup(groupId);
  if(group.getOwner().getAccountType()!=AccountType.COMMERCIAL) throw new ForbiddenException("Recurso comercial","Este grupo não é comercial");
  if(write) access.requireGroupWriteAccess(groupId);return group;
 }
 public Page<ExpenseResponseDto> list(UUID groupId,Pageable pageable){requireCommercial(groupId,false);return expenses.findByGroupId(groupId,pageable).map(this::toDto);}
 @Transactional public ExpenseResponseDto create(UUID groupId,ExpenseRequestDto dto){
  var group=requireCommercial(groupId,true);var expense=BusinessExpense.builder().group(group).createdBy(access.requireCurrentUser()).build();apply(expense,dto);return toDto(expenses.save(expense));
 }
 @Transactional public ExpenseResponseDto update(UUID groupId,UUID id,ExpenseRequestDto dto){requireCommercial(groupId,true);var expense=requireExpense(groupId,id);requireManual(expense);apply(expense,dto);return toDto(expenses.save(expense));}
 @Transactional public void delete(UUID groupId,UUID id){requireCommercial(groupId,true);var expense=requireExpense(groupId,id);requireManual(expense);expenses.delete(expense);}
 public MonthlyReportDto report(UUID groupId,YearMonth month){
  requireCommercial(groupId,false);var all=expenses.findByGroupIdAndExpenseDateBetweenOrderByExpenseDateAsc(groupId,month.atDay(1),month.atEndOfMonth());
  Map<String,BigDecimal> categories=new TreeMap<>();BigDecimal total=BigDecimal.ZERO,purchases=BigDecimal.ZERO;
  for(var expense:all){total=total.add(expense.getAmount());categories.merge(expense.getCategory(),expense.getAmount(),BigDecimal::add);if(expense.getShoppingList()!=null) purchases=purchases.add(expense.getAmount());}
  var zone=ZoneId.of(timeZone);
  long count=lists.countCompleted(groupId,month.atDay(1).atStartOfDay(zone).toInstant(),month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant());
  return new MonthlyReportDto(groupId,month.toString(),total,purchases,count,categories);
 }
 public String export(UUID groupId,YearMonth month){
  requireCommercial(groupId,false);StringBuilder csv=new StringBuilder("date,description,category,amount,supplier,shoppingListId\r\n");
  for(var expense:expenses.findByGroupIdAndExpenseDateBetweenOrderByExpenseDateAsc(groupId,month.atDay(1),month.atEndOfMonth())) csv.append(expense.getExpenseDate()).append(',').append(cell(expense.getDescription())).append(',').append(cell(expense.getCategory())).append(',').append(expense.getAmount().toPlainString()).append(',').append(cell(expense.getSupplier())).append(',').append(expense.getShoppingList()==null ? "" : expense.getShoppingList().getId()).append("\r\n");
  return csv.toString();
 }
 public List<GroupMemberResponseDto> employees(UUID groupId){requireCommercial(groupId,false);return groups.getGroupById(groupId).getMembers();}
 @Transactional public GroupInvitationResponseDto invite(UUID groupId,GroupInvitationRequestDto dto){requireCommercial(groupId,true);return invitations.create(groupId,dto);}
 @Transactional public GroupResponseDto role(UUID groupId,UUID userId,MemberRoleUpdateDto dto){requireCommercial(groupId,true);return groups.updateMemberRole(groupId,userId,dto.memberRole());}
 @Transactional public void removeEmployee(UUID groupId,UUID userId){requireCommercial(groupId,true);groups.removeMember(groupId,userId);}
 private BusinessExpense requireExpense(UUID groupId,UUID id){var expense=expenses.findById(id).orElseThrow(NotFoundException::new);if(!expense.getGroup().getId().equals(groupId)) throw new NotFoundException();return expense;}
 private void requireManual(BusinessExpense expense){if(expense.getShoppingList()!=null) throw new ConflictException("Compra concluída imutável","Este lançamento foi gerado automaticamente pela compra");}
 private void apply(BusinessExpense e,ExpenseRequestDto d){e.setDescription(d.description().trim());e.setCategory(d.category().trim());e.setAmount(d.amount());e.setExpenseDate(d.expenseDate());e.setSupplier(d.supplier());e.setUpdatedAt(Instant.now());}
 private ExpenseResponseDto toDto(BusinessExpense e){return new ExpenseResponseDto(e.getId(),e.getGroup().getId(),e.getDescription(),e.getCategory(),e.getAmount(),e.getExpenseDate(),e.getSupplier(),e.getShoppingList()==null ? null : e.getShoppingList().getId(),e.getCreatedBy().getId());}
 static String cell(String value){if(value==null) return "";if(!value.stripLeading().isEmpty() && "=+@-".indexOf(value.stripLeading().charAt(0))>=0) value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
}
