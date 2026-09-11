package com.ttt.vivm.repository;

import com.ttt.vivm.model.Pool;
import com.ttt.vivm.model.VersionPool;
import com.ttt.vivm.model.VersionPoolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionPoolRepository extends JpaRepository<VersionPool, VersionPoolId> {
    boolean existsByVersion_IdAndPool_Id(int versionId, int poolId);
    List<VersionPool> findByPool(Pool pool);
}
