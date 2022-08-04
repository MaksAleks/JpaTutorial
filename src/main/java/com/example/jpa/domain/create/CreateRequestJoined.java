package com.example.jpa.domain.create;

import com.example.jpa.domain.RequestJoinedStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "create_request_joined")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestJoined extends RequestJoinedStrategy {

    @Embedded
    CreatedObject createdObject;
}
