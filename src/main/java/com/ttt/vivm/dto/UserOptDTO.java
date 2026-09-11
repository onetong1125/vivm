package com.ttt.vivm.dto;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class UserOptDTO {
    private String email;
    private Integer role;
    private Double balance;
    private Integer ossSpace;

    public UserOptDTO() {
    }

    public UserOptDTO(String email, Integer role, Double balance, Integer ossSpace) {
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.ossSpace = ossSpace;
    }

}
