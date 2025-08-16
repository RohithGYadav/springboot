package com.example.backend.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonDeserialize(builder = PageResponse.PageResponseBuilder.class)
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // Tell Jackson to use "build()" method and ignore unknown fields
    @JsonPOJOBuilder(withPrefix = "")
    public static class PageResponseBuilder<T> {
    }
}
