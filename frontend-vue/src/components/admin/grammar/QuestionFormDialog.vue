<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" :width="dialogWidth" :fullscreen="isMobile"
    :close-on-click-modal="false" :before-close="handleBeforeClose" class="question-wizard-dialog" destroy-on-close
    :top="isMobile ? '0' : '5vh'">
    <el-steps :active="currentStep - 1" finish-status="success" :align-center="!isMobile"
      :direction="isMobile ? 'vertical' : 'horizontal'" class="steps-container">
      <el-step title="Thông tin chung" :description="!isMobile ? 'Loại câu hỏi & Nội dung' : ''">
        <template #icon>
          <div class="step-icon-wrapper"><el-icon>
              <DocumentAdd />
            </el-icon></div>
        </template>
      </el-step>
      <el-step title="Chi tiết câu hỏi" :description="!isMobile ? 'Đáp án & Cấu hình' : ''">
        <template #icon>
          <div class="step-icon-wrapper"><el-icon>
              <EditPen />
            </el-icon></div>
        </template>
      </el-step>
      <el-step title="Xem trước" :description="!isMobile ? 'Kiểm tra hiển thị' : ''">
        <template #icon>
          <div class="step-icon-wrapper"><el-icon>
              <View />
            </el-icon></div>
        </template>
      </el-step>
    </el-steps>

    <el-divider />

    <div class="dialog-body-content">
      <el-form ref="formRef" :model="formData" :rules="formRules" :label-width="labelWidth" label-position="top"
        @submit.prevent>

        <div v-show="currentStep === 1" class="step-content">
          <el-row :gutter="gutter">
            <el-col :xs="24" :sm="24" :md="12">
              <el-form-item label="Loại câu hỏi" prop="questionType">
                <el-select v-model="formData.questionType" placeholder="Chọn loại câu hỏi" style="width: 100%"
                  @change="handleQuestionTypeChange" :disabled="dialogMode === 'edit'" :size="formSize">
                  <el-option v-for="opt in questionTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value">
                    <div class="option-item">
                      <span class="option-text">{{ opt.label }}</span>
                    </div>
                  </el-option>
                </el-select>
                <div class="type-desc" v-if="currentQuestionTypeOption">
                  <el-text type="info" size="small">
                    <el-icon>
                      <InfoFilled />
                    </el-icon> {{ currentQuestionTypeOption.description }}
                  </el-text>
                </div>
              </el-form-item>
            </el-col>

            <el-col :xs="12" :sm="12" :md="6">
              <el-form-item label="Điểm số" prop="points">
                <el-input-number v-model="formData.points" :min="1" :max="100" style="width: 100%" :size="formSize" />
              </el-form-item>
            </el-col>

            <el-col :xs="12" :sm="12" :md="6">
              <el-form-item label="Thứ tự" prop="orderIndex">
                <el-input-number v-model="formData.orderIndex" :min="1" style="width: 100%" :size="formSize" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Nội dung câu hỏi (Đề bài)" prop="questionText">
            <QuillRichEditor v-model="formData.questionText" placeholder="Nhập nội dung câu hỏi..."
              :height="editorHeight" :width="editorWidth" toolbar="question" :show-word-count="true" />
            <template #extra>
              <el-text type="info" size="small">
                Sử dụng trình soạn thảo để định dạng văn bản, thêm hình ảnh hoặc liên kết
              </el-text>
            </template>
          </el-form-item>

        </div>

        <div v-show="currentStep === 2" class="step-content">
          <el-alert v-if="!formData.questionType" title="Vui lòng chọn loại câu hỏi ở Bước 1" type="warning" show-icon
            :closable="false" style="margin-bottom: 20px" />

          <div v-else class="form-component-wrapper">
            <el-card shadow="never" class="detail-card">
              <template #header>
                <div class="card-header">
                  <el-text tag="b"><el-icon>
                      <Edit />
                    </el-icon> Cấu hình chi tiết</el-text>
                  <el-tag>{{ currentQuestionTypeOption?.label }}</el-tag>
                </div>
              </template>

              <component :is="getFormComponent(formData.questionType)" v-model:metadata="formData.metadata"
                :question-text="formData.questionText" />
            </el-card>
          </div>
        </div>

        <div v-show="currentStep === 3" class="step-content">
          <el-card shadow="never" class="preview-container">
            <template #header>
              <div class="card-header">
                <el-text tag="b" size="large"><el-icon>
                    <View />
                  </el-icon> Xem trước câu hỏi</el-text>
                <el-text type="info" size="small">Kiểm tra cẩn thận trước khi lưu</el-text>
              </div>
            </template>
            <QuestionPreview :question="previewData" />
          </el-card>
        </div>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleBeforeClose" :size="buttonSize">
          <el-icon>
            <Close />
          </el-icon> <span v-if="!isMobile">Hủy bỏ</span>
        </el-button>

        <div class="step-actions">
          <el-button v-if="currentStep > 1" @click="prevStep" :size="buttonSize">
            <el-icon>
              <ArrowLeft />
            </el-icon> <span v-if="!isMobile">Quay lại</span>
          </el-button>

          <el-button v-if="currentStep < 3" type="primary" @click="handleNextStep" :size="buttonSize">
            <span v-if="!isMobile">Tiếp theo</span> <el-icon>
              <ArrowRight />
            </el-icon>
          </el-button>

          <el-button v-if="currentStep === 3" type="success" :loading="submitting" @click="onSubmit" :size="buttonSize">
            <el-icon>
              <Check />
            </el-icon> <span v-if="!isMobile">{{ submitButtonText }}</span>
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, defineAsyncComponent, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Check, Close,
  DocumentAdd, EditPen, View, Edit, InfoFilled
} from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { useGrammarQuestionForm } from '@/composables/grammar/useGrammarQuestions'

// Lazy Components
const MultipleChoiceForm = defineAsyncComponent(() => import('@/components/admin/questions/MultipleChoiceForm.vue'))
const TrueFalseForm = defineAsyncComponent(() => import('@/components/admin/questions/TrueFalseForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('@/components/admin/questions/FillBlankForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('@/components/admin/questions/MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('@/components/admin/questions/SentenceBuildingForm.vue'))
const ReadingComprehensionForm = defineAsyncComponent(() => import('@/components/admin/questions/ReadingComprehensionForm.vue'))
const ConversationForm = defineAsyncComponent(() => import('@/components/admin/questions/ConversationForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('@/components/admin/questions/PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('@/components/admin/questions/ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('@/components/admin/questions/OpenEndedForm.vue'))
const VerbFormForm = defineAsyncComponent(() => import('@/components/admin/questions/VerbFormForm.vue'))
const TextAnswerForm = defineAsyncComponent(() => import('@/components/admin/questions/TextAnswerForm.vue'))
const QuestionPreview = defineAsyncComponent(() => import('./QuestionPreview.vue'))

const props = defineProps({ lessonId: { type: Number, default: null } })
const emit = defineEmits(['success'])

const {
  dialogVisible, dialogMode, currentStep, formData, formRules, questionTypeOptions,
  dialogTitle, submitButtonText, currentQuestionTypeOption,
  openCreateDialog, openEditDialog, handleSubmit, closeDialog, handleQuestionTypeChange,
  nextStep, prevStep
} = useGrammarQuestionForm()

const formRef = ref(null)
const submitting = ref(false)

// Expose methods
const handleOpenCreate = async () => await openCreateDialog(props.lessonId)
const handleOpenEdit = (question) => openEditDialog(question)
defineExpose({ openCreate: handleOpenCreate, openEdit: handleOpenEdit })

// Mapping logic
const getFormComponent = (type) => {
  const map = {
    'MULTIPLE_CHOICE': MultipleChoiceForm,
    'TRUE_FALSE': TrueFalseForm,
    'FILL_BLANK': FillBlankForm,
    'TEXT_ANSWER': TextAnswerForm,
    'VERB_FORM': VerbFormForm,
    'ERROR_CORRECTION': ErrorCorrectionForm,
    'MATCHING': MatchingForm,
    'SENTENCE_BUILDING': SentenceBuildingForm,
    'COMPLETE_CONVERSATION': ConversationForm,
    'CONVERSATION': ConversationForm,
    'PRONUNCIATION': PronunciationForm,
    'READING_COMPREHENSION': ReadingComprehensionForm,
    'OPEN_ENDED': OpenEndedForm
  }
  return map[type] || null
}

const previewData = computed(() => ({
  questionText: formData.value.questionText,
  questionType: formData.value.questionType,
  explanation: formData.value.metadata?.explanation, // Preview lấy từ metadata
  metadata: formData.value.metadata,
  points: formData.value.points,
  orderIndex: formData.value.orderIndex,
  parentId: formData.value.parentId
}))

// Responsive logic
const isMobile = computed(() => window.innerWidth < 768)
const isTablet = computed(() => window.innerWidth >= 768 && window.innerWidth < 1024)
const dialogWidth = computed(() => isMobile.value ? '100%' : (isTablet.value ? '90%' : '1100px'))
const labelWidth = computed(() => isMobile.value ? '100%' : '140px')
const gutter = computed(() => isMobile.value ? 10 : 20)
const buttonSize = computed(() => isMobile.value ? 'small' : 'default')
const formSize = computed(() => isMobile.value ? 'small' : 'default')
const editorHeight = computed(() => isMobile.value ? '100px' : '300px')
const editorWidth = computed(() => isMobile.value ? '100%' : '100%')
const handleNextStep = async () => {
  if (currentStep.value === 1) {
    if (!formRef.value) return
    await formRef.value.validate((valid) => {
      if (valid) nextStep()
      else ElMessage.warning('Vui lòng điền đầy đủ thông tin bắt buộc')
    })
  } else {
    nextStep()
  }
}

const onSubmit = async () => {
  submitting.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      emit('success')
      closeDialog()
      ElMessage.success(dialogMode.value === 'create' ? 'Tạo câu hỏi thành công!' : 'Cập nhật thành công!')
    }
  } catch (error) {
    console.error('Submit error:', error)
    ElMessage.error('Có lỗi xảy ra khi lưu câu hỏi')
  } finally {
    submitting.value = false
  }
}

const handleBeforeClose = () => closeDialog()
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

.step-icon-wrapper {
  width: 40px;
  height: 40px;
  border: 2px solid var(--el-border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  font-size: 18px;
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

.step-content {
  min-height: 400px;
  padding: 20px 0;
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
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
  flex-wrap: wrap;
  gap: 12px;
}

.step-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .question-wizard-dialog :deep(.el-dialog__body) {
    padding: 16px;
    max-height: calc(100vh - 160px);
  }

  .dialog-footer {
    flex-direction: column;
    gap: 10px;
  }

  .step-actions {
    width: 100%;
    justify-content: space-between;
  }

  .step-actions .el-button {
    flex: 1;
  }
}
</style>
