<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :width="dialogWidth"
    :fullscreen="isMobile"
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
    class="question-flat-dialog"
    destroy-on-close
    top="5vh"
  >
    <div class="dialog-body-content">
      <el-form ref="formRef" :model="formData" :rules="formRules" :label-width="labelWidth" label-position="top" @submit.prevent>

        <div class="config-section">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="10">
              <el-form-item label="Loại câu hỏi" prop="questionType">
                <el-select
                  v-model="formData.questionType"
                  placeholder="Chọn loại"
                  style="width: 100%"
                  @change="handleQuestionTypeChange"
                  :disabled="dialogMode === 'edit'"
                  filterable
                >
                  <el-option
                    v-for="opt in questionTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :xs="12" :sm="6" :md="4">
              <el-form-item label="Điểm" prop="points">
                <el-input-number v-model="formData.points" :min="1" :max="100" style="width: 100%" controls-position="right" />
              </el-form-item>
            </el-col>

            <el-col :xs="12" :sm="6" :md="4">
              <el-form-item label="Thứ tự" prop="orderIndex">
                <el-input-number v-model="formData.orderIndex" :min="1" style="width: 100%" controls-position="right" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-divider class="my-3" />

        <div class="content-section" v-if="formData.questionType">
          <el-form-item label="Nội dung câu hỏi" prop="questionText">
            <QuillRichEditor
              v-model="formData.questionText"
              placeholder="Nhập nội dung câu hỏi..."
              :height="editorHeight"
              width="100%"
              toolbar="full"
            />
          </el-form-item>

          <div class="answer-config-box">
            <div class="box-header">
              <span class="title">Cấu hình đáp án ({{ currentQuestionTypeOption?.label }})</span>
            </div>
            <div class="box-body">
              <component
                :is="getFormComponent(formData.questionType)"
                v-model:metadata="formData.metadata"
                :question-text="formData.questionText"
                :question-type="formData.questionType"
              />
            </div>
          </div>
        </div>

        <el-empty v-else description="Vui lòng chọn loại câu hỏi để tiếp tục" :image-size="80" />

      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleBeforeClose">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit" icon="Check">
          {{ submitButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, defineAsyncComponent, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { useGrammarQuestionForm } from '@/composables/grammar/useGrammarQuestions'

// Import Forms
const MultipleChoiceForm = defineAsyncComponent(() => import('@/components/admin/questions/MultipleChoiceForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('@/components/admin/questions/FillBlankForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('@/components/admin/questions/MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('@/components/admin/questions/SentenceBuildingForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('@/components/admin/questions/PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('@/components/admin/questions/ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('@/components/admin/questions/OpenEndedForm.vue'))
const SentenceTransformationForm = defineAsyncComponent(() => import('@/components/admin/questions/SentenceTransformationForm.vue'))

const props = defineProps({ lessonId: { type: Number, default: null } })
const emit = defineEmits(['success'])

const {
  dialogVisible, dialogMode, formData, formRules, questionTypeOptions,
  dialogTitle, submitButtonText, currentQuestionTypeOption,
  openCreateDialog, openEditDialog, handleSubmit, closeDialog, handleQuestionTypeChange
} = useGrammarQuestionForm()

const formRef = ref(null)
const submitting = ref(false)

const handleOpenCreate = async () => await openCreateDialog(props.lessonId)
const handleOpenEdit = (question) => openEditDialog(question)
defineExpose({ openCreate: handleOpenCreate, openEdit: handleOpenEdit })

const getFormComponent = (type) => {
  const map = {
    'MULTIPLE_CHOICE': MultipleChoiceForm,
    'TRUE_FALSE': MultipleChoiceForm,
    'COMPLETE_CONVERSATION': MultipleChoiceForm,
    'FILL_BLANK': FillBlankForm,
    'TEXT_ANSWER': FillBlankForm,
    'VERB_FORM': FillBlankForm,
    'MATCHING': MatchingForm,
    'SENTENCE_BUILDING': SentenceBuildingForm,
    'PRONUNCIATION': PronunciationForm,
    'ERROR_CORRECTION': ErrorCorrectionForm,
    'OPEN_ENDED': OpenEndedForm,
    'SENTENCE_TRANSFORMATION': SentenceTransformationForm
  }
  return map[type] || null
}

// Responsive
const isMobile = computed(() => window.innerWidth < 768)
const dialogWidth = computed(() => isMobile.value ? '100%' : '900px')
const labelWidth = computed(() => isMobile.value ? '100%' : '120px')
const editorHeight = computed(() => isMobile.value ? '150px' : '200px')

const onSubmit = async () => {
  submitting.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      emit('success')
      closeDialog()
      ElMessage.success(dialogMode.value === 'create' ? 'Tạo thành công!' : 'Cập nhật thành công!')
    }
  } catch (error) { console.error(error) } finally { submitting.value = false }
}

const handleBeforeClose = () => closeDialog()
</script>

<style scoped>
.question-flat-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: 85vh;
  overflow-y: auto;
}
.dialog-body-content { padding: 20px; }

/* Layout Styling */
.config-section { margin-bottom: 10px; }
.answer-config-box {
  background-color: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  margin-top: 20px;
}
.box-header {
  padding: 10px 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color);
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.box-body { padding: 15px; }

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: flex-end;
  background-color: var(--el-bg-color);
}
.my-3 { margin: 12px 0; }
</style>
