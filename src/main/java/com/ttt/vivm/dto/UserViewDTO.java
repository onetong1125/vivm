package com.ttt.vivm.dto;

import com.ttt.vivm.model.User;
import lombok.Data;

@Data
public class UserViewDTO {
    private Integer userId;
    private String userName;
    private String email;
    private Integer role;
    private Double balance;
    private Long ossSpace;

    public UserViewDTO(User user) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.balance = user.getBalance().doubleValue();
        this.ossSpace = user.getOssSpace();
    }

}
