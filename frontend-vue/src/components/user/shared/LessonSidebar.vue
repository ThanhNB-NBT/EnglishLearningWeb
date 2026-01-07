<template>
  <div class="h-full flex flex-col bg-gradient-to-b from-white to-gray-50 dark:from-[#161616] dark:to-[#0f0f0f]">
    <!-- Header -->
    <div class="p-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1a1a1a]">
      <div class="flex items-center gap-2 mb-1">
        <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-sm">
          <el-icon class="text-white" :size="16"><Reading /></el-icon>
        </div>
        <h3 class="font-bold text-gray-800 dark:text-gray-100 text-sm">Bài học</h3>
      </div>
      <p class="text-xs text-gray-500 dark:text-gray-400">{{ lessons.length }} bài</p>
    </div>

    <!-- Lesson List -->
    <div class="flex-1 overflow-y-auto custom-scrollbar p-3">
      <ul v-if="lessons.length" class="space-y-1.5">
        <li v-for="(lesson, index) in lessons" :key="lesson.id">
          <button
            class="w-full text-left px-3 py-2.5 rounded-lg text-sm transition-all duration-200 flex items-center gap-2.5 group border"
            :class="getLessonClass(lesson)"
            @click="$emit('select-lesson', lesson.id)"
          >
            <!-- Number -->
            <div
              class="w-6 h-6 rounded-md flex items-center justify-center shrink-0 font-bold text-xs"
              :class="getNumberClass(lesson)"
            >
              {{ index + 1 }}
            </div>

            <!-- Title -->
            <span class="truncate flex-1 font-medium">
              {{ lesson.title }}
            </span>

            <!-- Status -->
            <el-icon
              v-if="lesson.isCompleted"
              class="text-green-500 shrink-0"
              :size="18"
            >
              <CircleCheckFilled />
            </el-icon>
            <el-icon
              v-else-if="!lesson.isUnlocked"
              class="text-gray-400 shrink-0"
              :size="16"
            >
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
    <div class="p-3 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1a1a1a]">
      <div class="flex items-center justify-between text-xs">
        <span class="text-gray-600 dark:text-gray-400">
          Hoàn thành: <strong class="text-green-600">{{ completedCount }}/{{ lessons.length }}</strong>
        </span>
        <div class="px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-600 font-bold">
          {{ completionRate }}%
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, Loading, Lock, Reading } from '@element-plus/icons-vue'

const props = defineProps({
  lessons: { type: Array, default: () => [] },
  currentLessonId: { type: [Number, String], default: null }
})

defineEmits(['select-lesson'])

const completedCount = computed(() => props.lessons.filter(l => l.isCompleted).length)
const completionRate = computed(() => {
  if (props.lessons.length === 0) return 0
  return Math.round((completedCount.value / props.lessons.length) * 100)
})

const getLessonClass = (lesson) => {
  const isActive = lesson.id === props.currentLessonId

  if (isActive) {
    return 'bg-blue-50 dark:bg-blue-900/20 border-blue-400 dark:border-blue-600 text-blue-700 dark:text-blue-400'
  }

  if (!lesson.isUnlocked) {
    return 'bg-gray-50 dark:bg-gray-800/30 border-gray-200 dark:border-gray-700 text-gray-400 opacity-60 cursor-not-allowed'
  }

  if (lesson.isCompleted) {
    return 'bg-white dark:bg-gray-800/50 border-green-200 dark:border-green-700 text-gray-700 dark:text-gray-300 hover:border-green-300 hover:bg-green-50 dark:hover:bg-green-900/10'
  }

  return 'bg-white dark:bg-gray-800/50 border-gray-200 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:border-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/10'
}

const getNumberClass = (lesson) => {
  const isActive = lesson.id === props.currentLessonId

  if (isActive) {
    return 'bg-blue-500 text-white shadow-sm'
  }

  if (!lesson.isUnlocked) {
    return 'bg-gray-200 dark:bg-gray-700 text-gray-400'
  }

  if (lesson.isCompleted) {
    return 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
  }

  return 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'
}
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

html.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #374151;
}
</style>
