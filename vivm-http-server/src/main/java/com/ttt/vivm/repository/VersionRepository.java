package com.ttt.vivm.repository;

import com.ttt.vivm.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionRepository extends JpaRepository<Version, Integer> {
    boolean existsByName(String name);
    List<Version> findByNameContaining(String name);
    List<Version> findByDescriptionContaining(String description);
}
