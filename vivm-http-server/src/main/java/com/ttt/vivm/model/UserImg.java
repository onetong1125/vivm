package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_img")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImg {
    @EmbeddedId
    private UserImgId id; // 复合主键

    @ManyToOne
    @MapsId("userId") // 关联复合主键中的 userId
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User user;

    @ManyToOne
    @MapsId("imgId") // 关联复合主键中的 imgId
    @JoinColumn(name = "img_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img img;
}
