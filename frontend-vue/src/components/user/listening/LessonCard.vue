<template>
  <el-card
    :body-style="{ padding: '0' }"
    shadow="hover"
    class="lesson-card !rounded-2xl !border-2 transition-all duration-300 cursor-pointer overflow-hidden"
    :class="cardClasses"
    @click="handleClick"
  >
    <!-- Header with difficulty badge -->
    <div class="p-6 pb-4">
      <div class="flex items-start justify-between mb-3">
        <el-tag
          :type="difficultyType"
          effect="dark"
          size="large"
          class="!rounded-full !px-4 !py-1 !font-bold"
        >
          {{ difficultyLabel }}
        </el-tag>

        <div class="flex items-center gap-2">
          <el-icon :size="20" :color="iconColor"><Headset /></el-icon>
          <span class="text-sm font-bold" :style="{ color: iconColor }">
            {{ lesson.orderIndex }}
          </span>
        </div>
      </div>

      <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-2 line-clamp-2">
        {{ lesson.title }}
      </h3>

      <!-- Stats -->
      <div class="flex items-center gap-4 text-sm text-gray-600 dark:text-gray-400 mb-4">
        <span class="flex items-center gap-1">
          <el-icon><QuestionFilled /></el-icon>
          {{ lesson.questionCount || 0 }} câu
        </span>
        <span class="flex items-center gap-1">
          <el-icon><Trophy /></el-icon>
          +{{ lesson.pointsReward }}đ
        </span>
      </div>

      <!-- Progress Bar -->
      <div v-if="lesson.isCompleted || lesson.attempts > 0" class="mb-4">
        <div class="flex justify-between items-center mb-1">
          <span class="text-xs text-gray-500">Điểm cao nhất</span>
          <span class="text-xs font-bold" :class="scoreClass">
            {{ lesson.scorePercentage?.toFixed(0) || 0 }}%
          </span>
        </div>
        <el-progress
          :percentage="parseFloat(lesson.scorePercentage || 0)"
          :color="progressColor"
          :stroke-width="8"
          :show-text="false"
        />
      </div>
    </div>

    <!-- Footer -->
    <div class="px-6 py-4 bg-gray-50 dark:bg-gray-800/50 border-t border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <el-icon
            v-if="lesson.isCompleted"
            :size="20"
            color="#67C23A"
            class="animate-bounce"
          >
            <CircleCheck />
          </el-icon>
          <span
            v-if="lesson.isCompleted"
            class="text-sm font-bold text-green-600 dark:text-green-400"
          >
            Đã hoàn thành
          </span>
          <span
            v-else-if="lesson.attempts > 0"
            class="text-sm text-gray-600 dark:text-gray-400"
          >
            {{ lesson.attempts }} lần thử
          </span>
          <span
            v-else-if="!lesson.isUnlocked"
            class="text-sm text-gray-400 dark:text-gray-500 flex items-center gap-1"
          >
            <el-icon><Lock /></el-icon> Đã khóa
          </span>
          <span v-else class="text-sm text-blue-600 dark:text-blue-400 font-bold">
            Bắt đầu ngay
          </span>
        </div>

        <el-button
          :type="lesson.isCompleted ? 'success' : 'primary'"
          :icon="lesson.isCompleted ? Refresh : VideoPlay"
          circle
          :disabled="!lesson.isUnlocked"
        />
      </div>
    </div>

    <!-- Lock Overlay -->
    <div
      v-if="!lesson.isUnlocked"
      class="absolute inset-0 bg-gray-900/60 backdrop-blur-sm flex items-center justify-center"
    >
      <div class="text-center text-white">
        <el-icon :size="50" class="mb-2">
          <Lock />
        </el-icon>
        <p class="font-bold">Hoàn thành bài trước</p>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import {
  Headset,
  QuestionFilled,
  Trophy,
  CircleCheck,
  Lock,
  VideoPlay,
  Refresh,
} from '@element-plus/icons-vue'

const props = defineProps({
  lesson: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['select'])

const difficultyType = computed(() => {
  const map = { BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[props.lesson.difficulty] || 'info'
})

const difficultyLabel = computed(() => {
  const map = { BEGINNER: 'Dễ', INTERMEDIATE: 'Trung bình', ADVANCED: 'Khó' }
  return map[props.lesson.difficulty] || props.lesson.difficulty
})

const iconColor = computed(() => {
  const map = { BEGINNER: '#67C23A', INTERMEDIATE: '#E6A23C', ADVANCED: '#F56C6C' }
  return map[props.lesson.difficulty] || '#409EFF'
})

const progressColor = computed(() => {
  const score = parseFloat(props.lesson.scorePercentage || 0)
  if (score >= 80) return '#67C23A'
  if (score >= 50) return '#E6A23C'
  return '#F56C6C'
})

const scoreClass = computed(() => {
  const score = parseFloat(props.lesson.scorePercentage || 0)
  if (score >= 80) return 'text-green-600 dark:text-green-400'
  if (score >= 50) return 'text-orange-600 dark:text-orange-400'
  return 'text-red-600 dark:text-red-400'
})

const cardClasses = computed(() => {
  if (!props.lesson.isUnlocked) {
    return '!border-gray-300 dark:!border-gray-700 opacity-60 cursor-not-allowed'
  }
  if (props.lesson.isCompleted) {
    return '!border-green-300 dark:!border-green-700 hover:!border-green-400 hover:shadow-xl hover:shadow-green-500/20'
  }
  return '!border-blue-300 dark:!border-blue-700 hover:!border-blue-400 hover:shadow-xl hover:shadow-blue-500/20'
})

const handleClick = () => {
  if (props.lesson.isUnlocked) {
    emit('select', props.lesson.id)
  }
}
</script>

<style scoped>
.lesson-card {
  position: relative;
  transform: translateY(0);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.lesson-card:hover:not(.cursor-not-allowed) {
  transform: translateY(-8px);
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(-25%);
    animation-timing-function: cubic-bezier(0.8, 0, 1, 1);
  }
  50% {
    transform: translateY(0);
    animation-timing-function: cubic-bezier(0, 0, 0.2, 1);
  }
}

.animate-bounce {
  animation: bounce 1s infinite;
}
</style>
