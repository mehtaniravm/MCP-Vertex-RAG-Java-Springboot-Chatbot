package com.company.bizapi.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data @Builder
public class SearchResultDto {
    private String query;
    private int    totalResults;
    private List<Map<String, Object>> results;
}
