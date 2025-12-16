<template>
  <div
    v-if="currentLesson"
    class="flex flex-col h-screen bg-gray-50 dark:bg-[#0a0a0a] font-sans overflow-hidden"
  >
    <!-- Header -->
    <header
      class="h-14 bg-white dark:bg-[#1e1e1e] border-b border-gray-200 dark:border-gray-800 flex items-center justify-between px-4 shadow-sm z-40 shrink-0"
    >
      <div class="flex items-center gap-3">
        <el-button @click="goBack" circle text>
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </el-button>
        <h1 class="text-base font-bold text-gray-800 dark:text-gray-100 truncate max-w-[400px]">
          {{ currentLesson?.title || 'Đang tải...' }}
        </h1>
      </div>

      <div class="flex items-center gap-3">
        <div
          v-if="!isSubmitted && currentLesson"
          class="flex items-center gap-2 px-3 py-1.5 bg-orange-50 dark:bg-orange-900/20 rounded border border-orange-200 dark:border-orange-800 font-mono text-sm font-bold"
          :class="timerClass"
        >
          <el-icon><Timer /></el-icon> {{ formatTime(remainingTime) }}
        </div>

        <button
          class="text-gray-600 dark:text-gray-400 hover:text-black dark:hover:text-white transition-colors p-2"
          @click="toggleSidebar"
        >
          <el-icon :size="22"><Menu /></el-icon>
        </button>
      </div>
    </header>

    <div class="flex-1 flex overflow-hidden relative">
      <div
        v-if="loadingLesson"
        v-loading="loadingLesson"
        class="absolute inset-0 z-50 bg-white/80 dark:bg-black/80 backdrop-blur-sm"
      ></div>

      <main class="flex-1 flex min-w-0">
        <!-- Content Area -->
        <div
          class="flex-1 flex flex-col h-full bg-white dark:bg-[#1e1e1e] border-r border-gray-200 dark:border-gray-800"
        >
          <!-- Toolbar -->
          <div
            class="flex items-center justify-between px-6 h-12 border-b border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-[#252525]"
          >
            <span class="text-xs font-bold uppercase tracking-wider text-gray-500"
              >Nội dung bài nghe</span
            >
          </div>

          <!-- Audio & Transcript Content -->
          <div class="flex-1 overflow-y-auto custom-scrollbar">
            <div class="max-w-4xl mx-auto">
              <!-- Audio Player -->
              <div
                class="bg-gray-50 dark:bg-gray-900 p-4 border dark:border-gray-700"
              >
                <div
                  class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2"
                >
                  <el-icon :size="18"><Headset /></el-icon>
                  Audio
                </div>

                <!-- Native HTML5 Audio -->
                <audio
                  ref="audioRef"
                  :src="audioSrc"
                  controls
                  controlsList="nodownload"
                  @play="handlePlayStart"
                  class="w-full"
                  style="height: 40px"
                >
                  Trình duyệt không hỗ trợ audio.
                </audio>

                <!-- Play Count Info -->
                <div
                  v-if="!currentLesson?.allowUnlimitedReplay"
                  class="text-xs text-gray-500 dark:text-gray-400 mt-2 text-center"
                >
                  Đã nghe: {{ currentLesson?.playCount || 0 }} / {{ currentLesson?.maxReplayCount }}
                </div>
              </div>

              <!-- Transcript -->
              <div
                class="bg-white dark:bg-[#1e1e1e] border border-gray-200 dark:border-gray-800"
              >
                <div class="flex items-center justify-between mb-4 p-2">
                  <h3 class="font-bold text-gray-800 dark:text-white flex items-center gap-2">
                    <el-icon><Document /></el-icon>
                    Transcript
                  </h3>
                  <el-button
                    v-if="!currentLesson?.transcriptUnlocked"
                    type="warning"
                    size="small"
                    @click="handleViewTranscript"
                    :disabled="(currentLesson?.playCount || 0) === 0"
                  >
                    Mở khóa
                  </el-button>
                  <el-tag v-else type="success">Đã mở khóa</el-tag>
                </div>

                <div
                  v-if="currentLesson?.transcriptUnlocked"
                  class="bg-gray-50 dark:bg-gray-900 rounded p-4 text-sm text-gray-700 dark:text-gray-300 leading-relaxed max-h-96 overflow-y-auto"
                >
                  <pre class="whitespace-pre-wrap font-sans">{{ formattedTranscript }}</pre>
                </div>
                <div v-else class="text-center py-12 text-gray-400">
                  <el-icon :size="48"><Lock /></el-icon>
                  <p class="mt-2 text-sm">Hoàn thành bài để mở khóa transcript</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Questions Area -->
        <div
          class="w-full md:w-[50%] lg:w-[55%] flex-shrink-0 flex flex-col bg-white dark:bg-[#121212]"
        >
          <!-- Header -->
          <div
            class="h-12 px-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-[#1e1e1e] shrink-0"
          >
            <span
              class="text-xs font-bold uppercase tracking-wider text-gray-600 dark:text-gray-400"
            >
              Câu hỏi ({{ answeredCount }}/{{ totalQuestions }})
            </span>
            <el-button
              v-if="!isSubmitted"
              type="primary"
              size="small"
              :disabled="answeredCount === 0"
              :loading="submitting"
              @click="handleSubmit(false)"
              class="!font-bold"
            >
              NỘP BÀI
            </el-button>
          </div>

          <div class="flex-1 overflow-y-auto custom-scrollbar p-5">
            <!-- Result Panel -->
            <transition name="el-zoom-in-top">
              <div
                v-if="isSubmitted && resultData"
                class="mb-6 p-4 rounded-lg text-center"
                :class="
                  resultData.isPassed
                    ? 'bg-green-50 dark:bg-green-900/20 border-2 border-green-300 dark:border-green-800'
                    : 'bg-orange-50 dark:bg-orange-900/20 border-2 border-orange-300 dark:border-orange-800'
                "
              >
                <div class="text-xs font-bold text-gray-500 uppercase mb-1">Kết quả</div>
                <div
                  class="text-4xl font-black mb-2"
                  :class="resultData.isPassed ? 'text-green-600' : 'text-orange-500'"
                >
                  {{ resultData.scorePercentage?.toFixed(0) }}%
                </div>
                <div class="flex justify-center gap-2 mb-3">
                  <el-tag :type="resultData.isPassed ? 'success' : 'warning'" effect="dark">
                    {{ resultData.isPassed ? 'ĐẠT' : 'CHƯA ĐẠT' }}
                  </el-tag>
                </div>
                <div class="flex justify-center gap-2">
                  <el-button size="small" @click="retryLesson" :icon="Refresh">Làm lại</el-button>
                  <el-button size="small" type="primary" @click="goToNextLesson">
                    Tiếp tục <el-icon class="ml-1"><Right /></el-icon>
                  </el-button>
                </div>
              </div>
            </transition>

            <!-- Questions List -->
            <div class="space-y-6">
              <div
                v-for="(q, idx) in currentLesson?.questions"
                :key="q.id"
                class="question-simple-item"
              >
                <!-- Question Number -->
                <div class="flex items-start gap-3 mb-3">
                  <div
                    class="w-7 h-7 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0"
                    :class="
                      isSubmitted
                        ? isCorrect(q.id)
                          ? 'bg-green-100 text-green-700 border-2 border-green-500 dark:bg-green-900/30 dark:text-green-400'
                          : 'bg-red-100 text-red-700 border-2 border-red-500 dark:bg-red-900/30 dark:text-red-400'
                        : 'bg-gray-100 text-gray-700 border border-gray-300 dark:bg-gray-800 dark:text-gray-300'
                    "
                  >
                    {{ idx + 1 }}
                  </div>

                  <div class="flex-1 min-w-0">
                    <!-- Question Text -->
                    <div
                      v-if="q.questionText"
                      class="text-sm text-gray-900 dark:text-gray-100 leading-relaxed mb-3 font-medium"
                      v-html="q.questionText"
                    ></div>

                    <!-- Answer Component -->
                    <component
                      :is="getQuestionComponent(q.questionType)"
                      :question="q"
                      v-model="userAnswers[q.id]"
                      :disabled="isSubmitted"
                      :showFeedback="isSubmitted"
                    />

                    <!-- Feedback -->
                    <div
                      v-if="isSubmitted && q.explanation"
                      class="mt-3 p-2 rounded text-xs"
                      :class="
                        isCorrect(q.id)
                          ? 'bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 text-green-800 dark:text-green-300'
                          : 'bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-300'
                      "
                    >
                      <span class="font-semibold">Giải thích:</span> {{ q.explanation }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- Sidebar -->
      <transition name="slide-right">
        <aside
          v-if="isSidebarOpen"
          class="absolute right-0 top-0 bottom-0 w-72 bg-white dark:bg-[#1a1a1a] border-l border-gray-200 dark:border-gray-800 z-40 flex flex-col shadow-2xl"
        >
          <div
            class="h-12 px-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-[#252525]"
          >
            <span class="font-bold text-sm uppercase text-gray-700 dark:text-gray-200"
              >Danh sách bài</span
            >
            <button
              @click="toggleSidebar"
              class="text-gray-500 hover:text-gray-900 dark:hover:text-white"
            >
              ✕
            </button>
          </div>

          <div class="flex-1 overflow-y-auto custom-scrollbar">
            <div
              v-for="(item, idx) in allLessons"
              :key="item.id"
              @click="handleSelectLesson(item)"
              class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-[#252525] transition-colors"
              :class="{
                'bg-blue-50 dark:bg-blue-900/20 font-bold': currentLesson?.id === item.id,
              }"
            >
              <div class="flex gap-2 items-start">
                <span class="text-gray-500 text-xs mt-0.5 shrink-0">{{ idx + 1 }}.</span>
                <span class="text-sm flex-1">{{ item.title }}</span>
                <span v-if="item.isCompleted" class="text-green-600 shrink-0">✓</span>
                <span v-else-if="!item.isUnlocked" class="text-gray-400 shrink-0">🔒</span>
              </div>
            </div>
          </div>
        </aside>
      </transition>

      <div
        v-if="isSidebarOpen"
        class="absolute inset-0 bg-black/20 z-30 lg:hidden"
        @click="toggleSidebar"
      ></div>
    </div>
  </div>

  <div v-else class="flex items-center justify-center h-screen">
    <el-icon class="is-loading" :size="40"><Loading /></el-icon>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listeningUserAPI } from '@/api'
import {
  ArrowLeft,
  Menu,
  Timer,
  Headset,
  Document,
  Lock,
  Refresh,
  Right,
  Loading,
} from '@element-plus/icons-vue'
import apiClient from '@/api/config'
import { ElMessage } from 'element-plus'

import { useListeningPlayer } from '@/composables/listening/useListeningPlayer'
import { formatTranscript } from '@/utils/textFormatter'

import QuestionFillBlank from '@/components/user/questions/QuestionFillBlank.vue'
import QuestionMultipleChoice from '@/components/user/questions/QuestionMultipleChoice.vue'
import QuestionTextAnswer from '@/components/user/questions/QuestionTextAnswer.vue'

const route = useRoute()
const router = useRouter()

const {
  loadingLesson,
  currentLesson,
  userAnswers,
  isSubmitted,
  submitting,
  resultData,
  remainingTime,
  hasPlayedOnce,
  totalQuestions,
  answeredCount,
  timerClass,
  loadLesson,
  handleSubmit,
  handlePlay,
  handleViewTranscript,
  isCorrect,
  retryLesson,
  formatTime,
  cleanup,
} = useListeningPlayer()

// Local state (UI only)
const allLessons = ref([])
const isSidebarOpen = ref(true)
const audioRef = ref(null)

// Computed - Audio source
const audioSrc = computed(() => {
  if (!currentLesson.value?.audioUrl) return ''
  const SERVER_HOST = apiClient.defaults.baseURL || 'http://localhost:8980'
  const url = currentLesson.value.audioUrl
  if (url.startsWith('http')) return url
  const cleanPath = url.startsWith('/') ? url.substring(1) : url
  return cleanPath.startsWith('media/')
    ? `${SERVER_HOST}/${cleanPath}`
    : `${SERVER_HOST}/media/${cleanPath}`
})

// Computed - Formatted transcript
const formattedTranscript = computed(() => {
  return formatTranscript(currentLesson.value?.transcript || '')
})

const loadAllLessons = async () => {
  try {
    const res = await listeningUserAPI.getLessons()
    if (res.data.success) {
      allLessons.value = res.data.data || []

      // Nếu không có lessonId trong route, redirect đến lesson đầu tiên
      if (!route.params.lessonId && allLessons.value.length > 0) {
        const firstLesson = allLessons.value.find((l) => l.isUnlocked) || allLessons.value[0]
        router.replace({ name: 'user-listening-player', params: { lessonId: firstLesson.id } })
      }
    }
  } catch (error) {
    console.error('Failed to load lessons:', error)
    ElMessage.error('Không thể tải danh sách bài học')
  }
}

const handlePlayStart = () => {
  if (!hasPlayedOnce.value) {
    hasPlayedOnce.value = true
    handlePlay()
  }
}

const handleSelectLesson = (lesson) => {
  if (lesson.isUnlocked) {
    router.push({ name: 'user-listening-player', params: { lessonId: lesson.id } })
  } else {
    ElMessage.warning('Bài học này chưa được mở khóa')
  }
}

const goToNextLesson = () => {
  const currentIndex = allLessons.value.findIndex((l) => l.id === currentLesson.value.id)
  const nextLesson = allLessons.value[currentIndex + 1]
  if (nextLesson && nextLesson.isUnlocked) {
    handleSelectLesson(nextLesson)
  } else {
    ElMessage.info('Đã hoàn thành tất cả bài học!')
  }
}

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const goBack = () => router.push({ name: 'home' })

const getQuestionComponent = (type) => {
  const t = type?.toUpperCase()
  if (['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(t)) return QuestionMultipleChoice
  if (['FILL_BLANK', 'LISTENING_COMPREHENSION'].includes(t)) return QuestionFillBlank
  return QuestionTextAnswer
}

watch(
  () => route.params.lessonId,
  (newId) => {
    if (newId) {
      hasPlayedOnce.value = false
      loadLesson(newId)
    }
  },
)

onMounted(async () => {
  await loadAllLessons()

  const lessonId = route.params.lessonId
  if (lessonId) {
    await loadLesson(lessonId)
  }

  if (window.innerWidth < 1280) isSidebarOpen.value = false
})

onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
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

.question-simple-item {
  padding-bottom: 1.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.question-simple-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

:global(.dark) .question-simple-item {
  border-bottom-color: #374151;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}
.slide-right-enter-from {
  transform: translateX(100%);
}
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
