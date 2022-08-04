package com.example.jpa.domain.create;

import com.example.jpa.domain.RequestJoinedStrategy;
import com.example.jpa.domain.RequestTablePerClass;
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
@Table(name = "create_request_tpc") //tpc suffix means table_per_class inheritance strategy
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestTablePerClass extends RequestTablePerClass {

    @Embedded
    CreatedObject createdObject;
}
