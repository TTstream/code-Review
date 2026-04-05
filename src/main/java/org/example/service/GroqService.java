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
        GroqRequest request = new GroqRequest("llama3-8b-8192", List.of(
                new GroqMessage("system", "You are an expert code reviewer. Please review the following code changes."),
                new GroqMessage("user", prompt)
        ));

        return this.webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON) // ✅ 필수: JSON 형식임을 명시
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
                    // ✅ 400 에러 발생 시 Groq가 뱉어내는 상세 에러 메시지를 출력합니다.
                    System.err.println("Groq API Error Status: " + ex.getStatusCode());
                    System.err.println("Groq API Error Body: " + ex.getResponseBodyAsString());
                    return Mono.error(ex);
                });
    }
}