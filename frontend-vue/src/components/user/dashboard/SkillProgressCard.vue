<template>
  <div class="skill-card">
    <div class="skill-header">
      <div class="skill-title-area">
        <span class="skill-icon">{{ icon }}</span>
        <span class="skill-name">{{ title }}</span>
      </div>
      <el-tag size="small" :type="getLevelTagType(data.level)" effect="light">
        {{ translateLevel(data.level) }}
      </el-tag>
    </div>

    <div class="skill-body">
      <!-- Progress -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">Tiến độ (Progress)</span>
          <span class="progress-value">{{ data.completed }}/{{ data.total }}</span>
        </div>
        <el-progress
          :percentage="getProgressPercentage"
          :stroke-width="8"
          :color="getColor"
        />
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <!-- Accuracy -->
        <div class="stat-item">
          <span class="stat-label">Độ chính xác (Accuracy)</span>
          <span class="stat-value" :style="{ color: getAccuracyColor }">
            {{ formatAccuracy(data.accuracy) }}%
          </span>
        </div>

        <!-- Rank (nếu có) hoặc Level -->
        <div class="stat-item">
          <span class="stat-label">Cấp độ (Level)</span>
          <span class="stat-value" :style="{ color: getLevelColor }">
            {{ translateLevel(data.level) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  icon: {
    type: String,
    required: true,
  },
  data: {
    type: Object,
    required: true,
  },
  color: {
    type: String,
    default: 'blue',
  },
})

const getProgressPercentage = computed(() => {
  if (!props.data.total || props.data.total === 0) return 0
  return Math.round((props.data.completed / props.data.total) * 100)
})

const getColor = computed(() => {
  const colors = {
    blue: '#3b82f6',
    yellow: '#f59e0b',
    red: '#ef4444',
    green: '#10b981',
    purple: '#8b5cf6',
  }
  return colors[props.color] || colors.blue
})

/**
 * ✅ Format accuracy (handle null/undefined)
 */
const formatAccuracy = (accuracy) => {
  if (accuracy == null || accuracy === undefined) return '0.0'
  return accuracy.toFixed(1)
}

const getAccuracyColor = computed(() => {
  const accuracy = props.data.accuracy || 0
  if (accuracy >= 85) return '#10b981' // Green
  if (accuracy >= 70) return '#f59e0b' // Yellow
  return '#ef4444' // Red
})

const getLevelColor = computed(() => {
  const level = props.data.level || 'BEGINNER'
  if (level.includes('ADVANCED')) return '#8b5cf6' // Purple
  if (level.includes('INTERMEDIATE')) return '#3b82f6' // Blue
  return '#10b981' // Green
})

/**
 * ✅ Translate level to Vietnamese
 */
const translateLevel = (level) => {
  if (!level) return 'Mới bắt đầu'

  const translations = {
    'BEGINNER': 'Mới bắt đầu',
    'ELEMENTARY': 'Sơ cấp',
    'INTERMEDIATE': 'Trung cấp',
    'UPPER_INTERMEDIATE': 'Khá',
    'ADVANCED': 'Nâng cao',
  }

  return translations[level] || level
}

const getLevelTagType = (level) => {
  if (!level) return 'info'
  if (level.includes('BEGINNER') || level.includes('ELEMENTARY')) return 'success'
  if (level.includes('INTERMEDIATE')) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.skill-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 20px;
  transition: all 0.3s ease;
}

.skill-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.skill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.skill-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skill-icon {
  font-size: 20px;
}

.skill-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.skill-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.progress-value {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
