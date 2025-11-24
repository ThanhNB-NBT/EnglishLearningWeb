package com.thanhnb.englishlearning.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginationHelper {
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "orderIndex,asc";

    /**
     * Tạo Pageable từ request parameters với validation
     */
    public static Pageable createPageable(Integer page, Integer size, String sort) {
        // Validate và set defaults
        int validPage = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int validSize = validateSize(size);
        
        // Parse sort parameter
        Sort sortObj = parseSort(sort);
        
        return PageRequest.of(validPage, validSize, sortObj);
    }

    /**
     * Validate size parameter
     */
    private static int validateSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        // Giới hạn max size để tránh overload
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Parse sort string thành Sort object
     * Format: "field,direction" hoặc "field1,asc|field2,desc"
     */
    private static Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            sort = DEFAULT_SORT;
        }

        try {
            String[] sortParams = sort.split("\\|");
            Sort result = null;

            for (String param : sortParams) {
                String[] parts = param.split(",");
                String field = parts[0].trim();
                Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

                Sort newSort = Sort.by(direction, field);
                result = (result == null) ? newSort : result.and(newSort);
            }

            return result;
        } catch (Exception e) {
            // Fallback to default sort if parsing fails
            return Sort.by(Sort.Direction.ASC, "orderIndex");
        }
    }
}
