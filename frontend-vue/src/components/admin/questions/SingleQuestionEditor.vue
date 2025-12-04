<template>
  <el-card shadow="hover" class="single-question-card" :class="{ 'is-collapsed': isCollapsed }">
    <template #header>
      <div class="card-header" @click="toggleCollapse">
        <div class="header-left">
          <el-button
            link
            :icon="isCollapsed ? 'CaretRight' : 'CaretBottom'"
            class="collapse-btn mr-2"
            @click.stop="toggleCollapse"
          />

          <el-tag type="primary" effect="dark" class="index-tag">#{{ index + 1 }}</el-tag>

          <el-select
            v-model="localQuestion.questionType"
            placeholder="Chọn loại câu hỏi"
            style="width: 240px"
            @change="handleTypeChange"
            @click.stop
            size="small"
          >
            <el-option
              v-for="opt in questionTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>

          <transition name="el-fade-in">
            <span v-if="isCollapsed && localQuestion.questionText" class="collapsed-summary text-truncate">
              - {{ getPreviewText(localQuestion.questionText) }}
            </span>
          </transition>
        </div>

        <div class="header-right" @click.stop>
          <el-input-number
            v-model="localQuestion.points"
            :min="1"
            controls-position="right"
            style="width: 100px"
            placeholder="Điểm"
            size="small"
          />
          <el-tooltip content="Xóa câu này" placement="top">
            <el-button type="danger" icon="Delete" circle size="small" @click="$emit('remove')" />
          </el-tooltip>
        </div>
      </div>
    </template>

    <el-collapse-transition>
      <div v-show="!isCollapsed">
        <div class="card-body">
          <div class="mb-4">
            <div class="label-text">Nội dung câu hỏi:</div>
            <QuillRichEditor
              v-model="localQuestion.questionText"
              height="120px"
              toolbar="question"
              placeholder="Nhập nội dung câu hỏi..."
            />
          </div>

          <div v-if="localQuestion.questionType" class="answer-config-area">
            <div class="label-text">Cấu hình đáp án ({{ getTypeName(localQuestion.questionType) }}):</div>

            <component
              :is="getFormComponent(localQuestion.questionType)"
              v-model:metadata="localQuestion.metadata"
              :question-text="localQuestion.questionText"
              :question-type="localQuestion.questionType"
            />
          </div>

          <el-empty v-else description="Vui lòng chọn loại câu hỏi để tiếp tục" :image-size="60" style="padding: 20px 0" />
        </div>
      </div>
    </el-collapse-transition>
  </el-card>
</template>

<script setup>
import { ref, watch, defineAsyncComponent } from 'vue'
import { Delete, CaretBottom, CaretRight } from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { useGrammarQuestionForm } from '@/composables/grammar/useGrammarQuestions'

// Import các form con (Đã gộp nhóm)
const MultipleChoiceForm = defineAsyncComponent(() => import('./MultipleChoiceForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('./FillBlankForm.vue'))

// Các form cũ giữ nguyên
const MatchingForm = defineAsyncComponent(() => import('./MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('./SentenceBuildingForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('./PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('./ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('./OpenEndedForm.vue'))
const SentenceTransformationForm = defineAsyncComponent(() => import('./SentenceTransformationForm.vue'))

const props = defineProps({
  modelValue: { type: Object, required: true },
  index: { type: Number, default: 0 },
  isInitiallyCollapsed: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'remove'])

const { questionTypeOptions } = useGrammarQuestionForm()

const localQuestion = ref({ ...props.modelValue })
const isCollapsed = ref(props.isInitiallyCollapsed)

watch(() => props.modelValue, (newVal) => {
  localQuestion.value = newVal
}, { deep: true })

watch(localQuestion, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })

const handleTypeChange = () => {
  localQuestion.value.metadata = {}
  isCollapsed.value = false
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const getFormComponent = (type) => {
  const map = {
    // Nhóm dùng MultipleChoiceForm (Trắc nghiệm, Đúng/Sai, Hội thoại)
    'MULTIPLE_CHOICE': MultipleChoiceForm,
    'TRUE_FALSE': MultipleChoiceForm,

    // Nhóm dùng FillBlankForm (Điền từ, Word Bank, Trả lời ngắn, Chia động từ)
    'FILL_BLANK': FillBlankForm,
    'TEXT_ANSWER': FillBlankForm,
    'VERB_FORM': FillBlankForm,

    // Các nhóm khác
    'ERROR_CORRECTION': ErrorCorrectionForm,
    'MATCHING': MatchingForm,
    'SENTENCE_BUILDING': SentenceBuildingForm,
    'PRONUNCIATION': PronunciationForm,
    'OPEN_ENDED': OpenEndedForm,
    'SENTENCE_TRANSFORMATION': SentenceTransformationForm
  }
  return map[type] || null
}

const getTypeName = (type) => {
  const opt = questionTypeOptions.find(o => o.value === type)
  return opt ? opt.label : type
}

const getPreviewText = (html) => {
  if (!html) return ''
  const tmp = document.createElement('DIV')
  tmp.innerHTML = html
  const text = tmp.textContent || tmp.innerText || ''
  return text.length > 50 ? text.substring(0, 50) + '...' : text
}

defineExpose({
  collapse: () => isCollapsed.value = true,
  expand: () => isCollapsed.value = false
})
</script>

<style scoped>
.single-question-card {
  margin-bottom: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  transition: all 0.3s;
}
.single-question-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}
.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.index-tag {
  font-size: 14px;
  font-weight: bold;
  min-width: 40px;
  text-align: center;
}

.card-body {
  padding: 10px 0;
}

.label-text {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.answer-config-area {
  background-color: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 6px;
  border: 1px dashed #dcdfe6;
}

.mr-2 { margin-right: 8px; }
.mb-4 { margin-bottom: 16px; }

.collapsed-summary {
  color: #909399;
  font-size: 13px;
  font-style: italic;
  margin-left: 10px;
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.is-collapsed :deep(.el-card__header) {
  border-bottom: none;
  transition: border-bottom 0.3s;
}
</style>
