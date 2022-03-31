package com.example.jpa.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class PostDetails {

    @Id
    Long id;

    String createdBy;

    Instant createdAt;

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    Post post;
}
