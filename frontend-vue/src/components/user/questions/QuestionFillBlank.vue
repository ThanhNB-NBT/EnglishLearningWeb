<template>
  <div class="w-full text-base text-gray-800 dark:text-gray-200">
    <!-- Word Bank (nếu có) -->
    <div
      v-if="wordBank.length > 0"
      class="mb-4 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700"
    >
      <div class="text-xs font-bold text-gray-600 dark:text-gray-400 mb-2 uppercase tracking-wide">
        Kho từ:
      </div>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="(word, idx) in wordBank"
          :key="idx"
          class="px-3 py-1.5 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded text-gray-800 dark:text-gray-200 font-medium text-sm shadow-sm"
        >
          {{ word }}
        </span>
      </div>
    </div>

    <!-- Chỉ hiển thị các ô input -->
    <div class="space-y-3">
      <div v-for="(blank, idx) in blanksMetadata" :key="idx" class="flex items-center gap-3 w-full">
        <span class="font-bold text-gray-400 dark:text-gray-500 shrink-0 select-none min-w-[32px]"
          >{{ idx + 1 }}.</span
        >

        <div class="flex-1">
          <input
            type="text"
            v-model="userAnswers[idx]"
            @input="handleInput"
            :disabled="disabled"
            class="w-full border-b-2 border-gray-400 dark:border-gray-600 bg-transparent py-1.5 px-2 outline-none focus:border-blue-600 transition-colors font-medium"
            :class="getInputClass(idx)"
            placeholder="Nhập câu trả lời..."
          />

          <!-- ✅ FIXED: Display correct answer - handle both correctAnswers array and string -->
          <div
            v-if="disabled && showFeedback && getCorrectAnswerDisplay(blank)"
            class="mt-1 text-xs"
          >
            <span class="text-gray-500 dark:text-gray-400">Đáp án đúng: </span>
            <span class="font-bold text-green-700 dark:text-green-400">
              {{ getCorrectAnswerDisplay(blank) }}
            </span>
          </div>
        </div>
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

const userAnswers = ref({})

const wordBank = computed(() => {
  return props.question?.data?.wordBank || []
})

const blanksMetadata = computed(() => {
  const metaBlanks = props.question?.data?.blanks
  if (Array.isArray(metaBlanks) && metaBlanks.length > 0) {
    return metaBlanks
  }

  const text = props.question?.questionText || ''
  const matches = text.match(/_{8,}/g)
  const count = matches ? matches.length : 1

  return Array.from({ length: count }, (_, i) => ({ position: i + 1 }))
})

const initAnswers = () => {
  if (props.modelValue && typeof props.modelValue === 'object' && props.modelValue !== null) {
    const converted = {}
    Object.keys(props.modelValue).forEach((key) => {
      const numKey = parseInt(key)
      if (numKey > 0) {
        converted[numKey - 1] = props.modelValue[key]
      } else {
        converted[key] = props.modelValue[key]
      }
    })
    userAnswers.value = converted
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

const handleInput = () => {
  const positionBasedAnswers = {}
  Object.keys(userAnswers.value).forEach((key) => {
    const idx = parseInt(key)
    positionBasedAnswers[idx + 1] = userAnswers.value[key]
  })

  emit('update:modelValue', positionBasedAnswers)
}

// ✅ FIX: Check correctness per blank
const isBlankCorrect = (blankIndex) => {
  if (!props.showFeedback || !props.disabled) return null

  const blank = blanksMetadata.value[blankIndex]
  if (!blank) return null

  const userAnswer = (userAnswers.value[blankIndex] || '').trim().toLowerCase()

  // ✅ Handle both array and string from backend
  let correctAnswers = []
  if (Array.isArray(blank.correctAnswers)) {
    correctAnswers = blank.correctAnswers.map((a) => a.trim().toLowerCase())
  } else if (blank.correctAnswer) {
    // Backend returns single correctAnswer string
    correctAnswers = [blank.correctAnswer.trim().toLowerCase()]
  }

  if (correctAnswers.length === 0) return null

  return correctAnswers.includes(userAnswer)
}

// ✅ NEW: Get correct answer display text
const getCorrectAnswerDisplay = (blank) => {
  if (!blank) return ''

  // Case 1: Array of answers (original format)
  if (Array.isArray(blank.correctAnswers) && blank.correctAnswers.length > 0) {
    return blank.correctAnswers.join(' / ')
  }

  // Case 2: Single answer string (backend response format)
  if (blank.correctAnswer) {
    return blank.correctAnswer
  }

  return ''
}

const getInputClass = (blankIndex) => {
  if (!props.disabled || !props.showFeedback) return ''

  const isCorrect = isBlankCorrect(blankIndex)

  if (isCorrect === true) {
    return 'text-green-700 font-bold border-green-600'
  } else if (isCorrect === false) {
    return 'text-red-600 font-bold border-red-500'
  }

  return ''
}
</script>
