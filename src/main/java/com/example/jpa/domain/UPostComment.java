package com.example.jpa.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "UPostComment")
@Table(name = "u_post_comment")
public class UPostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    String review;
}
