<template>
  <div class="w-full mt-1">
    <div class="flex flex-col sm:flex-row gap-4">
      <div class="flex-1">
        <label class="block text-xs font-bold text-gray-500 mb-1 uppercase">Lỗi sai</label>
        <input
          type="text"
          v-model="localModel.error"
          @input="emitUpdate"
          :disabled="disabled"
          class="w-full border-b border-gray-400 bg-transparent py-2 px-1 outline-none focus:border-blue-600 focus:border-b-2 transition-colors font-mono"
          :class="getInputClass()"
        />
      </div>
      <div class="hidden sm:flex items-end pb-3 text-gray-400">➔</div>
      <div class="flex-1">
        <label class="block text-xs font-bold text-gray-500 mb-1 uppercase">Sửa thành</label>
        <input
          type="text"
          v-model="localModel.correction"
          @input="emitUpdate"
          :disabled="disabled"
          class="w-full border-b border-gray-400 bg-transparent py-2 px-1 outline-none focus:border-blue-600 focus:border-b-2 transition-colors font-mono"
          :class="getInputClass()"
        />
      </div>
      <div v-if="disabled && showFeedback" class="flex items-end pb-2">
        <span v-if="isCorrect" class="text-green-600 font-bold text-lg">✓</span>
        <span v-else class="text-red-500 font-bold text-lg">✕</span>
      </div>
    </div>

    <!-- ✅ NEW: Display correct answer after submit -->
    <div
      v-if="disabled && showFeedback && question.data?.correction"
      class="mt-3 p-3 bg-green-50 dark:bg-green-900/20 rounded-lg border border-green-200 dark:border-green-800"
    >
      <div class="text-xs font-bold text-gray-600 dark:text-gray-400 mb-1">Đáp án đúng:</div>
      <div class="text-sm font-mono text-green-700 dark:text-green-400">
        {{ question.data.correction }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: [Object, String], default: () => ({ error: '', correction: '' }) },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])
const localModel = ref({ error: '', correction: '' })

const initData = () => {
  if (props.modelValue && typeof props.modelValue === 'object') {
    localModel.value = {
      error: props.modelValue.error || '',
      correction: props.modelValue.correction || '',
    }
  } else if (typeof props.modelValue === 'string') {
    try {
      const parsed = JSON.parse(props.modelValue)
      localModel.value = { error: parsed.error || '', correction: parsed.correction || '' }
    } catch (e) {
      localModel.value = { error: '', correction: '' }
      console.error('Failed to parse modelValue as JSON:', e)
    }
  }
}
onMounted(initData)
watch(() => props.modelValue, initData)
const emitUpdate = () => emit('update:modelValue', { ...localModel.value })

const isCorrect = computed(() => {
  return props.question.isCorrect === true
})
const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isCorrect.value
    ? 'text-green-700 font-bold border-green-600'
    : 'text-red-600 font-bold border-red-500'
}
</script>
