package org.example.service;

import org.example.dto.GroqMessage;
import org.example.dto.GroqRequest;
import org.example.dto.GroqResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GroqService {

    private final WebClient webClient;

    @Value("${groq.api.key}")
    private String apiKey;

    public GroqService(WebClient.Builder webClientBuilder) {
        // Groq API (OpenAI 호환)
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1").build();
    }

    public Mono<String> generateCodeReview(String prompt) {
        // ✅ 모델명을 정확하게 "llama-3.3-70b-versatile" 로 입력합니다.
        GroqRequest request = new GroqRequest("llama-3.3-70b-versatile", List.of(
                new GroqMessage("system", "You are an expert senior software engineer. Please review the following code changes. 설명과 피드백은 반드시 자연스러운 한국어로 작성해 주세요. 또한 개선이 필요한 부분이 있다면 수정된 코드를 마크다운(Markdown) 코드 블록으로 명확하게 함께 제시해 주세요."),
                new GroqMessage("user", prompt)
        ));

        return this.webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    return "No response from Groq API.";
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    System.err.println("Groq API Error Status: " + ex.getStatusCode());
                    System.err.println("Groq API Error Body: " + ex.getResponseBodyAsString());
                    return Mono.error(ex);
                });
    }
}