<template>
  <div class="w-full mt-2">
    <input
      type="text"
      :value="modelValue || ''"
      @input="$emit('update:modelValue', $event.target.value)"
      :disabled="disabled"
      class="w-full bg-transparent border-b border-gray-400 focus:border-blue-600 outline-none py-2 px-0 text-base placeholder-gray-400 transition-colors"
      :class="getInputClass()"
      placeholder="Nhập câu trả lời của bạn..."
    />

    <!-- ✅ Display correct answer after submit -->
    <div
      v-if="disabled && showFeedback && question.correctAnswer"
      class="mt-2 p-2 bg-green-50 dark:bg-green-900/20 rounded border border-green-200 dark:border-green-800"
    >
      <div class="text-xs font-semibold text-gray-600 dark:text-gray-400 mb-1">Đáp án đúng:</div>
      <div class="text-sm font-bold text-green-700 dark:text-green-400">
        {{ question.correctAnswer }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

defineEmits(['update:modelValue'])

const isCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) {
    return 'text-blue-800 dark:text-blue-300'
  }

  return isCorrect.value
    ? 'text-green-700 dark:text-green-400 font-bold border-green-600'
    : 'text-red-600 dark:text-red-400 font-bold border-red-500'
}
</script>
