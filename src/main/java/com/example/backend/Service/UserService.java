package com.example.backend.service;

import com.example.backend.dto.*;

import java.util.List;

public interface UserService {

    // Return type changed to PageResponse<UserResponse>
    PageResponse<UserResponse> list(int page, int size, String sort, Integer age, String search);

    UserResponse create(UserRequest req, String actor);

    UserResponse update(Long id, UserRequest req, String actor);

    void softDelete(List<Long> ids, String actor);

    void restore(Long id, String actor);
}
