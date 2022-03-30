package com.example.jpa.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;

@Entity(name = "Post")
@Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(
                name = "slug_uq",
                columnNames = "slug"
        )
)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    @NaturalId
    private String slug;
}
