package com.company.chatbot.service;

import com.company.chatbot.model.ChatRequest;
import com.google.cloud.aiplatform.v1beta1.*;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VertexAiService {

    @Autowired
    private PredictionServiceClient predictionServiceClient;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.region:us-central1}")
    private String region;

    @Value("${vertex.model:gemini-1.5-flash}")
    private String model;

    @Value("${vertex.temperature:0.2}")
    private float temperature;

    @Value("${vertex.max-output-tokens:1024}")
    private int maxTokens;

    public String generate(String systemPrompt, String userMessage,
                           List<ChatRequest.ChatMessage> history) {
        try {
            String endpoint = String.format(
                "projects/%s/locations/%s/publishers/google/models/%s",
                projectId, region, model);

            // Build contents list
            List<Content> contents = new ArrayList<>();

            // Add conversation history
            if (history != null) {
                for (var msg : history) {
                    contents.add(Content.newBuilder()
                        .setRole("user".equals(msg.getRole()) ? "user" : "model")
                        .addParts(Part.newBuilder().setText(msg.getText()).build())
                        .build());
                }
            }

            // Add current message with system context injected
            String fullPrompt = systemPrompt + "\n\nUSER QUESTION: " + userMessage;
            contents.add(Content.newBuilder()
                .setRole("user")
                .addParts(Part.newBuilder().setText(fullPrompt).build())
                .build());

            // Generation config
            GenerationConfig genConfig = GenerationConfig.newBuilder()
                .setTemperature(temperature)
                .setMaxOutputTokens(maxTokens)
                .build();

            GenerateContentRequest request = GenerateContentRequest.newBuilder()
                .setModel(endpoint)
                .addAllContents(contents)
                .setGenerationConfig(genConfig)
                .build();

            GenerateContentResponse response = predictionServiceClient.generateContent(request);

            if (response.getCandidatesCount() > 0) {
                Candidate candidate = response.getCandidates(0);
                if (candidate.getContent().getPartsCount() > 0) {
                    return candidate.getContent().getParts(0).getText();
                }
            }
            return "I was unable to generate a response. Please try again.";

        } catch (Exception e) {
            log.error("Vertex AI generation failed", e);
            return "An error occurred while processing your request. Please try again.";
        }
    }
}
