package com.frigus.coreapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_reads")
public class MessageRead {
    @EmbeddedId
    private MessageReadId id;

    @MapsId("id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message messages;

    @MapsId("id")
    @JoinColumns({
            @JoinColumn(name = "conversation_id",
                    referencedColumnName = "conversation_id",
                    nullable = false),
            @JoinColumn(name = "user_id",
                    referencedColumnName = "user_id",
                    nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ConversationParticipant conversationParticipants;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message messages1;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "read_at", nullable = false)
    private Instant readAt;


}