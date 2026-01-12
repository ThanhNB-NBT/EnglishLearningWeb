<template>
  <div class="audio-player-wrapper">
    <!-- Player Container -->
    <div class="audio-player-container">
      <div class="player-header">
        <el-icon><Headset /></el-icon>
        <span class="player-title">Audio Player</span>
        <el-tag v-if="playCount > 0" size="small" type="info"> Played: {{ playCount }}x </el-tag>
      </div>

      <!-- Audio Element -->
      <audio
        :key="audioKey"
        ref="audioRef"
        :src="fullAudioUrl"
        controls
        controlslist="nodownload"
        preload="metadata"
        class="audio-element"
        @play="handlePlay"
        @error="handleError"
        @loadedmetadata="handleLoaded"
        @loadstart="handleLoadStart"
      >
        Your browser does not support audio element.
      </audio>

      <!-- Status Messages -->
      <div v-if="loading" class="status-message loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>Loading audio...</span>
      </div>

      <div v-if="loaded && !error" class="status-message success">
        <el-icon><CircleCheck /></el-icon>
        <span>Ready to play - Duration: {{ formatDuration }}</span>
      </div>

      <div v-if="error" class="status-message error">
        <el-icon><CircleClose /></el-icon>
        <span>{{ error }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Headset, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  audioUrl: { type: String, required: true },
  playCount: { type: Number, default: 0 },
})

const emit = defineEmits(['play', 'error'])

const audioRef = ref(null)
const audioKey = ref(0)
const loading = ref(false)
const loaded = ref(false)
const error = ref(null)
const duration = ref(0)

// Compute full audio URL
const fullAudioUrl = computed(() => {
  if (!props.audioUrl) return ''

  const url = props.audioUrl.trim()

  // If already full URL
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // Prepend backend base URL
  const backendBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980'
  const cleanUrl = url.startsWith('/') ? url : `/${url}`
  return `${backendBaseUrl}${cleanUrl}`
})

const formatDuration = computed(() => {
  if (!duration.value || !isFinite(duration.value)) return '0:00'
  const mins = Math.floor(duration.value / 60)
  const secs = Math.floor(duration.value % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
})

// Event Handlers
const handleLoadStart = () => {
  console.log('🔄 Audio loading started')
  loading.value = true
  loaded.value = false
  error.value = null
}

const handleLoaded = (e) => {
  console.log('✅ Audio loaded successfully')
  loading.value = false
  loaded.value = true
  error.value = null

  const audio = e.target
  if (audio.duration && isFinite(audio.duration)) {
    duration.value = audio.duration
  }
}

const handlePlay = () => {
  console.log('▶️ Audio playing')
  emit('play')
}

const handleError = (e) => {
  console.error('❌ Audio error:', e)
  loading.value = false
  loaded.value = false

  const audio = e.target
  if (audio.error) {
    const errorMessages = {
      1: 'MEDIA_ERR_ABORTED - User aborted',
      2: 'MEDIA_ERR_NETWORK - Network error',
      3: 'MEDIA_ERR_DECODE - File corrupted',
      4: 'MEDIA_ERR_SRC_NOT_SUPPORTED - URL invalid or file not found',
    }
    error.value = errorMessages[audio.error.code] || 'Unknown error'
  } else {
    error.value = 'Cannot load audio'
  }

  emit('error', error.value)
}

// Reload audio when URL changes
watch(
  () => props.audioUrl,
  () => {
    console.log('🔄 Audio URL changed, reloading...')
    audioKey.value++
    loading.value = false
    loaded.value = false
    error.value = null
  },
)

// Expose methods for parent
defineExpose({
  play: () => audioRef.value?.play(),
  pause: () => audioRef.value?.pause(),
  reload: () => {
    audioKey.value++
    audioRef.value?.load()
  },
})
</script>

<style scoped>
.audio-player-wrapper {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-bottom: 1rem;
  background: rgb(249 250 251);
}

html.dark .audio-player-wrapper {
  background: rgb(17 24 39);
}

.audio-player-container {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

html.dark .audio-player-container {
  background: rgb(31 41 55);
  border-color: rgb(55 65 81);
}

.player-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(107 114 128);
}

html.dark .player-header {
  color: rgb(156 163 175);
}

.audio-element {
  width: 100%;
  height: 40px;
  outline: none;
}

.status-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
}

.status-message.loading {
  color: rgb(59 130 246);
  background: rgb(239 246 255);
}

html.dark .status-message.loading {
  color: rgb(96 165 250);
  background: rgba(59, 130, 246, 0.1);
}

.status-message.success {
  color: rgb(34 197 94);
  background: rgb(240 253 244);
}

html.dark .status-message.success {
  color: rgb(74 222 128);
  background: rgba(34, 197, 94, 0.1);
}

.status-message.error {
  color: rgb(239 68 68);
  background: rgb(254 242 242);
}

html.dark .status-message.error {
  color: rgb(248 113 113);
  background: rgba(239, 68, 68, 0.1);
}

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
</style>
