<template>
  <div class="lesson-preview">
    <div class="preview-header">
      <div class="header-top">
        <div class="title-section">
          <span class="order-badge">#{{ lesson.orderIndex }}</span>
          <h2 class="lesson-title">{{ lesson.title }}</h2>
        </div>
        <div class="status-badge">
          <el-tag :type="lesson.isActive ? 'success' : 'danger'" effect="dark" size="small">
            {{ lesson.isActive ? 'Active' : 'Inactive' }}
          </el-tag>
        </div>
      </div>

      <div class="header-meta">
        <el-tag :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'" effect="plain">
          {{ lesson.lessonType === 'THEORY' ? 'Lý thuyết' : 'Bài tập' }}
        </el-tag>
        <span class="created-at">Tạo ngày: {{ formatDate(lesson.createdAt) }}</span>
      </div>
    </div>

    <div class="preview-body">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="Thời gian ước tính">
          <el-icon class="mr-1"><Timer /></el-icon>
          {{ formatTime(lesson.estimatedDuration) }}
        </el-descriptions-item>

        <el-descriptions-item label="Điểm thưởng">
          <el-icon class="mr-1 text-warning"><Trophy /></el-icon>
          <span class="text-warning font-bold">+{{ lesson.pointsReward }}</span>
        </el-descriptions-item>

        <el-descriptions-item label="Topic ID">{{ lesson.topicId }}</el-descriptions-item>
        <el-descriptions-item label="Số câu hỏi">{{ lesson.questionCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <div class="content-section" v-if="lesson.content">
        <h4 class="section-title">Nội dung bài học:</h4>
        <div class="rich-text-content ql-editor" v-html="lesson.content"></div>
      </div>

      <el-empty v-else description="Chưa có nội dung chi tiết" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { Timer, Trophy } from '@element-plus/icons-vue'
// Import CSS của Quill để hiển thị content HTML đẹp (giống editor)
import '@vueup/vue-quill/dist/vue-quill.snow.css'

defineProps({
  lesson: { type: Object, required: true }
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const formatTime = (seconds) => {
  if (!seconds) return '0s'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m} phút ${s} giây` : `${s} giây`
}
</script>

<style scoped>
.lesson-preview {
  padding: 0 10px;
}

/* Header */
.preview-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 16px;
  margin-bottom: 20px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-badge {
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-secondary);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 14px;
}

.lesson-title {
  margin: 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.created-at {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* Body */
.mb-4 { margin-bottom: 16px; }
.mr-1 { margin-right: 4px; }
.text-warning { color: #e6a23c; }
.font-bold { font-weight: 600; }

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}

/* Content Area - Giả lập style của Quill Editor */
.rich-text-content {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  min-height: 100px;
  line-height: 1.6;
}

/* Dark mode overrides */
html.dark .rich-text-content {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-darker);
}
</style>
