package com.frigus.coreapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class MessageReadId implements Serializable {
    private static final long serialVersionUID = -7181128670226898548L;
    @NotNull
    @Column(name = "message_id", nullable = false)
    private Integer messageId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;


}