package com.ttt.vivm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(
        @NotBlank String userName,
        @Email @NotBlank String email,
        @NotBlank String userPassword,
        @NotBlank String confirmPassword) {
}
