package com.example.jpa.domain

import groovy.sql.Sql
import org.hibernate.engine.spi.PersistenceContext
import org.hibernate.internal.SessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.NoResultException
import javax.persistence.Query
import javax.sql.DataSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors

@SpringBootTest
class FirstLevelCacheTest extends Specification {

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

    Executor pool = Executors.newFixedThreadPool(1)

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

    def "find: cache hit for update"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        // warm up EM cache
        Post post = em.find(Post, 1L)
        // change data in database
        sql.executeUpdate("update post set title='updated' where id = 1")
        Post cachedPost = em.find(Post, 1L)

        then: "cache hit"
        // identical references
        post === cachedPost

        when: "clear EM => all entities are removed from EM"
        em.clear()
        Post otherPost = em.find(Post, 1L)

        then: "cache miss"
        otherPost.getTitle() == 'updated'
    }

    def "find: cache hit on delete"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        // warm up EM cache
        Post post = em.find(Post, 1L)
        // delete data from database
        sql.execute("delete from post where id = 1")
        Post cachedPost = em.find(Post, 1L)

        then: "cache hit"
        // identical references
        post === cachedPost
        sql.query("select * from post where id = 1", {
            assert !it.next()
        })
    }

    def "can warm up with JPQL"() {
        given: "warming up cache using query with JPQL syntax"
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up
        em.createQuery("select p from Post p").getResultList()
        // change data in db
        sql.executeUpdate("update post set title = 'updated' where id = 1")

        when:
        def post = em.find(Post, 1L)

        then: "cache hit"
        post.getTitle() == 'post'
    }

    def "can warm up cache with native query with specifying entity type"() {
        given: "trying to warm up cache with native query"
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up
        em.createNativeQuery(
                "select * from post",
                Post // entity type is specified
        ).getResultList()
        // change data in db
        sql.executeUpdate("update post set title = 'updated' where id = 1")

        when:
        def post = em.find(Post, 1L)

        then: "cache hit"
        post.getTitle() == 'post'
    }

    def "cannot warm up with native query without specifying entity type"() {
        given: "trying to warm up cache with native query"
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        em.createNativeQuery("select * from post") // entity type is not specified
                .getResultList()
        // change data in db
        sql.executeUpdate("update post set title = 'updated' where id = 1")

        when:
        def post = em.find(Post, 1L)

        then: "cache miss"
        post.getTitle() == 'updated'
    }

    def "JPQL: use cache single result for update"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up EM cache
        em.find(Post, 1L)
        // update data in database
        sql.executeUpdate("update post set title='updated post' where id = 1")

        when: "query for post"
        Query query = em.createQuery("select p from Post p where p.id = 1", Post)
        Post post = query.getSingleResult()

        then: "get entity from cache although the database contains already updated entity"
        post.getTitle() == 'post'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated post'
            }
        })
    }

    def "JPQL: phantom read single result for delete"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up EM cache
        em.find(Post, 1L)
        // delete data from db
        sql.executeUpdate("delete from post where id = 1")

        when: "query for post"
        Query query = em.createQuery("select p from Post p where p.id = 1", Post)
        query.getSingleResult()

        then: "nothing was found"
        thrown NoResultException
    }

    def "JPQL: use cache multiple results for update"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // change data in database
        sql.executeUpdate("update post set title='updated 1' where id = 1")
        sql.executeUpdate("update post set title='updated 2' where id = 2")

        when:
        Map<List, Post> posts = em.createQuery("select p from Post p", Post)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        posts.get(1L).getTitle() == 'post1'
        posts.get(2L).getTitle() == 'post2'
        sql.query("select * from post", {
            while (it.next()) {
                switch (it.getInt('id')) {
                    case 1L: assert it.getString('title') == 'updated 1'
                        break
                    case 2L: assert it.getString('title') == 'updated 2'
                        break
                }
            }
        })
    }

    def "JPQL: phantom read multiple results for delete"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // remove data from database
        sql.execute("delete from post where id = 1")

        when:
        List<Post> posts = em.createQuery("select p from Post p", Post)
                .getResultList()

        then:
        posts.size() == 1
        with(posts.get(0)) {
            getId() == 2L
            getTitle() == 'post2'
        }
        sql.query("select * from post where id = 1", {
            assert !it.next()
        })
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated 2'
            }
        })
    }

    def "JPQL: phantom read multiple result for insert"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // add data to database
        sql.executeInsert("insert into post values (3, 'post3', 'post3')")
        // change data in database
        sql.execute("update post set title = 'updated' where id = 2")

        when:
        Map<List, Post> posts = em.createQuery("select p from Post p", Post)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        posts.size() == 3
        posts.get(1L).getTitle() == 'post1'
        posts.get(2L).getTitle() == 'post2' // read from cache
        posts.get(3L).getTitle() == 'post3' // phantom read
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated'
            }
        })
    }
    def "Native Query: use cache single result for update"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up EM cache
        em.find(Post, 1L)
        // update data in database
        sql.executeUpdate("update post set title='updated post' where id = 1")

        when: "query for post"
        Query query = em.createNativeQuery("select * from post where id = 1", Post)
        Post post = query.getSingleResult() as Post

        then: "get entity from cache although the database contains already updated entity"
        post.getTitle() == 'post'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated post'
            }
        })
    }

    def "Native Query: phantom read single result for delete"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")
        // warm up EM cache
        em.find(Post, 1L)
        // delete data from db
        sql.executeUpdate("delete from post where id = 1")

        when: "query for post"
        Query query = em.createNativeQuery("select * from post where id = 1", Post)
        query.getSingleResult()

        then: "nothing was found"
        thrown NoResultException
    }

    def "Native Query: use cache multiple results for update"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // change data in database
        sql.executeUpdate("update post set title='updated 1' where id = 1")
        sql.executeUpdate("update post set title='updated 2' where id = 2")

        when:
        Query q = em.createNativeQuery("select * from post", Post.class)
        Map<List, Post> posts = q.getResultList()
                .stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        posts.get(1L).getTitle() == 'post1'
        posts.get(2L).getTitle() == 'post2'
        sql.query("select * from post", {
            while (it.next()) {
                switch (it.getInt('id')) {
                    case 1L: assert it.getString('title') == 'updated 1'
                        break
                    case 2L: assert it.getString('title') == 'updated 2'
                        break
                }
            }
        })
    }

    def "Native Query: phantom read multiple results for delete"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // remove data from database
        sql.execute("delete from post where id = 1")

        when:
        List<Post> posts = em.createNativeQuery("select * from post", Post)
                .getResultList()

        then:
        posts.size() == 1
        with(posts.get(0) as Post) {
            getId() == 2L
            getTitle() == 'post2'
        }
        sql.query("select * from post where id = 1", {
            assert !it.next()
        })
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated 2'
            }
        })
    }

    def "Native Query: phantom read multiple result for insert"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")
        // warm up EM cache
        em.createQuery("select p from Post p").getResultList()
        // add data to database
        sql.executeInsert("insert into post values (3, 'post3', 'post3')")
        // change data in database
        sql.execute("update post set title = 'updated' where id = 2")

        when:
        Map<List, Post> posts = em.createNativeQuery("select * from post", Post)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        posts.size() == 3
        posts.get(1L).getTitle() == 'post1'
        posts.get(2L).getTitle() == 'post2' // read from cache
        posts.get(3L).getTitle() == 'post3' // phantom read
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated'
            }
        })
    }

    /******************************************************
     **********     Tests for spring data jpa    **********
     ******************************************************/

    // warming up the cache

    def "warm up: findById"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        String title = txTemplate.execute({
            // it's important to use findById instead of getById
            // because getById returns only lazy reference
            postRepository.findById(1L)

            sql.executeUpdate("update post set title = 'updated' where id = 1")

            return postRepository.getById(1L).getTitle()
        })

        then:
        title == 'post'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated'
            }
        })
    }

    def "warm up: findAll"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")

        when:
        Map<Long, Post> postsById = txTemplate.execute({
            // it's important to use findById instead of getById
            // because getById returns only lazy reference
            postRepository.findAll()

            sql.executeUpdate("update post set title = 'updated 1' where id = 1")
            sql.executeUpdate("update post set title = 'updated 2' where id = 2")

            return postRepository.findAll()
        }).stream()
            .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        postsById.get(1L).getTitle() == 'post1'
        postsById.get(2L).getTitle() == 'post2'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated 1'
            }
        })
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated 2'
            }
        })
    }

    def "warm up: select by id JPQL"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        String title = txTemplate.execute({
            postRepository.selectById(1L)

            sql.executeUpdate("update post set title = 'updated' where id = 1")

            return postRepository.selectById(1L).getTitle()
        })

        then:
        title == 'post'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated'
            }
        })
    }

    def "warm up: select All JPQL"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")

        when:
        Map<Long, Post> postsById = txTemplate.execute({
            // it's important to use findById instead of getById
            // because getById returns only lazy reference
            postRepository.selectAll()

            sql.executeUpdate("update post set title = 'updated 1' where id = 1")
            sql.executeUpdate("update post set title = 'updated 2' where id = 2")

            return postRepository.selectAll()
        }).stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        postsById.get(1L).getTitle() == 'post1'
        postsById.get(2L).getTitle() == 'post2'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated 1'
            }
        })
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated 2'
            }
        })
    }

    def "warm up: select by id Native Query"() {
        given:
        sql.executeInsert("insert into post values (1, 'post', 'post')")

        when:
        String title = txTemplate.execute({
            postRepository.selectNativeById(1L)

            sql.executeUpdate("update post set title = 'updated' where id = 1")

            return postRepository.selectNativeById(1L).getTitle()
        })

        then:
        title == 'post'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated'
            }
        })
    }

    def "warm up: select All Native Query"() {
        given:
        sql.executeInsert("insert into post values (1, 'post1', 'post1')")
        sql.executeInsert("insert into post values (2, 'post2', 'post2')")

        when:
        Map<Long, Post> postsById = txTemplate.execute({
            // it's important to use findById instead of getById
            // because getById returns only lazy reference
            postRepository.selectNativeAll()

            sql.executeUpdate("update post set title = 'updated 1' where id = 1")
            sql.executeUpdate("update post set title = 'updated 2' where id = 2")

            return postRepository.selectNativeAll()
        }).stream()
                .collect(Collectors.toMap(Post::getId, it -> it))

        then:
        postsById.get(1L).getTitle() == 'post1'
        postsById.get(2L).getTitle() == 'post2'
        sql.query("select * from post where id = 1", {
            while (it.next()) {
                it.getString('title') == 'updated 1'
            }
        })
        sql.query("select * from post where id = 2", {
            while (it.next()) {
                it.getString('title') == 'updated 2'
            }
        })
    }
}
