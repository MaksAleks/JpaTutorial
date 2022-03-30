package com.example.jpa.domain

import groovy.sql.Sql
import org.hibernate.engine.spi.PersistenceContext
import org.hibernate.engine.spi.Status
import org.hibernate.internal.SessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Query
import javax.sql.DataSource

@SpringBootTest
class LifecycleTest extends Specification {

    @Autowired
    PostRepository postRepository

    @Autowired
    EntityManagerFactory emf

    @Autowired
    EntityManager txEm

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DataSource ds

    Sql sql

    EntityManager em

    EntityTransaction txn

    PersistenceContext pc

    def setup() {
        em = emf.createEntityManager()
        pc = em.unwrap(SessionImpl).getPersistenceContext()
        txn = em.getTransaction()
        sql = new Sql(ds)
    }

    def cleanup() {
        sql.execute("delete from post")
        em.close()
    }

    def "persist"() {
        given:
        def post = Post.builder()
                .title('post')
                .slug('post')
                .build()

        when:
        txn.begin()
        em.persist(post)
        txn.commit()

        then:
        em.contains(post)
        pc.getEntry(post)?.getStatus() == Status.MANAGED
        sql.query("select * from post where id = ${post.getId()}", {
            while (it.next()) {
                it.getLong('id') == post.getId()
                it.getString('title') == post.getTitle()
                it.getString('slug') == post.getSlug()
            }
        })

    }

    def "find"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        Post post = em.find(Post, 1L)

        then:
        em.contains(post)
        pc.getEntry(post)?.getStatus() == Status.MANAGED
        post.getId() == 1L
        post.getTitle() == "post"
        post.getSlug() == "post"
    }

    def "query single result"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        Query q = em.createQuery("select post from Post post where post.id = 1", Post)

        when:
        Post post = q.getSingleResult()

        then:
        em.contains(post)
        pc.getEntry(post)?.getStatus() == Status.MANAGED
        post.getId() == 1L
        post.getTitle() == "post"
        post.getSlug() == "post"
    }

    def "query multiple results"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        Query q = em.createQuery("select p from Post p", Post)

        when:
        List<Post> posts = q.getResultList()

        then:
        posts.size() == 2
        posts.forEach {
            em.contains(it)
            pc.getEntry(it)?.getStatus() == Status.MANAGED
        }
    }

    def "native query single result"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        Query q = em.createNativeQuery("select * from post where id = :id", Post.class)
                .setParameter("id", 1)

        when:
        Post post = q.getSingleResult() as Post

        then:
        em.contains(post)
        pc.getEntry(post)?.getStatus() == Status.MANAGED
        post.getId() == 1
        post.getTitle() == 'post'
        post.getSlug() == 'post'
    }

    def "native query multiple results"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        Query q = em.createNativeQuery("select * from post", Post.class)

        when:
        List<Post> posts = q.getResultList()

        then:
        posts.size() == 2
        posts.forEach {
            em.contains(it)
            pc.getEntry(it)?.getStatus() == Status.MANAGED
        }
    }

    def "data jpa query single result"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        boolean inManagedState = txTemplate.execute({
            def txPc = txEm.unwrap(SessionImpl).getPersistenceContext()

            when:
            Post p = postRepository.selectById(1L)
            txPc.getEntry(it)?.getStatus() == Status.MANAGED
            return txEm.contains(p)
        })

        then:
        inManagedState
    }

    def "data jpa query multiple results"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")

        when:
        boolean inManagedState = txTemplate.execute({
            def txPc = txEm.unwrap(SessionImpl).getPersistenceContext()

            when:
            List<Post> posts = postRepository.selectAll()

            boolean inManagedState = true
            posts.forEach {
                inManagedState &= txEm.contains(it)
                inManagedState &= txPc.getEntry(it)?.getStatus() == Status.MANAGED

            }
            return inManagedState
        })

        then:
        inManagedState
    }

    def "data jpa native query single result"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        boolean inManagedState = txTemplate.execute({
            def txPc = txEm.unwrap(SessionImpl).getPersistenceContext()

            when:
            Post p = postRepository.selectNativeById(1L)
            txPc.getEntry(it)?.getStatus() == Status.MANAGED
            return txEm.contains(p)
        })

        then:
        inManagedState
    }

    def "data jpa native query multiple results"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")

        when:
        boolean inManagedState = txTemplate.execute({
            def txPc = txEm.unwrap(SessionImpl).getPersistenceContext()

            when:
            List<Post> posts = postRepository.selectNativeAll()

            boolean inManagedState = true
            posts.forEach {
                inManagedState &= txEm.contains(it)
                inManagedState &= txPc.getEntry(it)?.getStatus() == Status.MANAGED
            }
            return inManagedState
        })

        then:
        inManagedState
    }

    def "detach"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txn.begin()
        def post = em.find(Post.class, 1L)

        then:
        em.contains(post)

        when:
        em.detach(post)
        txn.commit()

        then:
        !em.contains(post)
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getLong('id') == 1L
                it.getString('title') == 'post'
                it.getString('slug') == 'post'
            }
        })
    }

    def "remove"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        txn.begin()
        def post = em.find(Post, 1L)
        em.remove(post)

        then:
        !em.contains(post)
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getLong('id') == 1L
                it.getString('title') == 'post'
                it.getString('slug') == 'post'
            }
        })

        when:
        txn.commit()

        then:
        sql.query("select * from post where id = 1", {
            !it.next()
        })
    }

    def "merge detached entity"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        Post p = em.find(Post, 1L)
        em.close()
        em = emf.createEntityManager()

        then:
        !em.contains(p)

        when:
        p.setTitle('updated post')
        txn = em.getTransaction()
        txn.begin()
        def mergedPost = em.merge(p)

        then:
        !em.contains(p)
        em.contains(mergedPost)
        sql.query("select * from post where id = ${p.getId()}", {
            while (it.next()) {
                it.getString('title') == 'post'
            }
        })

        when:
        txn.commit()
        em.close()

        then:
        sql.query("select * from post where id = ${p.getId()}", {
            while (it.next()) {
                it.getString('title') == 'updated post'
            }
        })
    }
}
