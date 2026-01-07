package com.thanhnb.englishlearning.util;

import com.thanhnb.englishlearning.dto.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaginationHelper {

    /**
     * Tạo Pageable từ các tham số request
     */
    public static Pageable createPageable(int page, int size, String sort) {
        int pageNo = page > 0 ? page - 1 : 0; // Spring Page bắt đầu từ 0
        
        Sort.Direction direction = Sort.Direction.ASC;
        String sortProperty = "id";

        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(":");
            sortProperty = parts[0];
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) {
                direction = Sort.Direction.DESC;
            }
        }

        // Mặc định sort theo ID nếu không parse được hoặc sort rỗng
        return PageRequest.of(pageNo, size, Sort.by(direction, sortProperty));
    }

    /**
     * Chuyển đổi từ Spring Page<Entity> sang PaginatedResponse<DTO>
     */
    public static <T, R> PaginatedResponse<R> toPaginatedResponse(Page<T> pageData, Function<T, R> mapper) {
        PaginatedResponse<R> response = new PaginatedResponse<>();
        
        // Convert List<Entity> -> List<DTO>
        List<R> dtoList = pageData.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        response.setContent(dtoList);
        response.setPage(pageData.getNumber() + 1); // Trả về frontend số trang bắt đầu từ 1
        response.setSize(pageData.getSize());
        response.setTotalElements(pageData.getTotalElements());
        response.setTotalPages(pageData.getTotalPages());
        response.setLast(pageData.isLast());

        return response;
    }
}