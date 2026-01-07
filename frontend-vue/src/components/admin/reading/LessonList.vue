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
                <span
                  class="font-bold text-gray-800 dark:text-gray-100 text-[15px] hover:text-blue-600 transition-colors cursor-pointer"
                  @click="handleViewDetail(row)"
                >
                  {{ row.title }}
                </span>
              </div>

              <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                <span class="flex items-center gap-1">
                  <el-icon><Timer /></el-icon> {{ formatTime(row.timeLimitSeconds) }}
                </span>
                <span
                  v-if="row.questionCount > 0"
                  class="flex items-center gap-1 text-blue-600 dark:text-blue-400 font-medium bg-blue-50 dark:bg-blue-900/20 px-1.5 rounded"
                >
                  <el-icon><QuestionFilled /></el-icon> {{ row.questionCount }} câu hỏi
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
                  <el-dropdown-item command="questions" :icon="QuestionFilled"
                    >Quản lý câu hỏi</el-dropdown-item
                  >
                  <el-dropdown-item command="edit" :icon="Edit" divided>Chỉnh sửa</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" class="!text-red-500"
                    >Xóa bài học</el-dropdown-item
                  >
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

    <LessonFormDialog ref="lessonFormRef" :topic-id="selectedTopicId" @success="loadLessons" />

    <el-dialog
      v-model="previewVisible"
      title="Chi tiết bài học Reading"
      width="70%"
      top="5vh"
      destroy-on-close
    >
      <LessonPreview v-if="previewLesson" :lesson="previewLesson" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useReadingAdminStore } from '@/stores/admin/readingAdmin'
import { useTopicStore } from '@/composables/useTopicStore'
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

const readingStore = useReadingAdminStore()
const topicOps = useTopicStore('READING')

const lessonFormRef = ref(null)
const selectedTopicId = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const previewVisible = ref(false)
const previewLesson = ref(null)

const topicsList = computed(() => topicOps.topics.value?.filter((t) => t?.id && t?.name) || [])
const lessonsLoading = computed(() => readingStore.lessonsLoading)

const filteredLessons = computed(() => {
  let result = [...(readingStore.lessons || [])]
  if (searchQuery.value.trim()) {
    result = result.filter((l) => l.title?.toLowerCase().includes(searchQuery.value.toLowerCase()))
  }
  return result
})

const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLessons.value.slice(start, start + pageSize.value)
})

const handleCreate = () => lessonFormRef.value?.openCreate()
const handleEdit = (lesson) => lessonFormRef.value?.openEdit(lesson)
const handleViewDetail = async (row) => {
  try {
    const fullLesson = await readingStore.fetchLessonDetail(row.id)
    previewLesson.value = fullLesson
    previewVisible.value = true
  } catch (e) {
    console.error(e)
    ElMessage.error('Không thể tải chi tiết bài học')
  }
}
const handleDelete = async (lesson) => {
  try {
    await ElMessageBox.confirm(`Bạn có chắc muốn xóa "${lesson.title}"?`, 'Xác nhận xóa', {
      type: 'warning',
      confirmButtonText: 'Xóa',
    })
    await readingStore.deleteLesson(lesson.id)
    await loadLessons()
  } catch (e) {
    console.error(e)
  }
}
const handleToggleActive = async (row) => {
  try {
    await readingStore.toggleLessonStatus(row.id)
  } catch (e) {
    ElMessage.error('Cập nhật trạng thái thất bại')
    // Revert switch
    row.isActive = !row.isActive
    console.error(e)
  }
}
const handleActionCommand = (cmd, row) => {
  if (cmd === 'questions') emit('view-questions', row)
  else if (cmd === 'edit') handleEdit(row)
  else if (cmd === 'delete') handleDelete(row)
}

const loadLessons = async () => {
  if (selectedTopicId.value) await readingStore.fetchLessons(selectedTopicId.value, { size: 1000 })
}
const handleTopicChange = (val) => {
  selectedTopicId.value = val
  if (val) loadLessons()
  else readingStore.lessons = []
}
const handleRefresh = () => loadLessons()
const handleValidateOrder = async () => {
  try {
    await ElMessageBox.confirm('Sắp xếp lại thứ tự bài học?', 'Xác nhận', { type: 'info' })
    await readingStore.fixLessonOrder(selectedTopicId.value)
    ElMessage.success('Đã chuẩn hóa thứ tự')
    await loadLessons()
  } catch (e) {
    console.error(e)
  }
}

const formatTime = (seconds) => {
  if (!seconds) return '0s'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}p ${s}s` : `${s}s`
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}
const handlePageChange = (val) => (currentPage.value = val)

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
  try {
    await topicOps.fetchTopics({ size: 100 })
  } catch (e) {
    ElMessage.error('Lỗi tải danh sách chủ đề')
    console.error(e)
  }
})
</script>

<style scoped>
.rotate-90 {
  transform: rotate(90deg);
}
</style>
