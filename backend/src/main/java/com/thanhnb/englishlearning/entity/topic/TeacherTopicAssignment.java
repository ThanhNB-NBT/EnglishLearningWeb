package com.thanhnb.englishlearning.entity.topic;

import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.entity.user.User;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "teacher_topic_assignments", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "topic_id"}),
    indexes = {
        @Index(name = "idx_teacher_assignments", columnList = "teacher_id, is_active"),
        @Index(name = "idx_topic_assignments", columnList = "topic_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"teacher", "topic", "assignedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TeacherTopicAssignment {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The teacher being assigned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /**
     * The topic being assigned to the teacher
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    /**
     * Module type (for quick filtering)
     * Denormalized from topic.moduleType for performance
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 20)
    private ModuleType moduleType;

    /**
     * When this assignment was created
     */
    @Column(name = "assigned_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime assignedAt;

    /**
     * Admin who created this assignment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /**
     * Whether this assignment is currently active
     * Set to false instead of deleting for audit trail
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ==================== HELPER METHODS ====================

    /**
     * Activate this assignment
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate this assignment
     */
    public void deactivate() {
        this.isActive = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.topic != null) {
            this.moduleType = this.topic.getModuleType();
        }
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}