<template>
  <el-dialog
    v-model="visible"
    title="Tạo nhiều câu hỏi cùng lúc"
    width="95%"
    top="3vh"
    :close-on-click-modal="false"
    destroy-on-close
    class="!rounded-2xl"
  >
    <div class="flex flex-col h-[85vh]">
      <!-- Lesson Content (Transcript) -->
      <div v-if="currentLesson?.transcript && showContent" class="mb-4 border-b pb-4">
        <div class="flex items-center justify-between mb-2">
          <h4 class="font-bold text-gray-700 dark:text-gray-300 flex items-center gap-2">
            <el-icon><Headset /></el-icon>
            Transcript: {{ currentLesson.title }}
          </h4>
          <el-button type="primary" link size="small" @click="showContent = !showContent">
            Thu gọn
          </el-button>
        </div>

        <el-tabs type="border-card" class="!rounded-lg">
          <el-tab-pane label="🏴 Tiếng Anh">
            <div
              class="p-4 max-h-60 overflow-y-auto bg-gray-50 dark:bg-gray-800 rounded whitespace-pre-wrap leading-relaxed"
            >
              {{ formattedTranscript }}
            </div>
          </el-tab-pane>
          <el-tab-pane label="🇻🇳 Tiếng Việt" v-if="currentLesson.transcriptTranslation">
            <div
              class="p-4 max-h-60 overflow-y-auto bg-gray-50 dark:bg-gray-800 rounded whitespace-pre-wrap leading-relaxed"
            >
              {{ formattedTranslation }}
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else-if="currentLesson?.transcript" class="mb-4">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <span class="flex items-center gap-2">
              Bài nghe: <strong>{{ currentLesson.title }}</strong>
              <el-button type="primary" link size="small" @click="showContent = true">
                Xem transcript
              </el-button>
            </span>
          </template>
        </el-alert>
      </div>

      <!-- Question List -->
      <div class="flex-1 overflow-y-auto pr-2">
        <div v-if="questionList.length === 0" class="text-center py-10">
          <el-empty description="Chưa có câu hỏi nào">
            <el-button type="primary" :icon="Plus" @click="addNewQuestion">
              Thêm câu hỏi đầu tiên
            </el-button>
          </el-empty>
        </div>

        <div v-else class="space-y-4">
          <SingleQuestionEditor
            v-for="(question, index) in questionList"
            :key="index"
            :model-value="question"
            :index="index"
            @update:model-value="(val) => updateQuestion(index, val)"
            @remove="removeQuestion(index)"
            @clone="cloneQuestion(index)"
          />

          <div v-if="questionList.length > 0" class="text-center pt-4 pb-10">
            <el-button
              type="primary"
              plain
              :icon="Plus"
              class="!w-full !max-w-md !border-dashed !h-12"
              @click="addNewQuestion"
            >
              <span class="font-bold">Thêm câu hỏi</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center">
        <div class="text-sm text-gray-600">
          Tổng: <strong class="text-blue-600">{{ questionList.length }}</strong> câu hỏi
          <span v-if="totalPoints > 0" class="ml-3">
            | Tổng điểm: <strong class="text-green-600">{{ totalPoints }}</strong>
          </span>
        </div>
        <div class="flex gap-2">
          <el-button @click="handleClose">Hủy</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="questionList.length === 0"
            @click="handleSubmit"
          >
            Tạo {{ questionList.length }} câu hỏi
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, Headset } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useListeningStore } from '@/stores/listening'
import SingleQuestionEditor from '@/components/admin/questions/SingleQuestionEditor.vue'
import { formatTranscript } from '@/utils/textFormatter'

defineProps({
  lessonId: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['success'])

// Store
const store = useListeningStore()

// State
const visible = ref(false)
const currentLesson = ref(null)
const questionList = ref([])
const showContent = ref(true)
const submitting = ref(false)

// Computed
const totalPoints = computed(() => {
  return questionList.value.reduce((sum, q) => sum + (q.points || 0), 0)
})

const formattedTranscript = computed(() => {
  return formatTranscript(currentLesson.value?.transcript)
})

const formattedTranslation = computed(() => {
  return formatTranscript(currentLesson.value?.transcriptTranslation)
})

// Methods
const createEmptyQuestion = () => ({
  parentType: 'LISTENING',
  questionType: 'MULTIPLE_CHOICE',
  questionText: '',
  points: 10,
  explanation: '',
  metadata: {},
  isCollapsed: false,
})

const open = async (lesson) => {
  currentLesson.value = lesson
  questionList.value = [createEmptyQuestion()]
  showContent.value = true
  visible.value = true
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

const cloneQuestion = (index) => {
  const original = questionList.value[index]
  const cloned = JSON.parse(JSON.stringify(original))
  cloned.isCollapsed = false
  questionList.value.splice(index + 1, 0, cloned)
  ElMessage.success('Đã nhân bản câu hỏi')
}

const handleSubmit = async () => {
  if (questionList.value.length === 0) {
    ElMessage.warning('Chưa có câu hỏi nào để lưu')
    return
  }

  // Validate
  for (let i = 0; i < questionList.value.length; i++) {
    const q = questionList.value[i]
    if (!q.questionType) {
      ElMessage.error(`Câu ${i + 1}: Chưa chọn loại câu hỏi`)
      return
    }
  }

  submitting.value = true
  try {
    const questionsPayload = questionList.value.map((q, idx) => {
      const base = {
        parentId: currentLesson.value.id,
        parentType: 'LISTENING',
        questionType: q.questionType,
        questionText: q.questionText || '',
        points: q.points || 10,
        orderIndex: idx + 1,
        explanation: q.explanation || '',
      }

      const metadata = q.metadata || {}
      switch (q.questionType) {
        case 'MULTIPLE_CHOICE':
        case 'TRUE_FALSE':
          return { ...base, options: metadata.options }
        case 'FILL_BLANK':
          return { ...base, blanks: metadata.blanks, wordBank: metadata.wordBank }
        case 'TEXT_ANSWER':
          return {
            ...base,
            correctAnswer: metadata.correctAnswer,
            caseSensitive: metadata.caseSensitive,
          }
        default:
          return base
      }
    })

    await store.bulkCreateQuestions(currentLesson.value.id, questionsPayload)
    ElMessage.success(`Đã tạo ${questionList.value.length} câu hỏi thành công!`)
    handleClose()
    emit('success')
  } catch (error) {
    console.error('Bulk create error:', error)
    ElMessage.error(error.response?.data?.message || 'Lỗi khi tạo câu hỏi')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  visible.value = false
  currentLesson.value = null
  questionList.value = []
}

defineExpose({ open })
</script>
