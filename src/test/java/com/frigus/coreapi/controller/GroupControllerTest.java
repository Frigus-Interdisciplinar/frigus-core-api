package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.group.AddGroupMemberRequestDto;
import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.dto.group.GroupUpdateRequestDto;
import com.frigus.coreapi.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    @Test
    @DisplayName("Deve listar grupos do usuário")
    void shouldListGroups() {
        GroupResponseDto dto = GroupResponseDto.builder().id(UUID.randomUUID()).name("Família").build();
        when(groupService.listMyGroups()).thenReturn(List.of(dto));

        List<GroupResponseDto> result = groupController.listMyGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Família");
        verify(groupService).listMyGroups();
    }

    @Test
    @DisplayName("Deve criar grupo de pessoas")
    void shouldCreateGroup() {
        GroupCreateRequestDto request = GroupCreateRequestDto.builder().name("Família").build();
        GroupResponseDto expected = GroupResponseDto.builder().id(UUID.randomUUID()).name("Família").build();

        when(groupService.createGroup(request)).thenReturn(expected);

        GroupResponseDto result = groupController.createGroup(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Família");
        verify(groupService).createGroup(request);
    }

    @Test
    @DisplayName("Deve adicionar membro ao grupo")
    void shouldAddMember() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AddGroupMemberRequestDto request = AddGroupMemberRequestDto.builder().userId(userId).build();
        GroupResponseDto expected = GroupResponseDto.builder().id(groupId).name("Família").membersCount(2).build();

        when(groupService.addMember(groupId, request)).thenReturn(expected);

        GroupResponseDto result = groupController.addMember(groupId, request);

        assertThat(result).isNotNull();
        verify(groupService).addMember(groupId, request);
    }

    @Test
    @DisplayName("Deve remover membro do grupo")
    void shouldRemoveMember() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        groupController.removeMember(groupId, userId);

        verify(groupService).removeMember(groupId, userId);
    }
}
