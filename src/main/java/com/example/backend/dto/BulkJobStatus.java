package com.example.backend.dto;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BulkJobStatus {
    public enum State { PENDING, RUNNING, COMPLETED, FAILED }

    private String jobId;
    private State state;
    private Instant startedAt;
    private Instant finishedAt;
    private int totalRows;
    private int successCount;
    private int errorCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
