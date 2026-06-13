package com.company.bizapi.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SearchRequestDto {
    private String query;
    private String entityType;
    private Map<String, String> filters;
    private int    page  = 0;
    private int    size  = 20;
}
