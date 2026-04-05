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
        System.out.println("=================================================");
        System.out.println("[1] Received GitHub Webhook Payload: " + payload);

        if (payload == null || payload.isEmpty()) {
            System.out.println("[Error] Received Empty Payload");
            return Mono.just("Received empty payload. Please send valid JSON.");
        }

        String action = (String) payload.get("action");
        System.out.println("[2] GitHub Webhook Action: " + action);

        // PR이 열렸을 때 (action이 "opened"인 경우)
        if ("opened".equals(action)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            
            if (pullRequest == null) {
                return Mono.just("Ignored. Not a pull request event.");
            }

            // diff 내용을 받을 수 있는 URL
            String diffUrl = (String) pullRequest.get("diff_url");
            System.out.println("[3] Extracted Diff URL: " + diffUrl);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> head = (Map<String, Object>) pullRequest.get("head");
            if (head == null || head.get("repo") == null) {
                 return Mono.just("Ignored. Missing repo info in payload.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> repo = (Map<String, Object>) head.get("repo");
            String repoFullName = (String) repo.get("full_name");
            Integer prNumber = (Integer) pullRequest.get("number");
            
            System.out.println("[4] Target Repo: " + repoFullName + ", PR Number: " + prNumber);

            // 2. GitHub API를 호출하여 변경된 파일의 코드(diff)를 가져옵니다.
            return githubService.getPrDiff(diffUrl)
                    .flatMap(diffCode -> {
                        System.out.println("\n[5] === Extracted Diff Code from GitHub ===");
                        System.out.println(diffCode);
                        System.out.println("===========================================\n");

                        if (diffCode == null || diffCode.trim().isEmpty()) {
                            return Mono.just("Diff is empty. Nothing to review.");
                        }

                        String prompt = "You are an expert code reviewer. Please review the following code changes (diff format):\n\n" + diffCode;

                        // 3. GroqService를 호출하여 AI 리뷰 결과를 받습니다.
                        System.out.println("[6] Requesting AI Review to Groq API...");
                        return groqService.generateCodeReview(prompt)
                                .flatMap(review -> {
                                    System.out.println("\n[7] === Received AI Review Result ===");
                                    System.out.println(review);
                                    System.out.println("======================================\n");

                                    // 4. 리뷰 결과를 다시 GitHub PR 코멘트로 등록합니다.
                                    System.out.println("[8] Posting comment to GitHub PR...");
                                    return githubService.postCommentToPr(repoFullName, prNumber, review)
                                            .thenReturn("AI Review Comment successfully posted! Check your GitHub PR.");
                                });
                    })
                    .onErrorResume(error -> {
                        // 에러 발생 시 로그 출력 및 응답
                        System.err.println("[Error] Failed during processing: " + error.getMessage());
                        error.printStackTrace();
                        return Mono.just("Error occurred: " + error.getMessage());
                    });
        }

        return Mono.just("Webhook received. (Action: " + action + ")");
    }
}
