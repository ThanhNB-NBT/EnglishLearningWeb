<template>
  <div class="w-full flex flex-col h-full">
    <!-- Header Controls -->
    <div class="mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
      <div class="flex gap-3 items-center flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm bài nghe..."
          prefix-icon="Search"
          clearable
          class="w-full sm:w-64"
        />

        <el-select v-model="filterDifficulty" placeholder="Độ khó" clearable class="!w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="Dễ" value="BEGINNER" />
          <el-option label="Trung bình" value="INTERMEDIATE" />
          <el-option label="Khó" value="ADVANCED" />
        </el-select>

        <el-select v-model="filterStatus" placeholder="Trạng thái" clearable class="!w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="Kích hoạt" :value="true" />
          <el-option label="Ẩn" :value="false" />
        </el-select>
      </div>

      <div class="flex gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate" class="!rounded-lg font-bold">
          Thêm bài nghe
        </el-button>
        <el-button :icon="Refresh" @click="loadLessons" circle />
      </div>
    </div>

    <!-- Empty State -->
    <el-empty
      v-if="!lessons || lessons.length === 0"
      description="Chưa có bài nghe nào"
      :image-size="120"
    >
      <el-button type="primary" @click="handleCreate">Tạo bài nghe đầu tiên</el-button>
    </el-empty>

    <!-- Lessons Table -->
    <el-card
      v-else
      shadow="never"
      class="!border-gray-300 dark:!border-gray-700 !rounded-xl !overflow-hidden flex flex-col"
      :body-style="{ padding: '0px', flex: '1', display: 'flex', flexDirection: 'column' }"
    >
      <el-table
        :data="paginatedLessons"
        v-loading="loading"
        style="width: 100%"
        border
        stripe
        row-key="id"
        :header-cell-style="{ background: '#f9fafb', color: '#6b7280', fontWeight: '600' }"
      >
        <el-table-column label="STT" width="70" align="center" fixed="left">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-sm font-bold">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Bài nghe" min-width="350">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <el-tag
                  :type="getDifficultyType(row.difficulty)"
                  effect="dark"
                  size="small"
                  class="!rounded uppercase !text-[10px] !h-5 !px-2 tracking-wider"
                >
                  {{ getDifficultyLabel(row.difficulty) }}
                </el-tag>

                <span
                  class="font-bold text-gray-800 dark:text-gray-100 text-[15px] hover:text-blue-600 transition-colors cursor-pointer"
                  @click="handleViewDetail(row)"
                >
                  {{ row.title }}
                </span>
              </div>

              <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                <span class="flex items-center gap-1">
                  <el-icon><Timer /></el-icon>
                  {{ formatTime(row.timeLimitSeconds) }}
                </span>

                <span class="flex items-center gap-1">
                  <el-icon><Headset /></el-icon>
                  {{ row.allowUnlimitedReplay ? 'Unlimited' : `${row.maxReplayCount}x` }}
                </span>

                <span
                  v-if="row.questionCount > 0"
                  class="flex items-center gap-1 text-blue-600 dark:text-blue-400 font-medium bg-blue-50 dark:bg-blue-900/20 px-1.5 rounded"
                >
                  <el-icon><QuestionFilled /></el-icon>
                  {{ row.questionCount }} câu hỏi
                </span>

                <span class="flex items-center gap-1 text-orange-600 dark:text-orange-400 font-medium">
                  +{{ row.pointsReward }} điểm
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Audio" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.audioUrl"
              type="primary"
              link
              :icon="VideoPlay"
              @click="playAudio(row)"
              :loading="currentPlayingId === row.id && audioLoading"
            >
              {{ currentPlayingId === row.id ? 'Đang phát' : 'Nghe' }}
            </el-button>
            <span v-else class="text-gray-400 text-xs">Chưa có</span>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              size="small"
              @change="handleToggleActive(row)"
              @click.stop
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
            />
          </template>
        </el-table-column>

        <el-table-column label="" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(cmd) => handleActionCommand(cmd, row)">
              <el-button link class="!text-gray-400 hover:!text-gray-600 dark:hover:!text-gray-200">
                <el-icon :size="20" class="rotate-90"><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu class="min-w-[160px]">
                  <el-dropdown-item command="questions" :icon="QuestionFilled">
                    Quản lý câu hỏi
                  </el-dropdown-item>
                  <el-dropdown-item command="edit" :icon="Edit">
                    Chỉnh sửa bài nghe
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="delete"
                    :icon="Delete"
                    divided
                    class="!text-red-500 hover:!bg-red-50"
                  >
                    Xóa bài nghe
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="py-3 px-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredLessons.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Form Dialog -->
    <LessonFormDialog ref="lessonFormRef" @success="handleFormSuccess" />

    <!-- Detail Dialog with Enhanced Audio Player -->
    <el-dialog
      v-model="detailVisible"
      title="Chi tiết bài nghe"
      width="900px"
      align-center
      destroy-on-close
      class="!rounded-xl"
    >
      <div v-if="detailLesson" class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-xl font-bold">{{ detailLesson.title }}</h3>
          <el-tag :type="getDifficultyType(detailLesson.difficulty)" effect="dark">
            {{ getDifficultyLabel(detailLesson.difficulty) }}
          </el-tag>
        </div>

        <div class="flex gap-4 text-sm text-gray-600 dark:text-gray-400">
          <span><strong>Thời gian:</strong> {{ formatTime(detailLesson.timeLimitSeconds) }}</span>
          <span><strong>Điểm thưởng:</strong> +{{ detailLesson.pointsReward }}</span>
          <span><strong>Replay:</strong> {{ detailLesson.allowUnlimitedReplay ? 'Unlimited' : `${detailLesson.maxReplayCount}x` }}</span>
        </div>

        <!-- Enhanced Audio Player -->
        <div v-if="detailLesson.audioUrl" class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
          <div class="mb-2 text-sm text-gray-600 dark:text-gray-400">
            🔊 Audio Player
          </div>
          <audio
            ref="audioPlayerRef"
            controls
            class="w-full"
            @error="handleAudioError"
            @loadstart="audioLoading = true"
            @canplay="audioLoading = false"
            @play="currentPlayingId = detailLesson.id"
            @pause="currentPlayingId = null"
            @ended="currentPlayingId = null"
          >
            <source :src="getAudioUrl(detailLesson.audioUrl)" type="audio/mpeg">
            <source :src="getAudioUrl(detailLesson.audioUrl)" type="audio/wav">
            Trình duyệt không hỗ trợ audio.
          </audio>

          <!-- Audio Error Message -->
          <div v-if="audioError" class="mt-2 text-red-500 text-sm flex items-center gap-2">
            <el-icon><Warning /></el-icon>
            <span>{{ audioError }}</span>
          </div>

          <!-- Audio URL Debug Info (only in dev) -->
          <div v-if="isDev" class="mt-2 text-xs text-gray-500 break-all">
            <strong>Audio URL:</strong> {{ getAudioUrl(detailLesson.audioUrl) }}
          </div>
        </div>

        <!-- Tabs for Transcript -->
        <el-tabs type="border-card" class="!rounded-lg">
          <el-tab-pane label="📝 Transcript (English)">
            <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-60 overflow-y-auto whitespace-pre-wrap">
              {{ detailLesson.transcript }}
            </div>
          </el-tab-pane>

          <el-tab-pane label="🇻🇳 Bản dịch (Tiếng Việt)" v-if="detailLesson.transcriptTranslation">
            <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-60 overflow-y-auto whitespace-pre-wrap">
              {{ detailLesson.transcriptTranslation }}
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- Standalone Audio Player Dialog -->
    <el-dialog
      v-model="audioPlayerVisible"
      title="🎵 Audio Player"
      width="500px"
      align-center
      destroy-on-close
      class="!rounded-xl"
    >
      <div v-if="currentAudioLesson" class="space-y-4">
        <div class="text-center">
          <h3 class="text-lg font-bold mb-2">{{ currentAudioLesson.title }}</h3>
          <el-tag :type="getDifficultyType(currentAudioLesson.difficulty)" size="small">
            {{ getDifficultyLabel(currentAudioLesson.difficulty) }}
          </el-tag>
        </div>

        <div class="bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 p-6 rounded-xl">
          <audio
            ref="standaloneAudioRef"
            controls
            class="w-full"
            autoplay
            @error="handleAudioError"
            @loadstart="audioLoading = true"
            @canplay="audioLoading = false"
          >
            <source :src="getAudioUrl(currentAudioLesson.audioUrl)" type="audio/mpeg">
            <source :src="getAudioUrl(currentAudioLesson.audioUrl)" type="audio/wav">
            Trình duyệt không hỗ trợ audio.
          </audio>

          <!-- Loading State -->
          <div v-if="audioLoading" class="mt-3 text-center text-sm text-gray-600">
            <el-icon class="is-loading"><Loading /></el-icon>
            Đang tải audio...
          </div>

          <!-- Error State -->
          <div v-if="audioError" class="mt-3 text-red-500 text-sm flex items-center justify-center gap-2">
            <el-icon><Warning /></el-icon>
            <span>{{ audioError }}</span>
          </div>
        </div>

        <!-- Debug Info in Dev Mode -->
        <div v-if="isDev" class="text-xs text-gray-500 p-2 bg-gray-100 dark:bg-gray-800 rounded break-all">
          <strong>Debug Info:</strong><br>
          Audio URL: {{ getAudioUrl(currentAudioLesson.audioUrl) }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Edit,
  MoreFilled,
  QuestionFilled,
  Timer,
  Headset,
  VideoPlay,
  Warning,
  Loading,
} from '@element-plus/icons-vue'
import { useListeningStore } from '@/stores/listening'
import LessonFormDialog from './LessonFormDialog.vue'

const emit = defineEmits(['view-questions'])

const store = useListeningStore()
const lessonFormRef = ref(null)
const audioPlayerRef = ref(null)
const standaloneAudioRef = ref(null)

// State
const loading = ref(false)
const searchQuery = ref('')
const filterDifficulty = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailLesson = ref(null)

// Audio Player State
const audioPlayerVisible = ref(false)
const currentAudioLesson = ref(null)
const currentPlayingId = ref(null)
const audioLoading = ref(false)
const audioError = ref('')
const isDev = import.meta.env.DEV

const lessons = computed(() => store.lessons)

// Filter Logic
const filteredLessons = computed(() => {
  let result = [...store.lessons]

  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((l) => l.title.toLowerCase().includes(query))
  }

  if (filterDifficulty.value) {
    result = result.filter((l) => l.difficulty === filterDifficulty.value)
  }

  if (filterStatus.value !== '') {
    result = result.filter((l) => l.isActive === filterStatus.value)
  }

  return result.sort((a, b) => a.orderIndex - b.orderIndex)
})

const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLessons.value.slice(start, start + pageSize.value)
})

// Audio Helpers
const getAudioUrl = (audioUrl) => {
  if (!audioUrl) return ''

  // If already a full URL, return as is
  if (audioUrl.startsWith('http://') || audioUrl.startsWith('https://')) {
    return audioUrl
  }

  // Build full URL from base URL
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980'

  // Ensure proper path concatenation
  const cleanBaseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const cleanAudioPath = audioUrl.startsWith('/') ? audioUrl : `/${audioUrl}`

  return `${cleanBaseUrl}${cleanAudioPath}`
}

const handleAudioError = (event) => {
  console.error('Audio load error:', event)
  audioLoading.value = false

  const audio = event.target
  const error = audio.error

  if (error) {
    switch (error.code) {
      case error.MEDIA_ERR_ABORTED:
        audioError.value = 'Tải audio bị hủy'
        break
      case error.MEDIA_ERR_NETWORK:
        audioError.value = 'Lỗi mạng khi tải audio'
        break
      case error.MEDIA_ERR_DECODE:
        audioError.value = 'Lỗi giải mã audio'
        break
      case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
        audioError.value = 'Định dạng audio không được hỗ trợ hoặc file không tồn tại'
        break
      default:
        audioError.value = 'Lỗi không xác định khi phát audio'
    }
  } else {
    audioError.value = 'Không thể tải audio. Vui lòng kiểm tra đường dẫn file.'
  }

  ElMessage.error(audioError.value)
}

// Handlers
const loadLessons = async () => {
  loading.value = true
  try {
    await store.fetchLessons()
  } catch (error) {
    console.error('Failed to load lessons:', error)
    ElMessage.error('Không thể tải danh sách bài nghe')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => lessonFormRef.value?.openCreate()

const handleViewDetail = async (row) => {
  try {
    const lesson = await store.fetchLessonById(row.id)
    detailLesson.value = lesson
    audioError.value = '' // Reset error
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('Không thể tải chi tiết bài nghe')
    console.error(error)
  }
}

const playAudio = (lesson) => {
  if (!lesson.audioUrl) {
    ElMessage.warning('Bài nghe chưa có audio')
    return
  }

  audioError.value = '' // Reset error
  currentAudioLesson.value = lesson
  audioPlayerVisible.value = true
}

const handleFormSuccess = async () => {
  await loadLessons()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

const handleToggleActive = async (lesson) => {
  const originalState = lesson.isActive
  try {
    lesson.isActive = !originalState
    await store.updateLesson(lesson.id, lesson)
    ElMessage.success(`Đã ${lesson.isActive ? 'kích hoạt' : 'ẩn'} bài nghe`)
  } catch (error) {
    lesson.isActive = originalState
    ElMessage.error('Không thể thay đổi trạng thái')
  }
}

const handleActionCommand = (cmd, row) => {
  if (cmd === 'questions') emit('view-questions', row)
  if (cmd === 'edit') lessonFormRef.value?.openEdit(row)
  if (cmd === 'delete') handleDelete(row)
}

const handleDelete = async (lesson) => {
  try {
    await ElMessageBox.confirm(
      `Xóa bài nghe "${lesson.title}"? Hành động không thể hoàn tác.`,
      'Cảnh báo',
      {
        type: 'warning',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        confirmButtonClass: 'el-button--danger',
      },
    )
    await store.deleteLesson(lesson.id)
    await loadLessons()
    ElMessage.success('Đã xóa bài nghe')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('Không thể xóa bài nghe')
    }
  }
}

// Helpers
const getDifficultyType = (difficulty) => {
  const map = { BEGINNER: 'success', INTERMEDIATE: 'warning', ADVANCED: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty) => {
  const map = { BEGINNER: 'Dễ', INTERMEDIATE: 'TB', ADVANCED: 'Khó' }
  return map[difficulty] || difficulty
}

const formatTime = (seconds) => {
  if (!seconds) return '0 phút'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return secs > 0 ? `${minutes}:${secs.toString().padStart(2, '0')}` : `${minutes} phút`
}

// Lifecycle
onMounted(() => {
  loadLessons()
})
</script>

<style scoped>
/* Custom audio player styling */
audio::-webkit-media-controls-panel {
  background-color: #f3f4f6;
}

audio::-webkit-media-controls-play-button,
audio::-webkit-media-controls-pause-button {
  background-color: #3b82f6;
  border-radius: 50%;
}
</style>
