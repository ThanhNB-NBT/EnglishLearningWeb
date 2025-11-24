<template>
  <el-dialog
    v-model="localVisible"
    :title="dialogTitle"
    width="70%"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <!-- Steps Indicator -->
    <el-steps :active="currentStep - 1" finish-status="success" align-center class="steps-container">
      <el-step title="Basic Info" icon="Edit" />
      <el-step title="Metadata" icon="SetUp" />
      <el-step title="Preview" icon="View" />
    </el-steps>

    <!-- Form Container -->
    <el-form
      ref="formRef"
      :model="localFormData"
      :rules="formRules"
      label-position="top"
      class="question-form"
      @submit.prevent
    >
      <!-- Step 1: Basic Info -->
      <div v-show="currentStep === 1" class="form-step">
        <el-row :gutter="20">
          <!-- Question Type -->
          <el-col :span="24">
            <el-form-item label="Question Type" prop="questionType">
              <el-select
                v-model="localFormData.questionType"
                placeholder="Select question type"
                :disabled="mode === 'edit'"
                @change="handleQuestionTypeChangeLocal"
                style="width: 100%"
              >
                <el-option
                  v-for="type in questionTypeOptions"
                  :key="type.value"
                  :label="`${type.icon} ${type.label}`"
                  :value="type.value"
                >
                  <span>{{ type.icon }} {{ type.label }}</span>
                  <el-text size="small" type="info" style="margin-left: 8px">
                    {{ type.description }}
                  </el-text>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <!-- Question Text -->
          <el-col :span="24">
            <el-form-item label="Question Text" prop="questionText">
              <el-input
                v-model="localFormData.questionText"
                type="textarea"
                :rows="4"
                placeholder="Enter your question here..."
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-col>

          <!-- Points & Order Index -->
          <el-col :span="12">
            <el-form-item label="Points" prop="points">
              <el-input-number
                v-model="localFormData.points"
                :min="1"
                :max="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Order Index" prop="orderIndex">
              <el-input-number
                v-model="localFormData.orderIndex"
                :min="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <!-- Explanation (Optional) -->
          <el-col :span="24">
            <el-form-item label="Explanation (Optional)">
              <el-input
                v-model="localFormData.explanation"
                type="textarea"
                :rows="3"
                placeholder="Provide an explanation or hint for the correct answer..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- Step 2: Metadata -->
      <div v-show="currentStep === 2" class="form-step">
        <el-alert
          :title="`Configure ${currentQuestionTypeOption?.label || 'Question'} Settings`"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #default>
            {{ currentQuestionTypeOption?.description }}
          </template>
        </el-alert>

        <!-- Dynamic Metadata Form Components -->
        <component
          :is="metadataFormComponent"
          v-model:metadata="localFormData.metadata"
          :question-text="localFormData.questionText"
        />
      </div>

      <!-- Step 3: Preview -->
      <div v-show="currentStep === 3" class="form-step">
        <QuestionPreview :question="previewData" />
      </div>
    </el-form>

    <!-- Dialog Footer -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Cancel</el-button>

        <div class="step-buttons">
          <el-button v-if="currentStep > 1" @click="prevStep">
            <el-icon><ArrowLeft /></el-icon>
            Previous
          </el-button>

          <el-button v-if="currentStep < 3" type="primary" @click="nextStep">
            Next
            <el-icon><ArrowRight /></el-icon>
          </el-button>

          <el-button
            v-if="currentStep === 3"
            type="success"
            :loading="submitting"
            @click="handleSubmit"
          >
            <el-icon><Check /></el-icon>
            {{ submitButtonText }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, shallowRef } from 'vue'
import { useGrammarQuestionForm } from '@/composables/useGrammarQuestions'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Check } from '@element-plus/icons-vue'
import QuestionPreview from './QuestionPreview.vue'

// Lazy load metadata form components
const MultipleChoiceForm = () => import('./question-forms/MultipleChoiceForm.vue')
const TrueFalseForm = () => import('./question-forms/TrueFalseForm.vue')
const FillBlankForm = () => import('./question-forms/FillBlankForm.vue')
const ShortAnswerForm = () => import('./question-forms/ShortAnswerForm.vue')
const VerbFormForm = () => import('./question-forms/VerbFormForm.vue')
const ErrorCorrectionForm = () => import('./question-forms/ErrorCorrectionForm.vue')
const MatchingForm = () => import('./question-forms/MatchingForm.vue')
const SentenceBuildingForm = () => import('./question-forms/SentenceBuildingForm.vue')
const ConversationForm = () => import('./question-forms/ConversationForm.vue')
const PronunciationForm = () => import('./question-forms/PronunciationForm.vue')
const ReadingComprehensionForm = () => import('./question-forms/ReadingComprehensionForm.vue')
const OpenEndedForm = () => import('./question-forms/OpenEndedForm.vue')

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
  mode: {
    type: String,
    default: 'create', // 'create' | 'edit'
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
  lessonId: {
    type: Number,
    default: null,
  },
})

// Emits
const emit = defineEmits(['update:visible', 'submit', 'close'])

// Composable
const {
  formRules,
  questionTypeOptions,
  currentQuestionTypeOption,
  handleQuestionTypeChange,
} = useGrammarQuestionForm()

// Local State
const formRef = ref(null)
const currentStep = ref(1)
const submitting = ref(false)

// ✅ FIX: Create local ref for visible to avoid prop mutation
const localVisible = ref(props.visible)

const localFormData = ref({
  questionText: '',
  questionType: 'MULTIPLE_CHOICE',
  points: 5,
  orderIndex: 1,
  explanation: '',
  metadata: {},
})

// Computed
const dialogTitle = computed(() => {
  const prefix = props.mode === 'create' ? 'Create' : 'Edit'
  const typeLabel = currentQuestionTypeOption.value?.label || 'Question'
  return `${prefix} ${typeLabel}`
})

const submitButtonText = computed(() => {
  return props.mode === 'create' ? 'Create Question' : 'Update Question'
})

const metadataFormComponent = shallowRef(null)

// ✅ FIX: Sync localVisible with prop.visible
watch(
  () => props.visible,
  (newVal) => {
    localVisible.value = newVal
    if (newVal) {
      initializeForm()
    }
  }
)

// ✅ FIX: Emit update when localVisible changes
watch(localVisible, (newVal) => {
  emit('update:visible', newVal)
})

// Watch questionType to load correct metadata form
watch(
  () => localFormData.value.questionType,
  (newType) => {
    metadataFormComponent.value = getMetadataFormComponent(newType)
  },
  { immediate: true }
)

// Watch props.formData to sync form data
watch(
  () => props.formData,
  (newData) => {
    if (newData && Object.keys(newData).length > 0) {
      localFormData.value = { ...newData }
    }
  },
  { deep: true, immediate: true }
)

// Methods
const initializeForm = () => {
  currentStep.value = 1

  if (props.mode === 'create') {
    localFormData.value = {
      questionText: '',
      questionType: 'MULTIPLE_CHOICE',
      points: 5,
      orderIndex: 1,
      explanation: '',
      metadata: {},
    }
  } else if (props.formData && Object.keys(props.formData).length > 0) {
    localFormData.value = { ...props.formData }
  }
}

const getMetadataFormComponent = (questionType) => {
  const componentMap = {
    MULTIPLE_CHOICE: MultipleChoiceForm,
    TRUE_FALSE: TrueFalseForm,
    FILL_BLANK: FillBlankForm,
    SHORT_ANSWER: ShortAnswerForm,
    VERB_FORM: VerbFormForm,
    ERROR_CORRECTION: ErrorCorrectionForm,
    MATCHING: MatchingForm,
    SENTENCE_BUILDING: SentenceBuildingForm,
    COMPLETE_CONVERSATION: ConversationForm,
    PRONUNCIATION: PronunciationForm,
    READING_COMPREHENSION: ReadingComprehensionForm,
    OPEN_ENDED: OpenEndedForm,
  }

  return componentMap[questionType] || null
}

// ✅ FIX: Wrap handleQuestionTypeChange từ composable
const handleQuestionTypeChangeLocal = (newType) => {
  handleQuestionTypeChange(newType)
  localFormData.value.metadata = {}

  // Set default points based on type
  const typeOption = questionTypeOptions.find(opt => opt.value === newType)
  if (typeOption) {
    localFormData.value.points = typeOption.defaultPoints
  }
}

const nextStep = async () => {
  if (currentStep.value === 1) {
    // Validate basic info before going to metadata
    try {
      await formRef.value.validate()
      currentStep.value += 1
    } catch (error) {
      ElMessage.warning('Please fill in all required fields')
      console.error('Validation failed:', error)
    }
  } else {
    currentStep.value += 1
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value -= 1
  }
}

const previewData = computed(() => {
  return {
    ...localFormData.value,
    parentType: 'GRAMMAR',
    parentId: props.lessonId,
    id: props.mode === 'edit' ? props.formData.id : null,
  }
})

const handleSubmit = async () => {
  submitting.value = true

  try {
    // Build final DTO
    const dto = {
      ...localFormData.value,
      parentType: 'GRAMMAR',
      parentId: props.lessonId,
    }

    console.log('📤 Submitting question:', dto)

    emit('submit', dto)
    localVisible.value = false

    ElMessage.success('Question saved successfully!')
  } catch (error) {
    console.error('❌ Submit error:', error)
    ElMessage.error('Failed to save question')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  localVisible.value = false
  emit('close')
}
</script>

<style scoped>
.steps-container {
  margin-bottom: 24px;
}

.question-form {
  min-height: 400px;
  padding: 16px 0;
}

.form-step {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.step-buttons {
  display: flex;
  gap: 8px;
}

/* Responsive */
@media (max-width: 768px) {
  .dialog-footer {
    flex-direction: column;
  }

  .step-buttons {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
