<template>
  <div class="lesson-player">
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <el-icon class="is-loading" :size="50" color="#409EFF">
        <Loading />
      </el-icon>
    </div>

    <!-- Content -->
    <div v-else-if="lesson" class="space-y-6">
      <!-- Back Button -->
      <el-button :icon="ArrowLeft" @click="$emit('back')">
        Quay lại danh sách
      </el-button>

      <!-- Lesson Info Card -->
      <el-card class="!rounded-2xl !border-2 !border-blue-200 dark:!border-blue-800" shadow="hover">
        <div class="flex items-start justify-between mb-4">
          <div>
            <el-tag :type="difficultyType" effect="dark" size="large" class="!rounded-full !px-4 mb-3">
              {{ difficultyLabel }}
            </el-tag>
            <h2 class="text-3xl font-bold text-gray-800 dark:text-white">
              {{ lesson.title }}
            </h2>
          </div>
          <el-icon :size="50" color="#409EFF"><Headset /></el-icon>
        </div>

        <div class="flex gap-6 text-sm text-gray-600 dark:text-gray-400 mb-4">
          <span class="flex items-center gap-2">
            <el-icon><Timer /></el-icon>
            {{ formatTime(lesson.timeLimitSeconds) }}
          </span>
          <span class="flex items-center gap-2">
            <el-icon><QuestionFilled /></el-icon>
            {{ lesson.questions?.length || 0 }} câu hỏi
          </span>
          <span class="flex items-center gap-2">
            <el-icon><Trophy /></el-icon>
            +{{ lesson.pointsReward }} điểm
          </span>
        </div>

        <!-- Progress Info -->
        <div v-if="lesson.attempts > 0" class="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-xl">
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-600 dark:text-gray-400">Điểm cao nhất:</span>
            <span class="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {{ lesson.scorePercentage?.toFixed(0) || 0 }}%
            </span>
          </div>
          <div class="text-xs text-gray-500 mt-1">Số lần thử: {{ lesson.attempts }}</div>
        </div>
      </el-card>

      <!-- Audio Player -->
      <AudioPlayer
        :audio-url="lesson.audioUrl"
        :allow-unlimited-replay="lesson.allowUnlimitedReplay"
        :max-replay-count="lesson.maxReplayCount"
        :current-play-count="lesson.playCount"
        @play="handlePlay"
      />

      <!-- Transcript Card -->
      <TranscriptViewer
        :transcript="lesson.transcript"
        :transcript-translation="lesson.transcriptTranslation"
        :is-unlocked="lesson.transcriptUnlocked"
        :play-count="lesson.playCount"
        @view="handleViewTranscript"
      />

      <!-- Questions -->
      <QuestionSection
        v-if="!showResult"
        :questions="lesson.questions"
        :answers="answers"
        @update:answers="answers = $event"
        @submit="handleSubmit"
      />

      <!-- Result -->
      <ResultCard
        v-else
        :result="submitResult"
        :lesson="lesson"
        @retry="handleRetry"
        @back="$emit('complete')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { listeningUserAPI } from '@/api'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Loading,
  Headset,
  Timer,
  QuestionFilled,
  Trophy,
} from '@element-plus/icons-vue'
import AudioPlayer from './AudioPlayer.vue'
import TranscriptViewer from './TranscriptViewer.vue'
import QuestionSection from './QuestionSection.vue'
import ResultCard from './ResultCard.vue'

const props = defineProps({
  lessonId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['back', 'complete'])

const loading = ref(false)
const lesson = ref(null)
const answers = ref([])
const showResult = ref(false)
const submitResult = ref(null)

const difficultyType = computed(() => {
  const map = { BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[lesson.value?.difficulty] || 'info'
})

const difficultyLabel = computed(() => {
  const map = { BEGINNER: 'Dễ', INTERMEDIATE: 'Trung bình', ADVANCED: 'Khó' }
  return map[lesson.value?.difficulty] || lesson.value?.difficulty
})

const loadLesson = async () => {
  loading.value = true
  try {
    const response = await listeningUserAPI.getLessonDetail(props.lessonId)
    if (response.data.success) {
      lesson.value = response.data.data
      // Initialize answers
      answers.value = lesson.value.questions?.map(q => ({
        questionId: q.id,
        selectedAnswer: null,
        textAnswer: null,
      })) || []
    }
  } catch (error) {
    console.error('Failed to load lesson:', error)
    ElMessage.error(error.response?.data?.message || 'Không thể tải bài nghe')
    emit('back')
  } finally {
    loading.value = false
  }
}

const handlePlay = async () => {
  try {
    await listeningUserAPI.trackPlay(props.lessonId)
    // Reload để cập nhật playCount và transcriptUnlocked
    await loadLesson()
  } catch (error) {
    console.error('Failed to track play:', error)
  }
}

const handleViewTranscript = async () => {
  try {
    await listeningUserAPI.viewTranscript(props.lessonId)
    ElMessage.success('Đã mở khóa transcript')
  } catch (error) {
    ElMessage.error('Chưa thể xem transcript')
  }
}

const handleSubmit = async () => {
  try {
    const response = await listeningUserAPI.submitLesson(props.lessonId, { answers: answers.value })
    if (response.data.success) {
      submitResult.value = response.data.data
      showResult.value = true
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'Lỗi khi nộp bài')
  }
}

const handleRetry = () => {
  showResult.value = false
  submitResult.value = null
  loadLesson()
}

const formatTime = (seconds) => {
  if (!seconds) return '0 phút'
  const minutes = Math.floor(seconds / 60)
  return `${minutes} phút`
}

onMounted(() => {
  loadLesson()
})
</script>
