package com.ttt.vivm.controller;

import com.ttt.vivm.dto.UserViewDTO;
import com.ttt.vivm.model.User;
import com.ttt.vivm.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @GetMapping("/me")
    public UserViewDTO currentUser(@AuthenticationPrincipal User currentUser) {
        return new UserViewDTO(currentUser);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        if (!user.getUserPassword().equals(user.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "两次输入的密码不一致"));
        }

        Map<String, String> errors = userService.registerUser(user);
        if (errors != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record CsrfTokenResponse(String headerName, String parameterName, String token) {
    }
}
