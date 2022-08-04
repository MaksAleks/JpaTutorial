package com.example.jpa.domain.repository;

import com.example.jpa.domain.get.GetRequestJoined;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GetRequestJoinedRepo extends JpaRepository<GetRequestJoined, UUID> {
}
