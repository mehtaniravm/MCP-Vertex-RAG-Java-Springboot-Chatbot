package com.company.chatbot.config;

import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

/**
 * Vertex AI configuration.
 * Uses Application Default Credentials (ADC) automatically:
 *   - Cloud Run: attached service account (no key file needed)
 *   - Local dev: gcloud auth application-default login
 */
@Slf4j
@Configuration
public class VertexAiConfig {

    @Value("${gcp.region:us-central1}")
    private String region;

    @Bean
    public PredictionServiceClient predictionServiceClient() throws IOException {
        String endpoint = String.format("%s-aiplatform.googleapis.com:443", region);
        log.info("Connecting to Vertex AI endpoint: {}", endpoint);
        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
            .setEndpoint(endpoint)
            .build();
        return PredictionServiceClient.create(settings);
    }
}
