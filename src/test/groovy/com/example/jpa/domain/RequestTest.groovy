package com.example.jpa.domain

import com.example.jpa.domain.create.CreateRequestJoined
import com.example.jpa.domain.create.CreateRequestTablePerClass
import com.example.jpa.domain.create.CreatedObject
import com.example.jpa.domain.repository.CreateRequestJoinedRepo
import com.example.jpa.domain.repository.CreateRequestTablePerClassRepo
import com.example.jpa.domain.repository.GetRequestJoinedRepo
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.sql.DataSource

@SpringBootTest
class RequestTest extends Specification {

    @Autowired
    CreateRequestJoinedRepo createRequestJoinedRepo

    @Autowired
    CreateRequestTablePerClassRepo createRequestTpcRepo

    @Autowired
    GetRequestJoinedRepo getRequestRepo

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DataSource ds

    Sql sql

    def setup() {
        sql = new Sql(ds)
    }

    def cleanup() {
        sql.execute("delete from create_request_joined")
        sql.execute("delete from get_request_joined")
        sql.execute("delete from request_joined")
        sql.execute("delete from create_request_tpc")
    }

    def "create request joined strategy test"() {

        given:
        def data = CreatedObject.builder()
            .value("created first")
            .build()

        def cr = CreateRequestJoined.builder()
            .createdObject(data)
            .status(RequestJoinedStrategy.Status.NEW)
            .build()


        when:
        txTemplate.executeWithoutResult(status -> {
            createRequestJoinedRepo.save(cr)
        })

        then:
        String createRqId
        sql.query("select count(*) as cnt from request_joined", {
            while (it.next()) {
                assert it.getInt("cnt") == 1
            }
        })
        sql.query("select * from request_joined", {
            while (it.next()) {
                createRqId = it.getString("id")
            }
        })
        sql.query("select * from create_request_joined", {
            while (it.next()) {
                it.getString("id") == createRqId
            }
        })
    }


    def "create request table per class test"() {

        given:
        def data = CreatedObject.builder()
            .value("created first")
            .build()

        def cr = CreateRequestTablePerClass.builder()
            .createdObject(data)
            .status(RequestTablePerClass.Status.NEW)
            .build()


        when:
        CreateRequestTablePerClass saved = txTemplate.execute(status -> {
            return createRequestTpcRepo.save(cr)
        })

        then:
        sql.query("select * from create_request_tpc", {
            while (it.next()) {
                it.getString("id") == saved.getId()
            }
        })
    }

}
