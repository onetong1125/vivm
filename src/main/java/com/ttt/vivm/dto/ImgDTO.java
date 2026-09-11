package com.ttt.vivm.dto;

import com.ttt.vivm.model.Img;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImgDTO {
    private int imgId;
    private String imgName;
    private int imgType;
    private LocalDateTime createDate;
    private int isDelete;
    private int status;

    // 构造方法
    public ImgDTO(Img img) {
        this.imgId = img.getImgId();
        this.imgName = img.getImgName();
        this.imgType = img.getImgType();
        this.createDate = img.getCreateDate();
        this.isDelete = img.getIsDelete();
        this.status = img.getStatus();
    }
}
