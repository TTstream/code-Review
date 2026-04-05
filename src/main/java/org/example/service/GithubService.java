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
        this.webClient = webClientBuilder.build();
    }

    // 1. diffUrl에서 변경된 코드(diff)를 문자열로 가져오는 메서드
    public Mono<String> getPrDiff(String diffUrl) {
        return webClient.get()
                .uri(diffUrl)
                // Private 저장소이거나 Rate Limit 방지를 위해 토큰을 헤더에 담아줍니다.
                .header("Authorization", "Bearer " + githubToken)
                .retrieve()
                .bodyToMono(String.class);
    }

    // 2. AI의 리뷰 결과를 GitHub PR에 코멘트로 달아주는 메서드
    public Mono<Void> postCommentToPr(String repoFullName, Integer prNumber, String review) {
        // GitHub API 코멘트 등록 URL 구조: https://api.github.com/repos/{owner}/{repo}/issues/{issue_number}/comments
        // (PR 코멘트도 issues API를 사용합니다)
        String issueCommentUrl = "https://api.github.com/repos/" + repoFullName + "/issues/" + prNumber + "/comments";

        // GitHub API는 {"body": "코멘트 내용"} 형태의 JSON을 요구합니다.
        Map<String, String> requestBody = Map.of("body", review);

        return webClient.post()
                .uri(issueCommentUrl)
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json") // GitHub REST API 권장 헤더
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
