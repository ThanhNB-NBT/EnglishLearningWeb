<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-900">
    <!-- Header -->
    <div class="p-4 border-b border-gray-200 dark:border-gray-700">
      <h3 class="font-semibold text-gray-800 dark:text-gray-100">Danh sách bài học</h3>
      <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">{{ lessons.length }} bài học</p>
    </div>

    <!-- Lesson List -->
    <div class="flex-1 overflow-y-auto p-3">
      <ul v-if="lessons.length" class="space-y-2">
        <li v-for="(lesson, index) in lessons" :key="lesson.id">
          <button
            class="w-full text-left px-3 py-3 rounded text-sm transition-colors flex items-center gap-3"
            :class="getLessonClass(lesson)"
            :disabled="!lesson.isUnlocked"
            @click="lesson.isUnlocked && $emit('select-lesson', lesson.id)"
          >
            <!-- Number Badge -->
            <div
              class="w-7 h-7 rounded flex items-center justify-center shrink-0 font-medium text-xs"
              :class="getNumberClass(lesson)"
            >
              {{ index + 1 }}
            </div>

            <!-- Title -->
            <span class="truncate flex-1">{{ lesson.title }}</span>

            <!-- Status Icon -->
            <el-icon v-if="lesson.isCompleted" class="text-green-500 shrink-0" :size="18">
              <CircleCheckFilled />
            </el-icon>
            <el-icon v-else-if="!lesson.isUnlocked" class="text-gray-400 shrink-0" :size="16">
              <Lock />
            </el-icon>
          </button>
        </li>
      </ul>

      <div v-else class="flex flex-col items-center justify-center py-12 text-gray-400">
        <el-icon class="is-loading mb-2" :size="28"><Loading /></el-icon>
        <p class="text-xs">Đang tải...</p>
      </div>
    </div>

    <!-- Footer -->
    <div class="p-4 border-t border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between text-xs">
        <span class="text-gray-600 dark:text-gray-400">
          Đã hoàn thành: <strong>{{ completedCount }}/{{ lessons.length }}</strong>
        </span>
        <span class="font-semibold text-blue-600 dark:text-blue-400"> {{ completionRate }}% </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, Loading, Lock } from '@element-plus/icons-vue'

const props = defineProps({
  lessons: { type: Array, default: () => [] },
  currentLessonId: { type: [Number, String], default: null },
})

defineEmits(['select-lesson'])

const completedCount = computed(() => props.lessons.filter((l) => l.isCompleted).length)
const completionRate = computed(() => {
  if (props.lessons.length === 0) return 0
  return Math.round((completedCount.value / props.lessons.length) * 100)
})

const getLessonClass = (lesson) => {
  const isActive = lesson.id === props.currentLessonId

  if (isActive) {
    return 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 font-medium'
  }

  if (!lesson.isUnlocked) {
    return 'bg-gray-50 dark:bg-gray-800 text-gray-400 cursor-not-allowed'
  }

  if (lesson.isCompleted) {
    return 'hover:bg-gray-50 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-300'
  }

  return 'hover:bg-gray-50 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-300'
}

const getNumberClass = (lesson) => {
  const isActive = lesson.id === props.currentLessonId

  if (isActive) {
    return 'bg-blue-600 text-white'
  }

  if (!lesson.isUnlocked) {
    return 'bg-gray-200 dark:bg-gray-700 text-gray-400'
  }

  if (lesson.isCompleted) {
    return 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400'
  }

  return 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
}
</script>

<style scoped>
/* Minimalist scrollbar */
div::-webkit-scrollbar {
  width: 4px;
}

div::-webkit-scrollbar-track {
  background: transparent;
}

div::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

div::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

html.dark div::-webkit-scrollbar-thumb {
  background: #4b5563;
}

html.dark div::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}
</style>
