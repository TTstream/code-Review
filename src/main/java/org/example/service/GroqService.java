package org.example.service;

import org.example.dto.GroqMessage;
import org.example.dto.GroqRequest;
import org.example.dto.GroqResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
        // Groq에서 가장 빠르게 응답하는 오픈소스 Llama 3 8B 모델을 지정합니다. (llama3-70b-8192 등도 가능)
        GroqRequest request = new GroqRequest("llama3-8b-8192", List.of(
                new GroqMessage("system", "You are an expert code reviewer. Please review the following code changes."),
                new GroqMessage("user", prompt)
        ));

        return this.webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    return "No response from Groq API.";
                });
    }
}
