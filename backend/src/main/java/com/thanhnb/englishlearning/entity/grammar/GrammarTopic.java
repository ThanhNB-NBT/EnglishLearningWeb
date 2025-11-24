package com.thanhnb.englishlearning.entity.grammar;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "grammar_topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrammarTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level_required", length = 20)
    private EnglishLevel levelRequired = EnglishLevel.BEGINNER;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GrammarLesson> lessons;

}
