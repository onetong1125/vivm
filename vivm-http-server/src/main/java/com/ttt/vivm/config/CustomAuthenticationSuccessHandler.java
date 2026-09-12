package com.ttt.vivm.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
