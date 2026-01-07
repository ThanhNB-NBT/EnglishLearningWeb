package com.thanhnb.englishlearning.entity.topic;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"teacherAssignments", "grammarLessons", "readingLessons", "listeningLessons"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Topic {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 20)
    private ModuleType moduleType; // GRAMMAR, READING, LISTENING

    @Enumerated(EnumType.STRING)
    @Column(name = "level_required", length = 10)
    private EnglishLevel levelRequired; // A1 -> C1

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ==================== RELATIONSHIPS ====================

    // Mapping tới danh sách giáo viên quản lý topic này
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeacherTopicAssignment> teacherAssignments;

    // GRAMMAR
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GrammarLesson> grammarLessons = new ArrayList<>();

    // READING
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReadingLesson> readingLessons = new ArrayList<>();

    // LISTENING
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ListeningLesson> listeningLessons = new ArrayList<>();

    // ==================== TIMESTAMPS ====================
    
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // ==================== HELPER METHODS ====================
    
    public int getLessonCount() {
        if (moduleType == null) return 0;
        // Kiểm tra null safe tốt hơn
        return switch (moduleType) {
            case GRAMMAR -> (grammarLessons == null) ? 0 : grammarLessons.size();
            case READING -> (readingLessons == null) ? 0 : readingLessons.size();
            case LISTENING -> (listeningLessons == null) ? 0 : listeningLessons.size();
            default -> 0;
        };
    }
}