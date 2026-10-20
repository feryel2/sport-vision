package com.sportvision.training.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageOutput<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> PageOutput<T> from(Page<T> page) {
        return new PageOutput<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
