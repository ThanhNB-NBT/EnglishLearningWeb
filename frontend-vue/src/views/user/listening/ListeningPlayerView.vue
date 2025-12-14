<template>
  <div class="listening-player-page min-h-screen bg-gradient-to-br from-purple-50 via-blue-50 to-indigo-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
    <div class="max-w-7xl mx-auto px-4 py-8">
      <!-- Header -->
      <div class="mb-8">
        <el-button
          :icon="ArrowLeft"
          @click="$router.push('/user/home')"
          class="mb-4"
        >
          Về trang chủ
        </el-button>

        <h1 class="text-4xl font-bold text-gray-800 dark:text-white mb-2 flex items-center gap-3">
          <el-icon :size="40" color="#409EFF"><Headset /></el-icon>
          Listening Practice
        </h1>
        <p class="text-gray-600 dark:text-gray-400">
          Luyện nghe tiếng Anh với các bài học thực tế
        </p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center items-center py-20">
        <el-icon class="is-loading" :size="50" color="#409EFF">
          <Loading />
        </el-icon>
      </div>

      <!-- Lesson List View -->
      <div v-else-if="!selectedLessonId" class="space-y-4">
        <el-empty
          v-if="!lessons || lessons.length === 0"
          description="Chưa có bài nghe nào"
          :image-size="120"
        />

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <LessonCard
            v-for="lesson in lessons"
            :key="lesson.id"
            :lesson="lesson"
            @select="handleSelectLesson"
          />
        </div>
      </div>

      <!-- Lesson Player View -->
      <LessonPlayer
        v-else
        :lesson-id="selectedLessonId"
        @back="handleBack"
        @complete="handleComplete"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listeningUserAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { Headset, ArrowLeft, Loading } from '@element-plus/icons-vue'
import LessonCard from '@/components/user/listening/LessonCard.vue'
import LessonPlayer from '@/components/user/listening/LessonPlayer.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const lessons = ref([])
const selectedLessonId = ref(null)

const loadLessons = async () => {
  loading.value = true
  try {
    const response = await listeningUserAPI.getLessons()
    if (response.data.success) {
      lessons.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to load lessons:', error)
    ElMessage.error('Không thể tải danh sách bài nghe')
  } finally {
    loading.value = false
  }
}

const handleSelectLesson = (lessonId) => {
  selectedLessonId.value = lessonId
  router.push({ name: 'user-listening-detail', params: { lessonId } })
}

const handleBack = () => {
  selectedLessonId.value = null
  router.push({ name: 'user-listening' })
}

const handleComplete = async () => {
  await loadLessons()
  handleBack()
}

// Watch route params
watch(() => route.params.lessonId, (newId) => {
  if (newId) {
    selectedLessonId.value = parseInt(newId)
  } else {
    selectedLessonId.value = null
  }
}, { immediate: true })

onMounted(() => {
  loadLessons()
})
</script>

<style scoped>
.listening-player-page {
  min-height: 100vh;
  transition: background-color 0.3s ease;
}
</style>
