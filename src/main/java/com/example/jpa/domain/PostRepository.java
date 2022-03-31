package com.example.jpa.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(
            nativeQuery = true,
            value = "select * from post where  id = :id"
    )
    Post selectNativeById(@Param("id") Long id);

    @Query(
            nativeQuery = true,
            value = "select * from post"
    )
    List<Post> selectNativeAll();

    @Query(
            nativeQuery = true,
            value = "select title from post where id = :id"
    )
    String selectTitleById(@Param("id") Long id);

    @Query("select p from Post p where p.id = :id")
    Post selectById(@Param("id") Long id);

    @Query("select p from Post p")
    List<Post> selectAll();
}
