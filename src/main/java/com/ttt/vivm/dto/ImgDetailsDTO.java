package com.ttt.vivm.dto;

import com.ttt.vivm.model.Img;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImgDetailsDTO {
    private int imgId;
    private String imgName;
    private long imgSize;
    private String backImgPath;
    private String vmImgPath;
    private int imgType;
    private LocalDateTime createDate;
    private LocalDateTime deleteDate;
    private int isDelete;
    private String osType;
    private String description;
    private String tag;
    private int status;
    private String poolName;

    public ImgDetailsDTO(Img img) {
        this.imgId = img.getImgId();
        this.imgName = img.getImgName();
        this.imgSize = img.getImgSize();
        this.backImgPath = img.getBackImgPath();
        this.vmImgPath = img.getVmImgPath();
        this.imgType = img.getImgType();
        this.createDate = img.getCreateDate();
        this.deleteDate = img.getDeleteDate();
        this.isDelete = img.getIsDelete();
        this.osType = img.getOsType();
        this.description = img.getDescription();
        this.tag = img.getTag();
        this.status = img.getStatus();
        this.poolName = img.getImgPool().getPool().getName();
    }
}
