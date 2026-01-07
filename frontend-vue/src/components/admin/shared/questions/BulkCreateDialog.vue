<!-- src/components/admin/shared/questions/BulkCreateDialog.vue - UPDATED -->
<template>
  <el-dialog
    v-model="visible"
    width="98%"
    top="2vh"
    class="!rounded-xl"
    :close-on-click-modal="false"
    :show-close="false"
    destroy-on-close
  >
    <template #header>
      <div class="flex justify-between items-center px-6 py-3 border-b">
        <div class="flex items-center gap-4">
          <div>
            <h4 class="text-lg font-bold m-0">Soạn thảo hàng loạt</h4>
            <span class="text-xs text-gray-500" v-if="currentLesson">
              {{ config.lessonLabel }}: <b class="text-blue-600">{{ currentLesson.title }}</b>
            </span>
          </div>

          <el-button
            v-if="hasLessonContent"
            @click="showContent = !showContent"
            :type="showContent ? 'primary' : 'default'"
            link
            size="small"
          >
            <el-icon><component :is="config.contentIcon" /></el-icon>
            {{ showContent ? `Tắt ${config.contentLabel}` : `Xem ${config.contentLabel}` }}
          </el-button>
        </div>

        <el-button @click="handleClose" :icon="Close" circle text />
      </div>
    </template>

    <div class="flex overflow-hidden bg-gray-100 dark:bg-[#121212] h-[80vh]">
      <!-- Left Panel: Lesson Content -->
      <transition name="el-fade-in-linear">
        <div
          v-if="showContent && hasLessonContent"
          class="w-1/3 min-w-[400px] max-w-[50%] bg-white dark:bg-[#1d1d1d] border-r flex flex-col shadow-lg"
        >
          <div class="h-10 px-4 bg-gray-50 dark:bg-[#252525] border-b font-bold flex items-center">
            <el-icon class="mr-2"><component :is="config.contentIcon" /></el-icon>
            {{ config.contentLabel }}
          </div>

          <template v-if="config.moduleType !== 'LISTENING'">
            <el-tabs v-if="currentLesson.contentTranslation" type="border-card" class="flex-1">
              <el-tab-pane label="📖 English">
                <div class="p-6 overflow-y-auto h-full">
                  <div class="ql-editor !p-0" v-html="currentLesson.content"></div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="🇻🇳 Tiếng Việt">
                <div class="p-6 overflow-y-auto h-full">
                  <div class="ql-editor !p-0" v-html="currentLesson.contentTranslation"></div>
                </div>
              </el-tab-pane>
            </el-tabs>
            <div v-else class="p-6 overflow-y-auto">
              <div class="ql-editor !p-0" v-html="currentLesson.content"></div>
            </div>
          </template>

          <el-tabs v-else type="border-card" class="flex-1">
            <el-tab-pane label="🎧 English">
              <div class="p-6 overflow-y-auto h-full whitespace-pre-wrap">
                {{ currentLesson.transcript }}
              </div>
            </el-tab-pane>
            <el-tab-pane v-if="currentLesson.transcriptTranslation" label="🇻🇳 Tiếng Việt">
              <div class="p-6 overflow-y-auto h-full whitespace-pre-wrap">
                {{ currentLesson.transcriptTranslation }}
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </transition>

      <!-- Right Panel: Question List -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <div
          class="h-12 px-6 bg-gray-50 dark:bg-[#252525] border-b flex justify-between items-center"
        >
          <div class="text-sm text-gray-500">
            Danh sách: <b class="text-blue-600">{{ questionList.length }}</b> câu hỏi
            <span v-if="totalPoints > 0" class="ml-3 text-orange-600">
              | Tổng điểm: <b>{{ totalPoints }}</b>
            </span>
          </div>
          <div class="flex gap-2">
            <el-button @click="removeAll" :icon="Delete" type="danger" plain size="small">
              Xóa hết
            </el-button>
            <el-button type="primary" :icon="Plus" @click="addNewQuestion" size="small">
              Thêm dòng
            </el-button>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <el-empty v-if="questionList.length === 0" description="Chưa có câu hỏi nào">
            <el-button type="primary" :icon="Plus" @click="addNewQuestion">
              Thêm câu hỏi đầu tiên
            </el-button>
          </el-empty>

          <div v-else class="space-y-4">
            <SingleQuestionEditor
              v-for="(item, index) in questionList"
              :key="index"
              :model-value="item"
              :index="index"
              :task-groups="taskGroups"
              @update:model-value="(val) => updateQuestion(index, val)"
              @remove="removeQuestion(index)"
              @clone="cloneQuestion(index)"
            />

            <div class="text-center pt-4">
              <el-button
                type="primary"
                plain
                :icon="Plus"
                class="!w-full !max-w-md !border-dashed !h-12"
                @click="addNewQuestion"
              >
                Thêm câu hỏi tiếp theo
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center px-6 py-3 border-t">
        <div class="text-sm text-gray-600">
          Tổng: <strong class="text-blue-600">{{ questionList.length }}</strong> câu hỏi
          <span v-if="totalPoints > 0" class="ml-3">
            | Điểm: <strong class="text-green-600">{{ totalPoints }}</strong>
          </span>
        </div>
        <div class="flex gap-3">
          <el-button @click="visible = false" :disabled="isLoading">Hủy</el-button>
          <el-button
            type="primary"
            :loading="isLoading"
            :disabled="questionList.length === 0"
            @click="handleSubmit"
          >
            Lưu tất cả ({{ questionList.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, defineAsyncComponent } from 'vue'
import { Plus, Delete, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQuestionStore } from '@/stores/admin/questionAdmin'

const SingleQuestionEditor = defineAsyncComponent(() => import('./SingleQuestionEditor.vue'))

const props = defineProps({
  config: {
    type: Object,
    required: true,
    validator: (config) => {
      if (!config || !config.moduleType) {
        console.error('❌ BulkCreateDialog: config.moduleType is required!')
        return false
      }
      return true
    },
  },
})

const emit = defineEmits(['success'])

const questionStore = useQuestionStore()

const isLoading = computed(() => questionStore.loading)
const visible = ref(false)
const currentLesson = ref(null)
const questionList = ref([])
const showContent = ref(true)
const startOrderIndex = ref(1)

// ✅ NEW: TaskGroups
const taskGroups = computed(() => questionStore.taskGroups)

const hasLessonContent = computed(() => {
  if (!currentLesson.value) return false
  if (props.config.moduleType === 'LISTENING') {
    return !!currentLesson.value.transcript
  }
  return !!currentLesson.value.content
})

const totalPoints = computed(() => {
  return questionList.value.reduce((sum, q) => sum + (q.points || 0), 0)
})

const createEmptyQuestion = () => ({
  parentType: props.config.moduleType,
  questionType: 'MULTIPLE_CHOICE',
  questionText: '',
  points: 10,
  orderIndex: 0,
  explanation: '',
  metadata: {},
  isCollapsed: false,
  taskGroupId: null, // ✅ NEW: thay thế 3 fields cũ
})

const open = async (lesson) => {
  currentLesson.value = lesson
  questionList.value = []
  showContent.value = !!hasLessonContent.value
  visible.value = true

  // ✅ Load TaskGroups
  try {
    await questionStore.fetchTaskGroups(props.config.moduleType, lesson.id)
  } catch (e) {
    console.error('Failed to load task groups:', e)
  }

  try {
    startOrderIndex.value = await questionStore.fetchNextOrder(props.config.moduleType, lesson.id)
  } catch (e) {
    console.error('Fetch next order index error:', e)
    startOrderIndex.value = 1
  }

  addNewQuestion()
}

const addNewQuestion = () => {
  questionList.value.forEach((q) => (q.isCollapsed = true))
  questionList.value.push(createEmptyQuestion())
}

const updateQuestion = (index, newVal) => {
  questionList.value[index] = newVal
}

const removeQuestion = (index) => {
  questionList.value.splice(index, 1)
}

const removeAll = async () => {
  try {
    await ElMessageBox.confirm('Xóa hết danh sách?', 'Cảnh báo', { type: 'warning' })
    questionList.value = []
  } catch {
    // Cancelled
  }
}

const cloneQuestion = (index) => {
  const original = questionList.value[index]
  const cloned = JSON.parse(JSON.stringify(original))
  cloned.isCollapsed = false
  questionList.value.splice(index + 1, 0, cloned)
  ElMessage.success('Đã nhân bản câu hỏi')
}

const handleClose = () => {
  if (questionList.value.length > 0) {
    ElMessageBox.confirm('Bạn có thay đổi chưa lưu. Đóng dialog này?', 'Xác nhận', {
      type: 'warning',
      confirmButtonText: 'Đóng',
      cancelButtonText: 'Tiếp tục chỉnh sửa',
    })
      .then(() => {
        visible.value = false
        questionList.value = []
      })
      .catch(() => {
        // Cancelled - do nothing
      })
  } else {
    visible.value = false
  }
}

const handleSubmit = async () => {
  if (questionList.value.length === 0) {
    ElMessage.warning('Chưa có câu hỏi nào để lưu')
    return
  }

  // ✅ ENHANCED VALIDATION
  for (let i = 0; i < questionList.value.length; i++) {
    const q = questionList.value[i]

    if (!q.questionType) {
      ElMessage.error(`Câu ${i + 1}: Chưa chọn loại câu hỏi`)
      return
    }

    // ✅ FIX: Check metadata instead of q.options
    const metadata = q.metadata || {}

    if (['MULTIPLE_CHOICE', 'TRUE_FALSE', 'COMPLETE_CONVERSATION'].includes(q.questionType)) {
      const options = metadata.options || []
      if (options.length < 2) {
        ElMessage.error(`Câu ${i + 1}: Cần ít nhất 2 đáp án`)
        return
      }
      const hasCorrect = options.some((opt) => opt.isCorrect === true)
      if (!hasCorrect) {
        ElMessage.error(`Câu ${i + 1}: Chưa chọn đáp án đúng`)
        return
      }
    }

    if (['FILL_BLANK', 'VERB_FORM', 'TEXT_ANSWER'].includes(q.questionType)) {
      const blanks = metadata.blanks || []
      if (blanks.length === 0) {
        ElMessage.error(`Câu ${i + 1}: Chưa có vị trí điền từ`)
        return
      }
      for (const blank of blanks) {
        const answers = blank.correctAnswers || []
        if (answers.length === 0) {
          ElMessage.error(`Câu ${i + 1}: Vị trí [${blank.position}] chưa có đáp án`)
          return
        }
      }
    }

    if (q.questionType === 'MATCHING') {
      const pairs = metadata.pairs || []
      if (pairs.length < 2) {
        ElMessage.error(`Câu ${i + 1}: Cần ít nhất 2 cặp nối`)
        return
      }
    }
  }

  try {
    // ✅ FIX: Flatten metadata to root level like QuestionFormDialog does
    const questionsToSave = questionList.value.map((q, idx) => {
      const metadata = q.metadata || {}

      // ✅ Merge everything at root level (same as QuestionFormDialog)
      const payload = {
        parentType: props.config.moduleType,
        questionType: q.questionType,
        questionText: q.questionText || '',
        points: q.points || 10,
        orderIndex: startOrderIndex.value + idx,
        taskGroupId: q.taskGroupId || null,
        explanation: q.explanation || metadata.explanation || null,

        // ✅ Spread metadata fields at root level
        ...metadata
      }

      return payload
    })

    console.log('📤 Submitting questions:', questionsToSave)

    await questionStore.createQuestionsBulk(
      props.config.moduleType,
      currentLesson.value.id,
      questionsToSave
    )

    visible.value = false
    ElMessage.success(`Đã tạo ${questionList.value.length} câu hỏi thành công!`)
    emit('success')

    questionList.value = []
  } catch (error) {
    console.error('❌ Bulk create error:', error)
    ElMessage.error(error.response?.data?.message || 'Lỗi khi tạo câu hỏi')
  }
}

defineExpose({ open })
</script>

<style scoped>
.el-fade-in-linear-enter-active,
.el-fade-in-linear-leave-active {
  transition: all 0.3s;
}
</style>
