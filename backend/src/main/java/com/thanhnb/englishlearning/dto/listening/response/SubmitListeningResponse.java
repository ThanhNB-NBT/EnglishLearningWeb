package com.thanhnb.englishlearning.dto.listening.response;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitListeningResponse {
    private Long lessonId;
    private boolean isPassed;
    private double scorePercentage;
    private Integer totalScore;
    private int correctCount;
    private int totalQuestions;
    private int pointsEarned;
    
    // Hỗ trợ hiển thị kết quả chi tiết
    private List<QuestionResultDTO> results;
    
    // Điều hướng
    private boolean hasNextLesson;
    private Long nextLessonId;
}