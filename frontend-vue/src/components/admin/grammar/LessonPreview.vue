<!-- src/components/admin/grammar/LessonPreview.vue -->
<template>
  <div class="lesson-preview">
    <el-card shadow="never" class="preview-card">
      <!-- Header -->
      <template #header>
        <div class="preview-header">
          <el-text size="large" tag="b">
            <el-icon>
              <component :is="lesson.lessonType === 'THEORY' ? Reading : EditPen" />
            </el-icon>
            {{ lesson.title }}
          </el-text>
          <el-space>
            <el-tag :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'">
              {{ lesson.lessonType === 'THEORY' ? 'Lý thuyết' : 'Thực hành' }}
            </el-tag>
            <el-tag :type="lesson.isActive ? 'success' : 'danger'">
              {{ lesson.isActive ? 'Active' : 'Inactive' }}
            </el-tag>
          </el-space>
        </div>
      </template>

      <!-- Basic Info -->
      <div class="info-section">
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="Tiêu đề">
            {{ lesson.title }}
          </el-descriptions-item>
          <el-descriptions-item label="Loại">
            <el-tag :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'">
              {{ lesson.lessonType === 'THEORY' ? 'Lý thuyết' : 'Thực hành' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Thứ tự">
            <el-tag type="info">{{ lesson.orderIndex }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Điểm thưởng">
            <el-tag type="warning">{{ lesson.pointsReward }} điểm</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Thời gian ước tính">
            <el-tag type="info">{{ formatDuration(lesson.estimatedDuration) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Số câu hỏi">
            <el-tag type="primary">
              <el-icon><QuestionFilled /></el-icon>
              {{ lesson.questionCount || 0 }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Trạng thái">
            <el-tag :type="lesson.isActive ? 'success' : 'danger'">
              {{ lesson.isActive ? 'Active' : 'Inactive' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="ID">
            <el-text type="info">{{ lesson.id }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Description -->
      <div v-if="lesson.description" class="description-section">
        <el-divider content-position="left">
          <el-text tag="b" size="large">Mô tả</el-text>
        </el-divider>
        <el-card shadow="never" class="description-card">
          <div v-html="lesson.description"></div>
        </el-card>
      </div>

      <!-- Content (Theory lessons) -->
      <div v-if="lesson.lessonType === 'THEORY' && lesson.content" class="content-section">
        <el-divider content-position="left">
          <el-text tag="b" size="large">Nội dung bài học</el-text>
        </el-divider>
        <el-card shadow="never" class="content-card">
          <div v-html="lesson.content" class="lesson-content"></div>
        </el-card>
      </div>

      <!-- Objectives -->
      <div v-if="lesson.objectives && lesson.objectives.length > 0" class="objectives-section">
        <el-divider content-position="left">
          <el-text tag="b" size="large">Mục tiêu học tập</el-text>
        </el-divider>
        <el-card shadow="never" class="objectives-card">
          <ul class="objectives-list">
            <li v-for="(objective, index) in lesson.objectives" :key="index">
              <el-icon color="#67C23A"><Select /></el-icon>
              {{ objective }}
            </li>
          </ul>
        </el-card>
      </div>

      <!-- Practice Info (Practice lessons) -->
      <div v-if="lesson.lessonType === 'PRACTICE'" class="practice-section">
        <el-divider content-position="left">
          <el-text tag="b" size="large">Thông tin thực hành</el-text>
        </el-divider>
        <el-alert
          title="Bài tập thực hành"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>Lesson này chứa {{ lesson.questionCount || 0 }} câu hỏi thực hành.</p>
            <p v-if="lesson.estimatedDuration">
              Thời gian ước tính: {{ formatDuration(lesson.estimatedDuration) }}
            </p>
          </template>
        </el-alert>
      </div>

      <!-- Timestamps -->
      <div class="timestamps-section">
        <el-divider content-position="left">
          <el-text tag="b" size="large">Thông tin khác</el-text>
        </el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Ngày tạo">
            {{ formatDate(lesson.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="Cập nhật lần cuối">
            {{ formatDate(lesson.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Reading, EditPen, QuestionFilled, Select } from '@element-plus/icons-vue'

const props = defineProps({
  lesson: {
    type: Object,
    required: true
  }
})

const formatDuration = (seconds) => {
  if (!seconds) return '0 giây'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (minutes > 0) {
    return secs > 0 ? `${minutes} phút ${secs} giây` : `${minutes} phút`
  }
  return `${secs} giây`
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.lesson-preview {
  padding: 0;
}

.preview-card {
  border: 2px solid var(--el-border-color);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.info-section,
.description-section,
.content-section,
.objectives-section,
.practice-section,
.timestamps-section {
  margin-bottom: 24px;
}

.description-card,
.content-card,
.objectives-card {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}

.description-card {
  padding: 16px;
  line-height: 1.8;
}

.content-card {
  padding: 20px;
}

.lesson-content {
  line-height: 1.8;
  font-size: 15px;
}

.lesson-content :deep(h1),
.lesson-content :deep(h2),
.lesson-content :deep(h3) {
  margin-top: 20px;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.lesson-content :deep(p) {
  margin-bottom: 12px;
}

.lesson-content :deep(ul),
.lesson-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.lesson-content :deep(li) {
  margin-bottom: 8px;
}

.lesson-content :deep(code) {
  background: var(--el-fill-color);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.lesson-content :deep(pre) {
  background: var(--el-fill-color);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

.objectives-card {
  padding: 16px;
}

.objectives-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.objectives-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  line-height: 1.6;
}

.objectives-list li .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.practice-section p {
  margin: 8px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .preview-header {
    flex-direction: column;
    align-items: flex-start;
  }

  :deep(.el-descriptions) {
    font-size: 12px;
  }
}
</style>
