<template>
  <el-card
    class="topic-card"
    :class="{ 'inactive': !topic.isActive }"
    shadow="hover"
  >
    <!-- Header -->
    <template #header>
      <div class="card-header">
        <div class="topic-info">
          <el-tag
            :type="getLevelColor(topic.levelRequired)"
            size="small"
            class="level-tag"
          >
            {{ topic.levelRequired }}
          </el-tag>
          <span class="order-badge">#{{ topic.orderIndex }}</span>
        </div>

        <div class="actions">
          <el-button
            type="primary"
            size="small"
            :icon="Edit"
            circle
            @click.stop="handleEdit"
          />
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            circle
            @click.stop="handleDelete"
          />
          <el-switch
            v-model="isActive"
            active-text="Active"
            inactive-text="Inactive"
            @click.stop
            @change="handleToggleActive"
          />
        </div>
      </div>
    </template>

    <!-- Body -->
    <div class="card-body">
      <h3 class="topic-name">{{ topic.name }}</h3>
      <p class="topic-description">{{ topic.description || 'Chưa có mô tả' }}</p>

      <!-- Stats -->
      <div class="stats">
        <el-statistic
          title="Lessons"
          :value="topic.totalLessons || 0"
        >
          <template #prefix>
            <el-icon><Document /></el-icon>
          </template>
        </el-statistic>
      </div>

      <!-- Meta info -->
      <div class="meta">
        <el-text size="small" type="info">
          <el-icon><Clock /></el-icon>
          {{ formatDate(topic.createdAt) }}
        </el-text>
      </div>
    </div>

    <!-- Footer actions -->
    <template #footer>
      <div class="footer-actions">
        <el-button
          size="small"
          @click.stop="handleViewLessons"
        >
          <el-icon><FolderOpened /></el-icon>
          Xem Lessons
        </el-button>

        <el-button
          size="small"
          type="success"
          @click.stop="handleAddLesson"
        >
          <el-icon><Plus /></el-icon>
          Thêm Lesson
        </el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import {
  Edit,
  Delete,
  Document,
  Clock,
  FolderOpened,
  Plus
} from '@element-plus/icons-vue'
import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'

// Props
const props = defineProps({
  topic: {
    type: Object,
    required: true,
  },
})

// Emits
const emit = defineEmits(['edit', 'delete', 'toggle-active', 'view-lessons', 'add-lesson'])

// Computed
const isActive = computed({
  get: () => props.topic.isActive,
  set: (value) => {
    emit('toggle-active', { ...props.topic, isActive: value })
  }
})

// Methods
const handleEdit = () => {
  console.log('Edit clicked for topic:', props.topic.id)
  emit('edit', props.topic)
}

const handleDelete = () => {
  console.log('Delete clicked for topic:', props.topic.id)
  emit('delete', props.topic)
}

const handleToggleActive = (value) => {
  console.log('Toggle active for topic:', props.topic.id, value)
  emit('toggle-active', { ...props.topic, isActive: value })
}

const handleViewLessons = () => {
  console.log('View lessons clicked for topic:', props.topic.id)
  emit('view-lessons', props.topic)
}

const handleAddLesson = () => {
  console.log('Add lesson clicked for topic:', props.topic.id)
  emit('add-lesson', props.topic)
}

const getLevelColor = (level) => {
  const colors = {
    BEGINNER: 'success',
    ELEMENTARY: 'primary',
    INTERMEDIATE: 'warning',
    UPPER_INTERMEDIATE: 'danger',
    ADVANCED: 'danger',
  }
  return colors[level] || 'info'
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  try {
    return formatDistanceToNow(new Date(date), {
      addSuffix: true,
      locale: vi
    })
  } catch (error) {
    console.error('Date format error:', error)
    return 'N/A'
  }
}
</script>

<style scoped>
.topic-card {
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.topic-card.inactive {
  opacity: 0.6;
  border: 2px dashed var(--el-border-color);
}

.topic-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.topic-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-tag {
  font-weight: 600;
}

.order-badge {
  background: var(--el-color-info-light-9);
  color: var(--el-color-info);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-body {
  padding: 12px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topic-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.topic-description {
  color: var(--el-text-color-regular);
  margin: 0 0 12px 0;
  line-height: 1.5;
  min-height: 40px;
  font-size: 14px;
}

.stats {
  display: flex;
  gap: 20px;
  margin: 12px 0;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;
  padding-top: 8px;
}

.footer-actions {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

/* Responsive */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
