package com.company.bizapi.service;

import com.company.bizapi.model.dto.*;
import com.company.bizapi.model.entity.*;
import com.company.bizapi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessDataService {

    private final EntityRepository     entityRepo;
    private final EntityMetricRepository metricRepo;
    private final EventRepository      eventRepo;

    // ── Entity Summary with computed health score ─────────────────────────────
    public EntitySummaryDto getEntitySummary(String entityCode) {
        EntityRecord entity = entityRepo.findByCode(entityCode)
            .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityCode));

        LocalDateTime since24h = LocalDateTime.now().minusHours(24);

        // Fetch latest metrics
        double uptime  = metricRepo.avgMetricSince(entity.getId(), "uptime_percent",    since24h).orElse(100.0);
        double respTime= metricRepo.avgMetricSince(entity.getId(), "response_time_ms",  since24h).orElse(0.0);

        // Fetch open events
        List<EventRecord> openEvents = eventRepo.findOpenByEntityId(entity.getId());
        long criticalCount = openEvents.stream().filter(e -> "CRITICAL".equals(e.getSeverity())).count();

        // Business logic: compute health score (0-100)
        int healthScore = computeHealthScore(uptime, respTime, openEvents.size(), (int) criticalCount);
        String healthLabel = healthScore >= 80 ? "HEALTHY" : healthScore >= 50 ? "DEGRADED" : "CRITICAL";

        List<String> alerts = openEvents.stream()
            .map(e -> String.format("[%s] %s", e.getSeverity(), e.getDescription()))
            .collect(Collectors.toList());

        return EntitySummaryDto.builder()
            .entityCode(entity.getCode())
            .name(entity.getName())
            .category(entity.getCategory())
            .status(entity.getStatus())
            .healthScore(healthScore)
            .healthLabel(healthLabel)
            .uptimePercent(uptime)
            .avgResponseTimeMs(respTime)
            .openEventCount(openEvents.size())
            .criticalEventCount((int) criticalCount)
            .activeAlerts(alerts)
            .build();
    }

    /**
     * Business rule: health score computed from multiple dimensions.
     * Replace these thresholds with your own domain rules.
     */
    private int computeHealthScore(double uptime, double responseTimeMs, int openEvents, int criticalEvents) {
        int score = 100;
        // Uptime deductions
        if (uptime < 99.0) score -= 30;
        else if (uptime < 99.9) score -= 10;
        // Response time deductions
        if (responseTimeMs > 1000) score -= 20;
        else if (responseTimeMs > 500) score -= 10;
        // Open event deductions
        score -= openEvents    * 5;
        score -= criticalEvents * 15;
        return Math.max(0, score);
    }

    // ── KPI Report ────────────────────────────────────────────────────────────
    public KpiReportDto getKpiReport(String fromDate, String toDate, String category) {
        LocalDateTime from = LocalDateTime.parse(fromDate + "T00:00:00");
        LocalDateTime to   = LocalDateTime.parse(toDate   + "T23:59:59");

        List<EntityRecord> entities = category != null
            ? entityRepo.findByCategory(category.toUpperCase())
            : entityRepo.findAll();

        if (entities.isEmpty()) {
            return KpiReportDto.builder().period(fromDate + " to " + toDate)
                .category(category != null ? category : "ALL")
                .totalEntities(0).overallHealthLabel("NO_DATA").build();
        }

        // Aggregate metrics across all entities
        DoubleSummaryStatistics uptimeStats = entities.stream()
            .mapToDouble(e -> metricRepo.avgMetricSince(e.getId(), "uptime_percent", from).orElse(100.0))
            .summaryStatistics();

        DoubleSummaryStatistics respStats = entities.stream()
            .mapToDouble(e -> metricRepo.avgMetricSince(e.getId(), "response_time_ms", from).orElse(0.0))
            .summaryStatistics();

        long openEvents    = eventRepo.countByStatusAndOccurredAtAfter("OPEN", from);
        List<EventRecord> critEvents = eventRepo.findOpenBySeveritySince("CRITICAL", from);

        // Event breakdown by type
        Map<String, Long> byType = eventRepo.findOpenEventsSince(from).stream()
            .collect(Collectors.groupingBy(EventRecord::getEventType, Collectors.counting()));

        double slaCompliance = uptimeStats.getAverage() >= 99.9 ? 100.0
            : (uptimeStats.getAverage() / 99.9) * 100.0;

        String overallHealth = slaCompliance >= 95 ? "HEALTHY"
            : slaCompliance >= 80 ? "DEGRADED" : "CRITICAL";

        return KpiReportDto.builder()
            .period(fromDate + " to " + toDate)
            .category(category != null ? category : "ALL")
            .totalEntities(entities.size())
            .avgUptimePercent(uptimeStats.getAverage())
            .avgResponseTimeMs(respStats.getAverage())
            .totalOpenEvents(openEvents)
            .criticalEventCount(critEvents.size())
            .slaCompliancePercent(slaCompliance)
            .eventsByType(byType)
            .overallHealthLabel(overallHealth)
            .build();
    }

    // ── Active Events ─────────────────────────────────────────────────────────
    public List<EventDto> getActiveEvents(String severity, int lastHours) {
        LocalDateTime since = LocalDateTime.now().minusHours(lastHours);
        List<EventRecord> events = severity != null
            ? eventRepo.findOpenBySeveritySince(severity.toUpperCase(), since)
            : eventRepo.findOpenEventsSince(since);

        return events.stream().map(e -> {
            long durationMins = e.getOccurredAt() != null
                ? java.time.Duration.between(e.getOccurredAt(), LocalDateTime.now()).toMinutes()
                : 0;
            String urgency = durationMins > 240 ? "ESCALATE" : durationMins > 60 ? "URGENT" : "MONITOR";
            return EventDto.builder()
                .id(e.getId())
                .entityCode(e.getEntity() != null ? e.getEntity().getCode() : "UNKNOWN")
                .entityName(e.getEntity() != null ? e.getEntity().getName() : "Unknown")
                .eventType(e.getEventType())
                .severity(e.getSeverity())
                .description(e.getDescription())
                .status(e.getStatus())
                .occurredAt(e.getOccurredAt())
                .durationMinutes(durationMins)
                .urgencyLabel(urgency)
                .build();
        }).collect(Collectors.toList());
    }

    // ── SLA Status ────────────────────────────────────────────────────────────
    public List<SlaStatusDto> getSlaStatus(String entityCode) {
        List<EntityRecord> entities = entityCode != null
            ? entityRepo.findByCode(entityCode).map(List::of).orElse(List.of())
            : entityRepo.findByStatus("ACTIVE");

        return entities.stream().map(entity -> {
            double SLA_THRESHOLD = 99.9;
            LocalDateTime since30d = LocalDateTime.now().minusDays(30);
            double uptime = metricRepo.avgMetricSince(entity.getId(), "uptime_percent", since30d).orElse(100.0);
            double gap = SLA_THRESHOLD - uptime;
            String compliance = gap <= 0 ? "COMPLIANT" : gap < 0.5 ? "AT_RISK" : "BREACHED";
            double riskScore = Math.min(100.0, gap * 100.0);
            List<String> recs = new ArrayList<>();
            if ("BREACHED".equals(compliance))  recs.add("Immediate escalation required — SLA is breached");
            if ("AT_RISK".equals(compliance))   recs.add("Monitor closely — approaching SLA threshold");
            if (uptime < 99.0)                  recs.add("Investigate recurring downtime events");
            return SlaStatusDto.builder()
                .entityCode(entity.getCode())
                .entityName(entity.getName())
                .slaThresholdPercent(SLA_THRESHOLD)
                .currentUptimePercent(uptime)
                .complianceStatus(compliance)
                .breachRiskScore(riskScore)
                .recommendations(recs)
                .build();
        }).collect(Collectors.toList());
    }

    // ── Business Search ───────────────────────────────────────────────────────
    public SearchResultDto businessSearch(SearchRequestDto request) {
        String q = request.getQuery();
        List<EntityRecord> entities = entityRepo.searchByNameOrCode(q);
        List<Map<String, Object>> results = entities.stream().map(e -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type",     "entity");
            r.put("code",     e.getCode());
            r.put("name",     e.getName());
            r.put("category", e.getCategory());
            r.put("status",   e.getStatus());
            return r;
        }).collect(Collectors.toList());
        return SearchResultDto.builder()
            .query(q)
            .totalResults(results.size())
            .results(results)
            .build();
    }
}
