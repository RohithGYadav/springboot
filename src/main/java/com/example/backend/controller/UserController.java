package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.entity.User;
import com.example.backend.Repository.UserRepository;
import com.example.backend.Service.BulkUploadService;
import com.example.backend.service.UserService;
import com.example.backend.specification.UserSpecification;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final BulkUploadService bulkUploadService;
    private final UserRepository userRepository;

    public UserController(UserService service, BulkUploadService bulkUploadService, UserRepository userRepository) {
        this.service = service;
        this.bulkUploadService = bulkUploadService;
        this.userRepository = userRepository;
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

    // ---------- New Endpoints Below ----------

    /**
     * Bulk Upload (async) - accepts CSV file, starts background job
     */
    @PostMapping(value = "/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BulkUploadResponse>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        String actor = auth != null ? auth.getName() : "system";
        String jobId = bulkUploadService.startUpload(file, actor);
        return ResponseEntity.accepted().body(ApiResponse.ok("Bulk upload started", new BulkUploadResponse(jobId)));
    }

    /**
     * Check status of a bulk upload job
     */
    @GetMapping("/bulk-upload/{jobId}/status")
    public ResponseEntity<ApiResponse<BulkJobStatus>> bulkUploadStatus(@PathVariable String jobId) {
        BulkJobStatus status = bulkUploadService.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid jobId"));
        }
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * Download users as CSV (supports filters and sort like list())
     */
    @GetMapping(value = "/download", produces = "text/csv")
    @Transactional(readOnly = true)
    public void downloadCsv(
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name,asc") String sort,
            HttpServletResponse response
    ) throws IOException {
        // Filename with timestamp
        String filename = "users_" + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // Sort
        String[] parts = (sort == null || sort.isBlank()) ? new String[]{"name", "asc"} : sort.split(",");
        Comparator<User> cmp = Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER);
        if ("age".equalsIgnoreCase(parts[0])) {
            cmp = Comparator.comparing(u -> u.getAge() == null ? -1 : u.getAge());
        } else if ("email".equalsIgnoreCase(parts[0])) {
            cmp = Comparator.comparing(u -> u.getEmail() == null ? "" : u.getEmail(), String.CASE_INSENSITIVE_ORDER);
        }
        boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("desc");
        if (desc) cmp = cmp.reversed();

        // Spec: only non-deleted by default
        var spec = org.springframework.data.jpa.domain.Specification
                .where(UserSpecification.base(false))
                .and(UserSpecification.ageEquals(age))
                .and(UserSpecification.search(search));

        var users = userRepository.findAll(spec);
        users.sort(cmp);

        try (var writer = response.getWriter();
             var csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "name", "age", "email", "isDeleted"))) {
            for (User u : users) {
                csv.printRecord(
                        u.getId(),
                        u.getName(),
                        u.getAge(),
                        u.getEmail(),
                        u.getIsDeleted()
                );
            }
            csv.flush();
        }
    }

    // ---------- Helper DTOs ----------
    public static class IdsRequest {
        public List<Long> ids;
    }

    public static class RestoreRequest {
        public Long id;
    }
}
