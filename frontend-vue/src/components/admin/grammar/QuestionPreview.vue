<template>
  <div class="question-preview">
    <!-- Header -->
    <div class="preview-header">
      <div class="header-left">
        <el-icon :size="28" :color="getQuestionTypeColor(question.questionType)">
          <component :is="getQuestionTypeIcon(question.questionType)" />
        </el-icon>
        <el-text size="large" tag="b">
          {{ getQuestionTypeLabel(question.questionType) }}
        </el-text>
      </div>
      <el-tag :type="getQuestionTypeTagType(question.questionType)" size="large">
        {{ getQuestionTypeLabel(question.questionType) }}
      </el-tag>
    </div>

    <el-divider />

    <!-- Question Info -->
    <div class="info-section">
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="Điểm số">
          <el-tag type="warning" size="large">
            <el-icon><Trophy /></el-icon>
            {{ question.points }} điểm
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Thứ tự">
          <el-tag type="info" size="large">
            #{{ question.orderIndex }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Loại câu hỏi">
          <el-tag :type="getQuestionTypeTagType(question.questionType)" size="large">
            {{ getQuestionTypeLabel(question.questionType) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- Question Text -->
    <div class="question-section">
      <el-card shadow="never" class="question-card">
        <template #header>
          <div class="section-header">
            <el-icon :size="20"><QuestionFilled /></el-icon>
            <el-text tag="b" size="large">Đề bài</el-text>
          </div>
        </template>
        <div v-html="question.questionText" class="question-text"></div>
      </el-card>
    </div>

    <!-- Correct Answer Section - HIGHLIGHTED -->
    <div class="correct-answer-section">
      <el-card shadow="never" class="correct-answer-card">
        <template #header>
          <div class="section-header">
            <el-icon :size="20" color="var(--el-color-success)"><CircleCheckFilled /></el-icon>
            <el-text tag="b" size="large" type="success">Đáp án đúng</el-text>
          </div>
        </template>
        <div class="correct-answer-display">
          {{ getCorrectAnswerText(question) }}
        </div>
      </el-card>
    </div>

    <!-- Metadata Preview -->
    <div class="metadata-section">
      <el-card shadow="never" class="metadata-card">
        <template #header>
          <div class="section-header">
            <el-icon :size="20"><Files /></el-icon>
            <el-text tag="b" size="large">Chi tiết câu hỏi</el-text>
          </div>
        </template>
        <component
          :is="getPreviewComponent(question.questionType)"
          :metadata="question.metadata"
          :question-text="question.questionText"
        />
      </el-card>
    </div>

    <!-- Explanation -->
    <div v-if="question.explanation" class="explanation-section">
      <el-card shadow="never" class="explanation-card">
        <template #header>
          <div class="section-header">
            <el-icon :size="20"><InfoFilled /></el-icon>
            <el-text tag="b" size="large">Giải thích đáp án</el-text>
          </div>
        </template>
        <el-text class="explanation-text">{{ question.explanation }}</el-text>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import {
  QuestionFilled, Files, InfoFilled, Trophy, CircleCheckFilled,
  DocumentChecked, Select, Edit, Link,
  ChatDotSquare, Microphone, Reading, DocumentCopy
} from '@element-plus/icons-vue'

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

const props = defineProps({
  question: {
    type: Object,
    required: true,
  },
})

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

// Extract correct answer based on question type
const getCorrectAnswerText = (question) => {
  const metadata = question.metadata
  const type = question.questionType

  switch (type) {
    case 'MULTIPLE_CHOICE': {
      const correctOption = metadata.options?.find(o => o.isCorrect)
      return correctOption ? correctOption.text : 'N/A'
    }

    case 'TRUE_FALSE':
      return metadata.correctAnswer ? 'TRUE ' : 'FALSE '

    case 'FILL_BLANK':
    case 'SHORT_ANSWER':
    case 'VERB_FORM':
    case 'ERROR_CORRECTION':
      return metadata.correctAnswer || 'N/A'
    case 'MATCHING': {
      const pairCount = metadata.pairs?.length || 0
      return `${pairCount} cặp ghép đúng (xem chi tiết bên dưới)`
    }

    case 'SENTENCE_BUILDING':
      return metadata.correctSentence || 'N/A'

    case 'COMPLETE_CONVERSATION':
    case 'PRONUNCIATION': {
      const classCount = metadata.classifications?.length || 0
      return `${classCount} từ phân loại đúng (xem chi tiết bên dưới)`
    }
    case 'READING_COMPREHENSION': {
      const blankCount = metadata.blanks?.length || 0
      return `${blankCount} chỗ trống (xem chi tiết bên dưới)`
    }

    case 'OPEN_ENDED':
      return metadata.suggestedAnswer || 'Câu trả lời tự do (cần đánh giá)'

    default:
      return 'N/A'
  }
}

const questionTypeLabels = {
  MULTIPLE_CHOICE: 'Trắc nghiệm',
  TRUE_FALSE: 'Đúng/Sai',
  FILL_BLANK: 'Điền từ',
  SHORT_ANSWER: 'Trả lời ngắn',
  VERB_FORM: 'Dạng động từ',
  ERROR_CORRECTION: 'Sửa lỗi',
  MATCHING: 'Nối câu',
  SENTENCE_BUILDING: 'Sắp xếp câu',
  COMPLETE_CONVERSATION: 'Hoàn thành hội thoại',
  PRONUNCIATION: 'Phát âm',
  READING_COMPREHENSION: 'Đọc hiểu',
  OPEN_ENDED: 'Tự luận',
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
    OPEN_ENDED: 'info',
  }
  return typeMap[type] || 'info'
}

const getQuestionTypeColor = (type) => {
  const colorMap = {
    MULTIPLE_CHOICE: '#409EFF',
    TRUE_FALSE: '#67C23A',
    FILL_BLANK: '#E6A23C',
    SHORT_ANSWER: '#909399',
    VERB_FORM: '#909399',
    ERROR_CORRECTION: '#F56C6C',
    MATCHING: '#F56C6C',
    SENTENCE_BUILDING: '#409EFF',
    COMPLETE_CONVERSATION: '#E6A23C',
    PRONUNCIATION: '#67C23A',
    READING_COMPREHENSION: '#409EFF',
    OPEN_ENDED: '#909399',
  }
  return colorMap[type] || '#909399'
}

const getQuestionTypeIcon = (type) => {
  const iconMap = {
    MULTIPLE_CHOICE: DocumentChecked,
    TRUE_FALSE: Select,
    FILL_BLANK: Edit,
    SHORT_ANSWER: Edit,
    VERB_FORM: Edit,
    ERROR_CORRECTION: DocumentCopy,
    MATCHING: Link,
    SENTENCE_BUILDING: DocumentCopy,
    COMPLETE_CONVERSATION: ChatDotSquare,
    PRONUNCIATION: Microphone,
    READING_COMPREHENSION: Reading,
    OPEN_ENDED: DocumentCopy,
  }
  return iconMap[type] || QuestionFilled
}
</script>

<style scoped>
.question-preview {
  padding: 0;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-section,
.question-section,
.correct-answer-section,
.metadata-section,
.explanation-section {
  margin-bottom: 20px;
}

.question-card,
.correct-answer-card,
.metadata-card,
.explanation-card {
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
}

.question-card {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.correct-answer-card {
  border-color: var(--el-color-success);
  background: linear-gradient(135deg, var(--el-color-success-light-9) 0%, #f0f9ff 100%);
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.15);
}

.metadata-card {
  border-color: var(--el-color-info-light-5);
  background: var(--el-fill-color-lighter);
}

.explanation-card {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-text {
  line-height: 1.8;
  font-size: 16px;
  color: var(--el-text-color-primary);
  padding: 8px 0;
}

.question-text :deep(p) {
  margin-bottom: 12px;
}

.question-text :deep(ul),
.question-text :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.question-text :deep(li) {
  margin-bottom: 8px;
}

.question-text :deep(strong) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.question-text :deep(em) {
  font-style: italic;
}

.question-text :deep(code) {
  background: var(--el-fill-color);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.correct-answer-display {
  padding: 20px;
  background: white;
  border-radius: 6px;
  border-left: 4px solid var(--el-color-success);
  font-size: 17px;
  font-weight: 600;
  color: var(--el-color-success);
  line-height: 1.6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.explanation-text {
  line-height: 1.8;
  font-size: 15px;
  display: block;
  padding: 8px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .preview-header {
    flex-direction: column;
    align-items: flex-start;
  }

  :deep(.el-descriptions) {
    font-size: 13px;
  }

  .question-text {
    font-size: 15px;
  }

  .correct-answer-display {
    font-size: 15px;
    padding: 16px;
  }
}
</style>
