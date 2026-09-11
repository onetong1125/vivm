package com.ttt.vivm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class UserImgId implements Serializable {
    @Column(name = "user_id")
    private int userId;

    @Column(name = "img_id")
    private int imgId;

    // Constructors

    public UserImgId() {}

    public UserImgId(int userId, int imgId) {
        this.userId = userId;
        this.imgId = imgId;
    }
}
