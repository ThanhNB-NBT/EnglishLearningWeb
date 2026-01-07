package com.thanhnb.englishlearning.entity.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillStats implements Serializable {
    private Double accuracy;       // Độ chính xác (0.0 - 1.0)
    private Integer totalAttempts; // Tổng số lần làm bài
    private Integer correctAnswers; // Số câu đúng
    private Integer streak;        // Chuỗi đúng liên tiếp hiện tại

    // Helper update
    public void addResult(boolean isCorrect) {
        if (totalAttempts == null) totalAttempts = 0;
        if (correctAnswers == null) correctAnswers = 0;
        if (streak == null) streak = 0;

        totalAttempts++;
        if (isCorrect) {
            correctAnswers++;
            streak++;
        } else {
            streak = 0;
        }
        // Tính lại accuracy
        accuracy = (totalAttempts > 0) ? (double) correctAnswers / totalAttempts : 0.0;
    }
}