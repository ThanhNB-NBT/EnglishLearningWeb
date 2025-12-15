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
          class="! w-full sm:!w-64"
          :loading="lessonsLoading"
          @change="handleLessonChange"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.orderIndex}. ${lesson.title}`"
            :value="lesson. id"
          >
            <div class="flex items-center justify-between">
              <span>{{ lesson.orderIndex }}. {{ lesson.title }}</span>
              <el-tag v-if="lesson.questionCount > 0" type="info" size="small" class="ml-2">
                {{ lesson. questionCount }} câu
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
          class="! rounded-lg font-bold"
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
        <el-button :icon="Refresh" @click="handleRefresh" :disabled="!currentLessonId" circle />

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
      v-else-if="! questions || questions.length === 0"
      description="Chưa có câu hỏi nào"
      :image-size="120"
    >
      <el-button type="primary" @click="handleCreate">Tạo câu hỏi đầu tiên</el-button>
    </el-empty>

    <!-- Questions Table -->
    <el-card
      v-else
      shadow="never"
      class="! border-gray-300 dark:!border-gray-700 ! rounded-xl ! overflow-hidden flex flex-col"
      :body-style="{ padding: '0px', flex: '1', display: 'flex', flexDirection:  'column' }"
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
        <el-table-column type="selection" width="55" fixed="left" />

        <el-table-column label="STT" width="70" align="center" fixed="left">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-sm font-bold">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Câu hỏi" min-width="400">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <el-tag
                  :type="getQuestionTypeColor(row.questionType)"
                  size="small"
                  effect="dark"
                  class="!rounded ! text-[10px] !h-5 !px-2"
                >
                  {{ getQuestionTypeLabel(row.questionType) }}
                </el-tag>

                <el-tag type="info" size="small" effect="plain">
                  {{ row.points }} điểm
                </el-tag>
              </div>

              <div class="text-gray-800 dark:text-gray-200 text-sm font-medium">
                {{ row.questionText || '(Không có nội dung)' }}
              </div>

              <!-- Preview Metadata -->
              <div class="text-xs text-gray-500">
                <template v-if="row.questionType === 'MULTIPLE_CHOICE'">
                  <span>{{ row.metadata?. options?.length || 0 }} đáp án</span>
                </template>
                <template v-else-if="row.questionType === 'TRUE_FALSE'">
                  <span>Đúng/Sai</span>
                </template>
                <template v-else-if="row.questionType === 'FILL_BLANK'">
                  <span>{{ row.metadata?.blanks?. length || 0 }} chỗ trống</span>
                </template>
                <template v-else-if="row.questionType === 'TEXT_ANSWER'">
                  <span>Trả lời ngắn</span>
                </template>
                <template v-else-if="row.questionType === 'LISTENING_COMPREHENSION'">
                  <span>Nghe hiểu</span>
                </template>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Giải thích" min-width="250">
          <template #default="{ row }">
            <div class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
              {{ row.explanation || '—' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-1 justify-center">
              <el-tooltip content="Chỉnh sửa" placement="top">
                <el-button type="primary" :icon="Edit" size="small" circle @click="handleEdit(row)" />
              </el-tooltip>

              <el-tooltip content="Xóa" placement="top">
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  circle
                  @click="handleDelete(row)"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="px-4 py-3 border-t border-gray-200 dark:border-gray-700">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Question Form Dialog -->
    <QuestionFormDialog
      ref="questionFormRef"
      :current-lesson="currentLesson"
      @success="handleRefresh"
    />

    <!-- Bulk Create Dialog -->
    <BulkCreateDialog
      ref="bulkCreateRef"
      @success="handleRefresh"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  Plus,
  Refresh,
  Edit,
  Delete,
  Search,
  DocumentAdd,
} from '@element-plus/icons-vue'
import { useListeningQuestionList } from '@/composables/listening/useListeningQuestions'
import { useListeningStore } from '@/stores/listening'
import QuestionFormDialog from './QuestionFormDialog.vue'
import BulkCreateDialog from './BulkCreateDialog.vue'

// Store
const store = useListeningStore()

// Composables
const {
  currentLessonId,
  loading,
  questions,
  searchQuery,
  filterType,
  selectedRows,
  filteredQuestions,
  loadQuestions,
  deleteQuestion,
  bulkDeleteQuestions,
  handleSelectionChange,
  getQuestionTypeLabel,
  getQuestionTypeColor,
} = useListeningQuestionList()

// Refs
const questionFormRef = ref(null)
const bulkCreateRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

// Computed
const lessons = computed(() => store.lessons)
const lessonsLoading = computed(() => store.lessonsLoading)
const currentLesson = computed(() => store.lessons. find((l) => l.id === currentLessonId.value))

const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredQuestions.value.slice(start, end)
})

const totalElements = computed(() => filteredQuestions.value.length)

// Methods
const handleLessonChange = async (lessonId) => {
  if (! lessonId) {
    currentLessonId.value = null
    return
  }
  currentPage.value = 1
  await loadQuestions(lessonId)

  // Fetch lesson detail để có transcript
  await store.fetchLessonById(lessonId)
}

const handleRefresh = async () => {
  if (! currentLessonId.value) return
  await loadQuestions(currentLessonId.value)
  await store.fetchLessonById(currentLessonId.value)
}

const handleCreate = () => {
  questionFormRef.value?.openCreate(currentLessonId.value)
}

const handleEdit = (question) => {
  questionFormRef.value?.openEdit(question)
}

const handleDelete = async (question) => {
  await deleteQuestion(question)
}

const handleBulkCreate = () => {
  bulkCreateRef.value?.open(currentLesson.value)
}

const handleBulkDelete = async () => {
  await bulkDeleteQuestions(selectedRows.value)
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// Watch filters
watch([searchQuery, filterType], () => {
  currentPage.value = 1
})

// Lifecycle
onMounted(async () => {
  // Load all lessons for dropdown
  await store.fetchAllLessons()
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
