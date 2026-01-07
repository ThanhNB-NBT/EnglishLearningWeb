<template>
  <div class="w-full">
    <div class="flex items-start justify-between border-b border-gray-200 dark:border-gray-700 pb-5 mb-5">
      <div class="flex gap-4">
        <div class="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/50 rounded-lg flex items-center justify-center font-bold text-emerald-600 dark:text-emerald-400 text-lg">
          #{{ lesson.orderIndex }}
        </div>
        <div>
          <h2 class="text-xl font-bold text-gray-900 dark:text-white mb-1">{{ lesson.title }}</h2>
          <div class="flex items-center gap-3">
            <span class="text-xs text-gray-500 flex items-center gap-1">
              <el-icon><Calendar /></el-icon> {{ formatDate(lesson.createdAt) }}
            </span>
            <el-tag v-if="lesson.topicName" type="info" size="small" effect="plain">
              {{ lesson.topicName }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-tag :type="lesson.isActive ? 'success' : 'danger'" effect="dark" class="!rounded-full px-3">
        {{ lesson.isActive ? 'Active' : 'Inactive' }}
      </el-tag>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg border border-blue-100 dark:border-blue-800 flex items-center gap-3">
        <el-icon class="text-blue-500 text-xl"><Timer /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Thời lượng</div>
          <div class="font-bold text-blue-700 dark:text-blue-300">{{ formatTime(lesson.timeLimitSeconds) }}</div>
        </div>
      </div>

      <div class="bg-orange-50 dark:bg-orange-900/20 p-3 rounded-lg border border-orange-100 dark:border-orange-800 flex items-center gap-3">
        <el-icon class="text-orange-500 text-xl"><Trophy /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Điểm thưởng</div>
          <div class="font-bold text-orange-700 dark:text-orange-300">+{{ lesson.pointsReward }}</div>
        </div>
      </div>

      <div class="bg-purple-50 dark:bg-purple-900/20 p-3 rounded-lg border border-purple-100 dark:border-purple-800 flex items-center gap-3">
        <el-icon class="text-purple-500 text-xl"><QuestionFilled /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Câu hỏi</div>
          <div class="font-bold text-purple-700 dark:text-purple-300">{{ lesson.questionCount || 0 }}</div>
        </div>
      </div>

      <div class="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700 flex items-center gap-3">
        <el-icon class="text-gray-500 text-xl"><Document /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">ID</div>
          <div class="font-bold text-gray-700 dark:text-gray-300">{{ lesson.id }}</div>
        </div>
      </div>
    </div>

    <el-tabs type="border-card" class="mb-4 shadow-sm !rounded-xl !border-gray-200 dark:!border-gray-700 dark:!bg-[#1d1d1d]">
      <el-tab-pane label="Nội dung bài đọc">
        <div v-if="lesson.content" class="ql-editor !p-0 text-gray-800 dark:text-gray-200" v-html="lesson.content"></div>
        <el-empty v-else description="Chưa có nội dung" :image-size="60" />
      </el-tab-pane>

      <el-tab-pane label="Dịch nghĩa">
        <div v-if="lesson.contentTranslation" class="ql-editor !p-0 text-gray-800 dark:text-gray-200" v-html="lesson.contentTranslation"></div>
        <el-empty v-else description="Chưa có bản dịch" :image-size="60" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { Timer, Trophy, Calendar, QuestionFilled, Document } from '@element-plus/icons-vue'
import 'quill/dist/quill.snow.css'

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
