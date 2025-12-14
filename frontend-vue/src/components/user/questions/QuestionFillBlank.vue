<template>
  <div class="w-full">
    <!-- Word Bank (chỉ cho FILL_BLANK có nhiều blanks) -->
    <div
      v-if="hasWordBank"
      class="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border-2 border-blue-200 dark:border-blue-800 rounded-lg"
    >
      <div class="text-xs font-bold uppercase tracking-wider text-blue-700 dark:text-blue-400 mb-3 flex items-center gap-2">
        <el-icon><Collection /></el-icon>
        Word Bank (Ngân hàng từ)
      </div>
      <div class="flex flex-wrap gap-2">
        <div
          v-for="(word, idx) in wordBank"
          :key="idx"
          class="px-3 py-1.5 bg-white dark:bg-gray-800 border-2 border-blue-300 dark:border-blue-600 rounded text-sm font-semibold text-gray-800 dark:text-gray-200 shadow-sm transition-all"
          :class="[
            props.disabled && props.showFeedback && isWordUsedCorrectly(word)
              ? 'opacity-50 line-through decoration-2'
              : !props.disabled ? 'cursor-pointer hover:bg-blue-100 dark:hover:bg-blue-900/40 hover:scale-105' : ''
          ]"
          @click="handleWordClick(word)"
        >
          {{ word }}
        </div>
      </div>
      <div class="mt-2 text-xs text-gray-600 dark:text-gray-400 italic">
        💡 Click vào từ để điền tự động, hoặc gõ trực tiếp vào ô trống
      </div>
    </div>

    <!-- Question Content -->
    <div class="text-base leading-loose text-gray-900 dark:text-gray-100">
      <!-- FILL_BLANK:  Parse từ HTML với nhiều câu -->
      <div v-if="isFillBlankWithMultipleBlanks" class="space-y-4">
        <div
          v-for="(sentence, sIdx) in parsedSentences"
          :key="sIdx"
          class="flex items-start gap-2"
        >
          <!-- Sentence Number -->
          <span
            v-if="sentence.number"
            class="text-gray-500 font-bold shrink-0 mt-1"
          >
            {{ sentence.number }}.
          </span>

          <!-- Sentence Content -->
          <div class="flex-1 flex flex-wrap items-baseline gap-x-2 gap-y-2">
            <template v-for="(part, pIdx) in sentence.parts" :key="pIdx">
              <!-- Blank Input -->
              <span v-if="part.isBlank" class="inline-flex items-center relative">
                <input
                  type="text"
                  v-model="userAnswers[part.blankIndex]"
                  @input="emitAnswer"
                  spellcheck="false"
                  autocomplete="off"
                  :disabled="props.disabled"
                  class="fill-blank-input"
                  :class="getInputClass(part.blankIndex)"
                  :style="{ width: getInputWidth(part.blankIndex) }"
                  :placeholder="'(' + (part.position || part.blankIndex + 1) + ')'"
                />

                <!-- Feedback Icon -->
                <span v-if="props.showFeedback && props.disabled" class="absolute -right-6 top-1/2 -translate-y-1/2">
                  <el-icon v-if="isBlankCorrect(part.blankIndex)" class="text-green-600" :size="18">
                    <CircleCheckFilled />
                  </el-icon>
                  <el-icon v-else class="text-red-600" :size="18">
                    <CircleCloseFilled />
                  </el-icon>
                </span>
              </span>

              <!-- Normal Text -->
              <span v-else class="inline whitespace-pre-wrap" v-html="part.text"></span>
            </template>
          </div>
        </div>
      </div>

      <!-- VERB_FORM / TEXT_ANSWER / FILL_BLANK (1 blank): Parse inline -->
      <div v-else class="flex flex-wrap items-baseline gap-x-1 gap-y-2">
        <template v-for="(part, index) in parsedInlineContent" :key="index">
          <!-- Blank Input -->
          <span v-if="part.isBlank" class="inline-flex items-center relative">
            <input
              type="text"
              v-model="userAnswers[part.blankIndex]"
              @input="emitAnswer"
              spellcheck="false"
              autocomplete="off"
              :disabled="props.disabled"
              class="fill-blank-input"
              :class="getInputClass(part.blankIndex)"
              :style="{ width: getInputWidth(part.blankIndex) }"
              :placeholder="'(' + (part.blankIndex + 1) + ')'"
            />

            <!-- Feedback Icon -->
            <span v-if="props.showFeedback && props.disabled" class="absolute -right-6 top-1/2 -translate-y-1/2">
              <el-icon v-if="isBlankCorrect(part.blankIndex)" class="text-green-600" :size="18">
                <CircleCheckFilled />
              </el-icon>
              <el-icon v-else class="text-red-600" :size="18">
                <CircleCloseFilled />
              </el-icon>
            </span>
          </span>

          <!-- Normal Text -->
          <span v-else class="inline whitespace-pre-wrap" v-html="part.text"></span>
        </template>
      </div>
    </div>

    <!-- Correct Answers Display -->
    <div
      v-if="props.showFeedback && props.disabled && hasWrongAnswers"
      class="mt-6 p-4 bg-amber-50 dark:bg-amber-900/20 border-2 border-amber-200 dark:border-amber-800 rounded-lg"
    >
      <div class="font-bold text-amber-800 dark:text-amber-300 mb-3 flex items-center gap-2">
        <el-icon><InfoFilled /></el-icon>
        Đáp án đúng:
      </div>
      <div class="space-y-2 text-sm">
        <div v-for="(blank, idx) in blanksMetadata" :key="idx" class="flex items-start gap-2">
          <span class="text-gray-600 dark:text-gray-400 font-semibold shrink-0">
            ({{ blank.position || idx + 1 }}):
          </span>
          <span class="font-bold text-green-700 dark:text-green-400">
            {{ blank.correctAnswers ?  blank.correctAnswers.join(' / ') : blank.correctAnswer || '___' }}
          </span>
          <span
            v-if="userAnswers[idx] && !isBlankCorrect(idx)"
            class="text-red-600 dark:text-red-400 line-through"
          >
            (Bạn điền:  {{ userAnswers[idx] }})
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  InfoFilled,
  Collection
} from '@element-plus/icons-vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: [Object, String], default: () => ({}) },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])
const userAnswers = ref({})

// ===== MỚI: Handle word click từ word bank =====
const handleWordClick = (word) => {
  if (props.disabled) return

  // Tìm ô trống đầu tiên chưa điền
  const emptyIndex = blanksMetadata.value.findIndex(
    (_, idx) => !userAnswers.value[idx] || userAnswers.value[idx].trim() === ''
  )

  if (emptyIndex !== -1) {
    userAnswers.value[emptyIndex] = word
    emitAnswer()
  }
}

// Initialize answers
const initAnswers = () => {
  if (typeof props.modelValue === 'object' && props.modelValue !== null) {
    userAnswers.value = { ...props.modelValue }
  } else if (typeof props.modelValue === 'string') {
    userAnswers.value = { 0: props.modelValue }
  } else {
    userAnswers.value = {}
  }
}

// Get blanks metadata
const blanksMetadata = computed(() => {
  return props.question?.metadata?.blanks || []
})

// Kiểm tra là FILL_BLANK với nhiều blanks
const isFillBlankWithMultipleBlanks = computed(() => {
  const type = props.question?.questionType?.toUpperCase()
  return type === 'FILL_BLANK' && blanksMetadata.value.length > 1
})

// Check if has Word Bank
const hasWordBank = computed(() => {
  return isFillBlankWithMultipleBlanks.value
})

// Generate Word Bank
const wordBank = computed(() => {
  if (!hasWordBank.value) return []

  const words = []
  blanksMetadata.value.forEach(blank => {
    if (blank.correctAnswers && blank.correctAnswers.length > 0) {
      words.push(blank.correctAnswers[0])
    } else if (blank.correctAnswer) {
      words.push(blank.correctAnswer)
    }
  })
  // Shuffle words
  return words.sort(() => Math.random() - 0.5)
})

// Parse HTML content for FILL_BLANK (multiple sentences)
const parsedSentences = computed(() => {
  if (!isFillBlankWithMultipleBlanks.value) return []

  const rawHTML = props.question.questionText || ''

  // Split by <p> tags
  const pTagRegex = /<p>(.*?)<\/p>/gi
  const sentences = []
  let match
  let blankCounter = 0

  while ((match = pTagRegex.exec(rawHTML)) !== null) {
    const content = match[1].replace(/&nbsp;/g, ' ')

    // Extract sentence number
    const numberMatch = content.match(/^(\d+)\.\s*/)
    const number = numberMatch ? numberMatch[1] : null
    const textWithoutNumber = numberMatch ? content.substring(numberMatch[0].length) : content

    // Parse blanks
    const blankRegex = /(\.{2,}|…+|___+|\[\d+\]|\(\d+\))/g
    const parts = []
    let lastIndex = 0
    let blankMatch

    while ((blankMatch = blankRegex.exec(textWithoutNumber)) !== null) {
      if (blankMatch.index > lastIndex) {
        const textBefore = textWithoutNumber.substring(lastIndex, blankMatch.index)
        if (textBefore.trim()) {
          parts.push({
            isBlank: false,
            text: textBefore
          })
        }
      }

      const blankMeta = blanksMetadata.value[blankCounter] || {}
      parts.push({
        isBlank: true,
        blankIndex: blankCounter,
        position: blankMeta.position || blankCounter + 1
      })
      blankCounter++

      lastIndex = blankRegex.lastIndex
    }

    if (lastIndex < textWithoutNumber.length) {
      const remainingText = textWithoutNumber.substring(lastIndex)
      if (remainingText.trim()) {
        parts.push({
          isBlank: false,
          text: remainingText
        })
      }
    }

    if (parts.length > 0) {
      sentences.push({ number, parts })
    }
  }

  return sentences
})

// Parse inline content for VERB_FORM / TEXT_ANSWER
const parsedInlineContent = computed(() => {
  if (isFillBlankWithMultipleBlanks.value) return []

  const rawText = props.question.questionText || ''
  const cleanText = rawText.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ')

  const blankRegex = /(\.{2,}|…+|___+|\[\d+\]|\(\d+\))/g
  const parts = []
  let lastIndex = 0
  let blankCounter = 0
  let match

  while ((match = blankRegex.exec(cleanText)) !== null) {
    if (match.index > lastIndex) {
      parts.push({
        isBlank: false,
        text: cleanText.substring(lastIndex, match.index)
      })
    }

    parts.push({
      isBlank: true,
      blankIndex: blankCounter++
    })

    lastIndex = blankRegex.lastIndex
  }

  if (lastIndex < cleanText.length) {
    parts.push({
      isBlank: false,
      text: cleanText.substring(lastIndex)
    })
  }

  return parts
})

// Check if blank answer is correct
const isBlankCorrect = (blankIndex) => {
  const userAnswer = (userAnswers.value[blankIndex] || '').trim().toLowerCase()
  const blank = blanksMetadata.value[blankIndex]

  if (!blank) return false

  // Check correctAnswers array
  if (blank.correctAnswers && Array.isArray(blank.correctAnswers)) {
    return blank.correctAnswers.some(
      answer => answer.trim().toLowerCase() === userAnswer
    )
  }

  // Check single correctAnswer
  if (blank.correctAnswer) {
    return blank.correctAnswer.trim().toLowerCase() === userAnswer
  }

  return false
}

// Check if word is used correctly
const isWordUsedCorrectly = (word) => {
  return Object.values(userAnswers.value).some(
    answer => answer && answer.trim().toLowerCase() === word.trim().toLowerCase()
  )
}

// Check if has wrong answers
const hasWrongAnswers = computed(() => {
  return blanksMetadata.value.some((_, idx) =>
    userAnswers.value[idx] && !isBlankCorrect(idx)
  )
})

// Get input CSS class
const getInputClass = (blankIndex) => {
  const baseClass = 'border-0 border-b-2 focus:outline-none bg-transparent text-center px-2 py-1 font-sans font-bold transition-all'

  if (props.disabled) {
    if (props.showFeedback) {
      return isBlankCorrect(blankIndex)
        ? baseClass + ' border-green-500 text-green-700 dark:text-green-400 bg-green-50 dark:bg-green-900/20 cursor-not-allowed opacity-70'
        : baseClass + ' border-red-500 text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 cursor-not-allowed opacity-70'
    }
    return baseClass + ' cursor-not-allowed opacity-70 border-gray-400'
  }

  return baseClass + ' border-gray-400 dark:border-gray-500 text-blue-800 dark:text-blue-300 focus:border-blue-600 dark:focus:border-blue-400 cursor-text'
}

// Calculate input width
const getInputWidth = (blankIndex) => {
  const blank = blanksMetadata.value[blankIndex]
  const userAnswer = userAnswers.value[blankIndex] || ''

  let maxLength = 5
  if (blank?.correctAnswers) {
    maxLength = Math.max(...blank.correctAnswers.map(a => a.length))
  } else if (blank?.correctAnswer) {
    maxLength = blank.correctAnswer.length
  }
  maxLength = Math.max(maxLength, userAnswer.length)

  const minWidth = 80
  const maxWidth = 200
  const calculatedWidth = maxLength * 10 + 30

  return Math.min(Math.max(minWidth, calculatedWidth), maxWidth) + 'px'
}

const emitAnswer = () => {
  emit('update:modelValue', { ...userAnswers.value })
}

watch(() => props.modelValue, initAnswers, { immediate: true })
</script>

<style scoped>
.fill-blank-input {
  min-height: 34px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.fill-blank-input::placeholder {
  color: #9ca3af;
  font-size: 0.75rem;
  font-weight: 600;
}

.fill-blank-input:focus:not(:disabled) {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
  transform: translateY(-1px);
}

.fill-blank-input:disabled {
  background-color: transparent;
}

:global(.dark) .fill-blank-input::placeholder {
  color: #6b7280;
}
</style>
