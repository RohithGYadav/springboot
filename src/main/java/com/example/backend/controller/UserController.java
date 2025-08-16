package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String search
    ) {
        PageResponse<UserResponse> pr = service.list(page, size, sort, age, search);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest req, Authentication auth) {
        String actor = auth != null ? auth.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("User created", service.create(req, actor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @RequestBody UserRequest req, Authentication auth) {
        String actor = auth != null ? auth.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("User updated", service.update(id, req, actor)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> softDelete(@RequestBody IdsRequest ids, Authentication auth) {
        String actor = auth != null ? auth.getName() : "system";
        service.softDelete(ids.ids, actor);
        return ResponseEntity.ok(ApiResponse.ok("Users soft-deleted", null));
    }

    @PatchMapping("/restore")
    public ResponseEntity<ApiResponse<?>> restore(@RequestBody RestoreRequest body, Authentication auth) {
        String actor = auth != null ? auth.getName() : "system";
        service.restore(body.id, actor);
        return ResponseEntity.ok(ApiResponse.ok("User restored", null));
    }

    // helper DTOs
    public static class IdsRequest {
        public List<Long> ids;
    }

    public static class RestoreRequest {
        public Long id;
    }
}
