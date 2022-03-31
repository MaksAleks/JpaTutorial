package com.example.jpa

import com.example.jpa.domain.Post
import com.example.jpa.domain.PostComment
import com.example.jpa.domain.PostDetails
import com.example.jpa.domain.Tag
import com.example.jpa.domain.repository.PostCommentRepository
import com.example.jpa.domain.repository.PostDetailsRepository
import com.example.jpa.domain.repository.PostRepository
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.persistence.EntityExistsException
import javax.sql.DataSource
import java.time.Instant

@SpringBootTest
class MappingsTest extends Specification {

    @Autowired
    PostRepository postRepository

    @Autowired
    PostCommentRepository postCommentRepository

    @Autowired
    PostDetailsRepository postDetailsRepository

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DataSource ds

    Sql sql

    def setup() {
        sql = new Sql(ds)
    }

    def cleanup() {
        sql.execute("delete from post_tag")
        sql.execute("delete from tag")
        sql.execute("delete from post_details")
        sql.execute("delete from post_comment")
        sql.execute("delete from post")
    }

    def "many-to-one"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        def comment = txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def postComment = PostComment.builder()
                    .review("post comment review")
                    .post(post)
                    .build()
            return postCommentRepository.save(postComment)
        })

        then:
        sql.query("select * from post_comment where post_id=1", {
            while (it.next()) {
                it.getString('review') == 'post comment review'
            }
        })

        sql.query("select * from post where id=${comment.getPost().getId()}", {
            while (it.next()) {
                it.getString('title') == comment.getPost().getTitle()
                it.getString('slug') == comment.getPost().getSlug()
            }
        })
    }

    def "bi one-to-many add child"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def postComment = PostComment.builder()
                    .review("post comment review")
                    .post(post)
                    .build()
            post.addPostComment(postComment)
            // it doesn't necessary to explicitly save post,
            // because EM flushes changes after committing the transaction
            // but spring recommends do it explicitly in order to keep semantics consistent
            postRepository.save(post)
        })

        then:
        sql.query("select * from post_comment where post_id=1", {
            while (it.next()) {
                it.getString('review') == 'post comment review'
            }
        })
    }

    def "bi one-to-many cascade save delete"() {
        given:
        def post = Post.builder()
                .title("post")
                .slug("post")
                .build();

        def postComment = PostComment.builder()
                .review("post comment review")
                .build()

        post.addPostComment(postComment)

        when:
        long postId = txTemplate.execute({
            return postRepository.save(post).getId()
        })

        then:
        sql.query("select * from post", {
            while (it.next()) {
                it.getLong('id') == postId
                it.getString('title') == 'post'
                it.getString('slug') == 'post'
            }
        })
        sql.query("select * from post_comment where post_id = ${postId}", {
            while (it.next()) {
                it.getString('review') == 'post comment review'
            }
        })

        when:
        txTemplate.execute({
            postRepository.deleteById(postId)
        })

        then:
        sql.query("select * from post", {
            assert !it.next()
        })
        sql.query("select * from post_comment", {
            assert !it.next()
        })
    }

    def "by one-to-many orphan removal"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        sql.executeInsert("insert into post_comment values (1, 'post review', 1)")

        when:
        txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def comment = post.getPostComments().get(0)
            post.removePostComment(comment)

            postRepository.save(post)
        })

        then:
        sql.query("select * from post_comment", {
            assert !it.next()
        })
    }

    def "uni one-to-one"() {
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def postDetails = PostDetails.builder()
                .createdBy("admin")
                .createdAt(Instant.now())
                .post(post)
                .build()

            postDetailsRepository.save(postDetails)
        })

        then:
        sql.query("select * from post_details", {
            while (it.next()) {
                it.getLong('post_id') == 1
            }
        })
    }

    def "uni one-to-one shared primary key"() {
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def postDetails = PostDetails.builder()
                    .createdBy("admin")
                    .createdAt(Instant.now())
                    .post(post)
                    .build()

            postDetailsRepository.save(postDetails)
        })

        then:
        sql.query("select * from post_details", {
            while (it.next()) {
                it.getLong('post_id') == 1
            }
        })

        when:
        def postDetails = txTemplate.execute({
            return postDetailsRepository.findById(1L).get()
        })

        then:
        postDetails.getCreatedBy() == "admin"
    }

    def "one-to-one shared key error on duplicate"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txTemplate.execute({
            def post = postRepository.findById(1L).get()
            def postDetails = PostDetails.builder()
                    .createdBy("admin")
                    .createdAt(Instant.now())
                    .post(post)
                    .build()
            def duplicate = postDetails.toBuilder()
                    .createdBy("test")
                    .build()
            postDetailsRepository.save(postDetails)
            postDetailsRepository.save(duplicate)
        })

        then:
        def ex = thrown DataIntegrityViolationException
        ex.getCause() instanceof EntityExistsException
    }

    def "many-to-many persist"() {
        given:
        def tag1 = Tag.builder()
            .name("t1")
            .build()
        def tag2 = Tag.builder()
            .name("t2")
            .build()
        def tag3 = Tag.builder()
            .name("t3")
            .build()
        def post1 = Post.builder()
            .tag(tag1)
            .tag(tag2)
            .title("post1")
            .slug("post1")
            .build()
        def post2 = Post.builder()
            .tag(tag2)
            .tag(tag3)
            .title("post2")
            .slug("post2")
            .build()

        when:
        txTemplate.execute({
            postRepository.save(post1)
            postRepository.save(post2)
        })

        then:
        def tags = []
        sql.query("select * from tag", {
            while (it.next()) {
                tags.add(it.getString('name'))
            }
        })
        tags.containsAll(['t1', 't2', 't3'])
    }
}
