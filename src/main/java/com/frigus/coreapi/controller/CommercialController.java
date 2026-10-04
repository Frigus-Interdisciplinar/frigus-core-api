package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.commercial.*;
import com.frigus.coreapi.dto.group.*;
import com.frigus.coreapi.service.CommercialService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.YearMonth;
import java.util.*;
@RestController @RequestMapping("/commercial/groups/{groupId}") @RequiredArgsConstructor
public class CommercialController {
 private final CommercialService service;
 @GetMapping("/expenses") public Page<ExpenseResponseDto> expenses(@PathVariable UUID groupId,Pageable pageable){return service.list(groupId,pageable);}
 @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED) public ExpenseResponseDto create(@PathVariable UUID groupId,@Valid @RequestBody ExpenseRequestDto dto){return service.create(groupId,dto);}
 @PutMapping("/expenses/{id}") public ExpenseResponseDto update(@PathVariable UUID groupId,@PathVariable UUID id,@Valid @RequestBody ExpenseRequestDto dto){return service.update(groupId,id,dto);}
 @DeleteMapping("/expenses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID groupId,@PathVariable UUID id){service.delete(groupId,id);}
 @GetMapping("/reports/monthly") public MonthlyReportDto report(@PathVariable UUID groupId,@RequestParam String month){return service.report(groupId,month(month));}
 @GetMapping("/reports/monthly/export") public ResponseEntity<String> export(@PathVariable UUID groupId,@RequestParam String month){var parsed=month(month);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=frigus-expenses-"+parsed+".csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.export(groupId,parsed));}
 @GetMapping("/employees") public List<GroupMemberResponseDto> employees(@PathVariable UUID groupId){return service.employees(groupId);}
 @PostMapping("/employees/invitations") @ResponseStatus(HttpStatus.CREATED) public GroupInvitationResponseDto invite(@PathVariable UUID groupId,@Valid @RequestBody GroupInvitationRequestDto dto){return service.invite(groupId,dto);}
 @PatchMapping("/employees/{userId}/role") public GroupResponseDto role(@PathVariable UUID groupId,@PathVariable UUID userId,@Valid @RequestBody MemberRoleUpdateDto dto){return service.role(groupId,userId,dto);}
 @DeleteMapping("/employees/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable UUID groupId,@PathVariable UUID userId){service.removeEmployee(groupId,userId);}
 private YearMonth month(String raw){try{return YearMonth.parse(raw);}catch(java.time.format.DateTimeParseException e){throw new com.frigus.coreapi.exception.BadRequestException("Mês inválido","Use o formato AAAA-MM");}}
}
