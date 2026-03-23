package com.expensetracker.backend.controller;

import com.expensetracker.backend.service.MistralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private MistralService mistralService;

    @PostMapping("/extract")
    public ResponseEntity<?> extractFromImage(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, String> result = mistralService.extractExpenseFromImage(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process image: " + e.getMessage()));
        }
    }
}