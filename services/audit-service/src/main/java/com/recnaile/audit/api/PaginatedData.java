package com.recnaile.audit.api;

import org.springframework.data.domain.Page;
import java.util.List;

/** Platform-standard offset-based pagination response. */
public record PaginatedData<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PaginatedData<T> from(Page<T> result) {
        return new PaginatedData<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
