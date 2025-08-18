package com.example.backend.Service.impl;

import com.example.backend.dto.BulkJobStatus;
import com.example.backend.entity.User;
import com.example.backend.Repository.UserRepository;
import com.example.backend.Service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

    private final UserRepository userRepository;

    // In-memory job tracker (simple + good enough for week 2)
    private final Map<String, BulkJobStatus> jobs = new ConcurrentHashMap<>();

    @Override
    public String startUpload(MultipartFile file, String actor) {
        String jobId = UUID.randomUUID().toString();
        BulkJobStatus status = BulkJobStatus.builder()
                .jobId(jobId)
                .state(BulkJobStatus.State.PENDING)
                .startedAt(Instant.now())
                .build();
        jobs.put(jobId, status);

        try {
            byte[] bytes = file.getBytes(); // copy to heap (safe for async)
            processCsvAsync(bytes, actor, jobId);
        } catch (IOException e) {
            status.setState(BulkJobStatus.State.FAILED);
            status.getErrors().add("Failed to read multipart file");
        }
        return jobId;
    }

    @Override
    public BulkJobStatus getStatus(String jobId) {
        return jobs.getOrDefault(jobId, null);
    }

    @Async("bulkUploadExecutor")
    protected void processCsvAsync(byte[] fileBytes, String actor, String jobId) {
        BulkJobStatus status = jobs.get(jobId);
        if (status == null) return;

        status.setState(BulkJobStatus.State.RUNNING);

        try (Reader r = new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(r)) {

            int total = 0, ok = 0, err = 0;

            for (CSVRecord rec : parser) {
                total++;
                String name  = rec.get("name");
                String ageS  = safe(rec, "age");
                String email = safe(rec, "email");

                if (name == null || name.isBlank()) {
                    err++;
                    status.getErrors().add("Row " + rec.getRecordNumber() + ": name is required");
                    continue;
                }

                Integer age = null;
                try {
                    if (ageS != null && !ageS.isBlank()) age = Integer.parseInt(ageS);
                } catch (NumberFormatException nfe) {
                    err++;
                    status.getErrors().add("Row " + rec.getRecordNumber() + ": invalid age");
                    continue;
                }

                try {
                    User u = User.builder()
                            .name(name)
                            .age(age)
                            .email(email)
                            .isDeleted(false)
                            .createdBy(actor)
                            .updatedBy(actor)
                            .build();
                    userRepository.save(u);
                    ok++;
                } catch (DataIntegrityViolationException dive) {
                    err++;
                    status.getErrors().add("Row " + rec.getRecordNumber() + ": duplicate email or constraint");
                } catch (Exception ex) {
                    err++;
                    status.getErrors().add("Row " + rec.getRecordNumber() + ": " + ex.getMessage());
                }
            }

            status.setTotalRows(total);
            status.setSuccessCount(ok);
            status.setErrorCount(err);
            status.setState(BulkJobStatus.State.COMPLETED);
            status.setFinishedAt(Instant.now());

        } catch (Exception e) {
            status.setState(BulkJobStatus.State.FAILED);
            status.getErrors().add("Unexpected error: " + e.getMessage());
            status.setFinishedAt(Instant.now());
        }
    }

    private static String safe(CSVRecord rec, String key) {
        try { return rec.isMapped(key) ? rec.get(key) : null; }
        catch (IllegalArgumentException iae) { return null; }
    }
}
