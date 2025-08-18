package com.example.backend.Service.impl;

import com.example.backend.dto.*;
import com.example.backend.entity.User;
import com.example.backend.Repository.UserRepository;
import com.example.backend.service.UserService;
import com.example.backend.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    @Override
    @Cacheable(
            cacheNames = "users",
            key = "T(java.lang.String).format('p:%s:%s:%s:%s:%s', #page, #size, #sort, #age, #search)"
    )
    public PageResponse<UserResponse> list(int page, int size, String sort, Integer age, String search) {
        String[] parts = (sort == null || sort.isBlank()) ? new String[]{"name","asc"} : sort.split(",");
        String sortBy = parts[0];
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        Specification<User> spec = Specification
                .where(UserSpecification.base(false))
                .and(UserSpecification.ageEquals(age))
                .and(UserSpecification.search(search));

        Page<User> p = repo.findAll(spec, pageable);

        List<UserResponse> content = p.stream()
                .map(this::toResp)
                .toList();

        return PageResponse.<UserResponse>builder()
                .content(content)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public UserResponse create(UserRequest req, String actor) {
        User u = User.builder()
                .name(req.getName())
                .age(req.getAge())
                .email(req.getEmail())
                .isDeleted(false)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        u = repo.save(u);
        return toResp(u);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public UserResponse update(Long id, UserRequest req, String actor) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (req.getName() != null) u.setName(req.getName());
        if (req.getAge() != null) u.setAge(req.getAge());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        u.setUpdatedBy(actor);
        return toResp(repo.save(u));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void softDelete(List<Long> ids, String actor) {
        ids.forEach(id -> {
            repo.findById(id).ifPresent(u -> {
                u.setIsDeleted(true);
                u.setUpdatedBy(actor);
                repo.save(u);
            });
        });
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void restore(Long id, String actor) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setIsDeleted(false);
        u.setUpdatedBy(actor);
        repo.save(u);
    }

    private UserResponse toResp(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .age(u.getAge())
                .email(u.getEmail())
                .isDeleted(u.getIsDeleted())
                .build();
    }
}
