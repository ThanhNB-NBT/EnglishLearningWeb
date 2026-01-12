<!-- src/components/admin/shared/questions/QuestionFormDialog.vue - FIXED -->
<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? 'Cập nhật câu hỏi' : 'Tạo câu hỏi mới'"
    width="800px"
    destroy-on-close
    :close-on-click-modal="false"
    class="!rounded-xl"
    top="5vh"
  >
    <div v-loading="loading" class="p-2">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="flex flex-col gap-4"
      >
        <div class="flex gap-4">
          <el-form-item label="Loại câu hỏi" prop="questionType" class="flex-1">
            <el-select
              v-model="form.questionType"
              placeholder="Chọn loại câu hỏi"
              :disabled="!!form.id"
              @change="handleTypeChange"
              class="w-full"
            >
              <el-option-group label="Cơ bản">
                <el-option label="Trắc nghiệm (Multiple Choice)" value="MULTIPLE_CHOICE" />
                <el-option label="Đúng / Sai (True/False)" value="TRUE_FALSE" />
                <el-option label="Điền từ (Fill in the Blank)" value="FILL_BLANK" />
                <el-option label="Trả lời ngắn (Text Answer)" value="TEXT_ANSWER" />
              </el-option-group>
              <el-option-group label="Nâng cao">
                <el-option label="Nối từ (Matching)" value="MATCHING" />
                <el-option label="Sắp xếp câu (Sentence Building)" value="SENTENCE_BUILDING" />
                <el-option label="Viết lại câu (Transformation)" value="SENTENCE_TRANSFORMATION" />
                <el-option label="Tìm lỗi sai (Error Correction)" value="ERROR_CORRECTION" />
                <el-option label="Phát âm (Pronunciation)" value="PRONUNCIATION" />
                <el-option label="Câu hỏi mở (Open Ended)" value="OPEN_ENDED" />
              </el-option-group>
            </el-select>
          </el-form-item>

          <el-form-item label="Điểm số" prop="points" class="w-40">
            <el-input-number v-model="form.points" :min="1" :max="100" class="!w-full" />
          </el-form-item>
        </div>

        <el-form-item label="Nội dung câu hỏi" prop="questionText">
          <QuillRichEditor
            v-model="form.questionText"
            placeholder="Nhập nội dung câu hỏi..."
            height="150px"
          />
        </el-form-item>

        <el-form-item label="Giải thích đáp án">
          <el-input
            v-model="form.explanation"
            type="textarea"
            :rows="2"
            placeholder="Giải thích chi tiết (hiển thị sau khi user nộp bài)..."
          />
        </el-form-item>

        <el-divider content-position="left" class="!my-4">
          <span class="text-gray-500 font-bold text-xs uppercase flex items-center gap-2">
            <el-icon><Collection /></el-icon>
            Task Grouping (Optional)
          </span>
        </el-divider>

        <TaskGroupSelector
          v-model="form.taskGroupId"
          :task-groups="taskGroups"
          :loading="taskGroupsLoading"
          @create-task="handleCreateTaskGroup"
        />

        <el-divider content-position="left" class="!my-4">
          <span class="text-gray-500 font-bold text-xs uppercase">Cấu hình đáp án</span>
        </el-divider>

        <div
          class="bg-gray-50 dark:bg-[#1d1d1d] p-4 rounded-lg border border-gray-200 dark:border-gray-700"
        >
          <!-- ✅ FIX: Pass questionType prop -->
          <component
            :is="currentFormComponent"
            v-if="currentFormComponent"
            :metadata="formMetadata"
            :question-type="form.questionType"
            @update:metadata="handleMetadataUpdate"
          />
          <el-empty v-else description="Vui lòng chọn loại câu hỏi" :image-size="60" />
        </div>
      </el-form>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-800">
        <el-button @click="handleClose">Hủy bỏ</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ form.id ? 'Cập nhật' : 'Tạo mới' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, defineAsyncComponent, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection } from '@element-plus/icons-vue'
import { useQuestionStore } from '@/stores/admin/questionAdmin'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import TaskGroupSelector from './TaskGroupSelector.vue'

const MultipleChoiceForm = defineAsyncComponent(() => import('./forms/MultipleChoiceForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('./forms/FillBlankForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('./forms/MatchingForm.vue'))
const TextAnswerForm = defineAsyncComponent(() => import('./forms/TextAnswerForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('./forms/SentenceBuildingForm.vue'))
const SentenceTransformationForm = defineAsyncComponent(
  () => import('./forms/SentenceTransformationForm.vue'),
)
const ErrorCorrectionForm = defineAsyncComponent(() => import('./forms/ErrorCorrectionForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('./forms/PronunciationForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('./forms/OpenEndedForm.vue'))

const props = defineProps({
  config: { type: Object, required: true },
  currentLesson: Object,
})

const emit = defineEmits(['submit'])
const questionStore = useQuestionStore()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentLessonId = ref(null)

const taskGroups = computed(() => questionStore.taskGroups)
const taskGroupsLoading = computed(() => questionStore.taskGroupsLoading)

const defaultForm = {
  id: null,
  questionType: '',
  questionText: '',
  points: 1,
  explanation: '',
  taskGroupId: null,
  orderIndex: 1,
}

const form = ref({ ...defaultForm })
const formMetadata = ref({})

const rules = {
  questionType: [{ required: true, message: 'Vui lòng chọn loại câu hỏi', trigger: 'change' }],
  points: [{ required: true, message: 'Nhập điểm số', trigger: 'blur' }],
}

const currentFormComponent = computed(() => {
  const map = {
    MULTIPLE_CHOICE: MultipleChoiceForm,
    TRUE_FALSE: MultipleChoiceForm,
    COMPLETE_CONVERSATION: MultipleChoiceForm,
    FILL_BLANK: FillBlankForm,
    MATCHING: MatchingForm,
    TEXT_ANSWER: TextAnswerForm,
    SENTENCE_BUILDING: SentenceBuildingForm,
    SENTENCE_TRANSFORMATION: SentenceTransformationForm,
    ERROR_CORRECTION: ErrorCorrectionForm,
    PRONUNCIATION: PronunciationForm,
    OPEN_ENDED: OpenEndedForm,
  }
  return map[form.value.questionType] || null
})

const handleMetadataUpdate = (newMetadata) => {
  console.log('📝 Metadata updated:', newMetadata)
  formMetadata.value = { ...newMetadata }
}

const openCreate = async (lessonId) => {
  currentLessonId.value = lessonId
  await questionStore.fetchTaskGroups(props.config.moduleType, lessonId)

  const nextOrder = await questionStore.fetchNextOrder(props.config.moduleType, lessonId)

  form.value = {
    id: null,
    questionType: 'MULTIPLE_CHOICE',
    questionText: '',
    points: 1,
    explanation: '',
    taskGroupId: null,
    orderIndex: nextOrder,
  }

  formMetadata.value = {}
  visible.value = true
  loading.value = false
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = async (row) => {
  currentLessonId.value = row.parentId
  await questionStore.fetchTaskGroups(props.config.moduleType, row.parentId)

  form.value = {
    id: row.id,
    questionType: row.questionType,
    questionText: row.questionText,
    points: row.points,
    taskGroupId: row.taskGroupId || null,
    orderIndex: row.orderIndex,
    explanation: row.data?.explanation || '',
  }

  if (row.data) {
    formMetadata.value = { ...row.data }
  } else {
    formMetadata.value = {}
  }

  visible.value = true
  loading.value = false
  nextTick(() => formRef.value?.clearValidate())
}

const handleClose = () => {
  visible.value = false
  form.value = { ...defaultForm }
  formMetadata.value = {}
  nextTick(() => formRef.value?.clearValidate())
}

const handleTypeChange = (newType) => {
  const { id, questionText, points, explanation, taskGroupId, orderIndex } = form.value

  form.value = {
    id,
    questionType: newType,
    questionText,
    points,
    explanation,
    taskGroupId,
    orderIndex,
  }

  formMetadata.value = {}
}

const handleCreateTaskGroup = async (taskGroupData) => {
  try {
    const created = await questionStore.createTaskGroup(
      props.config.moduleType,
      currentLessonId.value,
      taskGroupData,
    )
    form.value.taskGroupId = created.id
    ElMessage.success('Tạo Task Group thành công!')
  } catch (error) {
    console.error('Failed to create task group:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        submitting.value = true

        const payload = {
          ...form.value,
          ...formMetadata.value,
        }

        console.log('📤 Submitting payload:', payload)

        if (payload.id) {
          await questionStore.updateQuestion(
            props.config.moduleType,
            currentLessonId.value,
            payload.id,
            payload,
          )
        } else {
          await questionStore.createQuestion(
            props.config.moduleType,
            currentLessonId.value,
            payload,
          )
        }

        emit('submit')
        handleClose()
      } catch (error) {
        console.error('Submit error', error)
      } finally {
        submitting.value = false
      }
    } else {
      ElMessage.warning('Vui lòng kiểm tra lại các trường bắt buộc')
      return false
    }
  })
}

defineExpose({
  openCreate,
  openEdit,
})
</script>
