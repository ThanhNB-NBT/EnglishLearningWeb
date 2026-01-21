<template>
  <div class="w-full mt-4 text-base text-gray-800 dark:text-gray-200">
    <div class="grid grid-cols-2 gap-x-8 gap-y-4 mb-6">
      <div>
        <h4 class="font-bold mb-3 border-b pb-1">Cột A</h4>
        <div class="space-y-3">
          <div
            v-for="(item, index) in leftItems"
            :key="`left-${index}`"
            class="flex items-start gap-2 p-2 rounded cursor-pointer transition-colors"
            :class="getLeftItemClass(index)"
            @click="!disabled && selectLeft(index)"
          >
            <span class="font-bold min-w-[20px]">{{ index + 1 }}.</span>
            <span v-html="item"></span>
          </div>
        </div>
      </div>

      <div>
        <h4 class="font-bold mb-3 border-b pb-1">Cột B</h4>
        <div class="space-y-3">
          <div
            v-for="(item, index) in rightItems"
            :key="`right-${index}`"
            class="flex items-start gap-2 p-2 rounded cursor-pointer transition-colors"
            :class="getRightItemClass(index)"
            @click="!disabled && selectRight(index)"
          >
            <span class="font-bold min-w-[20px]">{{ getChar(index) }}.</span>
            <span v-html="item"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6 border-t pt-4">
      <div class="font-bold mb-2">Trả lời:</div>
      <div class="flex flex-wrap gap-4">
        <div
          v-for="(item, leftIndex) in leftItems"
          :key="`ans-${leftIndex}`"
          class="flex items-center gap-1"
        >
          <span class="font-bold text-gray-600 dark:text-gray-400">{{ leftIndex + 1 }} - </span>

          <div
            class="w-10 h-8 border-b-2 border-gray-400 flex items-center justify-center font-bold text-blue-700 bg-gray-50 dark:bg-gray-800 dark:text-blue-400 transition-colors relative"
            :class="getAnswerBoxClass(leftIndex)"
          >
            {{ getMatchedChar(leftIndex) }}

            <button
              v-if="!disabled && currentMap[leftIndex] !== undefined"
              @click.stop="removeMapping(leftIndex)"
              class="absolute -top-2 -right-2 text-gray-400 hover:text-red-500 bg-white rounded-full shadow-sm"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
          </div>

          <!-- ✅ Hiển thị đáp án đúng CHỈ KHI SAI -->
          <div
            v-if="disabled && showFeedback && shouldShowCorrectAnswer(leftIndex)"
            class="ml-2 flex items-center gap-1 text-xs"
          >
            <svg class="w-3 h-3 text-red-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
            </svg>
            <span class="text-red-600 dark:text-red-400">→</span>
            <span class="font-bold text-green-600 dark:text-green-400">
              {{ getCorrectAnswerChar(leftIndex) }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="disabled && showFeedback" class="mt-3 font-medium">
        <span v-if="isQuestionCorrect" class="text-green-600">✓ Chính xác</span>
        <span v-else class="text-red-600">✕ Có đáp án sai</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

// ✅ State: Dùng INDEX thay vì text
const selectedLeft = ref(null) // Lưu index của left item được chọn
const currentMap = ref({}) // Format: { leftIndex: rightIndex }
const rightItems = ref([])

const leftItems = computed(() => props.question?.data?.leftItems || [])

const getChar = (index) => String.fromCharCode(65 + index)

// ✅ Parse chi tiết từ API response
const pairDetailsMap = computed(() => {
  if (!props.question?.correctAnswer) return null

  try {
    const parsed = JSON.parse(props.question.correctAnswer)
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed
    }
  } catch (e) {
    console.error('Error parsing correctAnswer:', e)
    // Not JSON or not detailed format
  }
  return null
})

const initData = () => {
  const rawRight = props.question?.data?.rightItems || []
  rightItems.value = [...rawRight].sort(() => Math.random() - 0.5)

  // ✅ Convert modelValue từ text-based sang index-based
  if (props.modelValue) {
    const newMap = {}
    Object.entries(props.modelValue).forEach(([leftText, rightText]) => {
      const leftIndex = leftItems.value.indexOf(leftText)
      const rightIndex = rightItems.value.indexOf(rightText)

      if (leftIndex !== -1 && rightIndex !== -1) {
        newMap[leftIndex] = rightIndex
      }
    })
    currentMap.value = newMap
  }
}

onMounted(initData)

watch(() => props.question, initData)

watch(
  () => props.modelValue,
  (newVal) => {
    if (!newVal) {
      currentMap.value = {}
      return
    }

    // Convert from text-based to index-based
    const newMap = {}
    Object.entries(newVal).forEach(([leftText, rightText]) => {
      const leftIndex = leftItems.value.indexOf(leftText)
      const rightIndex = rightItems.value.indexOf(rightText)

      if (leftIndex !== -1 && rightIndex !== -1) {
        newMap[leftIndex] = rightIndex
      }
    })

    if (JSON.stringify(newMap) !== JSON.stringify(currentMap.value)) {
      currentMap.value = newMap
    }
  },
  { deep: true },
)

// ✅ Select functions - Dùng INDEX
const selectLeft = (leftIndex) => {
  if (selectedLeft.value === leftIndex) {
    selectedLeft.value = null
  } else {
    selectedLeft.value = leftIndex
  }
}

const selectRight = (rightIndex) => {
  if (selectedLeft.value === null) return

  const newMap = { ...currentMap.value }

  // Gỡ rightIndex này nếu đã được ghép với left khác
  Object.keys(newMap).forEach((key) => {
    if (newMap[key] === rightIndex) delete newMap[key]
  })

  // Ghép mới
  newMap[selectedLeft.value] = rightIndex
  currentMap.value = newMap
  selectedLeft.value = null

  // ✅ Emit về dạng text cho backend
  emitTextBasedMap()
}

const removeMapping = (leftIndex) => {
  const newMap = { ...currentMap.value }
  delete newMap[leftIndex]
  currentMap.value = newMap
  emitTextBasedMap()
}

// ✅ Convert index-based map thành text-based map để gửi backend
const emitTextBasedMap = () => {
  const textMap = {}
  Object.entries(currentMap.value).forEach(([leftIdx, rightIdx]) => {
    const leftText = leftItems.value[parseInt(leftIdx)]
    const rightText = rightItems.value[parseInt(rightIdx)]
    if (leftText && rightText) {
      textMap[leftText] = rightText
    }
  })
  emit('update:modelValue', textMap)
}

const getMatchedChar = (leftIndex) => {
  const rightIndex = currentMap.value[leftIndex]
  return rightIndex !== undefined ? getChar(rightIndex) : ''
}

// ✅ Check correctness
const isPairCorrect = (leftIndex) => {
  if (!pairDetailsMap.value) return null

  const leftText = leftItems.value[leftIndex]
  const detail = pairDetailsMap.value[leftText]

  if (detail && detail.isCorrect !== undefined) {
    return detail.isCorrect
  }

  return null
}

const shouldShowCorrectAnswer = (leftIndex) => {
  const isCorrect = isPairCorrect(leftIndex)
  return isCorrect === false
}

const getCorrectAnswerChar = (leftIndex) => {
  if (!pairDetailsMap.value) return ''

  const leftText = leftItems.value[leftIndex]
  const detail = pairDetailsMap.value[leftText]

  if (detail && detail.correctRight) {
    const idx = rightItems.value.indexOf(detail.correctRight)
    return idx !== -1 ? getChar(idx) : '?'
  }

  return ''
}

// ✅ Styling functions - Dùng INDEX
const getLeftItemClass = (leftIndex) => {
  if (selectedLeft.value === leftIndex) {
    return 'bg-blue-100 text-blue-800 font-bold ring-2 ring-blue-400'
  }
  if (currentMap.value[leftIndex] !== undefined) {
    return 'bg-gray-100 dark:bg-gray-700 text-gray-500 line-through decoration-blue-500'
  }
  return 'hover:bg-gray-50 dark:hover:bg-gray-800 border border-gray-200'
}

const getRightItemClass = (rightIndex) => {
  // Check if this right item is already matched
  const isMatched = Object.values(currentMap.value).includes(rightIndex)

  if (isMatched) {
    return 'bg-gray-100 dark:bg-gray-700 text-gray-500 line-through decoration-blue-500'
  }

  if (selectedLeft.value !== null) {
    return 'hover:bg-blue-50 dark:hover:bg-blue-900/30 cursor-pointer ring-1 ring-transparent hover:ring-blue-300 border border-gray-200'
  }

  return 'border border-gray-200'
}

const isQuestionCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getAnswerBoxClass = (leftIndex) => {
  if (!props.disabled || !props.showFeedback) return ''

  const isPairCorrectValue = isPairCorrect(leftIndex)

  if (isPairCorrectValue === true) {
    return 'text-green-600 border-green-600 bg-green-50'
  } else if (isPairCorrectValue === false) {
    return 'text-red-500 border-red-500 bg-red-50'
  }

  return ''
}
</script>
