package com.ttt.vivm.repository;

import com.ttt.vivm.model.Pool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PoolRepository extends JpaRepository<Pool, Integer> {
    Optional<Pool> findByName(String name);
    List<Pool> findByNameContaining(String name);
}
