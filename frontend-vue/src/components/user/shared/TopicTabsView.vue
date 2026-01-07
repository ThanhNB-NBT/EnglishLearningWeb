<template>
  <div class="topic-tabs-container">
    <!-- Header -->
    <div class="header-section">
      <div class="title-group">
        <h1 class="module-title">
          <span v-html="icon"></span>
          {{ title }}
        </h1>
      </div>

      <!-- Progress Summary -->
      <div v-if="progressSummary" class="progress-summary">
        <el-progress
          :percentage="progressSummary.completionRate"
          :color="progressSummary.completionRate >= 80 ? '#67C23A' : '#409EFF'"
        >
          <span class="progress-text">
            {{ progressSummary.completedLessons }}/{{ progressSummary.totalLessons }} bài
          </span>
        </el-progress>
      </div>
    </div>

    <!-- Level Tabs -->
    <el-tabs v-model="selectedLevel" class="level-tabs" @tab-change="handleTabChange">
      <el-tab-pane v-for="level in levels" :key="level" :name="level">
        <template #label>
          <span class="tab-label">
            <span class="level-icon">{{ getLevelIcon(level) }}</span>
            <span class="level-name">{{ level }}</span>
            <el-tag size="small" type="info">{{ getLevelProgress(level) }}</el-tag>
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p>Đang tải...</p>
    </div>

    <!-- Main Layout:  Content + Sidebar -->
    <div v-else-if="currentTopicData" class="main-layout">
      <!-- Left:  Topic Content -->
      <div class="content-area">
        <el-card shadow="never">
          <div class="topic-header">
            <div class="topic-icon-large">{{ currentTopicData.image || defaultIcon }}</div>
            <h2>{{ currentTopicData.name }}</h2>
            <p class="topic-description">{{ currentTopicData.description }}</p>

            <!-- Level Badge -->
            <div v-if="currentTopicData.levelRequired" class="level-badge-wrapper">
              <el-tag :color="getLevelColor(currentTopicData.levelRequired)" size="large">
                Level {{ currentTopicData.levelRequired }}
              </el-tag>
              <span class="user-level-text">Bạn: {{ userLevel || 'Chưa xác định' }}</span>
            </div>

            <!-- Stats -->
            <div class="topic-stats">
              <div class="stat-item">
                <el-icon :size="20"><Document /></el-icon>
                <span>{{ currentTopicData.totalLessons || 0 }} bài học</span>
              </div>
              <div class="stat-item">
                <el-icon :size="20"><CircleCheck /></el-icon>
                <span>{{ currentTopicData.completedLessons || 0 }} đã hoàn thành</span>
              </div>
            </div>

            <!-- Action Button -->
            <el-button
              v-if="nextLesson"
              type="primary"
              size="large"
              @click="handleLessonClick(nextLesson, currentTopicData)"
            >
              <el-icon><CaretRight /></el-icon>
              {{ nextLesson.isCompleted ? 'Tiếp tục học' : 'Bắt đầu học' }}
            </el-button>
            <el-alert v-else type="success" :closable="false">
              🎉 Bạn đã hoàn thành tất cả bài học trong chủ đề này!
            </el-alert>
          </div>
        </el-card>
      </div>

      <!-- Right: Lessons Sidebar -->
      <div class="sidebar-area">
        <div class="sidebar-header">
          <h3>Danh sách bài học</h3>
          <el-tag>{{ currentLessons.length }} bài</el-tag>
        </div>

        <div class="lessons-sidebar">
          <div
            v-for="(lesson, idx) in currentLessons"
            :key="lesson.id"
            class="lesson-sidebar-item"
            :class="getLessonStatusClass(lesson, currentTopicData)"
            @click="handleLessonClick(lesson, currentTopicData)"
          >
            <!-- Lesson Number -->
            <div class="lesson-number">{{ idx + 1 }}</div>

            <!-- Lesson Content -->
            <div class="lesson-info">
              <div class="lesson-title-row">
                <span class="lesson-title">{{ lesson.title }}</span>
                <el-icon v-if="lesson.isCompleted" :size="18" color="#67C23A"
                  ><CircleCheck
                /></el-icon>
                <el-icon v-else-if="!lesson.isUnlocked" :size="18" color="#909399"
                  ><Lock
                /></el-icon>
                <el-icon v-else :size="18" color="#409EFF"><CaretRight /></el-icon>
              </div>

              <!-- Grammar:  Theory/Practice Tags -->
              <div v-if="moduleType === 'grammar' && lesson.lessonType" class="lesson-tags">
                <el-tag v-if="lesson.lessonType === 'THEORY'" size="small" type="info"
                  >Lý thuyết</el-tag
                >
                <el-tag v-if="lesson.lessonType === 'PRACTICE'" size="small" type="warning"
                  >Bài tập</el-tag
                >
              </div>

              <!-- Progress Bar -->
              <el-progress
                v-if="lesson.isCompleted || lesson.scorePercentage"
                :percentage="lesson.scorePercentage || 100"
                :color="lesson.scorePercentage >= 80 ? '#67C23A' : '#E6A23C'"
                :show-text="false"
              />

              <!-- Lock Warning -->
              <el-alert v-if="!lesson.isUnlocked" type="warning" :closable="false" show-icon>
                <template v-if="!canAccessTopic(currentTopicData)">
                  <small>🔒 Yêu cầu: {{ currentTopicData.levelRequired }}</small>
                </template>
                <small v-else>Hoàn thành bài trước</small>
              </el-alert>
            </div>
          </div>

          <el-empty v-if="currentLessons.length === 0" description="Chưa có bài học nào" />
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <el-empty v-else description="Không có chủ đề nào ở level này">
      <template #image>
        <el-icon :size="100"><FolderOpened /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLevelAccess } from '@/composables/useLevelAccess'
import {
  Loading,
  CircleCheck,
  Lock,
  CaretRight,
  FolderOpened,
  Document,
} from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: '📚' },
  defaultIcon: { type: String, default: '📖' },
  topics: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  moduleType: { type: String, required: true },
  moduleLabel: { type: String, required: true },
  routeName: { type: String, required: true },
  progressSummary: { type: Object, default: null },
})

const emit = defineEmits(['fetch-topics'])

const router = useRouter()
const { userLevel, canAccessTopic: canAccessTopicFn, getLevelColor } = useLevelAccess()

const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const selectedLevel = ref('A1')

// Current topic data (first topic in selected level)
const currentTopicData = computed(() => {
  const topicsInLevel = props.topics.filter((t) => t.levelRequired === selectedLevel.value)
  return topicsInLevel.length > 0 ? topicsInLevel[0] : null
})

// Lessons of current topic
const currentLessons = computed(() => {
  if (!currentTopicData.value) return []
  return currentTopicData.value.lessons || []
})

// Next lesson to learn
const nextLesson = computed(() => {
  return currentLessons.value.find((l) => l.isUnlocked && !l.isCompleted)
})

const getLevelProgress = (level) => {
  const topicsInLevel = props.topics.filter((t) => t.levelRequired === level)
  if (topicsInLevel.length === 0) return '0/0'

  const completed = topicsInLevel.reduce((sum, t) => sum + (t.completedLessons || 0), 0)
  const total = topicsInLevel.reduce((sum, t) => sum + (t.totalLessons || 0), 0)

  return `${completed}/${total}`
}

const getLevelIcon = (level) => {
  const topicsInLevel = props.topics.filter((t) => t.levelRequired === level)
  if (topicsInLevel.length === 0) return '⚪'

  const allCompleted = topicsInLevel.every(
    (t) => t.completedLessons > 0 && t.completedLessons === t.totalLessons,
  )

  const hasProgress = topicsInLevel.some((t) => (t.completedLessons || 0) > 0)

  if (allCompleted) return '✅'
  if (hasProgress) return '🟡'

  if (!userLevel.value) return '🔒'
  const levelIndex = levels.indexOf(level)
  const userLevelIndex = levels.indexOf(userLevel.value)

  return levelIndex > userLevelIndex ? '🔒' : '🟢'
}

const canAccessTopic = (topic) => {
  return canAccessTopicFn.value(topic)
}

const getLessonStatusClass = (lesson, topic) => {
  if (!canAccessTopic(topic)) return 'locked'
  if (lesson.isCompleted) return 'completed'
  if (lesson.isUnlocked) return 'unlocked'
  return 'locked'
}

const handleLessonClick = (lesson, topic) => {
  if (!canAccessTopic(topic)) {
    ElMessage.warning(`Bạn cần đạt trình độ ${topic.levelRequired} để mở khóa chủ đề này`)
    return
  }

  if (!lesson.isUnlocked) {
    ElMessage.warning('Hoàn thành bài học trước để mở khóa bài này')
    return
  }

  router.push({ name: props.routeName, params: { lessonId: lesson.id } })
}

const handleTabChange = (level) => {
  selectedLevel.value = level
}

// Watch for topics change to auto-select appropriate level
watch(
  () => props.topics,
  (newTopics) => {
    if (newTopics.length > 0 && !newTopics.some((t) => t.levelRequired === selectedLevel.value)) {
      // Auto-select first available level
      const firstAvailable = newTopics[0]?.levelRequired
      if (firstAvailable) {
        selectedLevel.value = firstAvailable
      }
    }
  },
  { immediate: true },
)

onMounted(() => {
  emit('fetch-topics')
})
</script>

<style scoped lang="scss">
.topic-tabs-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 2rem;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;

  .title-group {
    .module-title {
      font-size: 2rem;
      font-weight: 700;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
  }

  .progress-summary {
    min-width: 300px;
  }
}

.level-tabs {
  margin-bottom: 2rem;

  .tab-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;

    .level-icon {
      font-size: 1.25rem;
    }

    .level-name {
      font-weight: 600;
    }
  }
}

// ===== MAIN LAYOUT:  Content + Sidebar =====
.main-layout {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 1.5rem;
  min-height: 600px;
}

// ===== LEFT:  CONTENT AREA =====
.content-area {
  .topic-header {
    text-align: center;
    padding: 2rem;

    .topic-icon-large {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    h2 {
      font-size: 1.75rem;
      font-weight: 700;
      margin: 0 0 1rem 0;
    }

    .topic-description {
      font-size: 1rem;
      color: #606266;
      margin: 0 0 1.5rem 0;
      line-height: 1.6;
    }

    .level-badge-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      margin-bottom: 1.5rem;

      .user-level-text {
        font-size: 0.875rem;
        color: #909399;
      }
    }

    .topic-stats {
      display: flex;
      justify-content: center;
      gap: 2rem;
      margin-bottom: 2rem;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 1rem;
        color: #606266;
      }
    }
  }
}

// ===== RIGHT:  SIDEBAR =====
.sidebar-area {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 1rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 1rem;
    border-bottom: 1px solid #dcdfe6;
    margin-bottom: 1rem;

    h3 {
      margin: 0;
      font-size: 1.125rem;
      font-weight: 600;
    }
  }

  .lessons-sidebar {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 3px;
    }
  }
}

.lesson-sidebar-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;

  .lesson-number {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f2f5;
    border-radius: 50%;
    font-weight: 600;
    font-size: 0.875rem;
    color: #606266;
    flex-shrink: 0;
  }

  .lesson-info {
    flex: 1;
    min-width: 0;

    .lesson-title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      margin-bottom: 0.5rem;

      .lesson-title {
        flex: 1;
        font-weight: 600;
        font-size: 0.875rem;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .lesson-tags {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }
  }

  &.unlocked:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
    transform: translateX(4px);
  }

  &.completed {
    background: #f0f9ff;

    .lesson-number {
      background: #e1f3d8;
      color: #67c23a;
    }
  }

  &.locked {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: #909399;
}

// ===== RESPONSIVE =====
@media (max-width: 1200px) {
  .main-layout {
    grid-template-columns: 1fr;

    .sidebar-area {
      max-height: 500px;
    }
  }
}

@media (max-width: 768px) {
  .topic-tabs-container {
    padding: 1rem;
  }

  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;

    .progress-summary {
      width: 100%;
      min-width: unset;
    }
  }
}
</style>
