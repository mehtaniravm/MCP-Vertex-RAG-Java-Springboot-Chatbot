package com.company.bizapi.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * API Key filter. Active only when app.security.api-key-enabled=true.
 * Set API_KEY_ENABLED=false env var to run in no-auth mode (internal-only Cloud Run).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.security.api-key-enabled", havingValue = "true", matchIfMissing = true)
public class ApiKeyFilter implements Filter {

    private final String validApiKey;

    @Value("${app.security.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    public ApiKeyFilter(
            @Value("${gcp.project-id}") String projectId,
            @Value("${app.security.api-key-secret-name:biz-api-key}") String secretName) throws Exception {
        log.info("API Key auth ENABLED. Fetching key from Secret Manager: {}", secretName);
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String path = SecretVersionName.of(projectId, secretName, "latest").toString();
            AccessSecretVersionResponse resp = client.accessSecretVersion(path);
            this.validApiKey = resp.getPayload().getData().toStringUtf8().trim();
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String path = httpReq.getRequestURI();
        // Always allow health checks
        if (path.startsWith("/actuator/") || path.equals("/api/business/health")) {
            chain.doFilter(req, res);
            return;
        }

        String incomingKey = httpReq.getHeader(apiKeyHeader);
        if (incomingKey == null || !incomingKey.equals(validApiKey)) {
            log.warn("Unauthorized: invalid/missing API key for {}", path);
            httpRes.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpRes.setContentType("application/json");
            httpRes.getWriter().write("{\"error\":\"Invalid or missing API key\",\"status\":401}");
            return;
        }
        chain.doFilter(req, res);
    }
}
