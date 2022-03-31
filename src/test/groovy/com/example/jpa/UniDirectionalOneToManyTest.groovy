package com.example.jpa

import com.example.jpa.domain.UPost
import com.example.jpa.domain.UPostComment
import com.example.jpa.domain.repository.UPostRepository
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.sql.DataSource

@SpringBootTest
class UniDirectionalOneToManyTest extends Specification {

    @Autowired
    UPostRepository uPostRepository

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DataSource ds

    Sql sql

    def setup() {
        sql = new Sql(ds)
    }

    def cleanup() {
        sql.execute("delete from u_post_comment")
        sql.execute("delete from u_post")
    }

    def "u one-to-many test"() {
        given:
        def comment1 = UPostComment.builder()
                .review("comment1")
                .build()
        def comment2 = UPostComment.builder()
                .review("comment2")
                .build()
        def post = UPost.builder()
                .title('post')
                .slug('post')
                .postComment(comment1)
                .postComment(comment2)
                .build()

        when:
        txTemplate.execute({
            uPostRepository.save(post)
        })

        then:
        true

        when:
        long changedCommentId = txTemplate.execute({
            def savedPost = uPostRepository.findById(1L).get()
            def postComments = savedPost.getPostComments()

            def postComment = postComments.get(0)
            postComment.setReview("updated review")

            uPostRepository.save(savedPost)
            return postComment.getId()
        })

        then:
        sql.query("select * from u_post_comment where id = ${changedCommentId}", {
            while (it.next()) {
                assert it.getString('review') == 'updated review'
            }
        })
    }
}
