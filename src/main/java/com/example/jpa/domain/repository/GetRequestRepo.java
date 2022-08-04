package com.example.jpa.domain.repository;

import com.example.jpa.domain.get.GetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GetRequestRepo extends JpaRepository<GetRequest, UUID> {
}
