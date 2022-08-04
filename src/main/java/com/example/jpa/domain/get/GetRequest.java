package com.example.jpa.domain.get;

import com.example.jpa.domain.Request;
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
@Table(name = "get_request")
public class GetRequest extends Request {

    @Embedded
    CreatedObject createdObject;
}
