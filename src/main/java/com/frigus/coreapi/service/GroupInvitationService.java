package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.group.*;
import com.frigus.coreapi.model.GroupInvitation;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.utils.OneTimeTokens;
import com.frigus.coreapi.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.MailException;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GroupInvitationService {
 private final GroupInvitationRepository invitations;
 private final GroupRepository groups;
 private final GroupAccessService access;
 private final GroupService groupService;
 private final UserGroupRepository members;
 private final EmailService email;
 public List<GroupInvitationResponseDto> list(UUID groupId){access.requireGroupOwnerAccess(groupId);return invitations.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::toDto).toList();}
 @Transactional public GroupInvitationResponseDto create(UUID groupId,GroupInvitationRequestDto dto){
  access.requireGroupOwnerAccess(groupId);
  var group=groups.findByIdForUpdate(groupId).orElseThrow(NotFoundException::new);
  String address=dto.email().trim().toLowerCase(Locale.ROOT);
  var existing=invitations.findByGroupIdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNull(groupId,address);
  if(existing.isPresent()){
   if(existing.get().getExpiresAt().isAfter(Instant.now())) throw new ConflictException("Convite pendente","Revogue o convite anterior antes de reenviar");
   existing.get().setRevokedAt(Instant.now());invitations.saveAndFlush(existing.get());
  }
  String raw=OneTimeTokens.create();
  var invitation=invitations.save(GroupInvitation.builder().group(group).email(address).name(dto.name()).memberRole(dto.memberRole()).tokenHash(OneTimeTokens.hash(raw)).expiresAt(Instant.now().plus(Duration.ofDays(7))).build());
  try{email.sendLink(address,"Convite para "+group.getName(),"/accept-invitation",raw);}
  catch(MailException e){throw new ServiceUnavailableException("Falha no envio do convite","Tente novamente em alguns instantes");}
  return toDto(invitation);
 }
 @Transactional public void revoke(UUID groupId,UUID id){
  access.requireGroupOwnerAccess(groupId);
  var invitation=invitations.findById(id).orElseThrow(NotFoundException::new);
  if(!invitation.getGroup().getId().equals(groupId)) throw new NotFoundException();
  if(invitation.getAcceptedAt()!=null) throw new ConflictException("Convite já aceito","Remova o membro pelo gerenciamento do grupo");
  invitation.setRevokedAt(Instant.now());invitations.save(invitation);
 }
 @Transactional public GroupResponseDto accept(AcceptInvitationRequestDto dto){
  var invitation=invitations.lockByHash(OneTimeTokens.hash(dto.token())).orElseThrow(this::invalid);
  var user=access.requireCurrentUser();
  if(!user.getEmail().equalsIgnoreCase(invitation.getEmail())) throw new ForbiddenException("Convite para outro e-mail","Entre com a conta que recebeu o convite");
  if(invitation.getAcceptedAt()!=null && members.existsByUserIdAndGroupId(user.getId(),invitation.getGroup().getId())) return groupService.getGroupById(invitation.getGroup().getId());
  if(invitation.getRevokedAt()!=null || invitation.getAcceptedAt()!=null || !invitation.getExpiresAt().isAfter(Instant.now())) throw invalid();
  var result=groupService.joinInvitedGroup(invitation.getGroup().getId(),invitation.getMemberRole());
  invitation.setAcceptedAt(Instant.now());invitations.save(invitation);return result;
 }
 private BadRequestException invalid(){return new BadRequestException("Convite inválido ou expirado","Solicite um novo convite");}
 private GroupInvitationResponseDto toDto(GroupInvitation i){return new GroupInvitationResponseDto(i.getId(),i.getGroup().getId(),i.getEmail(),i.getName(),i.getMemberRole(),i.getExpiresAt(),i.getAcceptedAt(),i.getRevokedAt());}
}
