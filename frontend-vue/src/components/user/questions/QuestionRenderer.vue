<template>
  <component
    :is="questionComponent"
    v-if="questionComponent && question && question.id"
    :question="question"
    :key="`q-${question.id}-${componentKey}`"
    v-model="localAnswer"
    :disabled="disabled"
    :show-feedback="showFeedback"
    @update:modelValue="handleAnswerUpdate"
  />
  <el-alert v-else-if="question" type="warning" :closable="false">
    Loại câu hỏi không được hỗ trợ: {{ question.questionType }}
  </el-alert>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import QuestionMultipleChoice from './QuestionMultipleChoice.vue'
import QuestionFillBlank from './QuestionFillBlank.vue'
import QuestionMatching from './QuestionMatching.vue'
import QuestionTextAnswer from './QuestionTextAnswer.vue'
import QuestionSentenceBuilding from './QuestionSentenceBuilding.vue'
import QuestionSentenceTransformation from './QuestionSentenceTransformation.vue'
import QuestionErrorCorrection from './QuestionErrorCorrection.vue'
import QuestionPronunciation from './QuestionPronunciation.vue'
import QuestionOpenEnded from './QuestionOpenEnded.vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: [String, Array, Object], default: null },
  showFeedback: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

// ✅ Add component key for force re-render
const componentKey = ref(0)

// ✅ Initialize with proper null handling
const localAnswer = ref(props.modelValue ?? null)

const componentMap = {
  MULTIPLE_CHOICE: QuestionMultipleChoice,
  TRUE_FALSE: QuestionMultipleChoice,
  COMPLETE_CONVERSATION: QuestionMultipleChoice,
  FILL_BLANK: QuestionFillBlank,
  VERB_FORM: QuestionFillBlank,
  TEXT_ANSWER: QuestionTextAnswer,
  MATCHING: QuestionMatching,
  SENTENCE_BUILDING: QuestionSentenceBuilding,
  SENTENCE_TRANSFORMATION: QuestionSentenceTransformation,
  ERROR_CORRECTION: QuestionErrorCorrection,
  PRONUNCIATION: QuestionPronunciation,
  OPEN_ENDED: QuestionOpenEnded,
}

const questionComponent = computed(() => {
  return componentMap[props.question?.questionType] || null
})

const handleAnswerUpdate = (answer) => {
  localAnswer.value = answer
  emit('update:modelValue', answer)
}

// ✅ FIX: Watch question ID change để reset component
watch(
  () => props.question?.id,
  (newId, oldId) => {
    if (newId !== oldId) {
      // Force re-render by incrementing key
      componentKey.value++
      // Reset local answer
      localAnswer.value = null
    }
  },
)

watch(
  () => props.modelValue,
  (newVal) => {
    // ✅ So sánh để tránh update không cần thiết
    // Đây là key fix cho infinite loop!
    if (JSON.stringify(localAnswer.value) !== JSON.stringify(newVal)) {
      localAnswer.value = newVal ?? null
    }
  },
)
</script>
