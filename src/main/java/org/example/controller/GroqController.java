package org.example.controller;

import org.example.service.GroqService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class GroqController {

    private final GroqService groqService;

    public GroqController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping
    public Mono<String> requestReview(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return Mono.just("Prompt cannot be empty");
        }
        return groqService.generateCodeReview(prompt);
    }
}
