package com.ttt.vivm.dao.image.mapper;

import com.ttt.vivm.dao.image.record.ImageRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ImageMapper {
    String COLUMNS = """
            SELECT i.img_id AS id, i.img_name AS name, i.img_size AS size,
                   i.back_img_path AS backingImagePath, i.vm_img_path AS imagePath,
                   i.img_type AS imageType, i.is_delete AS deleted,
                   i.create_date AS createDate, i.delete_date AS deleteDate,
                   i.os_type AS osType, i.description, i.tag, i.status AS legacyStatus,
                   (SELECT MIN(ip.pool_id) FROM img_pool ip WHERE ip.img_id = i.img_id) AS poolId,
                   (SELECT MIN(p.name) FROM img_pool ip JOIN pool p ON p.id = ip.pool_id
                    WHERE ip.img_id = i.img_id) AS poolName,
                   (SELECT COUNT(*) FROM img_pool ip WHERE ip.img_id = i.img_id) AS poolCount,
                   (SELECT MIN(ic.img_id) FROM img_child ic WHERE ic.img_child_id = i.img_id) AS parentImageId,
                   (SELECT COUNT(*) FROM img_child ic WHERE ic.img_child_id = i.img_id) AS parentCount,
                   (SELECT MIN(ui.user_id) FROM user_img ui WHERE ui.img_id = i.img_id) AS linkedUserId,
                   (SELECT COUNT(*) FROM user_img ui WHERE ui.img_id = i.img_id) AS linkedUserCount
            FROM img i
            """;

    @Select(COLUMNS + """
            WHERE i.is_delete = 0
              AND (#{search} IS NULL OR i.img_name LIKE CONCAT('%', #{search}, '%'))
              AND (i.img_type = 0 OR #{admin} = true
                   OR EXISTS (SELECT 1 FROM user_img visible_user
                              WHERE visible_user.img_id = i.img_id AND visible_user.user_id = #{actorId}))
            ORDER BY i.img_id
            """)
    List<ImageRecord> selectActiveVisible(@Param("actorId") int actorId,
                                          @Param("admin") boolean admin,
                                          @Param("search") String search);

    @Select(COLUMNS + """
            WHERE i.img_id = #{imageId} AND i.is_delete = 0
              AND (i.img_type = 0 OR #{admin} = true
                   OR EXISTS (SELECT 1 FROM user_img visible_user
                              WHERE visible_user.img_id = i.img_id AND visible_user.user_id = #{actorId}))
            """)
    ImageRecord selectActiveVisibleById(@Param("actorId") int actorId,
                                        @Param("admin") boolean admin,
                                        @Param("imageId") int imageId);

    @Select(COLUMNS + "WHERE i.img_id = #{imageId}")
    ImageRecord selectById(@Param("imageId") int imageId);
}
