package com.ttt.vivm.dto;

import com.ttt.vivm.model.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VersionDTO {
    private int id;
    private String name;
    private LocalDateTime createTime;
    private String description;
    private String poolName;

    public VersionDTO(Version version) {
        this.id = version.getId();
        this.name = version.getName();
        this.createTime = version.getCreateTime();
        this.description = version.getDescription();
        this.poolName = version.getVersionPool().getPool().getName();
    }
}
