package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.group.*;
import com.frigus.coreapi.service.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequiredArgsConstructor
public class GroupInvitationController {
 private final GroupInvitationService invitations;
 private final GroupService groups;
 @GetMapping("/groups/{groupId}/invitations") public List<GroupInvitationResponseDto> list(@PathVariable UUID groupId){return invitations.list(groupId);}
 @PostMapping("/groups/{groupId}/invitations") @ResponseStatus(HttpStatus.CREATED) public GroupInvitationResponseDto create(@PathVariable UUID groupId,@Valid @RequestBody GroupInvitationRequestDto dto){return invitations.create(groupId,dto);}
 @DeleteMapping("/groups/{groupId}/invitations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable UUID groupId,@PathVariable UUID id){invitations.revoke(groupId,id);}
 @PostMapping("/group-invitations/accept") public GroupResponseDto accept(@Valid @RequestBody AcceptInvitationRequestDto dto){return invitations.accept(dto);}
 @PatchMapping("/groups/{groupId}/members/{userId}/role") public GroupResponseDto role(@PathVariable UUID groupId,@PathVariable UUID userId,@Valid @RequestBody MemberRoleUpdateDto dto){return groups.updateMemberRole(groupId,userId,dto.memberRole());}
}
