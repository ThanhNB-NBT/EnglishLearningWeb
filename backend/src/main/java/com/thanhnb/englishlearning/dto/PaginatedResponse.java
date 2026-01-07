package com.thanhnb.englishlearning.dto;

import lombok.Data; // Nếu bạn dùng Lombok
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PaginatedResponse<T> of(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(), // Data
                page.getNumber() + 1, // Page number (1-indexed cho frontend)
                page.getSize(), // Page size
                page.getTotalElements(), // Total records
                page.getTotalPages(), // Total pages
                page.isLast() // Is last page
        );
    }
}