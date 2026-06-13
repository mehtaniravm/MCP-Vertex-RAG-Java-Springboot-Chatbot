package com.company.bizapi.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class EventDto {
    private Long   id;
    private String entityCode;
    private String entityName;
    private String eventType;
    private String severity;
    private String description;
    private String status;
    private LocalDateTime occurredAt;
    private long   durationMinutes;   // computed
    private String urgencyLabel;       // computed
}
