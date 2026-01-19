package com.email.writer.app;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateEmail(@RequestBody EmailRequest emailRequest) {
        Map<String, String> response = new HashMap<>();

        try {
            String reply = emailGeneratorService.generateEmailReply(emailRequest);
            response.put("reply", reply);   // 🔥 Always JSON
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("reply", "Server error. Please try again.");
            return ResponseEntity.status(500).body(response);
        }
    }
}
