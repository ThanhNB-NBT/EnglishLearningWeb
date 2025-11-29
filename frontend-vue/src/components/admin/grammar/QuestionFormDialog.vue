<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="95%"
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
    class="question-wizard-dialog"
    destroy-on-close
    top="5vh"
  >
    <!-- Steps -->
    <el-steps
      :active="currentStep - 1"
      finish-status="success"
      align-center
      class="steps-container"
    >
      <el-step title="Thông tin chung" description="Loại câu hỏi & Nội dung">
        <template #icon>
          <div class="step-icon-wrapper">
            <el-icon><DocumentAdd /></el-icon>
          </div>
        </template>
      </el-step>
      <el-step title="Chi tiết câu hỏi" description="Đáp án & Cấu hình">
        <template #icon>
          <div class="step-icon-wrapper">
            <el-icon><EditPen /></el-icon>
          </div>
        </template>
      </el-step>
      <el-step title="Xem trước" description="Kiểm tra hiển thị">
        <template #icon>
          <div class="step-icon-wrapper">
            <el-icon><View /></el-icon>
          </div>
        </template>
      </el-step>
    </el-steps>

    <el-divider />

    <!-- Form Content -->
    <div class="dialog-body-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="140px"
        label-position="top"
        @submit.prevent
      >
        <!-- Step 1: Basic Info -->
        <div v-show="currentStep === 1" class="step-content">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Loại câu hỏi" prop="questionType">
                <el-select
                  v-model="formData.questionType"
                  placeholder="Chọn loại câu hỏi"
                  style="width: 100%"
                  @change="handleQuestionTypeChange"
                  :disabled="dialogMode === 'edit'"
                  size="large"
                >
                  <el-option
                    v-for="opt in questionTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  >
                    <div class="option-item">
                      <span class="option-text">{{ opt.label }}</span>
                    </div>
                  </el-option>
                </el-select>
                <div class="type-desc" v-if="currentQuestionTypeOption">
                  <el-text type="info" size="small">
                    <el-icon><InfoFilled /></el-icon>
                    {{ currentQuestionTypeOption.description }}
                  </el-text>
                </div>
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="Điểm số" prop="points">
                <el-input-number
                  v-model="formData.points"
                  :min="1"
                  :max="100"
                  style="width: 100%"
                  size="large"
                />
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="Thứ tự" prop="orderIndex">
                <el-input-number
                  v-model="formData.orderIndex"
                  :min="1"
                  style="width: 100%"
                  size="large"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Nội dung câu hỏi (Đề bài)" prop="questionText">
            <RichTextEditor
              v-model="formData.questionText"
              placeholder="Nhập nội dung câu hỏi..."
              height="250px"
            />
            <template #extra>
              <el-text type="info" size="small">
                Sử dụng trình soạn thảo để định dạng văn bản, thêm hình ảnh hoặc liên kết
              </el-text>
            </template>
          </el-form-item>

          <el-form-item label="Giải thích đáp án (Tùy chọn)" prop="explanation">
            <el-input
              v-model="formData.explanation"
              type="textarea"
              :rows="4"
              placeholder="Giải thích tại sao đáp án này đúng (hiển thị sau khi user nộp bài)..."
              show-word-limit
              maxlength="500"
            />
          </el-form-item>
        </div>

        <!-- Step 2: Question Details -->
        <div v-show="currentStep === 2" class="step-content">
          <el-alert
            v-if="!formData.questionType"
            title="Vui lòng chọn loại câu hỏi ở Bước 1"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 20px"
          />

          <div v-else class="form-component-wrapper">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-text tag="b">
                    <el-icon><Edit /></el-icon>
                    Cấu hình chi tiết
                  </el-text>
                  <el-tag>{{ currentQuestionTypeOption?.label }}</el-tag>
                </div>
              </template>

              <component
                :is="getFormComponent(formData.questionType)"
                v-model:metadata="formData.metadata"
                :question-text="formData.questionText"
              />
            </el-card>
          </div>
        </div>

        <!-- Step 3: Preview -->
        <div v-show="currentStep === 3" class="step-content">
          <el-card shadow="never" class="preview-container">
            <template #header>
              <div class="card-header">
                <el-text tag="b" size="large">
                  <el-icon><View /></el-icon>
                  Xem trước câu hỏi
                </el-text>
                <el-text type="info" size="small">
                  Kiểm tra cẩn thận trước khi lưu
                </el-text>
              </div>
            </template>

            <QuestionPreview :question="previewData" />
          </el-card>
        </div>
      </el-form>
    </div>

    <!-- Footer -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleBeforeClose" size="large">
          <el-icon><Close /></el-icon>
          Hủy bỏ
        </el-button>

        <div class="step-actions">
          <el-button
            v-if="currentStep > 1"
            @click="prevStep"
            size="large"
          >
            <el-icon><ArrowLeft /></el-icon>
            Quay lại
          </el-button>

          <el-button
            v-if="currentStep < 3"
            type="primary"
            @click="handleNextStep"
            size="large"
          >
            Tiếp theo
            <el-icon><ArrowRight /></el-icon>
          </el-button>

          <el-button
            v-if="currentStep === 3"
            type="success"
            :loading="submitting"
            @click="onSubmit"
            size="large"
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
import { ref, defineAsyncComponent, computed } from 'vue'
import { useGrammarQuestionForm } from '@/composables/grammar/useGrammarQuestions'
import {
  ArrowLeft, ArrowRight, Check, Close,
  DocumentAdd, EditPen, View, Edit, InfoFilled
} from '@element-plus/icons-vue'
import RichTextEditor from '@/components/common/QuillRichEditor.vue'

// Import Lazy Components
const MultipleChoiceForm = defineAsyncComponent(() => import('@/components/admin/questions/MultipleChoiceForm.vue'))
const TrueFalseForm = defineAsyncComponent(() => import('@/components/admin/questions/TrueFalseForm.vue'))
const ShortAnswerForm = defineAsyncComponent(() => import('@/components/admin/questions/ShortAnswerForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('@/components/admin/questions/MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('@/components/admin/questions/SentenceBuildingForm.vue'))
const ReadingComprehensionForm = defineAsyncComponent(() => import('@/components/admin/questions/ReadingComprehensionForm.vue'))
const ConversationForm = defineAsyncComponent(() => import('@/components/admin/questions/ConversationForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('@/components/admin/questions/PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('@/components/admin/questions/ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('@/components/admin/questions/OpenEndedForm.vue'))
const QuestionPreview = defineAsyncComponent(() => import('./QuestionPreview.vue'))

// Composable
const {
  dialogVisible,
  dialogMode,
  currentStep,
  formData,
  formRules,
  questionTypeOptions,
  dialogTitle,
  submitButtonText,
  currentQuestionTypeOption,
  openCreateDialog,
  openEditDialog,
  handleSubmit,
  closeDialog,
  handleQuestionTypeChange,
  nextStep,
  prevStep
} = useGrammarQuestionForm()

const formRef = ref(null)
const submitting = ref(false)

// Helper Functions
const getFormComponent = (type) => {
  const map = {
    'MULTIPLE_CHOICE': MultipleChoiceForm,
    'TRUE_FALSE': TrueFalseForm,
    'FILL_BLANK': ShortAnswerForm,
    'SHORT_ANSWER': ShortAnswerForm,
    'VERB_FORM': ShortAnswerForm,
    'ERROR_CORRECTION': ErrorCorrectionForm,
    'MATCHING': MatchingForm,
    'SENTENCE_BUILDING': SentenceBuildingForm,
    'COMPLETE_CONVERSATION': ConversationForm,
    'PRONUNCIATION': PronunciationForm,
    'READING_COMPREHENSION': ReadingComprehensionForm,
    'OPEN_ENDED': OpenEndedForm
  }
  return map[type] || null
}

const previewData = computed(() => ({
  questionText: formData.value.questionText,
  questionType: formData.value.questionType,
  explanation: formData.value.explanation,
  metadata: formData.value.metadata,
  points: formData.value.points,
  orderIndex: formData.value.orderIndex,
  parentId: formData.value.parentId
}))

const handleNextStep = async () => {
  if (currentStep.value === 1) {
    if (!formRef.value) return
    await formRef.value.validate((valid, fields) => {
      if (valid) {
        nextStep()
      } else {
        console.warn('Step 1 invalid', fields)
      }
    })
  } else {
    nextStep()
  }
}

const onSubmit = async () => {
  submitting.value = true
  const success = await handleSubmit(formRef.value)
  submitting.value = false
}

const handleBeforeClose = () => {
  closeDialog()
}

defineExpose({
  openCreateDialog,
  openEditDialog
})
</script>

<style scoped>
.question-wizard-dialog {
  max-width: 1200px;
  margin: 0 auto;
}

.question-wizard-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.steps-container {
  margin-bottom: 20px;
  padding: 0 20px;
}

/* Step icon with border */
.step-icon-wrapper {
  width: 48px;
  height: 48px;
  border: 2px solid var(--el-border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  transition: all 0.3s;
}

.steps-container :deep(.el-step__icon.is-text) {
  border: none;
  background: transparent;
}

.steps-container :deep(.el-step.is-process .step-icon-wrapper) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.steps-container :deep(.el-step.is-finish .step-icon-wrapper) {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.steps-container :deep(.el-step.is-wait .step-icon-wrapper) {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-secondary);
}

.step-content {
  min-height: 450px;
  padding: 20px 0;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-icon {
  font-size: 18px;
}

.option-text {
  font-weight: 500;
}

.type-desc {
  margin-top: 8px;
  padding: 10px 14px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  border-left: 3px solid var(--el-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-component-wrapper {
  padding: 0;
}

.detail-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-container {
  border: 2px solid var(--el-color-primary-light-5);
  border-radius: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  padding: 16px 0 0;
  border-top: 1px solid var(--el-border-color-lighter);
}

.step-actions {
  display: flex;
  gap: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  .question-wizard-dialog {
    width: 100% !important;
    margin: 0;
  }

  .step-content {
    min-height: 350px;
  }

  .dialog-footer {
    flex-direction: column;
    gap: 12px;
  }

  .step-actions {
    width: 100%;
    justify-content: space-between;
  }

  .step-icon-wrapper {
    width: 40px;
    height: 40px;
  }
}
</style>
