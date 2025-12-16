<template>
  <div class="w-full flex flex-col h-full">
    <div class="mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
      <div class="flex gap-3 items-center flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm bài nghe..."
          :prefix-icon="Search"
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
        <el-dropdown trigger="click" @command="handleDropdownCommand">
          <el-button :icon="MoreFilled" circle />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="validate" :icon="Setting">
                Validate OrderIndex
              </el-dropdown-item>
              <el-dropdown-item command="healthCheck" :icon="CircleCheck">
                Health Check
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-empty
      v-if="!lessons || lessons.length === 0"
      description="Chưa có bài nghe nào"
      :image-size="120"
    >
      <el-button type="primary" @click="handleCreate">Tạo bài nghe đầu tiên</el-button>
    </el-empty>

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

                <span
                  class="flex items-center gap-1"
                  :class="row.audioUrl ? 'text-green-600' : 'text-gray-400'"
                >
                  <el-icon><Headset /></el-icon>
                  {{ row.audioUrl ? 'Có Audio' : 'Chưa có Audio' }}
                </span>

                <span
                  v-if="row.questionCount > 0"
                  class="flex items-center gap-1 text-blue-600 dark:text-blue-400 font-medium bg-blue-50 dark:bg-blue-900/20 px-1.5 rounded"
                >
                  <el-icon><QuestionFilled /></el-icon>
                  {{ row.questionCount }} câu hỏi
                </span>

                <span
                  class="flex items-center gap-1 text-orange-600 dark:text-orange-400 font-medium"
                >
                  +{{ row.pointsReward }} điểm
                </span>
              </div>
            </div>
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

      <div
        class="py-3 px-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] flex justify-end"
      >
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

    <LessonFormDialog ref="lessonFormRef" @success="handleFormSuccess" />

    <!-- DETAIL DIALOG - FIXED AUDIO & TEXT DISPLAY -->
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

        <div class="flex gap-4 text-sm text-gray-600 dark:text-gray-400 flex-wrap">
          <span><strong>Thời gian:</strong> {{ formatTime(detailLesson.timeLimitSeconds) }}</span>
          <span><strong>Điểm thưởng:</strong> +{{ detailLesson.pointsReward }}</span>
          <span><strong>Câu hỏi:</strong> {{ detailLesson.questionCount || 0 }}</span>
          <span>
            <strong>Nghe lại:</strong>
            {{
              detailLesson.allowUnlimitedReplay
                ? 'Không giới hạn'
                : detailLesson.maxReplayCount + ' lần'
            }}
          </span>
        </div>

        <!-- AUDIO PLAYER - FIXED -->
        <div v-if="detailLesson.audioUrl" class="bg-gray-100 dark:bg-gray-800 p-4 rounded-lg">
          <div
            class="text-sm font-bold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2"
          >
            <el-icon><Headset /></el-icon>
            Audio File
          </div>
          <audio
            controls
            class="w-full"
            :key="detailLesson.id"
            controlsList="nodownload"
            preload="metadata"
          >
            <source :src="getAudioUrl(detailLesson.audioUrl)" type="audio/mpeg" />
            <source :src="getAudioUrl(detailLesson.audioUrl)" type="audio/mp3" />
            <source :src="getAudioUrl(detailLesson.audioUrl)" type="audio/wav" />
            Trình duyệt của bạn không hỗ trợ phát âm thanh.
          </audio>
          <div class="mt-2 text-xs text-gray-500">📂 {{ detailLesson.audioUrl }}</div>
        </div>
        <el-alert
          v-else
          title="Bài học này chưa có file âm thanh"
          type="warning"
          :closable="false"
          show-icon
        />

        <!-- TRANSCRIPT TABS - FIXED TEXT DISPLAY -->
        <el-tabs type="border-card" class="!rounded-lg">
          <el-tab-pane label="📜 Script (Tiếng Anh)">
            <div
              v-if="detailLesson.transcript"
              class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-96 overflow-y-auto text-gray-800 dark:text-gray-200 whitespace-pre-wrap leading-relaxed"
            >
              {{ formatTranscript(detailLesson.transcript) }}
            </div>
            <el-empty v-else description="Chưa có transcript" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane label="🇻🇳 Bản dịch (Tiếng Việt)">
            <div
              v-if="detailLesson.transcriptTranslation"
              class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-96 overflow-y-auto text-gray-800 dark:text-gray-200 whitespace-pre-wrap leading-relaxed"
            >
              {{ formatTranscript(detailLesson.transcriptTranslation) }}
            </div>
            <el-empty v-else description="Chưa có bản dịch" :image-size="60" />
          </el-tab-pane>
        </el-tabs>

        <!-- DEBUG INFO (có thể xóa sau khi test) -->
        <el-collapse>
          <el-collapse-item title="🔍 Debug Info" name="debug">
            <div
              class="text-xs font-mono bg-gray-900 text-green-400 p-3 rounded overflow-auto max-h-40"
            >
              <div><strong>Audio URL:</strong> {{ getAudioUrl(detailLesson.audioUrl) }}</div>
              <div><strong>Raw audioUrl:</strong> {{ detailLesson.audioUrl }}</div>
              <div><strong>Lesson ID:</strong> {{ detailLesson.id }}</div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Search,
  Edit,
  MoreFilled,
  QuestionFilled,
  Timer,
  Setting,
  CircleCheck,
  Headset,
} from '@element-plus/icons-vue'
import { useListeningStore } from '@/stores/listening'
import { useListeningLessonList } from '@/composables/listening/useListeningLessons'
import LessonFormDialog from './LessonFormDialog.vue'
import apiClient from '@/api/config'
import { formatTranscript, formatTime } from '@/utils/textFormatter'

// Logic lấy URL Backend từ API client
const SERVER_HOST =
  apiClient.defaults.baseURL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980'

const emit = defineEmits(['view-questions'])

const store = useListeningStore()
const lessonFormRef = ref(null)

// Use composable
const {
  loading,
  lessons,
  searchQuery,
  filterDifficulty,
  filterStatus,
  filteredLessons,
  loadLessons,
  deleteLesson,
  toggleStatus,
  validateLessonsOrder,
  healthCheck,
  getDifficultyType,
  getDifficultyLabel,
} = useListeningLessonList()

// State
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailLesson = ref(null)

const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLessons.value.slice(start, start + pageSize.value)
})

// Handlers
const handleFormSuccess = async () => await loadLessons({ size: 1000 })

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => (currentPage.value = val)

const handleCreate = () => lessonFormRef.value?.openCreate()

const handleViewDetail = async (row) => {
  try {
    const lesson = await store.fetchLessonById(row.id)
    detailLesson.value = lesson
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('Không thể tải chi tiết bài nghe')
    console.error(error)
  }
}

const handleToggleActive = async (lesson) => {
  try {
    await toggleStatus(lesson)
  } catch (error) {
    // Revert on error
    lesson.isActive = !lesson.isActive
    ElMessage.error('Không thể cập nhật trạng thái')
    console.error('Toggle active failed:', error)
  }
}

const handleActionCommand = (cmd, row) => {
  if (cmd === 'questions') emit('view-questions', row)
  if (cmd === 'edit') lessonFormRef.value?.openEdit(row)
  if (cmd === 'delete') handleDeleteLesson(row)
}

const handleDeleteLesson = async (lesson) => {
  await deleteLesson(lesson)
  await loadLessons({ size: 1000 })
}

const handleDropdownCommand = async (cmd) => {
  if (cmd === 'validate') {
    await validateLessonsOrder()
  }
  if (cmd === 'healthCheck') {
    await healthCheck()
  }
}

// Helpers
const getAudioUrl = (path) => {
  if (!path) return ''

  // Nếu path đã là full URL
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  // Loại bỏ dấu / ở đầu nếu có
  const cleanPath = path.startsWith('/') ? path.substring(1) : path

  // Nếu path không có prefix media/, thêm vào
  if (!cleanPath.startsWith('media/')) {
    return `${SERVER_HOST}/media/${cleanPath}`
  }

  return `${SERVER_HOST}/${cleanPath}`
}

// Lifecycle
onMounted(() => loadLessons({ size: 1000 }))
</script>

<style scoped>
/* Audio player custom styling */
audio {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

audio::-webkit-media-controls-panel {
  background-color: #f3f4f6;
}

/* Transcript text styling */
.whitespace-pre-wrap {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-size: 14px;
  line-height: 1.8;
}
</style>
