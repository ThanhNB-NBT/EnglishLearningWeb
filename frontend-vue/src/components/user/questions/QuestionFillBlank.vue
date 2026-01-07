{ type: uploaded file fileName: user/questions/QuestionFillBlank.vue fullContent:
<template>
  <div class="w-full text-base text-gray-800 dark:text-gray-200 text-left">
    <div v-if="hasInlineBlanks" class="leading-loose">
      <template v-for="(part, idx) in parsedParts" :key="idx">
        <span v-if="!part.isBlank" v-html="part.text"></span>
        <span v-else class="inline-block mx-1">
          <input
            type="text"
            v-model="userAnswers[part.index]"
            @input="handleInput"
            :disabled="disabled"
            class="min-w-[60px] max-w-[150px] border-b border-dotted border-gray-500 bg-transparent text-center focus:border-blue-600 focus:border-b-2 outline-none transition-colors px-1 font-medium"
            :class="getInputClass()"
            placeholder="..."
          />
        </span>
      </template>
    </div>

    <div v-else class="space-y-3 pt-1">
      <div v-for="(blank, idx) in blanksMetadata" :key="idx" class="flex items-center gap-2 w-full">
        <span class="font-bold text-gray-500 shrink-0 select-none">({{ idx + 1 }})</span>
        <input
          type="text"
          v-model="userAnswers[idx]"
          @input="handleInput"
          :disabled="disabled"
          class="flex-1 border-b border-gray-400 bg-transparent py-1 px-2 outline-none focus:border-blue-600 focus:border-b-2 transition-colors"
          :class="getInputClass()"
          placeholder="..."
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: [Object, Array, String], default: null },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const hasInlineBlanks = computed(() => (props.question?.questionText || '').includes('___'))

const blanksMetadata = computed(() => {
  const metaBlanks = props.question?.data?.blanks || props.question?.metadata?.blanks
  if (Array.isArray(metaBlanks) && metaBlanks.length > 0) return metaBlanks
  if (hasInlineBlanks.value) {
    return parsedParts.value.filter((p) => p.isBlank).map((_, i) => ({ position: i + 1 }))
  }
  return [{ position: 1 }]
})

const userAnswers = ref({})

const initAnswers = () => {
  if (props.modelValue && typeof props.modelValue === 'object' && props.modelValue !== null) {
    userAnswers.value = { ...props.modelValue }
  } else if (Array.isArray(props.modelValue)) {
    const obj = {}
    props.modelValue.forEach((val, idx) => {
      obj[idx] = val
    })
    userAnswers.value = obj
  } else {
    userAnswers.value = {}
  }
}

onMounted(initAnswers)
watch(
  () => props.modelValue,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(userAnswers.value)) initAnswers()
  },
)

const handleInput = () => emit('update:modelValue', { ...userAnswers.value })

const parsedParts = computed(() => {
  if (!hasInlineBlanks.value) return []
  const text = props.question?.questionText || ''
  const parts = []
  const regex = /_{3,}/g
  let lastIndex = 0
  let match
  let blankCount = 0
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex)
      parts.push({ isBlank: false, text: text.substring(lastIndex, match.index) })
    parts.push({ isBlank: true, index: blankCount++ })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < text.length) parts.push({ isBlank: false, text: text.substring(lastIndex) })
  return parts
})

const isQuestionCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getInputClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isQuestionCorrect.value
    ? 'text-green-700 font-bold border-green-600'
    : 'text-red-600 font-bold border-red-500'
}
</script>
}
