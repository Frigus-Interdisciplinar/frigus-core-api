package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.group.AddGroupMemberRequestDto;
import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.dto.group.GroupUpdateRequestDto;
import com.frigus.coreapi.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
@Tag(name = "Groups", description = "Endpoints para gerenciamento de grupos de pessoas (famílias/empresas)")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @Operation(summary = "Listar grupos do usuário autenticado")
    public List<GroupResponseDto> listMyGroups() {
        return groupService.listMyGroups();
    }

    @GetMapping("/page")
    @Operation(summary = "Listar grupos do usuário de forma paginada")
    public Page<GroupResponseDto> listMyGroupsPage(Pageable pageable) {
        return groupService.listMyGroups(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes e membros do grupo por ID")
    public GroupResponseDto getGroupById(@PathVariable UUID id) {
        return groupService.getGroupById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar um novo grupo de pessoas")
    public GroupResponseDto createGroup(@Valid @RequestBody GroupCreateRequestDto dto) {
        return groupService.createGroup(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar informações do grupo")
    public GroupResponseDto updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupUpdateRequestDto dto) {
        return groupService.updateGroup(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir grupo (soft delete)")
    public void deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Adicionar novo membro ao grupo respeitando o limite do plano")
    public GroupResponseDto addMember(@PathVariable UUID id, @Valid @RequestBody AddGroupMemberRequestDto dto) {
        return groupService.addMember(id, dto);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover membro do grupo ou sair do grupo")
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        groupService.removeMember(id, userId);
    }
}
