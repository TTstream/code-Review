package org.example.controller;

import org.example.service.GithubService;
import org.example.service.GroqService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class GithubWebhookController {

    private final GroqService groqService;
    private final GithubService githubService;

    public GithubWebhookController(GroqService groqService, GithubService githubService) {
        this.groqService = groqService;
        this.githubService = githubService;
    }

    @PostMapping("/github")
    public Mono<String> handleGithubWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        // 1. GitHub에서 보내준 Webhook Payload(JSON)를 받습니다.
        // Postman에서 Body를 아예 비워서 보내거나 JSON 형식이 안 맞으면 payload가 null이 될 수 있습니다.
        if (payload == null || payload.isEmpty()) {
            System.out.println("Received Empty Payload");
            return Mono.just("Received empty payload. Please send valid JSON.");
        }

        String action = (String) payload.get("action");
        System.out.println("Received GitHub Webhook Action: " + action);

        // PR이 열렸을 때 (action이 "opened"인 경우)
        if ("opened".equals(action)) {
            // payload 구조: { pull_request: { url: "...", diff_url: "...", comments_url: "...", head: { repo: { full_name: "..." } }, number: 1 } }
            @SuppressWarnings("unchecked")
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            
            if (pullRequest == null) {
                return Mono.just("Ignored. Not a pull request event.");
            }

            // diff 내용을 받을 수 있는 URL (ex. https://github.com/user/repo/pull/1.diff)
            String diffUrl = (String) pullRequest.get("diff_url");
            
            // 코멘트를 달기 위한 저장소 정보 및 PR 번호
            @SuppressWarnings("unchecked")
            Map<String, Object> head = (Map<String, Object>) pullRequest.get("head");
            if (head == null || head.get("repo") == null) {
                 return Mono.just("Ignored. Missing repo info in payload.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> repo = (Map<String, Object>) head.get("repo");
            String repoFullName = (String) repo.get("full_name"); // "user/repository" 형식
            Integer prNumber = (Integer) pullRequest.get("number");

            // 2. GitHub API를 호출하여 변경된 파일의 코드(diff)를 가져옵니다.
            return githubService.getPrDiff(diffUrl)
                    .flatMap(diffCode -> {
                        if (diffCode == null || diffCode.trim().isEmpty()) {
                            return Mono.just("Diff is empty. Nothing to review.");
                        }

                        // 3. 가져온 코드를 바탕으로 AI에게 리뷰 요청 프롬프트를 만듭니다.
                        String prompt = "You are an expert code reviewer. Please review the following code changes (diff format):\n\n" + diffCode;

                        // 4. GroqService를 호출하여 AI 리뷰 결과를 받습니다.
                        return groqService.generateCodeReview(prompt)
                                .flatMap(review -> {
                                    // 5. 리뷰 결과를 다시 GitHub PR 코멘트로 등록합니다.
                                    return githubService.postCommentToPr(repoFullName, prNumber, review)
                                            .thenReturn("AI Review Comment successfully posted!");
                                });
                    });
        }

        return Mono.just("Webhook received. (Action: " + action + ")");
    }
}
