package org.example.controller;

import org.example.service.GithubService;
import org.example.service.GroqService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(GithubWebhookController.class)
public class GithubWebhookControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GroqService groqService;

    @MockBean
    private GithubService githubService;

    @Test
    public void testHandleGithubWebhook_OpenedAction() {
        // Given (Mock 설정)
        String diffContent = "diff --git a/file.txt b/file.txt\n+added line";
        String aiReview = "Looks good!";

        when(githubService.getPrDiff(anyString())).thenReturn(Mono.just(diffContent));
        when(groqService.generateCodeReview(anyString())).thenReturn(Mono.just(aiReview));
        when(githubService.postCommentToPr(anyString(), anyInt(), anyString())).thenReturn(Mono.empty());

        // 가짜 GitHub Webhook Payload 생성
        Map<String, Object> repoMap = Map.of("full_name", "testuser/testrepo");
        Map<String, Object> headMap = Map.of("repo", repoMap);
        Map<String, Object> prMap = Map.of(
                "number", 1,
                "diff_url", "https://github.com/test/repo/pull/1.diff",
                "head", headMap
        );
        Map<String, Object> payload = Map.of(
                "action", "opened",
                "pull_request", prMap
        );

        // When & Then (API 호출 및 응답 검증)
        webTestClient.post()
                .uri("/api/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("AI Review Comment successfully posted!");
    }

    @Test
    public void testHandleGithubWebhook_OtherAction() {
        // Given (opened가 아닌 이벤트)
        Map<String, Object> payload = Map.of("action", "closed");

        // When & Then
        webTestClient.post()
                .uri("/api/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Webhook received. (Action: closed)");
    }
}
