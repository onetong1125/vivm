package com.ttt.vivm.dao.operation.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PoolAdmissionMapper {
    record VmStartTarget(int imageId, int poolId) {}

    @Select("""
            SELECT iv.img_id AS imageId, p.id AS poolId
            FROM img_vm iv
            JOIN img_pool ip ON ip.img_id = iv.img_id
            JOIN pool p ON p.id = ip.pool_id
            WHERE iv.vm_id = #{vmId}
            FOR UPDATE
            """)
    VmStartTarget lockVmStartTarget(@Param("vmId") int vmId);

    @Select("SELECT id FROM pool WHERE id = #{poolId} FOR UPDATE")
    int lockPool(@Param("poolId") int poolId);

    @Select("SELECT operation_id FROM pool_admission WHERE pool_id = #{poolId} FOR UPDATE")
    String findActiveOperation(@Param("poolId") int poolId);

    @Insert("""
            INSERT INTO pool_admission (pool_id, operation_id, acquired_at)
            VALUES (#{poolId}, #{operationId}, #{acquiredAt})
            """)
    void reserve(@Param("poolId") int poolId, @Param("operationId") String operationId,
                 @Param("acquiredAt") LocalDateTime acquiredAt);

    @Delete("DELETE FROM pool_admission WHERE pool_id = #{poolId} AND operation_id = #{operationId}")
    void release(@Param("poolId") int poolId, @Param("operationId") String operationId);
}
