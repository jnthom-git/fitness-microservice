package com.fitness.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", question)
                        })
                }
        );

        return webClient.post()
                .uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(
                Retry.backoff(3, Duration.ofSeconds(2))          // retry up to 3 times, starting at 2s
                        .filter(throwable ->                          // only retry on 503 — don't retry on 400/401/etc
                                throwable instanceof WebClientResponseException wcre
                                        && wcre.getStatusCode().value() == 503)
                        .doBeforeRetry(signal ->
                                log.warn("Gemini 503, retrying (attempt {})", signal.totalRetries() + 1))
        );
    }
}