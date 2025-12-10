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
          <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.id">
            <span class="float-left">{{ topic.name }}</span>
            <el-tag size="small" type="info" class="float-right ml-2">{{
              topic.levelRequired
            }}</el-tag>
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
                  <el-icon>
                    <Timer />
                  </el-icon>
                  {{ formatTime(row.timeLimitSeconds) }}
                </span>

                <span
                  v-if="row.questionCount > 0"
                  class="flex items-center gap-1 text-blue-600 dark:text-blue-400 font-medium bg-blue-50 dark:bg-blue-900/20 px-1.5 rounded"
                >
                  <el-icon>
                    <QuestionFilled />
                  </el-icon>
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
                <el-icon :size="20" class="rotate-90">
                  <MoreFilled />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu class="min-w-[160px]">
                  <el-dropdown-item command="questions" :icon="QuestionFilled">
                    Quản lý câu hỏi
                  </el-dropdown-item>
                  <el-dropdown-item command="edit" :icon="Edit">
                    Chỉnh sửa bài học
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="delete"
                    :icon="Delete"
                    divided
                    class="!text-red-500 hover:!bg-red-50"
                  >
                    Xóa bài học
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
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <LessonFormDialog
      ref="lessonFormRef"
      :topic-id="selectedTopicId"
      @success="handleFormSuccess"
    />

    <el-dialog
      v-model="previewVisible"
      title="Chi tiết bài học"
      width="800px"
      align-center
      destroy-on-close
      class="!rounded-xl"
    >
      <LessonPreview v-if="previewLesson" :lesson="previewLesson" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Tools,
  Edit,
  Delete,
  Timer,
  QuestionFilled,
  Search,
  MoreFilled,
} from '@element-plus/icons-vue'
import LessonFormDialog from './LessonFormDialog.vue'
import LessonPreview from './LessonPreview.vue'

const props = defineProps({ initTopicId: Number })
const emit = defineEmits(['view-questions'])
const store = useGrammarStore()

// State
const lessonFormRef = ref(null)
const selectedTopicId = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterType = ref('')
const previewVisible = ref(false)
const previewLesson = ref(null)

// Computed
const topics = computed(() => store.topics)
const lessonsLoading = computed(() => store.lessonsLoading)

const filteredLessons = computed(() => {
  let result = [...store.lessons]
  if (searchQuery.value.trim()) {
    result = result.filter((l) => l.title.toLowerCase().includes(searchQuery.value.toLowerCase()))
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

// Actions Handlers
const handleCreate = () => lessonFormRef.value?.openCreate()

const handleActionCommand = (command, row) => {
  switch (command) {
    case 'questions':
      handleViewQuestions(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

const handleEdit = (lesson) => lessonFormRef.value?.openEdit(lesson)

const handleViewDetail = async (row) => {
  const fullLesson = await store.fetchLessonById(row.id)
  previewLesson.value = fullLesson
  previewVisible.value = true
}

const handleFormSuccess = async () => {
  await loadLessons()
}

const handleDelete = async (lesson) => {
  try {
    await ElMessageBox.confirm(`Xóa "${lesson.title}"?`, 'Cảnh báo', {
      type: 'warning',
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      confirmButtonClass: 'el-button--danger',
    })
    await store.deleteLesson(lesson.id)
    ElMessage.success('Đã xóa bài học')
    await loadLessons()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('Xóa bài học thất bại.')
      console.error(e)
    }
  }
}

const handleToggleActive = async (lesson) => {
  const newStatus = lesson.isActive
  const actionName = newStatus ? 'kích hoạt' : 'tắt'

  try {
    if (newStatus) {
      await store.activateLesson(lesson.id)
    } else {
      await store.deactivateLesson(lesson.id)
    }
    ElMessage.success(`Đã ${actionName} bài học`)
  } catch (e) {
    // Revert UI nếu lỗi
    lesson.isActive = !newStatus
    ElMessage.error(`Lỗi khi ${actionName} bài học.`)
    console.error(e)
  }
}

const loadLessons = async () => {
  if (selectedTopicId.value) await store.fetchLessons(selectedTopicId.value, { size: 1000 })
}

const handleTopicChange = (val) => {
  selectedTopicId.value = val
  if (val) loadLessons()
  else store.clearLessons()
}

const formatTime = (seconds) => {
  if (!seconds) return '0s'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}p ${s > 0 ? s + 's' : ''}` : `${s}s`
}

const handleRefresh = () => loadLessons()
const handleValidateOrder = () => store.validateLessonsOrder(selectedTopicId.value)
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
  { immediate: true },
)

onMounted(async () => {
  if (store.topics.length === 0) await store.fetchTopics({ size: 100 })
})
</script>

<style scoped>
.rotate-90 {
  transform: rotate(90deg);
}

/* Override Element table header for dark mode */
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
