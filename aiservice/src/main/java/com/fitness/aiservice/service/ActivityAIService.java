package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;

    // A description of "a string will arrive later"
    public Mono<Recommendation> generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        log.info("PROMPT SENT TO AI: {}", prompt);

        return geminiService.getAnswer(prompt)
        //The lambdas inside .doOnNext() and .map() are not run at pipeline-construction time;
        // they're stored as instructions to run later, whenever a value actually arrives.
                .doOnNext(aiResponse -> log.info("RESPONSE FROM AI: {}", aiResponse))
                .map(aiResponse -> processAiResponse(activity, aiResponse));

/*      The aiResponse name only becomes a real, populated string at the moment Gemini's HTTP
        response comes back — which happens somewhere downstream, whenever .subscribe()
        triggers the whole chain and Reactor's thread pool eventually receives the response. */

//        OLD VERSION (BLOCKING)
//        String aiResponse = geminiService.getAnswer(prompt); // execution PAUSES here
//        log.info("RESPONSE FROM AI: {}", aiResponse); // by this line, aiResponse definitely exists
//        return processAiResponse(activity, aiResponse);
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse){
        try {
            /* Used for converting data between Java Objects (POJOs) and JSON Format
             The Language Translator
             It reads raw JSON text and parses it */
            ObjectMapper mapper = new ObjectMapper();
            /* JsonNode is a generic, flexible container that lets you read, explore,
             and pull information out of a JSON string immediately, without forcing you to
             write a matching Java class first. You tell the machine to build a tree map (JsonNode) from the raw text.*/
            JsonNode rootNode = mapper.readTree(aiResponse);
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("\\n```", "")
                    .trim();

            //log.info("PARSED RESPONSE FROM AI: {}", jsonContent);

            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall: ");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace: ");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate: ");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories: ");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safetyGuidelines = extractSafetyGuidelines(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getActivityType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safetyGuidelines)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getActivityType())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting with a fitness professional"))
                .safety(Arrays.asList(
                        "Always warmup before exercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(item -> safety.add(item.asText()));
        }
        return safety.isEmpty() ?
                Collections.singletonList("Follow general safety guidelines.") :
                safety;
    }


    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if(suggestionsNode.isArray()){
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));
            });
        }
        return suggestions.isEmpty() ?
                Collections.singletonList("No suggestions found for this activity.") :
                suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()){
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, detail));
            });
        }
        return improvements.isEmpty() ?
                Collections.singletonList("No improvements provided.") :
                improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if (!analysisNode.path(key).isMissingNode()) {
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
        Analyze this fitness activity and provide detailed recommendation in the following exact JSON format:
        {
            "analysis": {
                "overall": "Overall analysis here",
                "pace": "Pace analysis here",
                "heartRate": "Heart rate analysis here",
                "caloriesBurned": "Calories analysis here"
            },
            "improvements": [
                {
                    "area": "Area name",
                    "recommendation": "Detailed recommendation"
                }
            ],
            "suggestions": [
                {
                    "workout": "Workout name",
                    "description": "Detailed workout description"
                }
            ],
            "safety": [
                "Safety point 1",
                "Safety point 2"
            ]
        }

        Analyze this activity:
        Activity Type: %s
        Duration: %d minutes
        Calories Burned: %d
        Additional Metrics: %s

        Provide a detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
        Ensure the response follows the EXACT JSON format shown above.
        """,
                activity.getActivityType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }
}
        

