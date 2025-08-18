package com.example.backend.Service;

import com.example.backend.dto.BulkJobStatus;
import org.springframework.web.multipart.MultipartFile;

public interface BulkUploadService {
    String startUpload(MultipartFile file, String actor);          // returns jobId
    BulkJobStatus getStatus(String jobId);
}
