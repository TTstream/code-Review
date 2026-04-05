package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GithubService {

    private final WebClient webClient;

    @Value("${github.api.token}")
    private String githubToken;

    public GithubService(WebClient.Builder webClientBuilder) {
        // GitHub API 베이스 URL 설정
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    /**
     * PR의 diff URL에서 변경된 코드를 가져옵니다.
     */
    public Mono<String> getPrDiff(String diffUrl) {
        return WebClient.builder().build().get()
                .uri(diffUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                // GitHub diff를 가져오기 위한 Accept 헤더
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * 지정된 저장소와 PR 번호에 리뷰 코멘트를 등록합니다.
     */
    public Mono<Void> postCommentToPr(String repoFullName, Integer prNumber, String reviewComment) {
        String commentUrl = "/repos/" + repoFullName + "/issues/" + prNumber + "/comments";

        Map<String, String> body = Map.of("body", reviewComment);

        return this.webClient.post()
                .uri(commentUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
