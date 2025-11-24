<template>
  <div class="question-list-container">
    <!-- Header với actions -->
    <div class="list-header">
      <div class="header-left">
        <h3 class="header-title">
          <el-icon><QuestionFilled /></el-icon>
          Question Management
        </h3>
        <el-text v-if="selectedLessonInfo" type="info" size="small">
          Lesson: {{ selectedLessonInfo.title }}
        </el-text>
      </div>

      <div class="header-actions">
        <!-- Bulk Delete Button -->
        <el-button
          v-if="selectedQuestions.length > 0"
          type="danger"
          :icon="Delete"
          @click="handleBulkDelete"
        >
          Xóa {{ selectedQuestions.length }} câu
        </el-button>

        <!-- Create Button -->
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          :disabled="!currentLessonId"
        >
          Tạo Question
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-section">
      <el-space wrap>
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
            v-for="lesson in allLessons"
            :key="lesson.id"
            :label="`${lesson.title} (${lesson.lessonType})`"
            :value="lesson.id"
          >
            <span>{{ lesson.title }}</span>
            <el-tag size="small" :type="lesson.lessonType === 'THEORY' ? 'info' : 'success'" style="margin-left: 8px">
              {{ lesson.lessonType }}
            </el-tag>
          </el-option>
        </el-select>

        <!-- Question Type Filter -->
        <el-select
          v-model="filters.questionType"
          placeholder="Lọc theo loại"
          clearable
          style="width: 200px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="type in questionTypeOptions"
            :key="type.value"
            :label="`${type.icon} ${type.label}`"
            :value="type.value"
          />
        </el-select>

        <!-- Refresh Button -->
        <el-button :icon="Refresh" @click="loadQuestions">Làm mới</el-button>
      </el-space>
    </div>

    <!-- Questions Table -->
    <el-table
      v-loading="grammarStore.questionsLoading"
      :data="grammarStore.questions"
      stripe
      @selection-change="handleSelectionChange"
      style="width: 100%"
    >
      <!-- Checkbox Column -->
      <el-table-column type="selection" width="50" />

      <!-- Order Index -->
      <el-table-column prop="orderIndex" label="STT" width="70" sortable />

      <!-- Question Text -->
      <el-table-column label="Câu hỏi" min-width="300">
        <template #default="{ row }">
          <div class="question-text-cell">
            <el-text truncated :title="row.questionText">
              {{ row.questionText }}
            </el-text>
            <el-tag size="small" :type="getQuestionTypeTagType(row.questionType)" style="margin-top: 4px">
              {{ getQuestionTypeLabel(row.questionType) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- Metadata Preview -->
      <el-table-column label="Metadata" width="200">
        <template #default="{ row }">
          <div class="metadata-preview">
            <el-text size="small" type="info">
              {{ getMetadataPreview(row) }}
            </el-text>
          </div>
        </template>
      </el-table-column>

      <!-- Points -->
      <el-table-column prop="points" label="Điểm" width="80" align="center">
        <template #default="{ row }">
          <el-tag type="warning" size="small">{{ row.points }}</el-tag>
        </template>
      </el-table-column>

      <!-- Actions -->
      <el-table-column label="Thao tác" width="180" fixed="right">
        <template #default="{ row }">
          <el-button-group size="small">
            <el-button :icon="View" @click="handleView(row)">Xem</el-button>
            <el-button :icon="Edit" @click="handleEdit(row)">Sửa</el-button>
            <el-button :icon="Delete" type="danger" @click="handleDelete(row)">Xóa</el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="grammarStore.questionsPagination.totalElements"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Question Form Dialog -->
    <QuestionFormDialog
      v-model:visible="questionForm.dialogVisible"
      :mode="questionForm.dialogMode"
      :form-data="questionForm.formData"
      :lesson-id="currentLessonId"
      @submit="handleFormSubmit"
      @close="questionForm.dialogVisible = false"
    />

    <!-- Question Preview Dialog -->
    <el-dialog
      v-model="previewDialog.visible"
      title="Preview Question"
      width="60%"
      :close-on-click-modal="false"
    >
      <QuestionPreview
        v-if="previewDialog.question"
        :question="previewDialog.question"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Delete,
  Edit,
  View,
  Refresh,
  QuestionFilled,
} from '@element-plus/icons-vue'
import QuestionFormDialog from './QuestionFormDialog.vue'
import QuestionPreview from './QuestionPreview.vue'

// Props
const props = defineProps({
  initLessonId: {
    type: Number,
    default: null,
  },
})

// Store
const grammarStore = useGrammarStore()

// State
const currentLessonId = ref(props.initLessonId)
const allLessons = ref([])
const selectedQuestions = ref([])
const filters = ref({
  questionType: null,
})
const pagination = ref({
  page: 1,
  size: 10,
})

// Question Form State
const questionForm = ref({
  dialogVisible: false,
  dialogMode: 'create', // 'create' | 'edit'
  formData: {},
})

// Preview Dialog State
const previewDialog = ref({
  visible: false,
  question: null,
})

// Question Type Options (from composable)
const questionTypeOptions = [
  { value: 'MULTIPLE_CHOICE', label: 'Multiple Choice', icon: '☑️' },
  { value: 'TRUE_FALSE', label: 'True/False', icon: '✅' },
  { value: 'FILL_BLANK', label: 'Fill Blank', icon: '📝' },
  { value: 'SHORT_ANSWER', label: 'Short Answer', icon: '✏️' },
  { value: 'VERB_FORM', label: 'Verb Form', icon: '🔤' },
  { value: 'ERROR_CORRECTION', label: 'Error Correction', icon: '🔧' },
  { value: 'MATCHING', label: 'Matching', icon: '🔗' },
  { value: 'SENTENCE_BUILDING', label: 'Sentence Building', icon: '🧩' },
  { value: 'COMPLETE_CONVERSATION', label: 'Conversation', icon: '💬' },
  { value: 'PRONUNCIATION', label: 'Pronunciation', icon: '🔊' },
  { value: 'READING_COMPREHENSION', label: 'Reading', icon: '📖' },
  { value: 'OPEN_ENDED', label: 'Open Ended', icon: '📄' },
]

// Computed
const selectedLessonInfo = computed(() => {
  if (!currentLessonId.value) return null
  return allLessons.value.find((l) => l.id === currentLessonId.value)
})

// Methods
const loadAllLessons = async () => {
  try {
    // Load all topics first
    await grammarStore.fetchTopics({ page: 0, size: 100 })

    // Load lessons for each topic
    const lessonPromises = grammarStore.topics.map((topic) =>
      grammarStore.fetchLessons(topic.id, { page: 0, size: 100 })
    )
    await Promise.all(lessonPromises)

    // Flatten all lessons
    allLessons.value = grammarStore.lessons
    console.log('✅ Loaded all lessons:', allLessons.value.length)
  } catch (error) {
    console.error('❌ Error loading lessons:', error)
    ElMessage.error('Không thể tải danh sách lessons')
  }
}

const loadQuestions = async () => {
  if (!currentLessonId.value) {
    grammarStore.clearQuestions()
    return
  }

  try {
    const params = {
      page: pagination.value.page - 1, // Backend 0-indexed
      size: pagination.value.size,
      sort: 'orderIndex,asc',
    }
    await grammarStore.fetchQuestions(currentLessonId.value, params)
    console.log('✅ Loaded questions:', grammarStore.questions.length)
  } catch (error) {
    console.error('❌ Error loading questions:', error)
  }
}

const handleLessonChange = () => {
  pagination.value.page = 1
  loadQuestions()
}

const handleFilterChange = () => {
  pagination.value.page = 1
  loadQuestions()
}

const handlePageChange = (page) => {
  pagination.value.page = page
  loadQuestions()
}

const handleSizeChange = (size) => {
  pagination.value.size = size
  pagination.value.page = 1
  loadQuestions()
}

const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

const handleCreate = () => {
  if (!currentLessonId.value) {
    ElMessage.warning('Vui lòng chọn lesson trước')
    return
  }

  questionForm.value = {
    dialogVisible: true,
    dialogMode: 'create',
    formData: {},
  }
}

const handleEdit = (question) => {
  questionForm.value = {
    dialogVisible: true,
    dialogMode: 'edit',
    formData: { ...question },
  }
}

const handleView = (question) => {
  previewDialog.value = {
    visible: true,
    question: question,
  }
}

const handleDelete = async (question) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa câu hỏi: "${question.questionText}"?`,
      'Xác nhận xóa',
      {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning',
      }
    )

    await grammarStore.deleteQuestion(question.id)
    await loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting question:', error)
    }
  }
}

const handleBulkDelete = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('Vui lòng chọn ít nhất 1 câu hỏi')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa ${selectedQuestions.value.length} câu hỏi đã chọn?`,
      'Xác nhận xóa hàng loạt',
      {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning',
      }
    )

    const ids = selectedQuestions.value.map((q) => q.id)
    await grammarStore.bulkDeleteQuestions(ids)
    selectedQuestions.value = []
    await loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error bulk deleting:', error)
    }
  }
}

const handleFormSubmit = async () => {
  await loadQuestions()
  questionForm.value.dialogVisible = false
}

// Helper Functions
const getQuestionTypeLabel = (type) => {
  const option = questionTypeOptions.find((opt) => opt.value === type)
  return option ? `${option.icon} ${option.label}` : type
}

const getQuestionTypeTagType = (type) => {
  const typeMap = {
    MULTIPLE_CHOICE: 'primary',
    TRUE_FALSE: 'success',
    FILL_BLANK: 'warning',
    SHORT_ANSWER: 'info',
    MATCHING: 'danger',
    OPEN_ENDED: '',
  }
  return typeMap[type] || 'info'
}

const getMetadataPreview = (question) => {
  const meta = question.metadata
  if (!meta) return 'N/A'

  switch (question.questionType) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      return `${meta.options?.length || 0} options`
    case 'FILL_BLANK':
    case 'SHORT_ANSWER':
    case 'VERB_FORM':
    case 'ERROR_CORRECTION':
      return meta.correctAnswer ? `Answer: ${meta.correctAnswer.substring(0, 20)}...` : 'N/A'
    case 'MATCHING':
      return `${meta.pairs?.length || 0} pairs`
    case 'SENTENCE_BUILDING':
      return `${meta.words?.length || 0} words`
    case 'READING_COMPREHENSION':
      return `${meta.blanks?.length || 0} blanks`
    case 'PRONUNCIATION':
      return `${meta.words?.length || 0} words`
    case 'COMPLETE_CONVERSATION':
      return `${meta.options?.length || 0} options`
    case 'OPEN_ENDED':
      return meta.timeLimitSeconds ? `${meta.timeLimitSeconds}s limit` : 'No limit'
    default:
      return 'N/A'
  }
}

// Lifecycle
onMounted(async () => {
  await loadAllLessons()
  if (currentLessonId.value) {
    await loadQuestions()
  }
})

// Watch props
watch(
  () => props.initLessonId,
  (newVal) => {
    if (newVal) {
      currentLessonId.value = newVal
      loadQuestions()
    }
  }
)
</script>

<style scoped>
.question-list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filters-section {
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

.question-text-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metadata-preview {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

/* Responsive */
@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .filters-section {
    overflow-x: auto;
  }
}
</style>
