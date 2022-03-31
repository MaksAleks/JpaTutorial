package com.example.jpa.domain;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Post")
@Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(
                name = "slug_uq",
                columnNames = "slug"
        )
)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    private String slug;

    @Builder.Default
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PostComment> postComments = new ArrayList<>();

    void addPostComment(PostComment postComment) {
        postComments.add(postComment);
        postComment.setPost(this);
    }

    void removePostComment(PostComment postComment) {
        postComments.remove(postComment);
        postComment.setPost(null);
    }
}
