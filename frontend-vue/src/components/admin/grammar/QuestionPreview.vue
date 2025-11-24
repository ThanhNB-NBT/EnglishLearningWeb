<template>
  <div class="question-preview">
    <el-card shadow="never" class="preview-card">
      <!-- Header -->
      <template #header>
        <div class="preview-header">
          <el-text size="large" tag="b">
            <el-icon><View /></el-icon>
            Preview Question
          </el-text>
          <el-tag :type="getQuestionTypeTagType(props.question.questionType)">
            {{ getQuestionTypeLabel(props.question.questionType) }}
          </el-tag>
        </div>
      </template>

      <!-- Question Info -->
      <div class="question-info-section">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Type">
            {{ getQuestionTypeLabel(question.questionType) }}
          </el-descriptions-item>
          <el-descriptions-item label="Points">
            <el-tag type="warning">{{ question.points }} pts</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Order Index">
            {{ question.orderIndex }}
          </el-descriptions-item>
          <el-descriptions-item label="Parent ID">
            {{ question.parentId }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Question Text -->
      <div class="question-text-section">
        <el-text size="large" tag="b" class="section-title">Question:</el-text>
        <el-card shadow="never" class="question-text-card">
          {{ question.questionText }}
        </el-card>
      </div>

      <!-- Metadata Preview (Dynamic based on question type) -->
      <div class="metadata-section">
        <el-text size="large" tag="b" class="section-title">Details:</el-text>
        <component
          :is="getPreviewComponent(question.questionType)"
          :metadata="question.metadata"
          :question-text="question.questionText"
        />
      </div>

      <!-- Explanation (Optional) -->
      <div v-if="question.explanation" class="explanation-section">
        <el-text size="large" tag="b" class="section-title">Explanation:</el-text>
        <el-card shadow="never" class="explanation-card">
          <el-text type="info">{{ question.explanation }}</el-text>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { View } from '@element-plus/icons-vue'

// Lazy load preview components
const MultipleChoicePreview = () => import('./question-previews/MultipleChoicePreview.vue')
const TrueFalsePreview = () => import('./question-previews/TrueFalsePreview.vue')
const TextAnswerPreview = () => import('./question-previews/TextAnswerPreview.vue')
const MatchingPreview = () => import('./question-previews/MatchingPreview.vue')
const SentenceBuildingPreview = () => import('./question-previews/SentenceBuildingPreview.vue')
const ConversationPreview = () => import('./question-previews/ConversationPreview.vue')
const PronunciationPreview = () => import('./question-previews/PronunciationPreview.vue')
const ReadingComprehensionPreview = () => import('./question-previews/ReadingComprehensionPreview.vue')
const OpenEndedPreview = () => import('./question-previews/OpenEndedPreview.vue')

// Props
const props = defineProps({
  question: {
    type: Object,
    required: true,
  },
})

// ✅ Method để lấy component preview - template sử dụng trực tiếp
const getPreviewComponent = (questionType) => {
  const componentMap = {
    MULTIPLE_CHOICE: MultipleChoicePreview,
    TRUE_FALSE: TrueFalsePreview,
    FILL_BLANK: TextAnswerPreview,
    SHORT_ANSWER: TextAnswerPreview,
    VERB_FORM: TextAnswerPreview,
    ERROR_CORRECTION: TextAnswerPreview,
    MATCHING: MatchingPreview,
    SENTENCE_BUILDING: SentenceBuildingPreview,
    COMPLETE_CONVERSATION: ConversationPreview,
    PRONUNCIATION: PronunciationPreview,
    READING_COMPREHENSION: ReadingComprehensionPreview,
    OPEN_ENDED: OpenEndedPreview,
  }

  return componentMap[questionType] || null
}

// Helper functions
const questionTypeLabels = {
  MULTIPLE_CHOICE: '☑️ Multiple Choice',
  TRUE_FALSE: '✅ True/False',
  FILL_BLANK: '📝 Fill in the Blank',
  SHORT_ANSWER: '✏️ Short Answer',
  VERB_FORM: '🔤 Verb Form',
  ERROR_CORRECTION: '🔧 Error Correction',
  MATCHING: '🔗 Matching',
  SENTENCE_BUILDING: '🧩 Sentence Building',
  COMPLETE_CONVERSATION: '💬 Complete Conversation',
  PRONUNCIATION: '🔊 Pronunciation',
  READING_COMPREHENSION: '📖 Reading Comprehension',
  OPEN_ENDED: '📄 Open Ended',
}

const getQuestionTypeLabel = (type) => {
  return questionTypeLabels[type] || type
}

const getQuestionTypeTagType = (type) => {
  const typeMap = {
    MULTIPLE_CHOICE: 'primary',
    TRUE_FALSE: 'success',
    FILL_BLANK: 'warning',
    SHORT_ANSWER: 'info',
    VERB_FORM: 'info',
    ERROR_CORRECTION: 'danger',
    MATCHING: 'danger',
    SENTENCE_BUILDING: '',
    COMPLETE_CONVERSATION: 'warning',
    PRONUNCIATION: 'success',
    READING_COMPREHENSION: 'primary',
    OPEN_ENDED: '',
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped>
.question-preview {
  padding: 0;
}

.preview-card {
  border: 2px solid var(--el-border-color);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-info-section,
.question-text-section,
.metadata-section,
.explanation-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.question-text-card,
.explanation-card {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  padding: 16px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.question-text-card {
  font-size: 16px;
  font-weight: 500;
}

.explanation-card {
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .preview-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  :deep(.el-descriptions) {
    font-size: 12px;
  }
}
</style>
