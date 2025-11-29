<template>
  <div class="lesson-list-container">
    <!-- Header Actions -->
    <div class="header-actions">
      <div class="left-actions">
        <!-- Topic Selector -->
        <el-select
          v-model="selectedTopicId"
          placeholder="Chọn Topic"
          clearable
          filterable
          @change="handleTopicChange"
          style="width: 300px"
        >
          <el-option
            v-for="topic in topics"
            :key="topic.id"
            :label="`${topic.name} (${topic.levelRequired})`"
            :value="topic.id"
          >
            <span style="float: left">{{ topic.name }}</span>
            <el-tag :type="getLevelColor(topic.levelRequired)" size="small" style="float: right; margin-left: 10px">
              {{ topic.levelRequired }}
            </el-tag>
          </el-option>
        </el-select>

        <!-- Search Input -->
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm tiêu đề lesson..."
          :prefix-icon="Search"
          clearable
          style="width: 300px"
          @input="handleSearch"
        />

        <!-- Filter by Type -->
        <el-select
          v-model="filterType"
          placeholder="Lọc theo loại"
          clearable
          style="width: 150px"
          @change="handleFilter"
        >
          <el-option label="Tất cả" value="" />
          <el-option label="Lý thuyết" value="THEORY" />
          <el-option label="Thực hành" value="PRACTICE" />
        </el-select>

        <!-- Stats -->
        <div v-if="selectedTopicId" class="stats">
          <el-tag type="info" size="small">
            <el-icon><Document /></el-icon>
            {{ lessonsPagination.totalElements }}
          </el-tag>
          <el-tag type="success" size="small">
            <el-icon><Reading /></el-icon>
            {{ theoryCount }}
          </el-tag>
          <el-tag type="warning" size="small">
            <el-icon><EditPen /></el-icon>
            {{ practiceCount }}
          </el-tag>
        </div>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" :disabled="!selectedTopicId" size="small">
          Tạo Lesson
        </el-button>

        <el-button :icon="Refresh" @click="handleRefresh" :disabled="!selectedTopicId" size="small">
          Làm mới
        </el-button>

        <el-button :icon="Tools" @click="handleValidateOrder" :disabled="!selectedTopicId" size="small">
          Validate
        </el-button>
      </div>
    </div>

    <!-- Empty State -->
    <el-empty v-if="!selectedTopicId" description="Vui lòng chọn Topic để xem Lessons" :image-size="150">
      <template #image>
        <el-icon :size="80" color="#909399">
          <FolderOpened />
        </el-icon>
      </template>
    </el-empty>

    <!-- Lessons Table -->
    <div v-else>
      <el-table
        :data="paginatedLessons"
        v-loading="lessonsLoading"
        border
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
        @row-click="handleViewLesson"
        empty-text="Chưa có lesson nào"
        size="small"
        :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold' }"
        :row-style="{ cursor: 'pointer' }"
        class="custom-table"
      >
        <!-- Order Index -->
        <el-table-column prop="orderIndex" label="STT" width="70" sortable="custom" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.orderIndex }}</el-tag>
          </template>
        </el-table-column>

        <!-- Type Icon -->
        <el-table-column label="Type" width="70" align="center">
          <template #default="{ row }">
            <el-tooltip :content="getLessonTypeLabel(row.lessonType)">
              <el-icon :size="20" :color="getLessonTypeColor(row.lessonType)">
                <Reading v-if="row.lessonType === 'THEORY'" />
                <EditPen v-else />
              </el-icon>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- Title -->
        <el-table-column prop="title" label="Tiêu đề" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="lesson-title">
              <span class="title-text">{{ row.title }}</span>
              <el-tag v-if="!row.isActive" type="danger" size="small" style="margin-left: 8px">
                Inactive
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- Lesson Type -->
        <el-table-column prop="lessonType" label="Loại" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.lessonType === 'THEORY' ? 'success' : 'warning'" size="small">
              {{ getLessonTypeLabel(row.lessonType) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Question Count -->
        <el-table-column label="Câu hỏi" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">
              <el-icon><QuestionFilled /></el-icon>
              {{ row.questionCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Points -->
        <el-table-column prop="pointsReward" label="Điểm" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">
              {{ row.pointsReward }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Duration -->
        <el-table-column prop="estimatedDuration" label="Thời gian" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">
              {{ formatDuration(row.estimatedDuration) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Status -->
        <el-table-column prop="isActive" label="Active" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              size="small"
              @change="handleToggleActive(row)"
              :loading="row.statusLoading"
              @click.stop
            />
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Thao tác" width="280" fixed="right">
          <template #default="{ row }">
            <el-button-group size="small">
              <el-button
                :icon="QuestionFilled"
                @click.stop="handleViewQuestions(row)"
                :type="row.questionCount > 0 ? 'primary' : 'default'"
              >
                Questions ({{ row.questionCount || 0 }})
              </el-button>

              <el-button :icon="Edit" @click.stop="handleEdit(row)">Sửa</el-button>
              <el-button :icon="Delete" type="danger" @click.stop="handleDelete(row)">Xóa</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="lessonsPagination.totalElements"
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Lesson Form Dialog -->
    <LessonFormDialog
      :visible="dialogVisible"
      :mode="dialogMode"
      :form-data="formData"
      :topic-id="selectedTopicId"
      @close="handleCloseDialog"
      @success="handleFormSuccess"
    />

    <!-- Lesson Preview Dialog -->
    <el-dialog
      v-model="previewDialogVisible"
      title="Chi tiết Lesson"
      width="800px"
      destroy-on-close
    >
      <LessonPreview v-if="selectedLesson" :lesson="selectedLesson" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { useGrammarLessonForm } from '@/composables/grammar/useGrammarLessons'
import { getLevelColor } from '@/types/grammar.types'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus, Refresh, Tools, Edit, Delete,
  Document, Reading, EditPen, QuestionFilled, FolderOpened, Search
} from '@element-plus/icons-vue'
import LessonFormDialog from './LessonFormDialog.vue'
import LessonPreview from './LessonPreview.vue'

const props = defineProps({
  initTopicId: {
    type: Number,
    default: null,
  },
})

const grammarStore = useGrammarStore()
const {
  dialogVisible,
  dialogMode,
  formData,
  openCreateDialog,
  openEditDialog,
  closeDialog,
} = useGrammarLessonForm()

const selectedTopicId = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const previewDialogVisible = ref(false)
const selectedLesson = ref(null)
const searchQuery = ref('')
const filterType = ref('')

const topics = computed(() => grammarStore.topics)
const lessons = computed(() => grammarStore.lessons)

// CLIENT-SIDE FILTERING (like TopicList)
const filteredLessons = computed(() => {
  let result = [...lessons.value]

  // Filter by search query
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(lesson =>
      lesson.title.toLowerCase().includes(query)
    )
  }

  // Filter by type
  if (filterType.value) {
    result = result.filter(lesson => lesson.lessonType === filterType.value)
  }

  return result
})

// CLIENT-SIDE PAGINATION
const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredLessons.value.slice(start, end)
})

// Update pagination based on filtered results
const lessonsPagination = computed(() => ({
  totalElements: filteredLessons.value.length,
  totalPages: Math.ceil(filteredLessons.value.length / pageSize.value),
  currentPage: currentPage.value,
  pageSize: pageSize.value
}))
const lessonsLoading = computed(() => grammarStore.lessonsLoading)

const theoryCount = computed(() => filteredLessons.value.filter(l => l.lessonType === 'THEORY').length)
const practiceCount = computed(() => filteredLessons.value.filter(l => l.lessonType === 'PRACTICE').length)

const loadLessons = async () => {
  if (!selectedTopicId.value || typeof selectedTopicId.value !== 'number') {
    return
  }

  // Load ALL lessons without filtering (let client-side handle it)
  await grammarStore.fetchLessons(selectedTopicId.value, {
    page: 0,
    size: 1000, // Load all lessons
    sort: 'orderIndex,asc',
  })
}

const handleTopicChange = async (topicId) => {
  if (!topicId) {
    grammarStore.clearLessons()
    return
  }
  selectedTopicId.value = topicId
  currentPage.value = 1
  await loadLessons()
}

watch(() => props.initTopicId, async (newTopicId) => {
  if (newTopicId) {
    selectedTopicId.value = newTopicId
    await handleTopicChange(newTopicId)
  }
}, { immediate: true })

const handleCreate = async () => {
  if (!selectedTopicId.value) {
    ElMessage.warning('Vui lòng chọn Topic trước')
    return
  }
  await openCreateDialog(selectedTopicId.value)
}

defineExpose({ openCreateDialog: handleCreate })

const handleViewLesson = async (lesson) => {
  try {
    const fullLesson = await grammarStore.fetchLessonById(lesson.id)
    selectedLesson.value = fullLesson
    previewDialogVisible.value = true
  } catch (error) {
    console.error('Failed to load lesson:', error)
    ElMessage.error('Không thể tải chi tiết lesson')
  }
}

const handleEdit = async (lesson) => {
  try {
    const fullLesson = await grammarStore.fetchLessonById(lesson.id)
    openEditDialog(fullLesson)
  } catch (error) {
    console.error('Failed to load lesson:', error)
  }
}

const handleDelete = async (lesson) => {
  const cascade = lesson.lessonType === 'PRACTICE'
  try {
    await ElMessageBox.confirm(
      `Xóa lesson "${lesson.title}"?` + (lesson.questionCount > 0 ? ` (${lesson.questionCount} câu hỏi sẽ bị xóa)` : ''),
      'Xác nhận',
      { type: 'warning' }
    )
    await grammarStore.deleteLesson(lesson.id, cascade)
    await loadLessons()
  } catch (error) {
    if (error !== 'cancel') console.error('Delete error:', error)
  }
}

const handleToggleActive = async (lesson) => {
  try {
    lesson.statusLoading = true
    if (lesson.isActive) {
      await grammarStore.updateLesson(lesson.id, { ...lesson, isActive: true })
    } else {
      await grammarStore.deactivateLesson(lesson.id)
    }
  } catch (error) {
    console.error('Toggle active error:', error)
    lesson.isActive = !lesson.isActive
  } finally {
    lesson.statusLoading = false
  }
}

const emit = defineEmits(['view-questions'])

const handleViewQuestions = (lesson) => {
  console.log('Viewing questions for lesson:', lesson)
  emit('view-questions', lesson)
}

const handleRefresh = async () => {
  currentPage.value = 1
  await loadLessons()
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadLessons()
}

const handleFilter = async () => {
  currentPage.value = 1
  await loadLessons()
}

const handleValidateOrder = async () => {
  try {
    await grammarStore.validateLessonsOrder(selectedTopicId.value)
  } catch (error) {
    console.error('Validate error:', error)
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleSortChange = async ({ prop, order }) => {
  const sortOrder = order === 'ascending' ? 'asc' : 'desc'
  const params = {
    page: currentPage.value - 1,
    size: pageSize.value,
    sort: `${prop},${sortOrder}`,
  }

  // TODO: Backend chưa hỗ trợ search/filter
  // if (searchQuery.value.trim()) {
  //   params.search = searchQuery.value.trim()
  // }

  // if (filterType.value) {
  //   params.lessonType = filterType.value
  // }

  await grammarStore.fetchLessons(selectedTopicId.value, params)
}

const handleCloseDialog = () => closeDialog()
const handleFormSuccess = async () => await loadLessons()

const getLessonTypeLabel = (type) => type === 'THEORY' ? 'Lý thuyết' : 'Thực hành'
const getLessonTypeColor = (type) => type === 'THEORY' ? '#67C23A' : '#E6A23C'
const formatDuration = (s) => {
  if (!s) return '0s'
  const m = Math.floor(s / 60)
  return m > 0 ? `${m}m` : `${s}s`
}

onMounted(async () => {
  if (grammarStore.topics.length === 0) {
    await grammarStore.fetchTopics({ size: 100 })
  }
})
</script>

<style scoped>
.lesson-list-container {
  padding: 16px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.left-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.right-actions {
  display: flex;
  gap: 8px;
}

.stats {
  display: flex;
  gap: 6px;
}

.lesson-title {
  display: flex;
  align-items: center;
}

.title-text {
  font-weight: 500;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

/* Đảm bảo cột Actions có border đầy đủ - CSS mạnh hơn */
.custom-table {
  border: 1px solid var(--el-table-border-color);
}

.custom-table :deep(.el-table__inner-wrapper) {
  border: 1px solid var(--el-table-border-color);
}

.custom-table :deep(.el-table__cell) {
  border-right: 1px solid var(--el-table-border-color) !important;
  padding: 12px 8px;
}

.custom-table :deep(.el-table__header-wrapper .el-table__cell) {
  background: var(--el-fill-color-light) !important;
  color: var(--el-text-color-primary) !important;
  font-weight: bold;
}

/* Force border cho fixed column */
.custom-table :deep(.el-table__fixed-right) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

.custom-table :deep(.el-table__fixed-right .el-table__cell) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

.custom-table :deep(.el-table__body-wrapper .el-table__row .el-table__cell:last-child) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

.custom-table :deep(.el-table__row:hover) {
  background-color: var(--el-fill-color-light);
  cursor: pointer;
}

@media (max-width: 768px) {
  .lesson-list-container {
    padding: 12px;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .left-actions,
  .right-actions {
    width: 100%;
  }
}
</style>
