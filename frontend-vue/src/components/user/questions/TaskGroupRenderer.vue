<template>
  <div class="mb-10">
    <div
      v-if="task.taskInstruction"
      class="mb-6 pb-2 border-b border-gray-100 dark:border-gray-800"
    >
      <span class="text-lg font-bold text-gray-400 uppercase tracking-wider mr-2">{{ label }}</span>
      <span
        class="text-gray-800 dark:text-gray-200 font-medium italic"
        v-html="task.taskInstruction"
      ></span>
    </div>

    <div class="space-y-8">
      <div
        v-for="(q, index) in validQuestions"
        :key="`${taskKey}-q-${q.id}`"
        class="flex items-start gap-3"
      >
        <div class="shrink-0 w-8 text-right pt-1">
          <span
            class="text-lg font-bold font-mono transition-colors"
            :class="answers[q.id] ? 'text-blue-600' : 'text-gray-400'"
          >
            {{ calculateIndex(index) }}.
          </span>
        </div>

        <div class="flex-1 w-full min-w-0">
          <div
            v-if="shouldShowQuestionText(q)"
            class="mb-2 text-gray-900 dark:text-gray-100 font-medium text-base leading-relaxed"
            v-html="sanitizeQuestionText(q)"
          ></div>

          <QuestionRenderer
            :question="q"
            :model-value="answers[q.id] ?? null"
            @update:model-value="(val) => onAnswerChange(q.id, val)"
            :disabled="disabled"
            :show-feedback="showFeedback"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import QuestionRenderer from './QuestionRenderer.vue'

const props = defineProps({
  task: { type: Object, required: true },
  answers: { type: Object, required: true },
  label: { type: String, default: 'Task' },
  startIndex: { type: Number, default: 1 },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

const emit = defineEmits(['update-answer'])
const taskKey = computed(() => props.task.taskGroupId || `task-${props.label}`)
const validQuestions = computed(() => (props.task.questions || []).filter((q) => q && q.id))
const calculateIndex = (idx) => props.startIndex + idx
const onAnswerChange = (questionId, value) => emit('update-answer', { questionId, value })

const shouldShowQuestionText = (question) => {
  const type = question.questionType

  // Các loại này tự render questionText bên trong component
  const fullControlTypes = ['SENTENCE_TRANSFORMATION', 'SENTENCE_BUILDING', 'ERROR_CORRECTION']

  return !fullControlTypes.includes(type)
}

// Xử lý questionText cho FILL_BLANK: loại bỏ <ol> wrapper, giữ lại nội dung <li>
const sanitizeQuestionText = (question) => {
  if (question.questionType !== 'FILL_BLANK' && question.questionType !== 'VERB_FORM') {
    return question.questionText
  }

  let text = question.questionText || ''

  // Loại bỏ thẻ <ol> và </ol>
  text = text.replace(/<\/?ol[^>]*>/gi, '')

  // Thay thế <li> bằng div với số thứ tự ẩn (vì đã có ở input)
  text = text.replace(/<li[^>]*>/gi, '<div class="mb-1.5">')
  text = text.replace(/<\/li>/gi, '</div>')

  return text
}
</script>
