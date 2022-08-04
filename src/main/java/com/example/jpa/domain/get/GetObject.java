package com.example.jpa.domain.get;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class GetObject {

    @Builder.Default
    @Column(name = "object_id")
    String id = UUID.randomUUID().toString();

    @Column(name = "object_value")
    String value;
}
