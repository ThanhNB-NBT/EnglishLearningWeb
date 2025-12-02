<template>
  <div class="lesson-list-container">
    <div class="header-actions">
      <div class="left-actions">
        <el-select v-model="selectedTopicId" placeholder="Chọn Topic..." filterable class="topic-select"
          @change="handleTopicChange">
          <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.id">
            <span style="float: left">{{ topic.name }}</span>
            <el-tag size="small" type="info" style="float: right; margin-left: 10px">
              {{ topic.levelRequired }}
            </el-tag>
          </el-option>
        </el-select>

        <el-input v-model="searchQuery" placeholder="Tìm bài học..." :prefix-icon="Search" clearable
          class="search-input" />

        <el-select v-model="filterType" placeholder="Loại" clearable class="filter-select">
          <el-option label="Tất cả" value="" />
          <el-option label="Lý thuyết" value="THEORY" />
          <el-option label="Thực hành" value="PRACTICE" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" :disabled="!selectedTopicId">
          Tạo Mới
        </el-button>
        <el-button :icon="Refresh" @click="handleRefresh" :disabled="!selectedTopicId" circle />
        <el-button :icon="Tools" @click="handleValidateOrder" :disabled="!selectedTopicId" circle />
      </div>
    </div>

    <el-empty v-if="!selectedTopicId" description="Chọn Topic để xem danh sách" />

    <div v-else class="table-wrapper">
      <el-table :data="paginatedLessons" v-loading="lessonsLoading" style="width: 100%" row-key="id" border stripe
        highlight-current-row @row-click="handleViewDetail" row-class-name="clickable-row" class="custom-table">
        <el-table-column label="STT" width="50" align="center" fixed="left">
          <template #default="{ row }">
            <span class="order-badge">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Bài học" min-width="200">
          <template #default="{ row }">
            <div class="lesson-info">
              <span class="lesson-title">{{ row.title }}</span>
              <div class="lesson-meta">
                <el-tag size="small" :type="row.lessonType === 'THEORY' ? 'success' : 'warning'" effect="plain">
                  {{ row.lessonType === 'THEORY' ? 'Lý thuyết' : 'Bài tập' }}
                </el-tag>
                <span class="meta-item">
                  <el-icon>
                    <Timer />
                  </el-icon> {{ formatTime(row.estimatedDuration) }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Câu hỏi" width="80" align="center">
          <template #default="{ row }">
            <span class="font-bold" :class="row.questionCount > 0 ? 'text-primary' : 'text-gray'">
              {{ row.questionCount || 0 }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="Active" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isActive" size="small" @change="handleToggleActive(row)" @click.stop />
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="140" align="center" fixed="right" class-name="actions-col">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="Câu hỏi" placement="top" :hide-after="0">
                <el-button :icon="QuestionFilled" size="small" type="primary" plain class="square-btn"
                  @click.stop="handleViewQuestions(row)" />
              </el-tooltip>

              <el-tooltip content="Sửa" placement="top" :hide-after="0">
                <el-button :icon="Edit" size="small" type="warning" plain class="square-btn"
                  @click.stop="handleEdit(row)" />
              </el-tooltip>

              <el-tooltip content="Xóa" placement="top" :hide-after="0">
                <el-button :icon="Delete" size="small" type="danger" plain class="square-btn"
                  @click.stop="handleDelete(row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="filteredLessons.length" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </div>

    <LessonFormDialog ref="lessonFormRef" :topic-id="selectedTopicId" @success="handleFormSuccess" />

    <el-dialog v-model="previewVisible" title="Chi tiết bài học" width="800px" align-center destroy-on-close>
      <LessonPreview v-if="previewLesson" :lesson="previewLesson" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus, Refresh, Tools, Edit, Delete,
  Timer, QuestionFilled, Search
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
    result = result.filter(l => l.title.toLowerCase().includes(searchQuery.value.toLowerCase()))
  }
  if (filterType.value) {
    result = result.filter(l => l.lessonType === filterType.value)
  }
  return result
})

const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLessons.value.slice(start, start + pageSize.value)
})

// Actions
const handleCreate = () => lessonFormRef.value?.openCreate()
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
    await ElMessageBox.confirm(`Xóa "${lesson.title}"?`, 'Warning', { type: 'warning' })
    await store.deleteLesson(lesson.id)
    ElMessage.success('Đã xóa')
    await loadLessons()
  } catch (e) {
    ElMessage.error('Xóa bài học thất bại.')
    console.error(e)
  }
}

const handleToggleActive = async (lesson) => {
  try {
    await store.updateLesson(lesson.id, { ...lesson, isActive: lesson.isActive })
    ElMessage.success('Đã cập nhật trạng thái')
  } catch (e) {
    lesson.isActive = !lesson.isActive
    ElMessage.error('Cập nhật trạng thái thất bại.')
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

// Handlers cho Pagination
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}
const handlePageChange = (val) => {
  currentPage.value = val
}

watch(() => props.initTopicId, (val) => {
  if (val) { selectedTopicId.value = val; loadLessons() }
}, { immediate: true })

onMounted(async () => {
  if (store.topics.length === 0) await store.fetchTopics({ size: 100 })
})
</script>

<style scoped>
.lesson-list-container {
  padding: 16px;
}

/* Header & Filter Styles */
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: space-between;
  margin-bottom: 16px;
}

.left-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.topic-select {
  min-width: 180px;
}

.search-input {
  min-width: 150px;
  flex: 1;
}

.filter-select {
  width: 110px;
}

/* Table Info Styles */
.lesson-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lesson-title {
  font-weight: 600;
  font-size: 13px;
  line-height: 1.4;
}

.lesson-meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  align-items: center;
}

.meta-item {
  font-size: 11px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 2px;
}

.order-badge {
  font-weight: bold;
  color: #909399;
  font-size: 12px;
}

.text-primary {
  color: #409eff;
}

.text-gray {
  color: #c0c4cc;
}

/* Actions Column Style */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 0px;
}

.square-btn {
  border-radius: 4px;
  padding: 8px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.clickable-row) {
  cursor: pointer;
  transition: background-color 0.2s;
}

:deep(.clickable-row:hover) {
  background-color: var(--el-fill-color-light) !important;
}

/* --- FIX LỖI VIỀN (QUAN TRỌNG) --- */

/* 1. Ép viền trái cho header của cột fixed-right */
:deep(.el-table .el-table__fixed-right th.el-table__cell) {
  border-left: 1px solid var(--el-table-border-color) !important;
}

/* 2. Ép viền trái cho các ô dữ liệu của cột fixed-right */
:deep(.el-table .el-table__fixed-right td.el-table__cell) {
  border-left: 1px solid var(--el-table-border-color) !important;
}

/* 3. (Tuỳ chọn) Luôn hiển thị bóng đổ nhẹ để tách biệt rõ ràng hơn */
:deep(.el-table__fixed-right) {
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.05) !important;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .lesson-list-container {
    padding: 8px;
  }

  .left-actions {
    width: 100%;
  }

  .right-actions {
    width: 100%;
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
  }

  :deep(.el-table .cell) {
    padding: 0 4px;
  }
}
</style>
