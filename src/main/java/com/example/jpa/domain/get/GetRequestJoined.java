package com.example.jpa.domain.get;

import com.example.jpa.domain.RequestJoinedStrategy;
import com.example.jpa.domain.create.CreatedObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "get_request_joined")
public class GetRequestJoined extends RequestJoinedStrategy {

    @Embedded
    CreatedObject createdObject;
}
