<template>
  <div class="w-full flex flex-col h-full">
    <!-- Header Controls -->
    <div class="mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
      <div class="flex gap-3 items-center flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm bài đọc..."
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
          Thêm bài đọc
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

    <!-- Empty State -->
    <el-empty
      v-if="!lessons || lessons.length === 0"
      description="Chưa có bài đọc nào"
      :image-size="120"
    >
      <el-button type="primary" @click="handleCreate">Tạo bài đọc đầu tiên</el-button>
    </el-empty>

    <!-- Lessons Table -->
    <el-card
      v-else
      shadow="never"
      class="! border-gray-300 dark:!border-gray-700 !rounded-xl ! overflow-hidden flex flex-col"
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

        <el-table-column label="Bài đọc" min-width="350">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <el-tag
                  :type="getDifficultyType(row.difficulty)"
                  effect="dark"
                  size="small"
                  class="!rounded uppercase ! text-[10px] ! h-5 !px-2 tracking-wider"
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
              @click.
              stop
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
                    Chỉnh sửa bài đọc
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="delete"
                    :icon="Delete"
                    divided
                    class="! text-red-500 hover: !bg-red-50"
                  >
                    Xóa bài đọc
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
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

    <!-- Form Dialog -->
    <LessonFormDialog ref="lessonFormRef" @success="handleFormSuccess" />

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      title="Chi tiết bài đọc"
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
          <span><strong>Câu hỏi:</strong> {{ detailLesson.questionCount || 0 }}</span>
        </div>

        <!-- 🆕 Tabs cho 2 ngôn ngữ -->
        <el-tabs type="border-card" class="! rounded-lg">
          <el-tab-pane label="📖 Nội dung (Tiếng Anh)">
            <div
              class="ql-editor ! p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-96 overflow-y-auto"
              v-html="detailLesson.content"
            ></div>
          </el-tab-pane>

          <el-tab-pane label="🇻🇳 Bản dịch (Tiếng Việt)" v-if="detailLesson.contentTranslation">
            <div
              class="ql-editor !p-4 bg-gray-50 dark:bg-gray-800 rounded-lg max-h-96 overflow-y-auto"
              v-html="detailLesson.contentTranslation"
            ></div>
          </el-tab-pane>

          <el-tab-pane label="🇻🇳 Bản dịch (Tiếng Việt)" v-else disabled>
            <el-empty description="Chưa có bản dịch tiếng Việt" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
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
  Search,
  Edit,
  MoreFilled,
  QuestionFilled,
  Timer,
  Setting,
  CircleCheck,
} from '@element-plus/icons-vue'
import { useReadingStore } from '@/stores/reading'
import LessonFormDialog from './LessonFormDialog.vue'

const emit = defineEmits(['view-questions'])

const store = useReadingStore()
const lessonFormRef = ref(null)

// State
const loading = ref(false)
const searchQuery = ref('')
const filterDifficulty = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailLesson = ref(null)

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

// Handlers
const loadLessons = async () => {
  loading.value = true
  try {
    await store.fetchLessons({ size: 1000 })
  } catch (error) {
    console.error('Failed to load lessons:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => lessonFormRef.value?.openCreate()

const handleViewDetail = async (row) => {
  try {
    const lesson = await store.fetchLessonById(row.id)
    detailLesson.value = lesson
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('Không thể tải chi tiết bài đọc')
    console.error(error)
  }
}

const handleFormSuccess = async () => await loadLessons()

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => (currentPage.value = val)

const handleToggleActive = async (lesson) => {
  try {
    await store.toggleLessonStatus(lesson.id)
  } catch (error) {
    lesson.isActive = !lesson.isActive
    ElMessage.error('Cập nhật trạng thái thất bại')
    console.error('Toggle active failed:', error)
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
      `Xóa bài đọc "${lesson.title}"?  Hành động không thể hoàn tác. `,
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
    ElMessage.success('Đã xóa bài đọc')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

const handleDropdownCommand = async (cmd) => {
  if (cmd === 'validate') {
    try {
      await store.validateLessonsOrder()
    } catch (error) {
      console.error('Validate failed:', error)
    }
  }
  if (cmd === 'healthCheck') {
    try {
      await store.healthCheck()
    } catch (error) {
      console.error('Health check failed:', error)
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
  return `${minutes} phút`
}

// Lifecycle
onMounted(() => loadLessons())
</script>
