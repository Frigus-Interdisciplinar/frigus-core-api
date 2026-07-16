package com.frigus.coreapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ConversationParticipantId implements Serializable {
    private static final long serialVersionUID = -1670224644365072401L;
    @NotNull
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;


}