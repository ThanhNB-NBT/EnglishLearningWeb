<template>
  <div class="w-full flex flex-col h-full">
    <!-- Lesson Content Section -->
    <div v-if="currentLesson?.content" class="mb-4">
      <div
        class="flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg cursor-pointer select-none"
        @click="showLessonContent = !showLessonContent"
      >
        <div class="flex items-center gap-2 font-bold text-blue-700 dark:text-blue-300">
          <el-icon><Reading /></el-icon>
          <span>Nội dung {{ config.lessonLabel.toLowerCase() }} ({{ currentLesson.title }})</span>
        </div>
        <div class="flex items-center gap-2 text-xs text-blue-600">
          {{ showLessonContent ? 'Thu gọn' : 'Xem nội dung' }}
          <el-icon :class="{ 'rotate-180': showLessonContent }" class="transition-transform">
            <ArrowDown />
          </el-icon>
        </div>
      </div>

      <el-collapse-transition>
        <div
          v-show="showLessonContent"
          class="mt-2 bg-white dark:bg-[#1d1d1d] border border-gray-300 dark:border-gray-700 rounded-xl p-6 shadow-sm max-h-[300px] overflow-y-auto"
        >
          <div class="ql-editor !p-0 text-gray-800 dark:text-gray-200" v-html="currentLesson.content"></div>
        </div>
      </el-collapse-transition>
    </div>

    <!-- Filters & Actions -->
    <div class="mb-5 flex flex-wrap gap-3 items-center bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm">
      <div class="flex-1 flex flex-wrap gap-3 min-w-[300px]">
        <!-- Topic Selector -->
        <el-select
          v-model="selectedTopicId"
          :placeholder="`Chọn ${config.topicLabel}...`"
          filterable
          class="!w-full md:!w-56"
          @change="handleTopicChange"
        >
          <el-option v-for="topic in topics" :key="topic.id" :label="topic.name" :value="topic.id" />
        </el-select>

        <!-- Lesson Selector -->
        <el-select
          v-model="currentLessonId"
          :placeholder="`Chọn ${config.lessonLabel}...`"
          filterable
          class="!w-full md:!w-64"
          @change="handleLessonChange"
          :loading="lessonsLoading"
          :disabled="!selectedTopicId"
          :no-data-text="`Vui lòng chọn ${config.topicLabel} trước`"
        >
          <el-option
            v-for="lesson in siblingLessons"
            :key="lesson.id"
            :label="`${lesson.orderIndex}. ${lesson.title}`"
            :value="lesson.id"
          />
        </el-select>

        <!-- Search -->
        <el-input
          v-model="searchQuery"
          placeholder="Tìm nội dung..."
          :prefix-icon="Search"
          clearable
          class="!w-full md:!w-48"
        />

        <!-- Question Type Filter -->
        <el-select v-model="filterType" placeholder="Loại câu hỏi" clearable class="!w-48">
          <el-option label="Tất cả" value="" />
          <el-option-group label="Cơ bản">
            <el-option label="Trắc nghiệm" value="MULTIPLE_CHOICE" />
            <el-option label="Đúng / Sai" value="TRUE_FALSE" />
            <el-option label="Điền từ" value="FILL_BLANK" />
            <el-option label="Chia động từ" value="VERB_FORM" />
            <el-option label="Trả lời ngắn" value="TEXT_ANSWER" />
          </el-option-group>
          <el-option-group label="Nâng cao">
            <el-option label="Nối từ" value="MATCHING" />
            <el-option label="Sắp xếp câu" value="SENTENCE_BUILDING" />
            <el-option label="Viết lại câu" value="SENTENCE_TRANSFORMATION" />
            <el-option label="Tìm lỗi sai" value="ERROR_CORRECTION" />
            <el-option label="Phát âm" value="PRONUNCIATION" />
            <el-option label="Câu hỏi mở" value="OPEN_ENDED" />
          </el-option-group>
        </el-select>
      </div>

      <!-- Action Buttons -->
      <div class="flex gap-2">
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          :disabled="!currentLessonId"
          class="!rounded-lg font-bold"
        >
          Thêm
        </el-button>
        <el-button
          type="success"
          plain
          :icon="DocumentAdd"
          @click="handleBulkCreate"
          :disabled="!currentLessonId"
          class="!rounded-lg font-bold hidden sm:flex"
        >
          Bulk
        </el-button>
        <el-button :icon="Refresh" @click="loadQuestions" :disabled="!currentLessonId" circle />

        <el-tooltip content="Xóa hàng loạt" placement="top">
          <el-button
            type="danger"
            plain
            :icon="Delete"
            :disabled="selectedRows.length === 0"
            @click="handleBulkDelete"
            circle
          />
        </el-tooltip>
      </div>
    </div>

    <!-- Empty States -->
    <el-empty
      v-if="!currentLessonId"
      :description="`Vui lòng chọn ${config.topicLabel} và ${config.lessonLabel} để quản lý câu hỏi`"
      :image-size="120"
    />
    <el-empty
      v-else-if="!questions || questions.length === 0"
      :description="`${config.lessonLabel} này chưa có câu hỏi nào`"
      :image-size="120"
    >
      <div class="flex gap-3 mt-2">
        <el-button type="primary" @click="handleCreate">Tạo thủ công</el-button>
        <el-button type="success" @click="handleBulkCreate">Tạo nhiều câu hỏi</el-button>
      </div>
    </el-empty>

    <!-- Questions Table -->
    <el-card
      v-else
      shadow="never"
      class="!border-gray-300 dark:!border-gray-700 !rounded-xl !overflow-hidden flex flex-col"
      :body-style="{ padding: '0px', flex: '1', display: 'flex', flexDirection: 'column' }"
    >
      <el-table
        :data="paginatedQuestions"
        v-loading="loading"
        style="width: 100%"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#f9fafb', color: '#6b7280', fontWeight: '600' }"
      >
        <el-table-column type="selection" width="40" align="center" fixed="left" />

        <el-table-column label="STT" width="50" align="center" fixed="left">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-xs">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung câu hỏi" min-width="350">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-1.5 cursor-pointer" @click="handleViewDetail(row)">
              <div class="flex items-center gap-2">
                <span
                  class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border"
                  :class="getQuestionTypeClass(row.questionType)"
                >
                  {{ row.questionType.replace('_', ' ') }}
                </span>

                <span
                  class="text-[11px] font-bold text-orange-600 bg-orange-50 dark:bg-orange-900/20 dark:text-orange-400 px-1.5 rounded border border-orange-200 dark:border-orange-800"
                >
                  +{{ row.points }}đ
                </span>
              </div>

              <div
                class="text-sm font-medium text-gray-800 dark:text-gray-200 line-clamp-2 hover:text-blue-600 transition-colors"
              >
                {{ truncateHtml(row.questionText, 150) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Đáp án" min-width="200" class-name="hidden md:table-cell">
          <template #default="{ row }">
            <div class="text-xs text-gray-500 truncate max-w-[300px]" :title="getAnswerPreview(row)">
              <span v-html="getAnswerPreview(row)"></span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="" width="60" align="center" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
              <span class="cursor-pointer flex justify-center items-center p-2">
                <el-icon :size="18" style="vertical-align: middle">
                  <MoreFilled />
                </el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu class="min-w-[140px]">
                  <el-dropdown-item command="edit" :icon="Edit">Chỉnh sửa</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided class="!text-red-500 hover:!bg-red-50">
                    Xóa
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="py-3 px-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredQuestions.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Form Dialog -->
    <QuestionFormDialog
      ref="questionFormRef"
      :lesson-id="currentLessonId"
      :parent-type="config.parentType"
      @success="handleFormSuccess"
    />

    <!-- Bulk Create Dialog -->
    <BulkCreateDialog
      ref="bulkCreateDialogRef"
      :lesson-id="currentLessonId"
      :parent-type="config.parentType"
      @success="handleFormSuccess"
    />

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="Chi tiết câu hỏi" width="700px" align-center destroy-on-close class="!rounded-xl">
      <QuestionPreview v-if="previewQuestion" :question="previewQuestion" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, defineAsyncComponent } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Search,
  Edit,
  DocumentAdd,
  MoreFilled,
  Reading,
  ArrowDown,
} from '@element-plus/icons-vue'
import { useQuestionUtils } from '@/composables/questions/useQuestionUtils'
import QuestionFormDialog from './QuestionFormDialog.vue'
import QuestionPreview from './QuestionPreview.vue'

const BulkCreateDialog = defineAsyncComponent(() => import('./BulkCreateDialog.vue'))

const props = defineProps({
  initLessonId: { type: Number, default: null },
  config: {
    type: Object,
    required: true,
    validator: (c) => c.store && c.topicLabel && c.lessonLabel && c.parentType,
  },
})

const emit = defineEmits(['update:lessonId'])

// Get store from config
const store = props.config.store
const { getQuestionTypeClass, getAnswerPreview, truncateHtml } = useQuestionUtils()

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
const showLessonContent = ref(false)

const currentLesson = computed(() => store.lessons.find((lesson) => lesson.id === currentLessonId.value) || null)
const topics = computed(() => store.topics)
const questions = computed(() => store.questions)

// Filter Logic
const filteredQuestions = computed(() => {
  let result = [...store.questions]
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((q) => {
      const text = q.questionText.replace(/<[^>]*>?/gm, ' ').toLowerCase()
      return text.includes(query)
    })
  }
  if (filterType.value) {
    result = result.filter((q) => q.questionType === filterType.value)
  }
  return result.sort((a, b) => a.orderIndex - b.orderIndex)
})

const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredQuestions.value.slice(start, start + pageSize.value)
})

// Handlers
const handleTopicChange = async (topicId) => {
  selectedTopicId.value = topicId
  currentLessonId.value = null
  siblingLessons.value = []
  store.clearQuestions()
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

const handleCreate = () => questionFormRef.value?.openCreate()
const handleBulkCreate = () => bulkCreateDialogRef.value?.open()

const handleViewDetail = async (row) => {
  try {
    const questionDetail = await store.fetchQuestionById(row.id)
    console.log('=== DEBUG QUESTION DETAIL ===')
    console.log('Question ID:', questionDetail.id)
    console.log('Metadata type:', typeof questionDetail.metadata)
    console.log('Metadata value:', questionDetail.metadata)
    console.log('Is Array?:', Array.isArray(questionDetail.metadata))
    console.log('Constructor:', questionDetail.metadata?.constructor?.name)
    previewQuestion.value = questionDetail
    previewVisible.value = true
  } catch (error) {
    ElMessage.error('Không thể tải chi tiết câu hỏi')
    console.error(error)
  }
}

const handleFormSuccess = async () => await loadQuestions()
const handleSelectionChange = (val) => (selectedRows.value = val)
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}
const handlePageChange = (val) => (currentPage.value = val)

const handleAction = (cmd, row) => {
  if (cmd === 'edit') questionFormRef.value?.openEdit(row)
  if (cmd === 'delete') handleDelete(row)
}

const handleDelete = async (question) => {
  try {
    await ElMessageBox.confirm('Xóa câu hỏi này? Hành động không thể hoàn tác.', 'Cảnh báo', {
      type: 'warning',
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      confirmButtonClass: 'el-button--danger',
    })
    await store.deleteQuestion(question.id)
    ElMessage.success('Đã xóa')
    await loadQuestions()
  } catch (e) {}
}

const handleBulkDelete = async () => {
  try {
    await ElMessageBox.confirm(`Xóa ${selectedRows.value.length} câu hỏi đang chọn?`, 'Cảnh báo', { type: 'warning' })
    const ids = selectedRows.value.map((r) => r.id)
    await store.bulkDeleteQuestions(ids)
    ElMessage.success(`Đã xóa ${ids.length} câu`)
    selectedRows.value = []
    await loadQuestions()
  } catch (e) {}
}

// Initialization
const initializeData = async () => {
  if (store.topics.length === 0) await store.fetchTopics({ size: 100 })
  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId
    const lesson = await store.fetchLessonById(props.initLessonId)
    if (lesson?.topicId) {
      selectedTopicId.value = lesson.topicId
      if (store.lessons.length === 0) {
        lessonsLoading.value = true
        await store.fetchLessons(lesson.topicId, { size: 1000 })
        siblingLessons.value = store.lessons
        lessonsLoading.value = false
      } else {
        siblingLessons.value = store.lessons
      }
      await loadQuestions()
    }
  }
}

watch(() => props.initLessonId, (val) => {
  if (val && val !== currentLessonId.value) initializeData()
})

onMounted(initializeData)
</script>

<style scoped>
.rotate-180 {
  transform: rotate(180deg);
}
</style>
