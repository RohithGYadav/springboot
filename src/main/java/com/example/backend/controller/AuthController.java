package com.example.backend.controller;

import com.example.backend.config.JwtUtil;
import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        System.out.println("Login attempt:");
        System.out.println("Username: " + request.getUsername());
        System.out.println("Password: " + request.getPassword());

        if ("admin".equals(request.getUsername()) && "password".equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername());
            LoginResponse loginResponse = new LoginResponse(token);

            ApiResponse<LoginResponse> response =
                    new ApiResponse<>(true, "OK", loginResponse);

            System.out.println("Login success. Token: " + token);

            return ResponseEntity.ok(response);
        }

        System.out.println("Login failed: Invalid credentials");

        ApiResponse<LoginResponse> errorResponse =
                new ApiResponse<>(false, "Invalid username or password", null);

        return ResponseEntity.badRequest().body(errorResponse);
    }

}
