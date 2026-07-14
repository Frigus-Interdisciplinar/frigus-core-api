package com.frigus.coreapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @JoinColumns({
            @JoinColumn(name = "conversation_id",
                    referencedColumnName = "conversation_id",
                    nullable = false),
            @JoinColumn(name = "sender_id",
                    referencedColumnName = "user_id",
                    nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ConversationParticipant conversationParticipants;

    @Column(name = "message_type", columnDefinition = "message_type_enum not null")
    private Object messageType;

    @Column(name = "content", length = Integer.MAX_VALUE)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "related_shopping_list_product_id")
    private ShoppingListProduct relatedShoppingListProduct;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}