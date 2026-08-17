package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService aiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity){
        log.info("Received activity for processing: {}", activity.getId());
        log.info("Generated Recommendation: {}",
            aiService.generateRecommendation(activity)
                .flatMap(recommendation ->
                    Mono.fromCallable(() -> recommendationRepository.save(recommendation)) // Wraps the blocking save() call inside a lambda so it isn't executed immediately. This gives you a Mono<Recommendation> that .flatMap() is happy with, satisfying the type contract.
                            .subscribeOn(Schedulers.boundedElastic())
                                )
                    //.subscribe() is what actually starts the whole chain running — nothing above it executes until this call.
                    .subscribe(
                        saved -> log.info("Saved recommendation: {}", saved.getId()),
                             error -> log.error("Failed to process activity: {}", activity.getId(), error)
                    )
        );
    }
}
