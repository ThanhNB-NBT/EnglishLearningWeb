<template>
  <div class="w-full mt-1 text-base text-gray-800 dark:text-gray-200">
    <div class="mb-3 select-none text-left font-medium tracking-wide">
      {{ shuffledWordsString }}
    </div>
    <div class="flex items-center gap-2">
      <input
        type="text"
        :value="modelValue || ''"
        @input="$emit('update:modelValue', $event.target.value)"
        :disabled="disabled"
        class="w-full border-b border-gray-400 bg-transparent py-1 px-1 outline-none focus:border-blue-600 focus:border-b-2 transition-colors placeholder-gray-400"
        :class="getInputClass()"
        placeholder="Sắp xếp thành câu hoàn chỉnh..."
      />
      <div v-if="disabled && showFeedback" class="shrink-0">
        <span v-if="isCorrect" class="text-green-600 font-bold text-lg">✓</span>
        <span v-else class="text-red-500 font-bold text-lg">✕</span>
      </div>
    </div>

    <!-- ✅ FIXED: Chỉ hiển thị khi SAI -->
    <div
      v-if="disabled && showFeedback && !isCorrect && question.data?.correctAnswer"
      class="mt-3 p-3 bg-red-50 dark:bg-red-900/20 rounded-lg border border-red-200 dark:border-red-800"
    >
      <div class="flex items-start gap-2">
        <svg class="w-4 h-4 text-red-500 mt-0.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
          <path
            fill-rule="evenodd"
            d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
            clip-rule="evenodd"
          />
        </svg>
        <div class="flex-1">
          <div class="text-xs font-semibold text-red-700 dark:text-red-400 mb-0.5">
            Đáp án đúng:
          </div>
          <div class="text-sm font-bold text-red-800 dark:text-red-300">
            {{ question.data.correctAnswer }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})
defineEmits(['update:modelValue'])

const shuffledWords = ref([])
onMounted(() => {
  const data = props.question?.data || props.question?.metadata || {}
  const rawWords = data.words || data.buildingWords || []
  shuffledWords.value = [...rawWords].sort(() => Math.random() - 0.5)
})
const shuffledWordsString = computed(() => shuffledWords.value.join(' / '))

const isCorrect = computed(() => props.question.isCorrect === true)

const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isCorrect.value
    ? 'text-green-700 font-bold border-green-600'
    : 'text-red-600 font-bold border-red-500'
}
</script>
