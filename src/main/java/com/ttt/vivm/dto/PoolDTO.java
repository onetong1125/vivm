package com.ttt.vivm.dto;

import com.ttt.vivm.model.Pool;
import lombok.Data;

@Data
public class PoolDTO {
    private int id;
    private String name;
    private String dir;

    public PoolDTO(Pool pool) {
        this.id = pool.getId();
        this.name = pool.getName();
        this.dir = pool.getDir();
    }
}
