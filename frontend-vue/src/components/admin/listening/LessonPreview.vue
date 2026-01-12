<template>
  <div class="w-full">
    <!-- Header Section -->
    <div
      class="flex items-start justify-between border-b border-gray-200 dark:border-gray-700 pb-5 mb-5"
    >
      <div class="flex gap-4">
        <div
          class="w-12 h-12 bg-indigo-100 dark:bg-indigo-900/50 rounded-lg flex items-center justify-center font-bold text-indigo-600 dark:text-indigo-400 text-lg"
        >
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

      <el-tag
        :type="lesson.isActive ? 'success' : 'danger'"
        effect="dark"
        class="!rounded-full px-3"
      >
        {{ lesson.isActive ? 'Active' : 'Inactive' }}
      </el-tag>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <!-- Time Limit -->
      <div
        class="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg border border-blue-100 dark:border-blue-800 flex items-center gap-3"
      >
        <el-icon class="text-blue-500 text-xl"><Timer /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Thời lượng</div>
          <div class="font-bold text-blue-700 dark:text-blue-300">
            {{ formatTime(lesson.timeLimitSeconds) }}
          </div>
        </div>
      </div>

      <!-- Points Reward -->
      <div
        class="bg-orange-50 dark:bg-orange-900/20 p-3 rounded-lg border border-orange-100 dark:border-orange-800 flex items-center gap-3"
      >
        <el-icon class="text-orange-500 text-xl"><Trophy /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Điểm thưởng</div>
          <div class="font-bold text-orange-700 dark:text-orange-300">
            +{{ lesson.pointsReward }}
          </div>
        </div>
      </div>

      <!-- Question Count -->
      <div
        class="bg-purple-50 dark:bg-purple-900/20 p-3 rounded-lg border border-purple-100 dark:border-purple-800 flex items-center gap-3"
      >
        <el-icon class="text-purple-500 text-xl"><QuestionFilled /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Câu hỏi</div>
          <div class="font-bold text-purple-700 dark:text-purple-300">
            {{ lesson.questionCount || 0 }}
          </div>
        </div>
      </div>

      <!-- Replay Count -->
      <div
        class="bg-teal-50 dark:bg-teal-900/20 p-3 rounded-lg border border-teal-100 dark:border-teal-800 flex items-center gap-3"
      >
        <el-icon class="text-teal-500 text-xl"><VideoPlay /></el-icon>
        <div>
          <div class="text-xs text-gray-500 dark:text-gray-400">Lượt nghe</div>
          <div class="font-bold text-teal-700 dark:text-teal-300">
            {{ lesson.allowUnlimitedReplay ? 'Vô hạn' : lesson.maxReplayCount }}
          </div>
        </div>
      </div>
    </div>

    <!-- Audio Player Section -->
    <div
      v-if="lesson.audioUrl"
      class="mb-6 p-4 bg-gray-50 dark:bg-[#1a1a1a] rounded-xl border border-gray-200 dark:border-gray-700"
    >
      <div
        class="flex items-center gap-2 mb-3 text-gray-800 dark:text-white font-bold text-sm uppercase tracking-wide"
      >
        <el-icon><Headset /></el-icon> Audio Bài Học
      </div>

      <!-- DEBUG INFO (Optional - remove in production) -->
      <div
        class="mb-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded border border-blue-200 dark:border-blue-700"
      >
        <div class="text-xs font-mono space-y-1">
          <div><span class="font-bold">URL:</span> {{ fullAudioUrl }}</div>
          <div>
            <span class="font-bold">State:</span>
            <span v-if="audioLoading" class="text-blue-600">⏳ Loading...</span>
            <span v-else-if="audioLoaded" class="text-green-600">✅ Ready</span>
            <span v-else-if="audioError" class="text-red-600">❌ Error</span>
            <span v-else class="text-gray-500">Idle</span>
          </div>
        </div>

        <!-- Manual Reload Button -->
        <button
          @click="reloadAudio"
          class="mt-2 px-3 py-1 bg-blue-500 hover:bg-blue-600 text-white text-xs rounded transition"
        >
          🔄 Reload Audio
        </button>
      </div>

      <!-- Audio Player -->
      <audio
        :key="audioKey"
        ref="audioRef"
        controls
        preload="metadata"
        class="w-full h-10"
        @error="handleAudioError"
        @loadedmetadata="handleAudioLoaded"
        @loadstart="handleAudioLoadStart"
        @canplay="handleCanPlay"
      >
        <source :src="fullAudioUrl" type="audio/mpeg" />
        <source :src="fullAudioUrl" type="audio/wav" />
        <source :src="fullAudioUrl" type="audio/mp3" />
        Trình duyệt của bạn không hỗ trợ audio element.
      </audio>

      <!-- Loading State -->
      <div v-if="audioLoading" class="mt-2 flex items-center gap-2 text-xs text-blue-500">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>Đang tải audio... {{ loadingProgress }}</span>
      </div>

      <!-- Success State -->
      <div
        v-if="audioLoaded && !audioError"
        class="mt-2 flex items-center gap-2 text-xs text-green-500"
      >
        <el-icon><CircleCheck /></el-icon>
        <span>Audio đã sẵn sàng - Thời lượng: {{ audioDuration }}</span>
      </div>

      <!-- Error State -->
      <div
        v-if="audioError"
        class="mt-2 p-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded text-xs text-red-600 dark:text-red-400"
      >
        <div class="flex items-center gap-2 font-semibold mb-1">
          <el-icon><CircleClose /></el-icon>
          <span>Không thể load audio</span>
        </div>
        <div class="ml-5">{{ audioError }}</div>
        <div class="ml-5 mt-1 text-gray-500">
          Kiểm tra: URL có đúng? File có tồn tại? CORS có được cấu hình?
        </div>
      </div>
    </div>

    <!-- No Audio Warning -->
    <div
      v-else
      class="mb-6 p-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-xl border border-yellow-200 dark:border-yellow-800"
    >
      <div class="flex items-center gap-2 text-yellow-700 dark:text-yellow-400">
        <el-icon><Warning /></el-icon>
        <span class="text-sm font-semibold">Bài học chưa có file audio</span>
      </div>
    </div>

    <!-- Transcript & Translation Tabs -->
    <el-tabs
      type="border-card"
      class="mb-4 shadow-sm !rounded-xl !border-gray-200 dark:!border-gray-700 dark:!bg-[#1d1d1d]"
    >
      <el-tab-pane label="Transcript (Lời thoại)">
        <div
          v-if="lesson.transcript"
          class="prose dark:prose-invert max-w-none text-gray-800 dark:text-gray-200 p-4 whitespace-pre-wrap"
        >
          {{ lesson.transcript }}
        </div>
        <el-empty v-else description="Chưa có lời thoại" :image-size="60" />
      </el-tab-pane>

      <el-tab-pane label="Dịch nghĩa">
        <div
          v-if="lesson.transcriptTranslation"
          class="prose dark:prose-invert max-w-none text-gray-800 dark:text-gray-200 p-4 whitespace-pre-wrap"
        >
          {{ lesson.transcriptTranslation }}
        </div>
        <el-empty v-else description="Chưa có bản dịch" :image-size="60" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import {
  Timer,
  Trophy,
  Calendar,
  QuestionFilled,
  VideoPlay,
  Headset,
  Warning,
  Loading,
  CircleCheck,
  CircleClose,
} from '@element-plus/icons-vue'

const props = defineProps({
  lesson: { type: Object, required: true },
})

const audioRef = ref(null)
const audioError = ref(null)
const audioLoaded = ref(false)
const audioLoading = ref(false)
const audioKey = ref(0) // Force re-render key
const loadingProgress = ref('')
const audioDuration = ref('0:00')

// ✅ Compute full audio URL
const fullAudioUrl = computed(() => {
  if (!props.lesson?.audioUrl) return ''

  const url = props.lesson.audioUrl.trim()

  // If already full URL (starts with http), use as-is
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // Otherwise, prepend backend base URL
  const backendBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980'
  const cleanUrl = url.startsWith('/') ? url : `/${url}`
  const fullUrl = `${backendBaseUrl}${cleanUrl}`

  console.log('🔗 Audio URL resolved:', { raw: url, full: fullUrl })
  return fullUrl
})

// ✅ Force reload audio
const reloadAudio = () => {
  console.log('🔄 Manually reloading audio...')
  audioKey.value++ // Force component re-render
  audioError.value = null
  audioLoaded.value = false
  audioLoading.value = false
  loadingProgress.value = ''

  nextTick(() => {
    if (audioRef.value) {
      audioRef.value.load()
    }
  })
}

// Watch URL changes and reload
watch(
  fullAudioUrl,
  (newUrl, oldUrl) => {
    if (newUrl && newUrl !== oldUrl) {
      console.log('🔄 Audio URL changed, reloading...', { old: oldUrl, new: newUrl })
      reloadAudio()
    }
  },
  { immediate: false },
)

// Audio event handlers
const handleAudioLoadStart = () => {
  console.log('🔄 Audio loading started:', fullAudioUrl.value)
  audioLoading.value = true
  audioError.value = null
  audioLoaded.value = false
  loadingProgress.value = '(Fetching...)'
}

const handleCanPlay = () => {
  console.log('✅ Audio can play (buffered enough)')
  loadingProgress.value = '(Ready)'
}

const handleAudioLoaded = (e) => {
  console.log('✅ Audio metadata loaded successfully')
  audioLoading.value = false
  audioLoaded.value = true
  audioError.value = null

  const audio = e.target
  if (audio.duration && !isNaN(audio.duration) && isFinite(audio.duration)) {
    const mins = Math.floor(audio.duration / 60)
    const secs = Math.floor(audio.duration % 60)
    audioDuration.value = `${mins}:${secs.toString().padStart(2, '0')}`
    console.log('✅ Duration:', audioDuration.value)
  }
}

const handleAudioError = (e) => {
  console.error('❌ Audio error:', e)
  audioLoading.value = false
  audioLoaded.value = false

  const audio = e.target
  if (audio.error) {
    const errorMessages = {
      1: 'MEDIA_ERR_ABORTED - Người dùng hủy tải',
      2: 'MEDIA_ERR_NETWORK - Lỗi mạng khi tải file',
      3: 'MEDIA_ERR_DECODE - File audio bị lỗi hoặc không đúng định dạng',
      4: 'MEDIA_ERR_SRC_NOT_SUPPORTED - URL không hợp lệ hoặc file không tồn tại (hoặc CORS)',
    }

    audioError.value = errorMessages[audio.error.code] || 'Unknown error'

    console.group('❌ Audio Error Details')
    console.error('Error code:', audio.error.code)
    console.error('Error message:', audio.error.message)
    console.error('Attempted URL:', fullAudioUrl.value)
    console.groupEnd()
  } else {
    audioError.value = 'Không thể load audio - Kiểm tra URL và file'
  }
}

// Utility functions
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

const formatTime = (seconds) => {
  if (!seconds) return '0 giây'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m} phút ${s} giây` : `${s} giây`
}

// Test URL accessibility on mount
onMounted(async () => {
  if (!fullAudioUrl.value) return

  console.log('🧪 Testing audio URL on mount...')
  console.log('🔗 URL:', fullAudioUrl.value)

  try {
    const response = await fetch(fullAudioUrl.value, {
      method: 'HEAD',
      credentials: 'include', // Important for CORS with credentials
    })

    console.log('✅ HEAD check:', {
      status: response.status,
      contentType: response.headers.get('Content-Type'),
      contentLength: response.headers.get('Content-Length'),
      acceptRanges: response.headers.get('Accept-Ranges'),
      cors: response.headers.get('Access-Control-Allow-Origin'),
    })

    if (response.status !== 200) {
      console.warn(`⚠️ Server returned ${response.status}`)
      audioError.value = `Server returned ${response.status}`
    }
  } catch (error) {
    console.error('❌ HEAD check failed:', error)
    audioError.value = 'Cannot reach audio file'
  }
})
</script>

<style scoped>
/* Audio player styling */
audio::-webkit-media-controls-panel {
  background-color: #f3f4f6;
}

html.dark audio::-webkit-media-controls-panel {
  background-color: #1f2937;
}

/* Prose styling */
.prose {
  line-height: 1.8;
}

.prose p {
  margin-bottom: 0.75rem;
}

/* Loading animation */
.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Smooth transitions */
button {
  transition: all 0.2s ease;
}

button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

button:active {
  transform: translateY(0);
}
</style>
