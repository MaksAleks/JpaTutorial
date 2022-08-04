package com.example.jpa.domain.repository;

import com.example.jpa.domain.create.CreateRequestJoined;
import com.example.jpa.domain.create.CreateRequestTablePerClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreateRequestTablePerClassRepo extends JpaRepository<CreateRequestTablePerClass, UUID> {
}
