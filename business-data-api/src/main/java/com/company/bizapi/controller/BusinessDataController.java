package com.company.bizapi.controller;

import com.company.bizapi.model.dto.*;
import com.company.bizapi.service.BusinessDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")   // CORS for internal service calls
public class BusinessDataController {

    private final BusinessDataService service;

    /** Health check — no auth required */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "business-data-api"));
    }

    /** Entity summary with computed health score */
    @GetMapping("/entities/{entityCode}/summary")
    public ResponseEntity<EntitySummaryDto> getEntitySummary(@PathVariable String entityCode) {
        return ResponseEntity.ok(service.getEntitySummary(entityCode));
    }

    /** Aggregated KPI report */
    @GetMapping("/kpi/report")
    public ResponseEntity<KpiReportDto> getKpiReport(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.getKpiReport(from, to, category));
    }

    /** Active events with urgency classification */
    @GetMapping("/events/active")
    public ResponseEntity<List<EventDto>> getActiveEvents(
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "24") int lastHours) {
        return ResponseEntity.ok(service.getActiveEvents(severity, lastHours));
    }

    /** SLA compliance status */
    @GetMapping("/sla/status")
    public ResponseEntity<List<SlaStatusDto>> getSlaStatus(
            @RequestParam(required = false) String entityCode) {
        return ResponseEntity.ok(service.getSlaStatus(entityCode));
    }

    /** Business search across entities */
    @PostMapping("/search")
    public ResponseEntity<SearchResultDto> search(@RequestBody SearchRequestDto request) {
        return ResponseEntity.ok(service.businessSearch(request));
    }
}
