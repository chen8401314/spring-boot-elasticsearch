package com.example.demo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ResultDTO {

    private List<Map<String, Object>> content;

    private Object[] sorts;

    private long totalCount;

}
