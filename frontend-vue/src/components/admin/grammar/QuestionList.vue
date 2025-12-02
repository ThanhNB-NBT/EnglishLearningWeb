<template>
  <div class="question-list-container">
    <div class="header-actions">
      <div class="left-actions">
        <el-select v-model="selectedTopicId" placeholder="Chọn Chủ đề..." filterable class="filter-select topic-select"
          @change="handleTopicChange">
          <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.id" />
        </el-select>

        <el-select v-model="currentLessonId" placeholder="Chọn Bài học..." filterable
          class="filter-select lesson-select" @change="handleLessonChange" :loading="lessonsLoading"
          :disabled="!selectedTopicId" no-data-text="Vui lòng chọn Chủ đề trước">
          <el-option v-for="lesson in siblingLessons" :key="lesson.id" :label="`${lesson.orderIndex}. ${lesson.title}`"
            :value="lesson.id" />
        </el-select>

        <el-input v-model="searchQuery" placeholder="Tìm nội dung..." :prefix-icon="Search" clearable
          class="search-input" />

        <el-select v-model="filterType" placeholder="Loại câu hỏi" clearable class="filter-select type-select">
          <el-option label="Tất cả" value="" />
          <el-option label="Trắc nghiệm" value="MULTIPLE_CHOICE" />
          <el-option label="Đúng/Sai" value="TRUE_FALSE" />
          <el-option label="Điền từ" value="FILL_BLANK" />
          <el-option label="Nối từ" value="MATCHING" />
          <el-option label="Xây dựng câu" value="SENTENCE_BUILDING" />
          <el-option label="Hội thoại" value="CONVERSATION" />
          <el-option label="Phát âm" value="PRONUNCIATION" />
          <el-option label="Trả lời ngắn" value="SHORT_ANSWER" />
          <el-option label="Văn bản" value="TEXT_ANSWER" />
          <el-option label="Chia động từ" value="VERB_FORM" />
          <el-option label="Sửa lỗi sai" value="ERROR_CORRECTION" />
          <el-option label="Câu hỏi mở" value="OPEN_ENDED" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" :disabled="!currentLessonId">
          Thêm mới
        </el-button>

        <el-button :icon="Refresh" @click="loadQuestions" :disabled="!currentLessonId" circle />

        <el-tooltip content="Xóa hàng loạt" placement="top">
          <el-button type="danger" plain :icon="Delete" :disabled="selectedRows.length === 0" @click="handleBulkDelete"
            circle />
        </el-tooltip>
      </div>
    </div>

    <el-empty v-if="!currentLessonId" description="Vui lòng chọn Chủ đề và Bài học để quản lý câu hỏi"
      :image-size="100" />

    <el-empty v-else-if="!questions || questions.length === 0" description="Bài học này chưa có câu hỏi nào"
      :image-size="100">
      <el-button type="primary" @click="handleCreate">Tạo câu hỏi ngay</el-button>
    </el-empty>

    <div v-else class="table-wrapper">
      <el-table :data="paginatedQuestions" v-loading="loading" style="width: 100%" border stripe row-key="id"
        @selection-change="handleSelectionChange" @row-click="handleViewDetail" row-class-name="clickable-row">
        <el-table-column type="selection" width="40" align="center" fixed="left" />

        <el-table-column label="#" width="50" align="center" fixed="left">
          <template #default="{ row }">
            <span class="font-bold text-gray-500">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung câu hỏi" min-width="400">
          <template #default="{ row }">
            <div class="question-content-cell">
              <div class="question-meta">
                <el-tag size="small" :type="getQuestionTypeColor(row.questionType)" effect="plain" class="type-tag">
                  {{ row.questionType }}
                </el-tag>
              </div>

              <div class="question-text ql-editor-preview" v-html="truncateHtml(row.questionText, 150)"></div>

              <div class="answer-preview">
                <div v-if="row.questionType === 'MULTIPLE_CHOICE' && row.metadata?.options" class="options-list">
                  <span v-for="(opt, idx) in row.metadata.options" :key="idx" class="mini-option"
                    :class="{ 'is-correct': opt.isCorrect }">
                    {{ opt.text }}
                    <el-icon v-if="opt.isCorrect" class="check-icon">
                      <Check />
                    </el-icon>
                  </span>
                </div>

                <div v-else-if="row.questionType === 'TRUE_FALSE' && row.metadata" class="bool-preview">
                  <el-tag size="small" :type="row.metadata.correctAnswer === true ? 'success' : 'danger'" effect="dark">
                    {{ row.metadata.correctAnswer ? 'TRUE' : 'FALSE' }}
                  </el-tag>
                </div>

                <div v-else-if="row.questionType === 'FILL_BLANK'" class="text-xs text-info">
                  <el-icon>
                    <EditPen />
                  </el-icon> {{ row.metadata?.blanks?.length || 0 }} chỗ trống
                </div>

                <div v-else-if="row.questionType === 'MATCHING'" class="text-xs text-info">
                  <el-icon>
                    <Connection />
                  </el-icon> {{ row.metadata?.pairs?.length || 0 }} cặp
                </div>

                <span v-else class="text-xs text-gray-400 italic">
                  (Click để xem chi tiết)
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Điểm" width="80" align="center">
          <template #default="{ row }">
            <span class="font-bold text-orange-500">+{{ row.points }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="100" align="center" fixed="right" class-name="actions-col">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="Sửa" placement="top" :hide-after="0">
                <el-button :icon="Edit" size="small" type="primary" plain class="square-btn"
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
          :total="filteredQuestions.length" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </div>

    <QuestionFormDialog ref="questionFormRef" :lesson-id="currentLessonId" @success="handleFormSuccess" />

    <el-dialog v-model="previewVisible" title="Chi tiết câu hỏi" width="700px" align-center destroy-on-close>
      <QuestionPreview v-if="previewQuestion" :question="previewQuestion" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus, Refresh, Delete, Search, Edit, Check, EditPen, Connection
} from '@element-plus/icons-vue'
import QuestionFormDialog from './QuestionFormDialog.vue'
import QuestionPreview from './QuestionPreview.vue'

const props = defineProps({
  initLessonId: { type: Number, default: null }
})

const emit = defineEmits(['update:lessonId'])
const store = useGrammarStore()
const questionFormRef = ref(null)

// State
const loading = ref(false)
const lessonsLoading = ref(false)
const selectedTopicId = ref(null) // Mới: State cho Topic
const currentLessonId = ref(null)
const siblingLessons = ref([])
const searchQuery = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// Preview State
const previewVisible = ref(false)
const previewQuestion = ref(null)

// Computed
const topics = computed(() => store.topics)
const questions = computed(() => store.questions)

const filteredQuestions = computed(() => {
  let result = [...store.questions]
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(q => {
      const text = q.questionText.replace(/<[^>]*>?/gm, ' ').toLowerCase()
      return text.includes(query)
    })
  }
  if (filterType.value) {
    result = result.filter(q => q.questionType === filterType.value)
  }
  return result.sort((a, b) => a.orderIndex - b.orderIndex)
})

const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredQuestions.value.slice(start, start + pageSize.value)
})

// --- Logic Load Data ---

// 1. Load danh sách Lessons khi chọn Topic
const handleTopicChange = async (topicId) => {
  selectedTopicId.value = topicId
  currentLessonId.value = null // Reset lesson khi đổi topic
  siblingLessons.value = []
  store.clearQuestions() // Clear bảng câu hỏi

  if (topicId) {
    lessonsLoading.value = true
    try {
      await store.fetchLessons(topicId, { size: 1000 })
      siblingLessons.value = store.lessons
    } finally {
      lessonsLoading.value = false
    }
  }
}

// 2. Load câu hỏi khi chọn Lesson
const handleLessonChange = (newId) => {
  currentLessonId.value = newId
  emit('update:lessonId', newId)
  loadQuestions()
}

const loadQuestions = async () => {
  if (!currentLessonId.value) return
  loading.value = true
  try {
    await store.fetchQuestions(currentLessonId.value, { size: 1000 })
  } catch (error) {
    console.error('Failed to load questions:', error)
  } finally {
    loading.value = false
  }
}

// 3. Logic khởi tạo thông minh (Auto-fill nếu có initLessonId)
const initializeData = async () => {
  // Luôn load Topics trước để có dữ liệu cho Selector
  if (store.topics.length === 0) {
    await store.fetchTopics({ size: 100 })
  }

  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId

    // Tìm lesson detail để biết topicId
    const lesson = await store.fetchLessonById(props.initLessonId)
    if (lesson && lesson.topicId) {
      selectedTopicId.value = lesson.topicId

      // Load lessons của topic đó để fill vào selector
      lessonsLoading.value = true
      await store.fetchLessons(lesson.topicId, { size: 1000 })
      siblingLessons.value = store.lessons
      lessonsLoading.value = false

      // Load câu hỏi
      await loadQuestions()
    }
  }
}

// --- Handlers ---
const handleCreate = () => questionFormRef.value?.openCreate()
const handleEdit = (question) => questionFormRef.value?.openEdit(question)

const handleViewDetail = (row) => {
  previewQuestion.value = row
  previewVisible.value = true
}

const handleDelete = async (question) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc muốn xóa câu hỏi này?', 'Cảnh báo', {
      type: 'warning', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy'
    })
    await store.deleteQuestion(question.id)
    ElMessage.success('Đã xóa câu hỏi thành công')
    await loadQuestions()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('Xóa thất bại')
  }
}

const handleBulkDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc muốn xóa ${selectedRows.value.length} câu hỏi đã chọn?`,
      'Cảnh báo', { type: 'warning' }
    )
    const ids = selectedRows.value.map(r => r.id)
    await store.bulkDeleteQuestions(ids)
    ElMessage.success(`Đã xóa ${ids.length} câu hỏi`)
    selectedRows.value = []
    await loadQuestions()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('Lỗi khi xóa hàng loạt')
  }
}

const handleFormSuccess = async () => await loadQuestions()
const handleSelectionChange = (val) => selectedRows.value = val
const handleSizeChange = (val) => { pageSize.value = val; currentPage.value = 1 }
const handlePageChange = (val) => currentPage.value = val

// Helpers
const truncateHtml = (html, limit) => {
  if (!html) return ''
  const text = html.replace(/<[^>]*>?/gm, ' ')
  if (text.length <= limit) return html
  return text.substring(0, limit) + '...'
}

const getQuestionTypeColor = (type) => {
  const map = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'warning',
    'FILL_BLANK': 'success',
    'MATCHING': 'info',
    'SENTENCE_BUILDING': 'danger',
    'CONVERSATION': 'primary',
    'PRONUNCIATION': 'warning',
    'SHORT_ANSWER': 'info',
    'TEXT_ANSWER': 'info',
    'VERB_FORM': 'success',
    'ERROR_CORRECTION': 'danger',
    'OPEN_ENDED': 'info'
  }
  return map[type] || 'info'
}

watch(() => props.initLessonId, (val) => {
  if (val && val !== currentLessonId.value) {
    initializeData()
  }
})

onMounted(() => {
  initializeData()
})
</script>

<style scoped>
.question-list-container {
  padding: 24px;
}

/* Header */
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 24px;
}

.left-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  flex: 1;
  min-width: 300px;
}

.right-actions {
  display: flex;
  gap: 8px;
}

/* Responsive Selectors */
.topic-select {
  min-width: 200px;
  flex: 1;
}

.lesson-select {
  min-width: 200px;
  flex: 1.5;
}

.search-input {
  min-width: 150px;
  flex: 1;
}

.type-select {
  width: 140px;
}

/* Table Content */
.question-content-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
}

.type-tag {
  font-weight: bold;
  border: none;
}

.question-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  font-weight: 500;
}

/* Answer Preview Styles */
.answer-preview {
  margin-top: 4px;
}

.options-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mini-option {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #606266;
  border: 1px solid #e9e9eb;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Correct Answer Style (Viền đậm) */
.mini-option.is-correct {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #67c23a;
  /* Viền rõ ràng */
  font-weight: 600;
}

.check-icon {
  font-size: 12px;
}

/* Actions */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
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

/* Fixed Column Border */
:deep(.el-table .el-table__fixed-right) {
  border-left: 1px solid var(--el-table-border-color) !important;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.05);
}

:deep(.clickable-row) {
  cursor: pointer;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1024px) {
  .left-actions {
    width: 100%;
  }

  .right-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 12px;
  }

  .topic-select,
  .lesson-select {
    min-width: 100%;
  }

  /* Xuống dòng trên tablet */
}

@media (max-width: 768px) {
  .question-list-container {
    padding: 16px;
  }

  :deep(.el-table .cell) {
    padding: 0 4px;
  }
}
</style>
