<template>
  <el-card class="!rounded-2xl !border-2 !border-purple-200 dark:!border-purple-800" shadow="hover">
    <div class="flex items-center gap-3 mb-4">
      <el-icon :size="30" color="#9333EA"><Headset /></el-icon>
      <h3 class="text-xl font-bold text-gray-800 dark:text-white">Audio Player</h3>
    </div>

    <!-- Replay Counter -->
    <div v-if="!allowUnlimitedReplay" class="mb-4 flex items-center justify-between p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg">
      <span class="text-sm text-gray-700 dark:text-gray-300">
        <el-icon><Refresh /></el-icon>
        Số lần nghe:
      </span>
      <span class="text-lg font-bold" :class="replayCountClass">
        {{ currentPlayCount }} / {{ maxReplayCount }}
      </span>
    </div>

    <!-- Audio Element -->
    <audio
      ref="audioRef"
      :src="audioSrc"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
      @play="handlePlayStart"
      class="hidden"
    ></audio>

    <!-- Waveform (Simulated) -->
    <div class="mb-4 h-20 bg-gradient-to-r from-purple-100 via-blue-100 to-indigo-100 dark:from-purple-900/20 dark:via-blue-900/20 dark:to-indigo-900/20 rounded-lg flex items-center justify-center relative overflow-hidden">
      <div
        class="absolute left-0 top-0 h-full bg-gradient-to-r from-purple-500/30 to-blue-500/30 transition-all duration-300"
        :style="{ width: `${progress}%` }"
      ></div>
      <span class="relative z-10 text-2xl font-bold text-gray-700 dark:text-gray-300">
        {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
      </span>
    </div>

    <!-- Progress Slider -->
    <el-slider
      v-model="sliderValue"
      :max="100"
      :show-tooltip="false"
      @change="handleSeek"
      class="mb-4"
    />

    <!-- Controls -->
    <div class="flex items-center justify-center gap-4">
      <el-button
        :icon="Minus"
        circle
        size="large"
        @click="changeSpeed(-0.25)"
        :disabled="playbackRate <= 0.5"
      />

      <el-button
        :icon="DArrowLeft"
        circle
        size="large"
        @click="skip(-10)"
      />

      <el-button
        :type="isPlaying ? 'warning' : 'primary'"
        :icon="isPlaying ? VideoPause : VideoPlay"
        circle
        size="large"
        @click="togglePlay"
        :disabled="!canPlay"
        class="!w-16 !h-16"
      />

      <el-button
        :icon="DArrowRight"
        circle
        size="large"
        @click="skip(10)"
      />

      <el-button
        :icon="Plus"
        circle
        size="large"
        @click="changeSpeed(0.25)"
        :disabled="playbackRate >= 2"
      />
    </div>

    <!-- Speed Display -->
    <div class="text-center mt-4">
      <el-tag type="info" effect="plain" size="large">
        Tốc độ: {{ playbackRate }}x
      </el-tag>
    </div>

    <!-- Warning -->
    <el-alert
      v-if="!canPlay"
      type="warning"
      :closable="false"
      class="mt-4"
      show-icon
    >
      <template #title>
        Đã hết lượt nghe! Bạn đã nghe {{ currentPlayCount }} / {{ maxReplayCount }} lần.
      </template>
    </el-alert>
  </el-card>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Headset,
  VideoPlay,
  VideoPause,
  Refresh,
  DArrowLeft,
  DArrowRight,
  Plus,
  Minus,
} from '@element-plus/icons-vue'

const props = defineProps({
  audioUrl: String,
  allowUnlimitedReplay: Boolean,
  maxReplayCount: Number,
  currentPlayCount: Number,
})

const emit = defineEmits(['play'])

const audioRef = ref(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const playbackRate = ref(1)
const sliderValue = ref(0)
const hasPlayedOnce = ref(false)

const audioSrc = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980'
  return `${baseUrl}${props.audioUrl}`
})

const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const canPlay = computed(() => {
  if (props.allowUnlimitedReplay) return true
  return props.currentPlayCount < props.maxReplayCount
})

const replayCountClass = computed(() => {
  const remaining = props.maxReplayCount - props.currentPlayCount
  if (remaining === 0) return 'text-red-600 dark:text-red-400'
  if (remaining === 1) return 'text-orange-600 dark:text-orange-400'
  return 'text-green-600 dark:text-green-400'
})

const togglePlay = () => {
  if (!canPlay.value) return

  if (isPlaying.value) {
    audioRef.value?.pause()
  } else {
    audioRef.value?.play()
  }
}

const handlePlayStart = () => {
  isPlaying.value = true
  if (!hasPlayedOnce.value) {
    hasPlayedOnce.value = true
    emit('play')
  }
}

const handleTimeUpdate = () => {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime
    sliderValue.value = progress.value
  }
}

const handleLoadedMetadata = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration
  }
}

const handleEnded = () => {
  isPlaying.value = false
  currentTime.value = 0
  sliderValue.value = 0
}

const handleSeek = (value) => {
  if (audioRef.value) {
    const time = (value / 100) * duration.value
    audioRef.value.currentTime = time
  }
}

const skip = (seconds) => {
  if (audioRef.value) {
    audioRef.value.currentTime = Math.max(0, Math.min(duration.value, audioRef.value.currentTime + seconds))
  }
}

const changeSpeed = (delta) => {
  const newRate = Math.max(0.5, Math.min(2, playbackRate.value + delta))
  playbackRate.value = Math.round(newRate * 100) / 100
  if (audioRef.value) {
    audioRef.value.playbackRate = playbackRate.value
  }
}

const formatTime = (seconds) => {
  if (isNaN(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

watch(isPlaying, (newVal) => {
  if (!newVal && audioRef.value) {
    audioRef.value.pause()
  }
})
</script>
