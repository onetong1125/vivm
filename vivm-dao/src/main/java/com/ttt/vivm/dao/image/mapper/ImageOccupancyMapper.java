package com.ttt.vivm.dao.image.mapper;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ImageOccupancyMapper {
    @Select("""
            SELECT id FROM pool
            WHERE id = (SELECT pool_id FROM img_pool WHERE img_id = #{imageId})
            FOR UPDATE
            """)
    int lockPoolForImage(@Param("imageId") int imageId);

    @Select("""
            <script>
            SELECT image_id, mode FROM image_occupancy WHERE image_id IN
            <foreach collection="requested" item="entry" open="(" separator="," close=")">
                #{entry.imageId}
            </foreach>
            FOR UPDATE
            </script>
            """)
    List<ImageOccupancy> findHeld(@Param("requested") Set<ImageOccupancy> requested);

    @Insert("""
            <script>
            INSERT INTO image_occupancy (image_id, holder_id, mode, vm_id, acquired_at) VALUES
            <foreach collection="requested" item="entry" separator=",">
                (#{entry.imageId}, #{holderId}, #{entry.mode}, #{vmId}, #{acquiredAt})
            </foreach>
            </script>
            """)
    void insertGroup(@Param("requested") Set<ImageOccupancy> requested, @Param("holderId") String holderId,
                     @Param("vmId") int vmId, @Param("acquiredAt") LocalDateTime acquiredAt);

    @Delete("DELETE FROM image_occupancy WHERE holder_id = #{holderId} AND vm_id = #{vmId}")
    void deleteForVm(@Param("vmId") int vmId, @Param("holderId") String holderId);
}
