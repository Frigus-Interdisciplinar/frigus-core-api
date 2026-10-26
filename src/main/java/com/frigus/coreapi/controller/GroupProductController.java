package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.product.*;
import com.frigus.coreapi.service.GroupProductService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/groups/{groupId}/products") @RequiredArgsConstructor
public class GroupProductController {
 private final GroupProductService service;
 @GetMapping public List<ProductResponseDto> list(@PathVariable UUID groupId){return service.list(groupId);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProductResponseDto create(@PathVariable UUID groupId,@Valid @RequestBody ProductCreateRequestDto dto){return service.create(groupId,dto);}
 @PutMapping("/{id}") public ProductResponseDto update(@PathVariable UUID groupId,@PathVariable Integer id,@Valid @RequestBody ProductUpdateRequestDto dto){return service.update(groupId,id,dto);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID groupId,@PathVariable Integer id){service.delete(groupId,id);}
}
