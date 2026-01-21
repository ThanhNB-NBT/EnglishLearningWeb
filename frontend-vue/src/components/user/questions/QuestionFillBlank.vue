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

    <!-- Các ô input -->
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

          <!-- ✅ FIXED: Hiển thị đáp án đúng CHỈ KHI SAI -->
          <div
            v-if="disabled && showFeedback && shouldShowCorrectAnswer(idx)"
            class="mt-2 p-2 bg-red-50 dark:bg-red-900/20 rounded border border-red-200 dark:border-red-800"
          >
            <div class="flex items-start gap-2">
              <svg class="w-4 h-4 text-red-500 mt-0.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
              </svg>
              <div class="flex-1">
                <div class="text-xs font-semibold text-red-700 dark:text-red-400 mb-0.5">
                  Đáp án đúng:
                </div>
                <div class="text-sm font-bold text-red-800 dark:text-red-300">
                  {{ getCorrectAnswerDisplay(blank) }}
                </div>
              </div>
            </div>
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

// ✅ NEW: Parse chi tiết từ API response
const blankDetailsMap = computed(() => {
  if (!props.question?.correctAnswer) return null

  try {
    const parsed = JSON.parse(props.question.correctAnswer)
    // Check if it's the detailed format from backend
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed
    }
  } catch (e) {
    console.warn('Failed to parse blank details:', e)
    // Not JSON or not detailed format
  }
  return null
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

// ✅ FIXED: Check correctness per blank using detailed data from backend
const isBlankCorrect = (blankIndex) => {
  if (!props.showFeedback || !props.disabled) return null

  const blank = blanksMetadata.value[blankIndex]
  if (!blank) return null

  const position = blank.position

  // Try to get detailed info from API response
  if (blankDetailsMap.value) {
    const detail = blankDetailsMap.value[position] || blankDetailsMap.value[String(position)]
    if (detail && detail.isCorrect !== undefined) {
      return detail.isCorrect
    }
  }

  // Fallback: compare locally (original logic)
  const userAnswer = (userAnswers.value[blankIndex] || '').trim().toLowerCase()

  let correctAnswers = []
  if (Array.isArray(blank.correctAnswers)) {
    correctAnswers = blank.correctAnswers.map((a) => a.trim().toLowerCase())
  } else if (blank.correctAnswer) {
    correctAnswers = [blank.correctAnswer.trim().toLowerCase()]
  }

  if (correctAnswers.length === 0) return null

  return correctAnswers.includes(userAnswer)
}

// ✅ NEW: Chỉ hiển thị đáp án đúng khi câu trả lời SAI
const shouldShowCorrectAnswer = (blankIndex) => {
  const isCorrect = isBlankCorrect(blankIndex)
  return isCorrect === false // Chỉ hiển thị khi SAI (không phải null hoặc true)
}

// ✅ FIXED: Get correct answer display từ detailed data hoặc fallback
const getCorrectAnswerDisplay = (blank) => {
  if (!blank) return ''

  const position = blank.position

  // Try detailed data first
  if (blankDetailsMap.value) {
    const detail = blankDetailsMap.value[position] || blankDetailsMap.value[String(position)]
    if (detail && detail.correctAnswer) {
      return detail.correctAnswer
    }
  }

  // Fallback to blank metadata
  if (Array.isArray(blank.correctAnswers) && blank.correctAnswers.length > 0) {
    return blank.correctAnswers.join(' / ')
  }

  if (blank.correctAnswer) {
    return blank.correctAnswer
  }

  return ''
}

const getInputClass = (blankIndex) => {
  if (!props.disabled || !props.showFeedback) return ''

  const isCorrect = isBlankCorrect(blankIndex)

  if (isCorrect === true) {
    return 'text-green-700 dark:text-green-400 font-bold border-green-600'
  } else if (isCorrect === false) {
    return 'text-red-600 dark:text-red-400 font-bold border-red-500'
  }

  return ''
}
</script>
