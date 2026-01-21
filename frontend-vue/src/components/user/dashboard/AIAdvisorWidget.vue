<template>
  <div class="ai-widget">
    <!-- Header -->
    <div class="widget-header">
      <div class="header-left">
        <span class="ai-icon">🤖</span>
        <div>
          <h3 class="title">AI Learning Advisor</h3>
          <p class="subtitle">Gợi ý học tập thông minh dành riêng cho bạn</p>
        </div>
      </div>
      <el-button
        link
        type="primary"
        :loading="aiStore.isLoading"
        @click="handleRefresh"
        size="small"
      >
        <el-icon><Refresh /></el-icon>
        Làm mới
      </el-button>
    </div>

    <!-- Loading State -->
    <div v-if="aiStore.isLoading" class="loading-state">
      <el-icon class="is-loading" :size="40" color="#409eff">
        <Loading />
      </el-icon>
      <p class="loading-text">AI đang phân tích hồ sơ của bạn...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="!aiStore.hasRecommendations" class="empty-state">
      <span class="empty-icon">📚</span>
      <p class="empty-title">Chào bạn! 👋</p>
      <p class="empty-desc">Hãy làm bài tập để AI có dữ liệu phân tích nhé.</p>
      <el-button type="primary" @click="$router.push('/user/grammar')">
        Bắt đầu học
      </el-button>
    </div>

    <!-- Recommendations List -->
    <div v-else class="recommendations-list">
      <div
        v-for="rec in aiStore.sortedRecommendations"
        :key="rec.id"
        class="rec-card"
        :class="getPriorityClass(rec.priority)"
      >
        <!-- Card Header -->
        <div class="rec-header">
          <span class="rec-icon">{{ getTypeIcon(rec.type) }}</span>
          <div class="rec-title-area">
            <h4 class="rec-title">{{ rec.title }}</h4>
            <div class="rec-tags">
              <el-tag size="small" :type="getSkillColor(rec.targetSkill)" effect="light">
                {{ rec.targetSkill }}
              </el-tag>
              <el-tag
                v-if="rec.priority >= 5"
                size="small"
                type="danger"
                effect="dark"
              >
                Ưu tiên cao
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Description -->
        <p class="rec-desc">{{ rec.description }}</p>

        <!-- Lesson Info (if available) -->
        <div v-if="rec.targetLessonTitle" class="lesson-info">
          <div class="info-row">
            <span class="info-label">📖 Bài học:</span>
            <span class="info-value">{{ rec.targetLessonTitle }}</span>
          </div>
          <div v-if="rec.targetTopicName" class="info-row">
            <span class="info-label">📂 Chủ đề:</span>
            <span class="info-value">{{ rec.targetTopicName }}</span>
          </div>
          <div class="info-stats">
            <span v-if="rec.estimatedMinutes" class="stat-item">
              <el-icon><Timer /></el-icon>
              ~{{ rec.estimatedMinutes }} phút
            </span>
            <span v-if="rec.totalQuestions" class="stat-item">
              <el-icon><Document /></el-icon>
              {{ rec.totalQuestions }} câu hỏi
            </span>
            <span v-if="rec.pointsReward" class="stat-item reward">
              <el-icon><StarFilled /></el-icon>
              +{{ rec.pointsReward }} điểm
            </span>
          </div>
        </div>

        <!-- Motivation Message -->
        <div v-if="rec.motivationMessage" class="motivation">
          💡 {{ rec.motivationMessage }}
        </div>

        <!-- Actions -->
        <div class="rec-actions">
          <el-button
            type="primary"
            size="small"
            @click="handleAction(rec)"
            :class="{ 'urgent-btn': rec.priority >= 6 }"
          >
            {{ getActionButtonText(rec.type) }}
            <el-icon><Right /></el-icon>
          </el-button>
          <el-button
            text
            size="small"
            @click="handleDismiss(rec)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- Reasoning (Collapsible) -->
        <div v-if="rec.reasoning" class="reasoning-section">
          <el-button
            link
            size="small"
            type="info"
            @click="toggleReasoning(rec.id)"
          >
            <el-icon>
              <component :is="showReasoning[rec.id] ? 'ArrowUp' : 'InfoFilled'" />
            </el-icon>
            {{ showReasoning[rec.id] ? 'Ẩn chi tiết' : 'Tại sao?' }}
          </el-button>
          <el-collapse-transition>
            <div v-if="showReasoning[rec.id]" class="reasoning-content">
              {{ rec.reasoning }}
            </div>
          </el-collapse-transition>
        </div>
      </div>
    </div>

    <!-- Summary Stats -->
    <div v-if="aiStore.hasRecommendations" class="summary-stats">
      <div class="stat-box">
        <span class="stat-number">{{ aiStore.recommendations.length }}</span>
        <span class="stat-label">Gợi ý</span>
      </div>
      <div v-if="urgentCount > 0" class="stat-box urgent">
        <span class="stat-number">{{ urgentCount }}</span>
        <span class="stat-label">Khẩn cấp</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { useAIRecommendationStore } from '@/stores/aiRecommendation'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Refresh,
  Right,
  Close,
  Loading,
  Timer,
  Document,
  StarFilled,
} from '@element-plus/icons-vue'

const aiStore = useAIRecommendationStore()
const router = useRouter()

const showReasoning = reactive({})

const urgentCount = computed(() => {
  return aiStore.recommendations.filter(r => r.priority >= 5).length
})

onMounted(async () => {
  console.log('🎯 AIAdvisorWidget mounted')

  if (!aiStore.hasRecommendations) {
    console.log('📡 Fetching recommendations...')
    await aiStore.fetchRecommendations()

    console.log('✅ Recommendations fetched:', {
      count: aiStore.recommendations.length,
      types: aiStore.recommendations.map(r => r.type),
    })
  }
})

const handleRefresh = async () => {
  console.log('🔄 Refreshing...')
  await aiStore.refreshRecommendations()
}

const handleAction = async (rec) => {
  console.log('👆 Action:', rec.type)
  await aiStore.handleRecommendationAction(rec, router)
}

const handleDismiss = async (rec) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc muốn ẩn gợi ý này?', 'Xác nhận', {
      confirmButtonText: 'Ẩn',
      cancelButtonText: 'Hủy',
      type: 'warning',
    })
    await aiStore.dismissRecommendation(rec.id)
  } catch {
    // User cancelled
  }
}

const toggleReasoning = (recId) => {
  showReasoning[recId] = !showReasoning[recId]
}

// ========== HELPERS ==========

const getTypeIcon = (type) => {
  const icons = {
    'STREAK_SAVER': '🔥',
    'REVIEW_LESSON': '📄',
    'MISTAKE_PATTERN': '🎯',
    'TOPIC_MASTERY': '🏆',
    'PRACTICE_WEAK_SKILL': '💪',
    'CONTINUE_TOPIC': '📚',
    'TIME_OPTIMAL': '⏰',
    'NEXT_LESSON': '➡️',
    'VARIETY': '🎨',
  }
  return icons[type] || '📖'
}

const getSkillColor = (skill) => {
  const map = {
    GRAMMAR: 'success',
    READING: 'warning',
    LISTENING: 'danger'
  }
  return map[skill] || 'info'
}

const getPriorityClass = (priority) => {
  if (priority >= 6) return 'priority-urgent'
  if (priority >= 5) return 'priority-high'
  return 'priority-normal'
}

const getActionButtonText = (type) => {
  const texts = {
    'STREAK_SAVER': 'Cứu streak!',
    'REVIEW_LESSON': 'Ôn lại ngay',
    'MISTAKE_PATTERN': 'Khắc phục',
    'TOPIC_MASTERY': 'Hoàn thành',
    'PRACTICE_WEAK_SKILL': 'Luyện tập',
    'CONTINUE_TOPIC': 'Tiếp tục',
    'TIME_OPTIMAL': 'Bắt đầu',
    'NEXT_LESSON': 'Học ngay',
  }
  return texts[type] || 'Bắt đầu'
}
</script>

<style scoped>
.ai-widget {
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* ==================== HEADER ==================== */

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f3f4f6;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon {
  font-size: 32px;
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

/* ==================== LOADING STATE ==================== */

.loading-state {
  text-align: center;
  padding: 40px 20px;
}

.loading-text {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
}

/* ==================== EMPTY STATE ==================== */

.empty-state {
  text-align: center;
  padding: 50px 20px;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.empty-desc {
  color: #6b7280;
  font-size: 14px;
  margin: 0 0 20px 0;
}

/* ==================== RECOMMENDATIONS LIST ==================== */

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rec-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #d1d5db;
  transition: all 0.3s ease;
}

.rec-card:hover {
  background: #f3f4f6;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.rec-card.priority-urgent {
  border-left-color: #dc2626;
  border-left-width: 5px;
  background: linear-gradient(to right, #fef2f2 0%, #f9fafb 100%);
}

.rec-card.priority-high {
  border-left-color: #f59e0b;
  border-left-width: 5px;
  background: linear-gradient(to right, #fffbeb 0%, #f9fafb 100%);
}

.rec-card.priority-normal {
  border-left-color: #3b82f6;
}

/* Card Header */

.rec-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.rec-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.rec-title-area {
  flex: 1;
  min-width: 0;
}

.rec-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.rec-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* Description */

.rec-desc {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-line;
}

/* Lesson Info */

.lesson-info {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #6b7280;
  font-weight: 500;
}

.info-value {
  color: #111827;
  font-weight: 600;
}

.info-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.stat-item.reward {
  color: #f59e0b;
  font-weight: 600;
}

/* Motivation */

.motivation {
  font-size: 13px;
  color: #6366f1;
  background: #eef2ff;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 12px;
  font-weight: 500;
}

/* Actions */

.rec-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rec-actions .el-button {
  flex: 1;
}

.urgent-btn {
  animation: pulse-btn 2s ease-in-out infinite;
}

@keyframes pulse-btn {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.03);
  }
}

/* Reasoning */

.reasoning-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.reasoning-content {
  margin-top: 8px;
  padding: 10px;
  background: white;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  font-style: italic;
  line-height: 1.6;
}

/* ==================== SUMMARY STATS ==================== */

.summary-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid #f3f4f6;
}

.stat-box {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.stat-box.urgent .stat-number {
  color: #dc2626;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* ==================== RESPONSIVE ==================== */

@media (max-width: 768px) {
  .ai-widget {
    padding: 16px;
  }

  .widget-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .rec-card {
    padding: 16px;
  }

  .rec-header {
    flex-direction: column;
  }

  .rec-icon {
    font-size: 24px;
  }

  .info-stats {
    flex-direction: column;
    gap: 8px;
  }

  .rec-actions {
    flex-direction: column;
  }

  .rec-actions .el-button {
    width: 100%;
  }

  .summary-stats {
    gap: 24px;
  }
}

/* ==================== DARK MODE ==================== */

@media (prefers-color-scheme: dark) {
  .ai-widget {
    background: #1f2937;
    border-color: #374151;
  }

  .title {
    color: #f9fafb;
  }

  .subtitle {
    color: #9ca3af;
  }

  .rec-card {
    background: #111827;
  }

  .rec-card:hover {
    background: #1f2937;
  }

  .rec-title {
    color: #f9fafb;
  }

  .rec-desc {
    color: #d1d5db;
  }

  .lesson-info {
    background: #1f2937;
    border-color: #374151;
  }

  .info-value {
    color: #f9fafb;
  }

  .motivation {
    background: #1e3a8a;
    color: #bfdbfe;
  }

  .reasoning-content {
    background: #1f2937;
    color: #9ca3af;
  }

  .stat-number {
    color: #f9fafb;
  }
}
</style>
