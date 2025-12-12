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
      <!-- Lesson Content (Reading) -->
      <div v-if="currentLesson?.content && showContent" class="mb-4 border-b pb-4">
        <div class="flex items-center justify-between mb-2">
          <h4 class="font-bold text-gray-700 dark:text-gray-300 flex items-center gap-2">
            <el-icon><Reading /></el-icon>
            Nội dung bài đọc: {{ currentLesson.title }}
          </h4>
          <el-button type="primary" link size="small" @click="showContent = !showContent">
            Thu gọn
          </el-button>
        </div>

        <el-tabs type="border-card" class="! rounded-lg">
          <el-tab-pane label="📖 Tiếng Anh">
            <div
              class="ql-editor ! p-4 max-h-60 overflow-y-auto bg-gray-50 dark:bg-gray-800 rounded"
              v-html="currentLesson.content"
            ></div>
          </el-tab-pane>
          <el-tab-pane label="🇻🇳 Tiếng Việt" v-if="currentLesson.contentTranslation">
            <div
              class="ql-editor ! p-4 max-h-60 overflow-y-auto bg-gray-50 dark:bg-gray-800 rounded"
              v-html="currentLesson.contentTranslation"
            ></div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else-if="currentLesson?.content" class="mb-4">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <span class="flex items-center gap-2">
              Bài đọc: <strong>{{ currentLesson.title }}</strong>
              <el-button type="primary" link size="small" @click="showContent = true">
                Xem nội dung
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
              class="!w-full ! max-w-md ! border-dashed ! h-12"
              @click="addNewQuestion"
              >Thêm câu hỏi tiếp theo</el-button
            >
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div
        class="flex justify-between items-center px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] z-20"
      >
        <div class="flex items-center gap-3">
          <el-button
            :icon="Delete"
            type="danger"
            plain
            @click="removeAll"
            :disabled="questionList.length === 0"
          >
            Xóa tất cả
          </el-button>
          <span class="text-sm text-gray-500">Tổng: {{ questionList.length }} câu hỏi</span>
        </div>

        <div class="flex gap-3">
          <el-button @click="visible = false" class="!rounded-lg !h-10 !px-6">Hủy bỏ</el-button>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
            :disabled="questionList.length === 0"
            class="!rounded-lg ! font-bold px-8 ! h-10 !text-base shadow-lg shadow-blue-500/20"
          >
            Lưu tất cả ({{ questionList.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, defineAsyncComponent } from 'vue'
import { useReadingStore } from '@/stores/reading' // 🆕 Reading Store
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Reading } from '@element-plus/icons-vue'

const SingleQuestionEditor = defineAsyncComponent(
  () => import('@/components/admin/questions/SingleQuestionEditor.vue'),
)

const props = defineProps({ lessonId: Number })
const emit = defineEmits(['success'])
const store = useReadingStore() // 🆕 Reading Store

const visible = ref(false)
const loading = ref(false)
const questionList = ref([])
const currentLesson = ref(null)
const showContent = ref(true)

const createEmptyQuestion = () => ({
  questionType: 'MULTIPLE_CHOICE',
  questionText: '',
  points: 10,
  explanation: '',
  metadata: {},
  isCollapsed: false,
  orderIndex: 0,
})

const open = async () => {
  questionList.value = [createEmptyQuestion()]
  visible.value = true

  if (props.lessonId) {
    try {
      const res = await store.fetchLessonById(props.lessonId) // 🆕 Dùng Reading store
      currentLesson.value = res
      showContent.value = !!(res && res.content && res.content.length > 50)
    } catch (e) {
      console.error('Failed to load lesson:', e)
      ElMessage.error('Không thể tải thông tin bài đọc')
    }
  }
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

const removeAll = () => {
  ElMessageBox.confirm('Xóa hết danh sách? ', 'Warning', { type: 'warning' })
    .then(() => (questionList.value = []))
    .catch(() => {})
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
      ElMessage.error(`Câu ${i + 1}:  Chưa chọn loại câu hỏi`)
      return
    }
    // Add more validation if needed
  }

  loading.value = true
  try {
    // Map to CreateQuestionDTO format
    const questionsPayload = questionList.value.map((q, idx) => {
      const base = {
        parentId: props.lessonId,
        parentType: 'READING',
        questionType: q.questionType,
        questionText: q.questionText || '',
        points: q.points || 10,
        orderIndex: idx + 1,
        explanation: q.explanation || '',
      }

      // Map metadata based on question type
      const metadata = q.metadata || {}
      switch (q.questionType) {
        case 'MULTIPLE_CHOICE':
        case 'TRUE_FALSE':
          return { ...base, options: metadata.options }
        case 'FILL_BLANK':
          return { ...base, blanks: metadata.blanks, wordBank: metadata.wordBank }
        case 'VERB_FORM':
          return { ...base, blanks: metadata.blanks }
        case 'TEXT_ANSWER':
          return {
            ...base,
            correctAnswer: metadata.correctAnswer,
            caseSensitive: metadata.caseSensitive,
          }
        case 'MATCHING':
          return { ...base, pairs: metadata.pairs }
        case 'SENTENCE_BUILDING':
          return { ...base, words: metadata.words, correctSentence: metadata.correctSentence }
        case 'SENTENCE_TRANSFORMATION':
          return {
            ...base,
            originalSentence: metadata.originalSentence,
            beginningPhrase: metadata.beginningPhrase,
            correctAnswers: metadata.correctAnswers,
          }
        case 'ERROR_CORRECTION':
          return { ...base, errorText: metadata.errorText, correction: metadata.correction }
        case 'PRONUNCIATION':
          return {
            ...base,
            words: metadata.words,
            categories: metadata.categories,
            classifications: metadata.classifications,
          }
        case 'OPEN_ENDED':
          return {
            ...base,
            suggestedAnswer: metadata.suggestedAnswer,
            timeLimitSeconds: metadata.timeLimitSeconds,
            minWord: metadata.minWord,
            maxWord: metadata.maxWord,
          }
        default:
          return base
      }
    })

    await store.createQuestionsInBulk(props.lessonId, questionsPayload) // 🆕 Reading store
    ElMessage.success(`Đã tạo ${questionList.value.length} câu hỏi thành công! `)
    visible.value = false
    emit('success')
  } catch (error) {
    console.error('Bulk create error:', error)
    ElMessage.error(error.response?.data?.message || 'Lỗi khi tạo câu hỏi')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
