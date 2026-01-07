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

// ✅ FIX: Chỉ dùng API, không tự tính
const isCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isCorrect.value ? 'text-green-700 font-bold' : 'text-red-600 border-red-500'
}
</script>
