package com.ttt.vivm.dao.operation.mapper;

import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface VmStartMapper {
    @Select("SELECT vm_id FROM vm WHERE vm_name = #{name}")
    Optional<Integer> findVmId(@Param("name") String name);

    @Update("UPDATE vm SET vm_status = 1, vm_vnc = #{vnc} WHERE vm_id = #{id}")
    int markRunning(@Param("id") int id, @Param("vnc") String vnc);

    @Update("""
            UPDATE img SET status = 0 WHERE img_id IN
                (SELECT image_id FROM image_occupancy WHERE holder_id = #{holder} AND mode = 'WRITE')
            """)
    void markLegacyImageLocked(@Param("holder") String holder);

    @Delete("DELETE FROM pool_admission WHERE operation_id = #{holder}")
    void releasePool(@Param("holder") String holder);
}
