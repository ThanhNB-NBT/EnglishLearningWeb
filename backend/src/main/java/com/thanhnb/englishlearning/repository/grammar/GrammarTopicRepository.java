package com.thanhnb.englishlearning.repository.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrammarTopicRepository extends JpaRepository<GrammarTopic, Long> {

    // ========= ADMIN CRUD =================
    // Tìm tất cả topic đã sắp xếp theo orderIndex
    @Query("SELECT gt FROM GrammarTopic gt ORDER BY gt.orderIndex ASC")
    List<GrammarTopic> findAllByOrderIndexAsc();

    // Tìm topic active
    @Query("SELECT gt FROM GrammarTopic gt WHERE gt.isActive = :isActive ORDER BY gt.orderIndex ASC")
    List<GrammarTopic> findByIsActiveOrderByOrderIndexAsc(@Param("isActive") Boolean isActive);

    // Lấy orderIndex lớn nhất
    @Query("SELECT MAX(t.orderIndex) FROM GrammarTopic t")
    Integer findMaxOrderIndex();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GrammarTopic t set t.orderIndex = t.orderIndex - 1 where t.orderIndex > :deletedPosition")
    int shiftOrderAfterDelete(@Param("deletedPosition") Integer deletedPosition);

    // Kiểm tra tồn tại
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Kiểm tra nếu topic tồn tại và active
    boolean existsByIdAndIsActiveTrue(Long id);

    // Tìm theo tên
    List<GrammarTopic> findByNameContainingIgnoreCaseOrderByOrderIndexAsc(String name);

    // =========== USER VIEW ================
    // Tìm Active topic đã sắp xếp theo orderIndex
    List<GrammarTopic> findByIsActiveTrueOrderByOrderIndexAsc();

    // Tìm topic theo level và active đã sắp xếp theo orderIndex
    List<GrammarTopic> findByLevelRequiredAndIsActiveTrueOrderByOrderIndexAsc(
            EnglishLevel level);

    // Tìm topic người dùng có thể chọn (based on Level)
    @Query("SELECT gt FROM GrammarTopic gt WHERE gt.isActive = true AND gt.levelRequired <= :userLevel ORDER BY gt.orderIndex ASC")
    List<GrammarTopic> findAccessibleTopics(@Param("userLevel") EnglishLevel userLevel);

    // Tìm topic theo lesson( với điều kiện active)
    @Query("SELECT gt FROM GrammarTopic gt LEFT JOIN FETCH gt.lessons gl WHERE gt.id = :topicId AND gt.isActive = true")
    Optional<GrammarTopic> findByIdWithLessons(@Param("topicId") Long topicId);

}
