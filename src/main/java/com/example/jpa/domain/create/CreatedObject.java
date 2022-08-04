package com.example.jpa.domain.create;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CreatedObject {

    @Builder.Default
    @Column(name = "object_id")
    String id = UUID.randomUUID().toString();

    @Column(name = "object_value")
    String value;
}
