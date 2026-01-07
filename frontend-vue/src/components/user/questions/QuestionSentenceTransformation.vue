<template>
  <div class="w-full mt-1 text-base text-gray-800 dark:text-gray-200">
    <div class="mb-2 font-medium">{{ originalSentence }}</div>
    <div class="flex flex-wrap items-end gap-2 w-full">
      <span class="whitespace-nowrap pb-1">{{ beginningPhrase }}</span>
      <input
        type="text"
        :value="modelValue || ''"
        @input="$emit('update:modelValue', $event.target.value)"
        :disabled="disabled"
        class="flex-1 min-w-[200px] border-b border-gray-400 bg-transparent px-1 py-1 outline-none focus:border-blue-600 focus:border-b-2 transition-colors placeholder-gray-400"
        :class="getInputClass()"
        placeholder="..."
      />
      <div v-if="disabled && showFeedback" class="pb-1">
        <span v-if="isCorrect" class="text-green-600 font-bold text-lg">✓</span>
        <span v-else class="text-red-500 font-bold text-lg">✕</span>
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

const originalSentence = computed(
  () => props.question?.data?.originalSentence || props.question?.questionText || '',
)
const beginningPhrase = computed(
  () => props.question?.data?.beginningPhrase || props.question?.data?.beginWith || '',
)

// ✅ FIX: Chỉ dùng API
const isCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isCorrect.value ? 'text-green-700 font-bold' : 'text-red-600 border-red-500'
}
</script>
