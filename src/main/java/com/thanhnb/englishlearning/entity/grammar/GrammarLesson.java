package com.thanhnb.englishlearning.entity.grammar;

import com.thanhnb.englishlearning.enums.LessonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "grammar_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrammarLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private GrammarTopic topic;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false)
    private LessonType lessonType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    //Điểm cần thiết mở khóa bài học tiếp theo
    @Column(name = "points_required")
    private Integer pointsRequired = 0; 

    //Điểm thưởng khi hoàn thành bài học
    @Column(name = "points_reward")
    private Integer pointsReward = 10;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    //Relationships
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GrammarQuestion> questions;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGrammarProgress> userProgresses;
    
}
