<!-- src/components/admin/shared/questions/QuestionList.vue - FULL VERSION WITH TASK GROUPS -->
<template>
  <div class="w-full flex flex-col h-full">
    <!-- Lesson Content Preview -->
    <div v-if="shouldShowLessonContent" class="mb-4">
      <div
        class="flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg cursor-pointer select-none"
        @click="showLessonContent = !showLessonContent"
      >
        <div class="flex items-center gap-2 font-bold text-blue-700 dark:text-blue-300">
          <el-icon><component :is="config.contentIcon" /></el-icon>
          <span>{{ config.contentLabel }} ({{ currentLesson?.title }})</span>
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
          <div
            v-if="config.moduleType === 'GRAMMAR' || config.moduleType === 'READING'"
            class="ql-editor !p-0 text-gray-800 dark:text-gray-200"
            v-html="currentLesson?.content"
          ></div>

          <el-tabs v-else-if="config.moduleType === 'LISTENING'" type="border-card">
            <el-tab-pane label="🇬🇧 English">
              <div class="p-4 whitespace-pre-wrap">{{ currentLesson?.transcript }}</div>
            </el-tab-pane>
            <el-tab-pane v-if="currentLesson?.transcriptTranslation" label="🇻🇳 Tiếng Việt">
              <div class="p-4 whitespace-pre-wrap">{{ currentLesson?.transcriptTranslation }}</div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-collapse-transition>
    </div>

    <!-- Toolbar -->
    <div
      class="mb-5 flex flex-wrap gap-3 items-center bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm"
    >
      <div class="flex-1 flex flex-wrap gap-3 min-w-[300px]">
        <el-select
          v-if="config.showTopicSelector"
          v-model="selectedTopicId"
          :placeholder="`Chọn ${config.topicLabel}...`"
          filterable
          class="!w-full md:!w-56"
          @change="handleTopicChange"
        >
          <el-option
            v-for="topic in topics"
            :key="topic.id"
            :label="topic.name"
            :value="topic.id"
          />
        </el-select>

        <el-select
          v-model="currentLessonId"
          :placeholder="`Chọn ${config.lessonLabel}...`"
          filterable
          class="!w-full md:!w-64"
          @change="handleLessonChange"
          :loading="lessonsLoading"
          :disabled="config.showTopicSelector && !selectedTopicId"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.orderIndex}. ${lesson.title}`"
            :value="lesson.id"
          />
        </el-select>

        <el-input
          v-model="searchQuery"
          placeholder="Tìm nội dung..."
          :prefix-icon="Search"
          clearable
          class="!w-full md:!w-48"
        />

        <el-select v-model="filterType" placeholder="Loại câu hỏi" clearable class="!w-48">
          <el-option label="Tất cả" value="" />
          <el-option-group
            v-for="group in questionTypeGroups"
            :key="group.label"
            :label="group.label"
          >
            <el-option
              v-for="type in group.types"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-option-group>
        </el-select>
      </div>

      <div class="flex gap-2">
        <el-button
          type="success"
          :icon="Grid"
          @click="handleOpenTaskGroupManager"
          :disabled="!currentLessonId"
          class="!rounded-lg font-bold"
        >
          Task Groups
        </el-button>
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

        <el-button
          type="warning"
          plain
          :icon="Sort"
          :disabled="!currentLessonId"
          @click="handleValidateOrder"
        >
          Sắp xếp lại
        </el-button>

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
      :description="`Vui lòng chọn ${config.lessonLabel.toLowerCase()} để quản lý câu hỏi`"
      :image-size="120"
    />
    <el-empty
      v-else-if="!allQuestions || allQuestions.length === 0"
      :description="`${config.lessonLabel} này chưa có câu hỏi nào`"
      :image-size="120"
    >
      <div class="flex gap-3 mt-2">
        <el-button type="primary" @click="handleCreate">Tạo thủ công</el-button>
        <el-button type="success" @click="handleBulkCreate">Tạo nhiều câu hỏi</el-button>
      </div>
    </el-empty>

    <!-- Main Content: Task Groups or Flat List -->
    <el-card
      v-else
      shadow="never"
      class="!border-gray-300 dark:!border-gray-700 !rounded-xl !overflow-hidden flex flex-col"
      :body-style="{ padding: '0px', flex: '1', display: 'flex', flexDirection: 'column' }"
    >
      <!-- Toggle View Mode & Stats -->
      <div
        class="p-4 border-b flex flex-wrap justify-between items-center bg-gray-50 dark:bg-[#252525] gap-3"
      >
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-600 dark:text-gray-400">Chế độ hiển thị:</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="grouped">
              <el-icon class="mr-1"><Grid /></el-icon>
              Task Groups
            </el-radio-button>
            <el-radio-button value="flat">
              <el-icon class="mr-1"><List /></el-icon>
              Flat List
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="text-sm text-gray-500">
          Tổng: <strong class="text-blue-600">{{ allQuestions.length }}</strong> câu hỏi
          <span v-if="totalPoints > 0" class="ml-3 text-orange-600">
            | Điểm: <strong>{{ totalPoints }}</strong>
          </span>
        </div>
      </div>

      <!-- Task Group View -->
      <div v-if="viewMode === 'grouped'" class="flex-1 overflow-y-auto p-4">
        <TaskGroupDisplay
          :task-groups="taskGroups"
          :standalone-questions="standaloneQuestions"
          :show-stats="false"
          :selectable="false"
          :default-expanded="true"
          @view-detail="handleViewDetail"
          @action="handleAction"
        />
      </div>

      <!-- Flat Table View -->
      <div v-else class="flex-1 overflow-hidden flex flex-col">
        <el-table
          :data="paginatedQuestions"
          v-loading="questionStore.loading"
          style="width: 100%"
          border
          stripe
          row-key="id"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
          class="cursor-pointer hover:bg-gray-50 transition-colors flex-1"
          :header-cell-style="{ background: '#f9fafb', color: '#6b7280', fontWeight: '600' }"
        >
          <el-table-column type="selection" width="40" align="center" fixed="left" />

          <el-table-column label="STT" width="60" align="center" fixed="left">
            <template #default="{ row }">
              <span class="text-gray-500 font-mono text-xs font-bold">{{ row.orderIndex }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Nội dung câu hỏi" min-width="350">
            <template #default="{ row }">
              <div class="py-2 flex flex-col gap-1.5" @click.stop="handleViewDetail(row)">
                <div class="flex items-center gap-2">
                  <span
                    class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border"
                    :class="getQuestionTypeClass(row.questionType)"
                  >
                    {{ row.questionType.replace('_', ' ') }}
                  </span>

                  <span
                    class="text-[11px] font-bold text-orange-600 bg-orange-50 dark:bg-orange-900/20 px-1.5 rounded border border-orange-200 dark:border-orange-800"
                  >
                    +{{ row.points }}đ
                  </span>

                  <el-tag v-if="row.taskGroup" size="small" type="primary" class="!text-[10px]">
                    {{ row.taskGroup }}
                  </el-tag>
                </div>

                <div class="text-sm font-medium text-gray-800 dark:text-gray-200 line-clamp-2">
                  {{ truncateHtml(row.questionText, 150) }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Đáp án" min-width="200" class-name="hidden md:table-cell">
            <template #default="{ row }">
              <div class="text-sm" v-html="getAnswerPreview(row)"></div>
            </template>
          </el-table-column>

          <el-table-column label="" width="60" align="center" fixed="right">
            <template #default="{ row }">
              <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
                <span class="cursor-pointer flex justify-center items-center p-2" @click.stop>
                  <el-icon :size="18"><MoreFilled /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu class="min-w-[140px]">
                    <el-dropdown-item command="edit" :icon="Edit">Chỉnh sửa</el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      :icon="Delete"
                      divided
                      class="!text-red-500 hover:!bg-red-50"
                    >
                      Xóa
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <div
          class="py-3 px-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] flex justify-end"
        >
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
      </div>
    </el-card>

    <!-- Preview Dialog -->
    <el-dialog
      v-model="previewVisible"
      title="Chi tiết câu hỏi"
      width="700px"
      align-center
      destroy-on-close
      class="!rounded-xl"
    >
      <QuestionPreview v-if="previewQuestion" :question="previewQuestion" />

      <template #footer>
        <el-button @click="previewVisible = false">Đóng</el-button>
        <el-button type="primary" :icon="Edit" @click="handleAction('edit', previewQuestion)">
          Chỉnh sửa
        </el-button>
      </template>
    </el-dialog>

    <!-- Form Dialogs -->
    <QuestionFormDialog
      ref="questionFormRef"
      :config="config"
      :current-lesson="currentLesson"
      @submit="handleFormSuccess"
    />

    <TaskGroupManagementDialog
      ref="taskGroupDialogRef"
      :parent-type="config.moduleType"
      :lesson-id="currentLessonId"
      @refresh="handleFormSuccess"
    />

    <BulkCreateDialog ref="bulkDialogRef" :config="config" @success="handleFormSuccess" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Search,
  Edit,
  DocumentAdd,
  MoreFilled,
  ArrowDown,
  Sort,
  Grid,
  List,
} from '@element-plus/icons-vue'
import { useQuestionUtils } from '@/composables/questions/useQuestionUtils'
import { useQuestionStore } from '@/stores/admin/questionAdmin'
import QuestionPreview from './QuestionPreview.vue'
import QuestionFormDialog from './QuestionFormDialog.vue'
import BulkCreateDialog from './BulkCreateDialog.vue'
import TaskGroupDisplay from './TaskGroupDisplay.vue'
import TaskGroupManagementDialog from './TaskGroupManagementDialog.vue'

const props = defineProps({
  initLessonId: { type: Number, default: null },
  config: {
    type: Object,
    required: true,
    validator: (c) => c.moduleType && c.lessonLabel && c.contentLabel,
  },
})

// ==================== COMPOSABLES ====================
const { getQuestionTypeClass, getAnswerPreview, truncateHtml } = useQuestionUtils()
const questionStore = useQuestionStore()

// ==================== REFS ====================
const questionFormRef = ref(null)
const bulkDialogRef = ref(null)
const taskGroupDialogRef = ref(null)

// ==================== STATE ====================
const lessonsLoading = ref(false)
const selectedTopicId = ref(null)
const currentLessonId = ref(null)
const currentLesson = ref(null)
const lessons = ref([])
const topics = ref([])
const searchQuery = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const previewVisible = ref(false)
const previewQuestion = ref(null)
const showLessonContent = ref(false)
const viewMode = ref('grouped') // 'grouped' or 'flat'

// ==================== COMPUTED ====================

// ✅ Get all questions (flatten from grouped structure)
const allQuestions = computed(() => {
  let result = []

  // Add standalone questions
  if (questionStore.groupedQuestions?.standaloneQuestions) {
    result.push(...questionStore.groupedQuestions.standaloneQuestions)
  }

  // Add questions from tasks
  if (questionStore.groupedQuestions?.tasks) {
    questionStore.groupedQuestions.tasks.forEach((task) => {
      if (task.questions) {
        result.push(...task.questions)
      }
    })
  }

  // Sort by orderIndex
  result.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))

  return result
})

// ✅ Task Groups
const taskGroups = computed(() => questionStore.groupedQuestions?.tasks || [])
const handleOpenTaskGroupManager = () => {
  if (!currentLessonId.value) {
    console.error('No lesson ID available')
    return
  }
  taskGroupDialogRef.value?.open()
}

// ✅ Standalone Questions
const standaloneQuestions = computed(
  () => questionStore.groupedQuestions?.standaloneQuestions || [],
)

// ✅ Total Points
const totalPoints = computed(() => {
  return allQuestions.value.reduce((sum, q) => sum + (q.points || 0), 0)
})

const shouldShowLessonContent = computed(() => {
  if (!currentLesson.value) return false
  if (props.config.moduleType === 'GRAMMAR' || props.config.moduleType === 'READING') {
    return currentLesson.value.content && currentLesson.value.content.length > 50
  }
  if (props.config.moduleType === 'LISTENING') {
    return currentLesson.value.transcript && currentLesson.value.transcript.length > 50
  }
  return false
})

const filteredQuestions = computed(() => {
  let result = [...allQuestions.value]
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter((q) => {
      const text = q.questionText?.replace(/<[^>]*>?/gm, ' ').toLowerCase() || ''
      return text.includes(query)
    })
  }
  if (filterType.value) {
    result = result.filter((q) => q.questionType === filterType.value)
  }
  return result
})

const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredQuestions.value.slice(start, start + pageSize.value)
})

const questionTypeGroups = computed(() => {
  const baseTypes = [
    { label: 'Trắc nghiệm', value: 'MULTIPLE_CHOICE' },
    { label: 'Đúng/Sai', value: 'TRUE_FALSE' },
    { label: 'Điền từ', value: 'FILL_BLANK' },
    { label: 'Chia động từ', value: 'VERB_FORM' },
    { label: 'Trả lời ngắn', value: 'TEXT_ANSWER' },
  ]
  const advancedTypes = [
    { label: 'Nối từ', value: 'MATCHING' },
    { label: 'Sắp xếp câu', value: 'SENTENCE_BUILDING' },
    { label: 'Viết lại câu', value: 'SENTENCE_TRANSFORMATION' },
    { label: 'Tìm lỗi sai', value: 'ERROR_CORRECTION' },
    { label: 'Phát âm', value: 'PRONUNCIATION' },
    { label: 'Câu hỏi mở', value: 'OPEN_ENDED' },
  ]
  if (props.config.moduleType === 'LISTENING') {
    return [{ label: 'Cơ bản', types: baseTypes.slice(0, 4) }]
  }
  return [
    { label: 'Cơ bản', types: baseTypes },
    { label: 'Nâng cao', types: advancedTypes },
  ]
})

// ==================== METHODS ====================

const handleTopicChange = async (topicId) => {
  selectedTopicId.value = topicId
  currentLessonId.value = null
  currentLesson.value = null

  if (topicId && props.config.fetchLessons) {
    lessonsLoading.value = true
    try {
      lessons.value = await props.config.fetchLessons(topicId)
    } finally {
      lessonsLoading.value = false
    }
  }
}

const handleLessonChange = async (newId) => {
  currentLessonId.value = newId
  if (newId) {
    await loadQuestions()
    await loadLessonDetail()
  }
}

const loadLessonDetail = async () => {
  if (!currentLessonId.value) return

  try {
    if (props.config.fetchLessonDetail) {
      currentLesson.value = await props.config.fetchLessonDetail(currentLessonId.value)
    } else {
      currentLesson.value = lessons.value.find((l) => l.id === currentLessonId.value)
    }
  } catch (error) {
    console.error('Failed to load lesson detail:', error)
    currentLesson.value = lessons.value.find((l) => l.id === currentLessonId.value)
  }
}

const loadQuestions = async () => {
  if (!currentLessonId.value) return
  try {
    await questionStore.fetchQuestions(props.config.moduleType, currentLessonId.value)
  } catch (error) {
    console.error('Failed to load questions:', error)
  }
}

const handleRowClick = (row) => {
  handleViewDetail(row)
}

const handleViewDetail = async (row) => {
  try {
    previewQuestion.value = row
    previewVisible.value = true
  } catch (error) {
    console.error('Failed to load question detail:', error)
    previewQuestion.value = row
    previewVisible.value = true
  }
}

const handleCreate = () => {
  if (!questionFormRef.value) {
    console.error('questionFormRef is null')
    return
  }
  questionFormRef.value.openCreate(currentLessonId.value)
}

const handleBulkCreate = () => {
  if (!bulkDialogRef.value) {
    console.error('bulkDialogRef is null')
    return
  }
  bulkDialogRef.value.open(currentLesson.value)
}

const handleAction = (cmd, row) => {
  if (cmd === 'detail') handleViewDetail(row)
  if (cmd === 'edit') {
    previewVisible.value = false
    questionFormRef.value?.openEdit(row)
  }
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
    await questionStore.deleteQuestion(props.config.moduleType, currentLessonId.value, question.id)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleBulkDelete = async () => {
  try {
    await ElMessageBox.confirm(`Xóa ${selectedRows.value.length} câu hỏi đã chọn?`, 'Cảnh báo', {
      type: 'warning',
    })
    const ids = selectedRows.value.map((r) => r.id)
    await questionStore.deleteQuestionsBulk(props.config.moduleType, currentLessonId.value, ids)
    selectedRows.value = []
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleValidateOrder = async () => {
  try {
    await ElMessageBox.confirm(
      'Hành động này sẽ sắp xếp và đánh số lại toàn bộ câu hỏi từ 1 đến hết. Bạn có chắc chắn không?',
      'Xác nhận sắp xếp',
      { type: 'warning', confirmButtonText: 'Đồng ý', cancelButtonText: 'Hủy' },
    )
    await questionStore.fixOrderIndexes(props.config.moduleType, currentLessonId.value)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleFormSuccess = async () => {
  await loadQuestions()
}

const handleSelectionChange = (val) => (selectedRows.value = val)
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}
const handlePageChange = (val) => (currentPage.value = val)

// ==================== LIFECYCLE ====================
const initializeData = async () => {
  if (props.config.showTopicSelector && props.config.fetchTopics) {
    topics.value = await props.config.fetchTopics()
  }
  if (props.config.fetchAllLessons) {
    lessonsLoading.value = true
    try {
      lessons.value = await props.config.fetchAllLessons()
    } finally {
      lessonsLoading.value = false
    }
  }
  if (props.initLessonId) {
    currentLessonId.value = props.initLessonId
    await loadQuestions()
    await loadLessonDetail()
  }
}

watch(
  () => props.initLessonId,
  (val) => {
    if (val && val !== currentLessonId.value) {
      currentLessonId.value = val
      loadQuestions()
      loadLessonDetail()
    }
  },
)

onMounted(initializeData)

defineExpose({
  refresh: loadQuestions,
})
</script>

<style scoped>
.rotate-180 {
  transform: rotate(180deg);
}
</style>
