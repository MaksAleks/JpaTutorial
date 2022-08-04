package com.example.jpa.domain.repository;

import com.example.jpa.domain.create.CreateRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreateRequestRepo extends JpaRepository<CreateRequest, UUID> {
}
