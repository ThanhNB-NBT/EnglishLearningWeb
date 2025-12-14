<template>
  <div class="w-full flex flex-col h-full">
    <!-- Header Controls -->
    <div class="mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
      <div class="flex gap-3 items-center flex-wrap w-full sm:w-auto">
        <!-- Lesson Selector -->
        <el-select
          v-model="currentLessonId"
          placeholder="Chọn bài nghe"
          filterable
          clearable
          class="!w-full sm:!w-64"
          :loading="lessonsLoading"
          @change="handleLessonChange"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.orderIndex}. ${lesson.title}`"
            :value="lesson.id"
          >
            <div class="flex items-center justify-between">
              <span>{{ lesson.orderIndex }}. {{ lesson.title }}</span>
              <el-tag v-if="lesson.questionCount > 0" type="info" size="small" class="ml-2">
                {{ lesson.questionCount }} câu
              </el-tag>
            </div>
          </el-option>
        </el-select>

        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm câu hỏi..."
          :prefix-icon="Search"
          clearable
          class="!w-full md:!w-48"
        />

        <!-- Question Type Filter -->
        <el-select v-model="filterType" placeholder="Loại câu hỏi" clearable class="!w-48">
          <el-option label="Tất cả" value="" />
          <el-option label="Nghe hiểu" value="LISTENING_COMPREHENSION" />
          <el-option label="Trắc nghiệm" value="MULTIPLE_CHOICE" />
          <el-option label="Đúng/Sai" value="TRUE_FALSE" />
          <el-option label="Điền từ" value="FILL_BLANK" />
          <el-option label="Trả lời ngắn" value="TEXT_ANSWER" />
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
      description="Vui lòng chọn bài nghe để quản lý câu hỏi"
      :image-size="120"
    />
    <el-empty
      v-else-if="!questions || questions.length === 0"
      description="Bài nghe này chưa có câu hỏi nào"
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

              <div class="text-sm font-medium text-gray-800 dark:text-gray-200 line-clamp-2 hover:text-blue-600 transition-colors">
                {{ truncateHtml(row.questionText, 150) || '(Câu hỏi nghe hiểu)' }}
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
      parent-type="LISTENING"
      @success="handleFormSuccess"
    />

    <!-- Bulk Create Dialog -->
    <BulkCreateDialog
      ref="bulkCreateDialogRef"
      :lesson-id="currentLessonId"
      parent-type="LISTENING"
      @success="handleFormSuccess"
    />

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="Chi tiết câu hỏi" width="700px" align-center destroy-on-close class="!rounded-xl">
      <QuestionPreview v-if="previewQuestion" :question="previewQuestion" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Search,
  Edit,
  DocumentAdd,
  MoreFilled,
} from '@element-plus/icons-vue'
import { useQuestionUtils } from '@/composables/questions/useQuestionUtils'
import { useListeningStore } from '@/stores/listening'
import QuestionFormDialog from './QuestionFormDialog.vue'
import BulkCreateDialog from './BulkCreateDialog.vue'
import QuestionPreview from '@/components/admin/shared/questions/QuestionPreview.vue'

const props = defineProps({
  initLessonId: { type: Number, default: null },
})

const emit = defineEmits(['update:lessonId'])

// Store & Utils
const store = useListeningStore()
const { getQuestionTypeClass, getAnswerPreview, truncateHtml } = useQuestionUtils()

const questionFormRef = ref(null)
const bulkCreateDialogRef = ref(null)

// State
const loading = ref(false)
const lessonsLoading = ref(false)
const currentLessonId = ref(null)
const searchQuery = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const previewVisible = ref(false)
const previewQuestion = ref(null)

const lessons = computed(() => store.lessons)
const questions = computed(() => store.questions)

// Filter Logic
const filteredQuestions = computed(() => {
  let result = [...store.questions]
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((q) => {
      const text = (q.questionText || '').replace(/<[^>]*>?/gm, '').toLowerCase()
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
const loadLessons = async () => {
  lessonsLoading.value = true
  try {
    await store.fetchLessons()
  } finally {
    lessonsLoading.value = false
  }
}

const handleLessonChange = (newId) => {
  currentLessonId.value = newId
  emit('update:lessonId', newId)
  if (newId) loadQuestions()
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

const handleCreate = () => questionFormRef.value?.openCreate(currentLessonId.value)
const handleBulkCreate = () => bulkCreateDialogRef.value?.open()

const handleViewDetail = async (row) => {
  try {
    const questionDetail = await store.fetchQuestionById(row.id)
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
    await loadQuestions()
    ElMessage.success('Đã xóa câu hỏi')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

const handleBulkDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `Xóa ${selectedRows.value.length} câu hỏi đã chọn?`,
      'Cảnh báo',
      {
        type: 'warning',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
      }
    )
    const ids = selectedRows.value.map((q) => q.id)
    await store.bulkDeleteQuestions(ids)
    await loadQuestions()
    selectedRows.value = []
    ElMessage.success('Đã xóa câu hỏi')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Bulk delete failed:', error)
    }
  }
}

// Watchers
watch(() => props.initLessonId, (newVal) => {
  if (newVal && newVal !== currentLessonId.value) {
    currentLessonId.value = newVal
    loadQuestions()
  }
}, { immediate: true })

// Lifecycle
onMounted(async () => {
  await loadLessons()
  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId
    await loadQuestions()
  }
})
</script>
