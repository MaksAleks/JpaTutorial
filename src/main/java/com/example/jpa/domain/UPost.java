package com.example.jpa.domain;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "UPost")
@Table(
        name = "u_post",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "u_slug_uq",
                        columnNames = "slug"
                )
        }
)
public class UPost {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    private String slug;

    @Singular
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "u_post_id", nullable = false, updatable = false)
    List<UPostComment> postComments;
}
