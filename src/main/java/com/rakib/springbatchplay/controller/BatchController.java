package com.rakib.springbatchplay.controller;


import com.rakib.springbatchplay.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {
    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<?> executeBatch(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(xlsx|xlsm|xlsb|xltx|xltm)$")) {
            return ResponseEntity.badRequest().body("Invalid file type. Excel file: (.xlsx, .xlsm, .xlsb)");
        }

        try {
            Path tempFile = Files.createTempFile("excel-", "." +
                    filename.substring(filename.lastIndexOf(".") + 1));
            file.transferTo(tempFile);

            Thread.ofVirtual().start(() -> batchService.startBatchDataProcessing(tempFile.toString()));
            return ResponseEntity.accepted().body("Batch processing started");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process the file: " + e.getMessage());
        }
    }
}
