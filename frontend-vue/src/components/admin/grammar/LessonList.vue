<!-- src/components/admin/grammar/LessonList.vue - GRAMMAR VERSION -->
<template>
  <div class="w-full">
    <div
      class="mb-5 flex flex-wrap gap-3 items-center bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm"
    >
      <div class="flex-1 flex flex-wrap gap-3 min-w-[300px]">
        <el-select
          v-model="selectedTopicId"
          placeholder="Chọn Topic..."
          filterable
          class="!w-64"
          @change="handleTopicChange"
        >
          <el-option
            v-for="topic in topicsList"
            :key="topic.id"
            :label="topic.name"
            :value="topic.id"
          >
            <span class="float-left">{{ topic.name }}</span>
            <el-tag size="small" type="info" class="float-right ml-2">
              {{ topic.levelRequired }}
            </el-tag>
          </el-option>
        </el-select>

        <el-input
          v-model="searchQuery"
          placeholder="Tìm bài học..."
          :prefix-icon="Search"
          clearable
          class="!w-60"
        />

        <el-select v-model="filterType" placeholder="Loại" clearable class="!w-32">
          <el-option label="Tất cả" value="" />
          <el-option label="Lý thuyết" value="THEORY" />
          <el-option label="Thực hành" value="PRACTICE" />
        </el-select>
      </div>

      <div class="flex gap-2">
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          :disabled="!selectedTopicId"
          class="!rounded-lg font-bold"
        >
          Tạo Mới
        </el-button>
        <el-button :icon="Refresh" @click="handleRefresh" :disabled="!selectedTopicId" circle />
        <el-button :icon="Tools" @click="handleValidateOrder" :disabled="!selectedTopicId" circle />
      </div>
    </div>

    <el-empty
      v-if="!selectedTopicId"
      description="Vui lòng chọn Topic để xem danh sách"
      :image-size="120"
    />

    <el-card
      v-else
      shadow="never"
      class="!border-gray-300 dark:!border-gray-700 !rounded-xl overflow-hidden"
      :body-style="{ padding: '0px' }"
    >
      <el-table
        :data="paginatedLessons"
        v-loading="lessonsLoading"
        style="width: 100%"
        row-key="id"
        stripe
        :header-cell-style="{ background: '#f9fafb', color: '#6b7280', fontWeight: '600' }"
      >
        <el-table-column label="STT" width="60" align="center">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-xs">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Thông tin bài học" min-width="300">
          <template #default="{ row }">
            <div class="py-2">
              <div class="flex items-center gap-2 mb-1.5">
                <el-tag
                  size="small"
                  :type="row.lessonType === 'THEORY' ? 'primary' : 'warning'"
                  effect="dark"
                  class="!rounded uppercase !text-[10px] !h-5 !px-1.5 tracking-wider"
                >
                  {{ row.lessonType === 'THEORY' ? 'Lý thuyết' : 'Thực hành' }}
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

                <span class="flex items-center gap-1 text-orange-600 dark:text-orange-400 font-medium">
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
                  <el-dropdown-item command="edit" :icon="Edit" divided>
                    Chỉnh sửa
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" class="!text-red-500">
                    Xóa bài học
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredLessons.length > 0" class="flex justify-center p-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredLessons.length"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <LessonFormDialog
      ref="lessonFormRef"
      :topic-id="selectedTopicId"
      @success="loadLessons"
    />

    <el-dialog v-model="previewVisible" title="Chi tiết bài học" width="70%" top="5vh">
      <LessonPreview v-if="previewLesson" :lesson="previewLesson" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarAdminStore } from '@/stores/admin/grammarAdmin'
import { useTopicStore } from '@/composables/useTopicStore'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus, Refresh, Tools, Edit, Delete, Timer,
  QuestionFilled, Search, MoreFilled,
} from '@element-plus/icons-vue'
import LessonFormDialog from './LessonFormDialog.vue'
import LessonPreview from './LessonPreview.vue'

const props = defineProps({
  initTopicId: Number,
})

const emit = defineEmits(['view-questions'])

const grammarStore = useGrammarAdminStore()
const topicOps = useTopicStore('GRAMMAR')

// State
const lessonFormRef = ref(null)
const selectedTopicId = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterType = ref('')
const previewVisible = ref(false)
const previewLesson = ref(null)

// ✅ FIX: Safely access topics with validation
const topicsList = computed(() => {
  const rawTopics = topicOps.topics.value || []

  // Filter out invalid entries
  return rawTopics.filter(topic =>
    topic &&
    typeof topic === 'object' &&
    topic.id !== undefined &&
    topic.name !== undefined
  )
})

const lessonsLoading = computed(() => grammarStore.lessonsLoading)

const filteredLessons = computed(() => {
  let result = [...(grammarStore.lessons || [])]

  if (searchQuery.value.trim()) {
    result = result.filter((l) =>
      l.title && l.title.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }

  if (filterType.value) {
    result = result.filter((l) => l.lessonType === filterType.value)
  }

  return result
})

const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLessons.value.slice(start, start + pageSize.value)
})

// Actions
const handleCreate = () => lessonFormRef.value?.openCreate()

const handleActionCommand = (command, row) => {
  const actions = {
    questions: () => handleViewQuestions(row),
    edit: () => handleEdit(row),
    delete: () => handleDelete(row),
  }
  actions[command]?.()
}

const handleEdit = (lesson) => lessonFormRef.value?.openEdit(lesson)

const handleViewDetail = async (row) => {
  try {
    const fullLesson = await grammarStore.fetchLessonDetail(row.id)
    previewLesson.value = fullLesson
    previewVisible.value = true
  } catch (e) {
    console.error('Error fetching lesson:', e)
    ElMessage.error('Không thể tải chi tiết bài học')
  }
}

const handleDelete = async (lesson) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc muốn xóa bài học "${lesson.title}"?\nTất cả câu hỏi liên quan sẽ bị xóa!`,
      'Xác nhận xóa',
      {
        type: 'warning',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
      },
    )
    await grammarStore.deleteLesson(lesson.id)
    await loadLessons()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleToggleActive = async (row) => {
  try {
    await grammarStore.toggleLessonStatus(row.id)
  } catch (e) {
    console.error(e)
  }
}

const loadLessons = async () => {
  if (!selectedTopicId.value) return

  try {
    await grammarStore.fetchLessons(selectedTopicId.value, { size: 1000 })
  } catch (error) {
    console.error('Error loading lessons:', error)
    ElMessage.error('Không thể tải danh sách bài học')
  }
}

const handleTopicChange = (val) => {
  selectedTopicId.value = val
  if (val) {
    loadLessons()
  } else {
    grammarStore.lessons = []
  }
}

const formatTime = (seconds) => {
  if (!seconds) return '0s'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}p ${s > 0 ? s + 's' : ''}` : `${s}s`
}

const handleRefresh = () => loadLessons()

const handleValidateOrder = async () => {
  if (!selectedTopicId.value) return

  try {
    await ElMessageBox.confirm(
      'Chuẩn hóa thứ tự các bài học (1, 2, 3...)?\nThao tác này sẽ sắp xếp lại tất cả lessons.',
      'Xác nhận',
      { type: 'info', confirmButtonText: 'Đồng ý', cancelButtonText: 'Hủy' }
    )

    await grammarStore.fixLessonOrder(selectedTopicId.value)
    ElMessage.success('Đã chuẩn hóa thứ tự thành công')
    await loadLessons()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleViewQuestions = (row) => emit('view-questions', row)

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

watch(
  () => props.initTopicId,
  (val) => {
    if (val) {
      selectedTopicId.value = val
      loadLessons()
    }
  },
  { immediate: true }
)

onMounted(async () => {
  console.log('🔄 [Grammar LessonList] Mounting')

  try {
    await topicOps.fetchTopics({ size: 100 })
    console.log('✅ [Grammar LessonList] Topics loaded:', topicsList.value.length)
  } catch (error) {
    console.error('❌ [Grammar LessonList] Error:', error)
    ElMessage.error('Không thể tải danh sách chủ đề')
  }
})
</script>

<style scoped>
.rotate-90 {
  transform: rotate(90deg);
}

html.dark :deep(.el-table__header th) {
  background-color: #252525 !important;
  color: #a3a3a3 !important;
  border-bottom-color: #333 !important;
}

html.dark :deep(.el-table__row td) {
  background-color: #1d1d1d !important;
  border-bottom-color: #333 !important;
}
</style>
