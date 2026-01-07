package com.thanhnb.englishlearning.dto.topic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicUserDto {
    private Long id;
    private String name;
    private String description;
    private ModuleType moduleType;
    private Integer orderIndex;
    
    // Thống kê tiến độ
    private Integer totalLessons;
    private Integer completedLessons;

    private EnglishLevel levelRequired; 
    
    private Boolean isLocked;          // Topic có bị khóa không (user level thấp hơn required)
    private String lockMessage; 

    // ✅ List chứa bài học (ListeningLessonDTO, ReadingLessonDTO...)
    private List<?> lessons; 
}