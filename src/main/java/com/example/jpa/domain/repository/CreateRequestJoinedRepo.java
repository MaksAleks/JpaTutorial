package com.example.jpa.domain.repository;

import com.example.jpa.domain.create.CreateRequestJoined;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreateRequestJoinedRepo extends JpaRepository<CreateRequestJoined, UUID> {
}
