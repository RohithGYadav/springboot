package com.example.backend.service;

import com.example.backend.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    Page<UserResponse> list(int page, int size, String sort, Integer age, String search);

    UserResponse create(UserRequest req, String actor);

    UserResponse update(Long id, UserRequest req, String actor);

    void softDelete(List<Long> ids, String actor);

    void restore(Long id, String actor);
}
