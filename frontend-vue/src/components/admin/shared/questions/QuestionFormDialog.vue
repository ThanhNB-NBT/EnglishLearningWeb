<template>
  <el-dialog
    v-model="dialogVisible"
    width="90%"
    top="5vh"
    class="!rounded-xl !p-0 overflow-hidden flex flex-col"
    :close-on-click-modal="false"
    destroy-on-close
    :show-close="false"
  >
    <template #header="{ close }">
      <div
        class="flex justify-between items-center px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d]"
      >
        <div class="flex items-center gap-4">
          <h4 class="text-xl font-bold text-gray-800 dark:text-white m-0">{{ dialogTitle }}</h4>

          <div
            v-if="currentLesson?.content"
            class="pl-4 border-l border-gray-300 dark:border-gray-600"
          >
            <el-button
              @click="showContent = !showContent"
              :type="showContent ? 'primary' : 'default'"
              link
              size="small"
              class="!font-bold"
            >
              <el-icon class="mr-1"><Reading /></el-icon>
              {{ showContent ? 'Tắt bài đọc' : 'Xem bài đọc' }}
            </el-button>
          </div>
        </div>

        <button
          @click="handleClose"
          class="w-8 h-8 flex items-center justify-center rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-[#333] transition-all"
        >
          <el-icon :size="20"><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="flex-1 flex overflow-hidden bg-gray-100 dark:bg-[#121212] h-[80vh]">
      <transition name="el-fade-in-linear">
        <div
          v-if="showContent && currentLesson?.content"
          class="w-1/3 min-w-[350px] bg-white dark:bg-[#1d1d1d] border-r border-gray-200 dark:border-gray-700 flex flex-col shadow-lg z-10"
        >
          <div
            class="h-10 px-4 bg-gray-50 dark:bg-[#252525] border-b border-gray-200 dark:border-gray-700 font-bold text-gray-600 dark:text-gray-300 flex items-center shrink-0"
          >
            <el-icon class="mr-2"><Document /></el-icon> Nội dung bài học
          </div>
          <div class="flex-1 overflow-y-auto p-6">
            <div
              class="ql-editor !p-0 text-gray-800 dark:text-gray-200 text-base leading-relaxed"
              v-html="currentLesson.content"
            ></div>
          </div>
        </div>
      </transition>

      <div class="flex-1 overflow-y-auto p-6">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="top"
          class="max-w-4xl mx-auto pb-10"
        >
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-4">
            <el-form-item label="Loại câu hỏi" prop="questionType">
              <el-select
                v-model="formData.questionType"
                class="!w-full"
                :disabled="dialogMode === 'edit'"
                @change="handleQuestionTypeChange"
                filterable
              >
                <el-option-group label="Cơ bản">
                  <el-option label="Trắc nghiệm" value="MULTIPLE_CHOICE" />
                  <el-option label="Đúng/Sai" value="TRUE_FALSE" />
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
            </el-form-item>

            <el-form-item label="Thứ tự" prop="orderIndex">
              <el-input-number
                v-model="formData.orderIndex"
                :min="1"
                class="!w-full"
                controls-position="right"
              />
            </el-form-item>

            <el-form-item label="Điểm số" prop="points">
              <el-input-number
                v-model="formData.points"
                :min="1"
                class="!w-full"
                controls-position="right"
              />
            </el-form-item>
          </div>

          <el-form-item prop="questionText" class="!mb-6">
            <template #label>
              <div class="flex justify-between w-full">
                <span>Nội dung câu hỏi</span>
                <el-tag size="small" type="info" effect="plain" class="font-normal"
                  >Có thể để trống</el-tag
                >
              </div>
            </template>
            <div
              class="w-full border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden bg-white dark:bg-[#252525]"
            >
              <QuillRichEditor
                v-model="formData.questionText"
                placeholder="Nhập nội dung (hoặc để trống)..."
                height="150px"
              />
            </div>
          </el-form-item>

          <div
            class="bg-gray-50 dark:bg-[#252525] p-5 rounded-xl border border-dashed border-gray-300 dark:border-gray-600 mb-6"
          >
            <h3
              class="text-sm font-bold text-blue-600 dark:text-blue-400 uppercase mb-4 flex items-center gap-2"
            >
              <el-icon><Tools /></el-icon> Cấu hình đáp án
            </h3>

            <component
              :is="currentFormComponent"
              v-model:metadata="formData.metadata"
              :question-type="formData.questionType"
            />
          </div>

          <el-form-item label="Giải thích đáp án" prop="explanation">
            <el-input
              v-model="formData.explanation"
              type="textarea"
              :rows="3"
              placeholder="Giải thích..."
            />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <div
        class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] z-20"
      >
        <el-button @click="handleClose" class="!rounded-lg !h-10 !px-6">Hủy</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="submitForm"
          class="!rounded-lg !font-bold px-8 !h-10"
        >
          {{ submitButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, defineAsyncComponent, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { useGrammarQuestionForm } from '@/composables/grammar/useGrammarQuestions'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { Close, Reading, Document, Tools, EditPen } from '@element-plus/icons-vue'

// Import Components Lazy
const MultipleChoiceForm = defineAsyncComponent(
  () => import('@/components/admin/questions/MultipleChoiceForm.vue'),
)
const FillBlankForm = defineAsyncComponent(
  () => import('@/components/admin/questions/FillBlankForm.vue'),
)
const MatchingForm = defineAsyncComponent(
  () => import('@/components/admin/questions/MatchingForm.vue'),
)
const SentenceBuildingForm = defineAsyncComponent(
  () => import('@/components/admin/questions/SentenceBuildingForm.vue'),
)
const ErrorCorrectionForm = defineAsyncComponent(
  () => import('@/components/admin/questions/ErrorCorrectionForm.vue'),
)
const PronunciationForm = defineAsyncComponent(
  () => import('@/components/admin/questions/PronunciationForm.vue'),
)
const OpenEndedForm = defineAsyncComponent(
  () => import('@/components/admin/questions/OpenEndedForm.vue'),
)
const SentenceTransformationForm = defineAsyncComponent(
  () => import('@/components/admin/questions/SentenceTransformationForm.vue'),
)
const VerbFormForm = defineAsyncComponent(
  () => import('@/components/admin/questions/VerbFormForm.vue'),
)

const props = defineProps({ lessonId: Number })
const emit = defineEmits(['success'])
const formRef = ref(null)
const loading = ref(false)
const store = useGrammarStore()
const currentLesson = ref(null)
const showContent = ref(true)

const {
  dialogVisible,
  dialogMode,
  formData,
  formRules,
  dialogTitle,
  submitButtonText,
  openCreateDialog: originalOpenCreate,
  openEditDialog: originalOpenEdit,
  closeDialog,
  handleQuestionTypeChange,
  handleSubmit,
} = useGrammarQuestionForm()

// --- LOGIC LOAD BÀI ĐỌC (ĐÃ SỬA) ---
const loadLessonContent = async (lId) => {
  if (!lId) return
  try {
    const res = await store.fetchLessonById(lId)
    currentLesson.value = res
    // Chỉ hiện panel nếu có nội dung > 50 ký tự
    showContent.value = !!(res && res.content && res.content.length > 50)
  } catch (e) {
    console.error(e)
  }
}

const openCreate = async (lId) => {
  // Ưu tiên lId truyền vào, nếu không thì dùng props.lessonId
  const idToUse = lId || props.lessonId
  await originalOpenCreate(idToUse)
  await loadLessonContent(idToUse)
}

const openEdit = async (q) => {
  originalOpenEdit(q)
  // Lấy lessonId từ parentId của câu hỏi, hoặc props
  const idToUse = q.parentId || q.lessonId || props.lessonId
  if (idToUse) await loadLessonContent(idToUse)
}
// ------------------------------------

const handleClose = () => {
  closeDialog()
  if (formRef.value) formRef.value.resetFields()
}

const submitForm = async () => {
  loading.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      emit('success')
      closeDialog()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const currentFormComponent = computed(() => {
  const type = formData.value.questionType
  switch (type) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      return MultipleChoiceForm
    case 'FILL_BLANK':
    case 'TEXT_ANSWER':
      return FillBlankForm
    case 'VERB_FORM':
      return VerbFormForm
    case 'MATCHING':
      return MatchingForm
    case 'SENTENCE_BUILDING':
      return SentenceBuildingForm
    case 'SENTENCE_TRANSFORMATION':
      return SentenceTransformationForm
    case 'ERROR_CORRECTION':
      return ErrorCorrectionForm
    case 'PRONUNCIATION':
      return PronunciationForm
    case 'OPEN_ENDED':
      return OpenEndedForm
    default:
      return null
  }
})

defineExpose({ openCreate, openEdit })
</script>

<style scoped>
.el-fade-in-linear-enter-active,
.el-fade-in-linear-leave-active {
  transition: all 0.3s;
}
</style>
