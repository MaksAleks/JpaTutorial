package com.example.jpa.domain

import com.example.jpa.domain.create.CreateRequest
import com.example.jpa.domain.create.CreatedObject
import com.example.jpa.domain.repository.CreateRequestRepo
import com.example.jpa.domain.repository.GetRequestRepo
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.sql.DataSource

@SpringBootTest
class RequestTest extends Specification {

    @Autowired
    CreateRequestRepo createRequestRepo

    @Autowired
    GetRequestRepo getRequestRepo

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    DataSource ds

    Sql sql

    def setup() {
        sql = new Sql(ds)
    }

    def cleanup() {
        sql.execute("delete from create_request")
        sql.execute("delete from get_request")
        sql.execute("delete from request")
    }

    def "create request test"() {

        given:
        def data = CreatedObject.builder()
            .value("created first")
            .build()

        def cr = CreateRequest.builder()
            .createdObject(data)
            .status(Request.Status.NEW)
            .build()


        when:
        txTemplate.executeWithoutResult(status -> {
            createRequestRepo.save(cr)
        })

        then:
        String createRqId
        sql.query("select count(*) as cnt from request", {
            while (it.next()) {
                assert it.getInt("cnt") == 1
            }
        })
        sql.query("select * from request", {
            while (it.next()) {
                createRqId = it.getString("id")
            }
        })
        sql.query("select * from create_request", {
            while (it.next()) {
                it.getString("id") == createRqId
            }
        })
    }

}
