package com.example.jpa.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Inheritance(
        strategy = InheritanceType.JOINED
)
@Table(name = "request")
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
public class Request {

    @Id
    @Builder.Default
    String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    Status status;

    @CreatedDate
    @Column(updatable = false)
    Instant created;

    enum Status {
        NEW,
        SENT,
        SUCCESS,
        FAILURE
    }
}
