<!-- QuestionList.vue - Improved Version -->
<template>
  <div class="question-list-container">
    <!-- Header Actions -->
    <div class="header-actions">
      <div class="left-actions">
        <!-- Lesson Selector -->
        <el-select
          v-model="currentLessonId"
          placeholder="Chọn Lesson"
          clearable
          filterable
          style="width: 300px"
          @change="handleLessonChange"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.title} (${lesson.lessonType})`"
            :value="lesson.id"
          >
            <span style="float: left">{{ lesson.title }}</span>
            <el-tag
              :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'"
              size="small"
              style="float: right; margin-left: 10px"
            >
              {{ lesson.lessonType }}
            </el-tag>
          </el-option>
        </el-select>

        <!-- Search Input -->
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm nội dung câu hỏi..."
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
          style="width: 200px"
          @change="handleFilter"
        >
          <el-option label="Tất cả" value="" />
          <el-option
            v-for="type in questionTypes"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>

        <!-- Stats -->
        <div v-if="currentLessonId" class="stats">
          <el-tag type="info" size="small">
            <el-icon>
              <QuestionFilled />
            </el-icon>
            {{ questionsPagination.totalElements }} câu hỏi
          </el-tag>
        </div>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" :disabled="!currentLessonId" size="small">
          Tạo câu hỏi
        </el-button>

        <el-button :icon="Refresh" @click="handleRefresh" :disabled="!currentLessonId" size="small">
          Làm mới
        </el-button>
      </div>
    </div>

    <!-- Empty State -->
    <el-empty v-if="!currentLessonId" description="Vui lòng chọn Lesson để xem câu hỏi" :image-size="150">
      <template #image>
        <el-icon :size="80" color="#909399">
          <QuestionFilled />
        </el-icon>
      </template>
    </el-empty>

    <!-- Questions Table -->
    <div v-else>
      <el-table
        :data="paginatedQuestions"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
        @row-click="handleRowClick"
        empty-text="Chưa có câu hỏi nào"
        size="small"
        :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold' }"
        :row-style="{ cursor: 'pointer' }"
        class="custom-table"
      >
        <!-- Order Index -->
        <el-table-column prop="orderIndex" label="STT" width="80" sortable="custom" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.orderIndex }}</el-tag>
          </template>
        </el-table-column>

        <!-- Question Type -->
        <el-table-column label="Loại" width="200" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeColor(row.questionType)">
              {{ formatType(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Content -->
        <el-table-column label="Nội dung" min-width="350" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-html="truncateHtml(row.questionText, 100)" class="question-content"></div>
          </template>
        </el-table-column>

        <!-- Points -->
        <el-table-column prop="points" label="Điểm" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">
              <el-icon><Trophy /></el-icon>
              {{ row.points }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Thao tác" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button-group size="small">
              <el-button :icon="Edit" @click.stop="handleEdit(row)" type="primary">
                Sửa
              </el-button>
              <el-button :icon="Delete" type="danger" @click.stop="handleDelete(row)">
                Xóa
              </el-button>
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
          :total="questionsPagination.totalElements"
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Question Form Dialog -->
    <QuestionFormDialog ref="dialogRef" @success="handleFormSuccess" />

    <!-- Question Preview Dialog -->
    <el-dialog
      v-model="previewDialogVisible"
      title="Chi tiết câu hỏi"
      width="900px"
      destroy-on-close
      top="5vh"
    >
      <QuestionPreview v-if="selectedQuestion" :question="selectedQuestion" />

      <template #footer>
        <el-button @click="previewDialogVisible = false" size="large">Đóng</el-button>
        <el-button type="primary" @click="handleEditFromPreview" size="large">
          <el-icon><Edit /></el-icon>
          Chỉnh sửa
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import QuestionFormDialog from './QuestionFormDialog.vue'
import QuestionPreview from './QuestionPreview.vue'
import { Plus, Edit, Delete, QuestionFilled, Refresh, Trophy, Search } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  initLessonId: {
    type: Number,
    default: null
  }
})

const store = useGrammarStore()
const dialogRef = ref(null)
const currentLessonId = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const previewDialogVisible = ref(false)
const selectedQuestion = ref(null)
const searchQuery = ref('')
const filterType = ref('')

const questionTypes = [
  { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
  { value: 'TRUE_FALSE', label: 'Đúng/Sai' },
  { value: 'FILL_BLANK', label: 'Điền từ' },
  { value: 'SHORT_ANSWER', label: 'Trả lời ngắn' },
  { value: 'VERB_FORM', label: 'Dạng động từ' },
  { value: 'ERROR_CORRECTION', label: 'Sửa lỗi' },
  { value: 'MATCHING', label: 'Nối câu' },
  { value: 'SENTENCE_BUILDING', label: 'Sắp xếp câu' },
  { value: 'COMPLETE_CONVERSATION', label: 'Hoàn thành hội thoại' },
  { value: 'PRONUNCIATION', label: 'Phát âm' },
  { value: 'READING_COMPREHENSION', label: 'Đọc hiểu' },
  { value: 'OPEN_ENDED', label: 'Tự luận' }
]

const loading = computed(() => store.questionsLoading)
const questions = computed(() => store.questions)

// CLIENT-SIDE FILTERING
const filteredQuestions = computed(() => {
  let result = [...questions.value]

  // Filter by search query
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(q => {
      const div = document.createElement("div")
      div.innerHTML = q.questionText
      const text = (div.textContent || div.innerText || "").toLowerCase()
      return text.includes(query)
    })
  }

  // Filter by type
  if (filterType.value) {
    result = result.filter(q => q.questionType === filterType.value)
  }

  return result
})

// CLIENT-SIDE PAGINATION
const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredQuestions.value.slice(start, end)
})

// Update pagination based on filtered results
const questionsPagination = computed(() => ({
  totalElements: filteredQuestions.value.length,
  totalPages: Math.ceil(filteredQuestions.value.length / pageSize.value),
  currentPage: currentPage.value,
  pageSize: pageSize.value
}))
const lessons = computed(() => store.lessons)

watch(() => props.initLessonId, async (newId) => {
  if (newId) {
    currentLessonId.value = newId
    await handleLessonChange(newId)
  }
}, { immediate: true })

const fetchData = async () => {
  if (currentLessonId.value) {
    // Load ALL questions without filtering (let client-side handle it)
    await store.fetchQuestions(currentLessonId.value, {
      page: 0,
      size: 1000, // Load all
      sort: 'orderIndex,asc'
    })
  }
}

onMounted(async () => {
  if (store.lessons.length === 0) {
    if (store.currentTopic?.id) {
      await store.fetchLessons(store.currentTopic.id, { size: 100 })
    }
  }

  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId
    await fetchData()
  }
})

// --- ACTIONS ---

const handleLessonChange = async (lessonId) => {
  if (lessonId) {
    currentPage.value = 1
    await fetchData()
  } else {
    store.clearQuestions()
  }
}

const handleCreate = () => {
  if (dialogRef.value && currentLessonId.value) {
    dialogRef.value.openCreateDialog(currentLessonId.value)
  }
}

const handleRowClick = (row) => {
  selectedQuestion.value = row
  previewDialogVisible.value = true
}

const handleEdit = (row) => {
  if (dialogRef.value) {
    dialogRef.value.openEditDialog(row)
  }
}

const handleEditFromPreview = () => {
  if (selectedQuestion.value && dialogRef.value) {
    previewDialogVisible.value = false
    dialogRef.value.openEditDialog(selectedQuestion.value)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Xóa câu hỏi này?`,
      'Xác nhận',
      { type: 'warning' }
    )
    await store.deleteQuestion(row.id)
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('Delete error:', error)
  }
}

const handleRefresh = async () => {
  currentPage.value = 1
  await fetchData()
}

const handleFormSuccess = async () => {
  await handleRefresh()
}

const handleSearch = () => {
  currentPage.value = 1 // Reset to first page
}

const handleFilter = () => {
  currentPage.value = 1 // Reset to first page
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleSortChange = ({ prop, order }) => {
  // Client-side sort
  const sortOrder = order === 'ascending' ? 1 : -1
  filteredQuestions.value.sort((a, b) => {
    if (a[prop] > b[prop]) return sortOrder
    if (a[prop] < b[prop]) return -sortOrder
    return 0
  })
}

const formatType = (type) => {
  const typeMap = {
    'MULTIPLE_CHOICE': 'Trắc nghiệm',
    'TRUE_FALSE': 'Đúng/Sai',
    'FILL_BLANK': 'Điền từ',
    'SHORT_ANSWER': 'Trả lời ngắn',
    'VERB_FORM': 'Dạng động từ',
    'ERROR_CORRECTION': 'Sửa lỗi',
    'MATCHING': 'Nối câu',
    'SENTENCE_BUILDING': 'Sắp xếp câu',
    'COMPLETE_CONVERSATION': 'Hoàn thành hội thoại',
    'PRONUNCIATION': 'Phát âm',
    'READING_COMPREHENSION': 'Đọc hiểu',
    'OPEN_ENDED': 'Tự luận'
  }
  return typeMap[type] || type
}

const getTypeColor = (type) => {
  const colorMap = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'success',
    'FILL_BLANK': 'warning',
    'SHORT_ANSWER': 'info',
    'VERB_FORM': '',
    'ERROR_CORRECTION': 'danger',
    'MATCHING': 'danger',
    'SENTENCE_BUILDING': '',
    'COMPLETE_CONVERSATION': 'warning',
    'PRONUNCIATION': 'success',
    'READING_COMPREHENSION': 'primary',
    'OPEN_ENDED': 'info'
  }
  return colorMap[type] || ''
}

const truncateHtml = (html, length) => {
  const div = document.createElement("div")
  div.innerHTML = html
  const text = div.textContent || div.innerText || ""
  return text.length > length ? text.substring(0, length) + "..." : text
}
</script>

<style scoped>
.question-list-container {
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

.question-content {
  line-height: 1.5;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

/* Custom table styling with better borders - FORCE FIXED COLUMN BORDER */
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

.custom-table :deep(.el-table__row:hover) {
  background-color: var(--el-fill-color-light);
}

.custom-table :deep(tbody tr) {
  transition: background-color 0.2s;
}

/* Force border cho fixed column bên phải */
.custom-table :deep(.el-table__fixed-right) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

.custom-table :deep(.el-table__fixed-right .el-table__cell) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

.custom-table :deep(.el-table__body-wrapper .el-table__row .el-table__cell:last-child) {
  border-left: 2px solid var(--el-table-border-color) !important;
}

@media (max-width: 768px) {
  .question-list-container {
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
