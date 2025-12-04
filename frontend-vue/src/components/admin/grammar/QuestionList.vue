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
          <el-option label="Phát âm" value="PRONUNCIATION" />
          <el-option label="Xây dựng câu" value="SENTENCE_BUILDING" />
          <el-option label="Viết lại câu" value="SENTENCE_TRANSFORMATION" />
          <el-option label="Tìm lỗi sai" value="ERROR_CORRECTION" />
          <el-option label="Câu hỏi mở" value="OPEN_ENDED" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" :disabled="!currentLessonId">
          Thêm mới
        </el-button>
        <el-button type="success" plain :icon="DocumentAdd" @click="handleBulkCreate" :disabled="!currentLessonId">
          Thêm nhiều
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
            <span class="order-index">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung câu hỏi" min-width="400">
          <template #default="{ row }">
            <div class="question-content-cell">
              <div class="question-header">
                <el-tag size="small" :type="getQuestionTypeColor(row.questionType)" effect="dark" class="type-tag">
                  {{ row.questionType }}
                </el-tag>
                <span class="points-badge">+{{ row.points }}đ</span>
              </div>

              <div class="question-text">
                {{ truncateHtml(row.questionText, 120) }}
              </div>

              <div class="answer-preview">
                <div
                  v-if="(row.questionType === 'MULTIPLE_CHOICE' || row.questionType === 'COMPLETE_CONVERSATION') && row.metadata?.options"
                  class="options-list">
                  <span v-for="(opt, idx) in row.metadata.options" :key="idx" class="mini-option"
                    :class="{ 'is-correct': opt.isCorrect }">
                    {{ opt.text }}
                    <el-icon v-if="opt.isCorrect" class="check-icon">
                      <Check />
                    </el-icon>
                  </span>
                </div>

                <div v-else-if="row.questionType === 'TRUE_FALSE' && row.metadata" class="bool-preview">
                  <el-tag size="small" :type="getTrueFalseValue(row.metadata) ? 'success' : 'danger'" effect="plain">
                    ĐÁP ÁN: {{ getTrueFalseValue(row.metadata) ? 'TRUE' : 'FALSE' }}
                  </el-tag>
                </div>

                <div v-else-if="['FILL_BLANK', 'VERB_FORM', 'TEXT_ANSWER'].includes(row.questionType)"
                  class="text-xs text-info">
                  <div v-if="row.metadata?.wordBank?.length" class="mb-1">
                    <span class="text-muted">Word Bank: </span>
                    <span class="text-primary">{{ row.metadata.wordBank.join(', ') }}</span>
                  </div>
                  <el-tag size="small" type="info">
                    <el-icon>
                      <EditPen />
                    </el-icon> {{ row.metadata?.blanks?.length || 0 }} chỗ trống
                  </el-tag>
                </div>

                <div v-else-if="row.questionType === 'SENTENCE_BUILDING'" class="text-xs">
                  <span class="text-muted">Từ: </span>
                  <span class="text-warning">{{ row.metadata?.words?.join(' / ') }}</span>
                </div>

                <div v-else-if="row.questionType === 'SENTENCE_TRANSFORMATION'" class="text-xs">
                  <div class="text-muted">Câu gốc: {{ row.metadata?.originalSentence }}</div>
                </div>

                <div v-else-if="row.questionType === 'PRONUNCIATION'" class="text-xs">
                  <span class="text-muted">Từ: </span>
                  <span>{{ row.metadata?.words?.join(', ') }}</span>
                </div>

                <div v-else-if="row.questionType === 'ERROR_CORRECTION'" class="text-xs">
                  Lỗi sai: <span class="text-danger">{{ row.metadata?.errorText }}</span>
                  ➔ <span class="text-success">{{ row.metadata?.correction }}</span>
                </div>

                <div v-else-if="row.questionType === 'MATCHING'" class="text-xs text-info">
                  <el-icon>
                    <Connection />
                  </el-icon> {{ row.metadata?.pairs?.length || 0 }} cặp
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="120" align="center" fixed="right" class-name="actions-col">
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
    <BulkCreateDialog ref="bulkCreateDialogRef" :lesson-id="currentLessonId" @success="handleFormSuccess" />
    <el-dialog v-model="previewVisible" title="Chi tiết câu hỏi" width="700px" align-center destroy-on-close
      class="preview-dialog">
      <QuestionPreview v-if="previewQuestion" :question="previewQuestion" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, defineAsyncComponent } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus, Refresh, Delete, Search, Edit, Check, EditPen, Connection, DocumentAdd
} from '@element-plus/icons-vue'
import QuestionFormDialog from './QuestionFormDialog.vue'
import QuestionPreview from './QuestionPreview.vue'
const BulkCreateDialog = defineAsyncComponent(() => import('./BulkCreateDialog.vue'))

const props = defineProps({ initLessonId: { type: Number, default: null } })
const emit = defineEmits(['update:lessonId'])
const store = useGrammarStore()
const questionFormRef = ref(null)
const bulkCreateDialogRef = ref(null)

// State
const loading = ref(false)
const lessonsLoading = ref(false)
const selectedTopicId = ref(null)
const currentLessonId = ref(null)
const siblingLessons = ref([])
const searchQuery = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
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

// --- Handlers (Giữ nguyên logic cũ) ---
const handleTopicChange = async (topicId) => {
  selectedTopicId.value = topicId
  currentLessonId.value = null
  siblingLessons.value = []
  store.clearQuestions()
  if (topicId) {
    lessonsLoading.value = true
    try { await store.fetchLessons(topicId, { size: 1000 }); siblingLessons.value = store.lessons }
    finally { lessonsLoading.value = false }
  }
}

const handleLessonChange = (newId) => {
  currentLessonId.value = newId; emit('update:lessonId', newId); loadQuestions()
}

const loadQuestions = async () => {
  if (!currentLessonId.value) return
  loading.value = true
  try { await store.fetchQuestions(currentLessonId.value, { size: 1000 }) }
  catch (error) { console.error('Failed to load questions:', error) }
  finally { loading.value = false }
}

const initializeData = async () => {
  if (store.topics.length === 0) await store.fetchTopics({ size: 100 })
  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId
    const lesson = await store.fetchLessonById(props.initLessonId)
    if (lesson && lesson.topicId) {
      selectedTopicId.value = lesson.topicId
      lessonsLoading.value = true
      await store.fetchLessons(lesson.topicId, { size: 1000 })
      siblingLessons.value = store.lessons
      lessonsLoading.value = false
      await loadQuestions()
    }
  }
}

const getTrueFalseValue = (meta) => {
  if (meta.options && Array.isArray(meta.options)) {
    const trueOpt = meta.options.find(o => o.text === 'True')
    if (trueOpt) return trueOpt.isCorrect
  }
  return meta.correctAnswer !== undefined ? meta.correctAnswer : false
}

const handleCreate = () => questionFormRef.value?.openCreate()
const handleEdit = (question) => questionFormRef.value?.openEdit(question)
const handleBulkCreate = () => bulkCreateDialogRef.value?.open()
const handleViewDetail = (row) => { previewQuestion.value = row; previewVisible.value = true }
const handleFormSuccess = async () => await loadQuestions()
const handleSelectionChange = (val) => selectedRows.value = val
const handleSizeChange = (val) => { pageSize.value = val; currentPage.value = 1 }
const handlePageChange = (val) => currentPage.value = val

const handleDelete = async (question) => {
  try {
    await ElMessageBox.confirm('Xóa câu hỏi này?', 'Cảnh báo', { type: 'warning', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy' })
    await store.deleteQuestion(question.id)
    ElMessage.success('Đã xóa'); await loadQuestions()
  } catch (e) { }
}
const handleBulkDelete = async () => {
  try {
    await ElMessageBox.confirm(`Xóa ${selectedRows.value.length} câu hỏi?`, 'Cảnh báo', { type: 'warning' })
    const ids = selectedRows.value.map(r => r.id)
    await store.bulkDeleteQuestions(ids)
    ElMessage.success(`Đã xóa ${ids.length} câu`); selectedRows.value = []; await loadQuestions()
  } catch (e) { }
}

const truncateHtml = (html, limit) => {
  if (!html) return ''
  const text = html.replace(/<[^>]*>?/gm, ' ')
  return text.length <= limit ? text : text.substring(0, limit) + '...'
}

const getQuestionTypeColor = (type) => {
  const map = { 'MULTIPLE_CHOICE': 'primary', 'TRUE_FALSE': 'warning', 'FILL_BLANK': 'success', 'MATCHING': 'info', 'ERROR_CORRECTION': 'danger' }
  return map[type] || 'info'
}

watch(() => props.initLessonId, (val) => { if (val && val !== currentLessonId.value) initializeData() })
onMounted(initializeData)
</script>

<style scoped>
.question-list-container {
  padding: 24px;
}

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

/* TABLE CONTENT ALIGNMENT */
.question-content-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
  align-items: flex-start;
  /* Quan trọng: Căn trái tất cả */
  width: 100%;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.points-badge {
  font-size: 11px;
  font-weight: bold;
  color: #E6A23C;
  background: #fdf6ec;
  padding: 1px 6px;
  border-radius: 4px;
}

.question-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  /* Dùng biến hệ thống để fix Dark Mode */
  line-height: 1.5;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.answer-preview {
  width: 100%;
  margin-top: 4px;
}

.options-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* Mini option styling */
.mini-option {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--el-fill-color);
  /* Tự động đổi màu nền */
  color: var(--el-text-color-regular);
  /* Tự động đổi màu chữ */
  border: 1px solid var(--el-border-color);
  display: flex;
  align-items: center;
  gap: 4px;
}

.mini-option.is-correct {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  border-color: var(--el-color-success);
  font-weight: 600;
}

.text-muted {
  color: var(--el-text-color-secondary);
}

.text-primary {
  color: var(--el-color-primary);
}

.text-danger {
  color: var(--el-color-danger);
}

.text-success {
  color: var(--el-color-success);
}

.text-warning {
  color: var(--el-color-warning);
}

/* FIX ACTION COLUMN */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

.square-btn {
  border-radius: 4px;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table .actions-col) {
  border-left: 1px solid var(--el-table-border-color) !important;
  border-bottom: 1px solid var(--el-table-border-color) !important;
  position: sticky !important;
  right: 0 !important;
  z-index: 5 !important;
}

:deep(.el-table__header th.actions-col) {
  border-top: 1px solid var(--el-table-border-color) !important;
  background-color: var(--el-table-header-bg-color) !important;
}

:deep(.el-table__body td.actions-col) {
  background-color: var(--el-table-tr-bg-color) !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.actions-col) {
  background-color: var(--el-table-row-hover-bg-color) !important;
}

/* Pagination */
.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* DARK MODE SPECIFIC OVERRIDES */
html.dark .question-text {
  color: #E5EAF3;
}

html.dark .points-badge {
  background: #2b2318;
  color: #E6A23C;
}

html.dark .mini-option {
  background: #262727;
  border-color: #4c4d4f;
  color: #cfd3dc;
}

html.dark .mini-option.is-correct {
  background: #1c2518;
  color: #67c23a;
  border-color: #67c23a;
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
