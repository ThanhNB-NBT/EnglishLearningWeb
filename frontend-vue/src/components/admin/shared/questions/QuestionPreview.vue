<!-- src/components/admin/shared/questions/QuestionPreview.vue - FIXED -->
<template>
  <div class="w-full">
    <!-- Header -->
    <div class="flex items-center justify-between border-b pb-4 mb-4">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-blue-50 dark:bg-blue-900/20 rounded-lg flex items-center justify-center font-bold text-blue-600">
          Q{{ question.orderIndex }}
        </div>
        <div>
          <div class="text-xs text-gray-500 uppercase font-bold">Loại câu hỏi</div>
          <div class="font-bold text-gray-800 dark:text-white">
            {{ getQuestionTypeName(question.questionType) }}
          </div>
        </div>
      </div>
      <div class="text-right">
        <div class="text-xs text-gray-500 uppercase font-bold">Điểm số</div>
        <div class="font-bold text-orange-500 text-lg">+{{ question.points }}</div>
      </div>
    </div>

    <!-- Question Text -->
    <div v-if="shouldShowQuestionText" class="mb-6">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Nội dung câu hỏi</div>
      <div class="bg-gray-50 dark:bg-[#1a1a1a] p-5 rounded-xl border min-h-[60px]">
        <div class="ql-editor !p-0 text-base" v-html="question.questionText"></div>
      </div>
    </div>

    <!-- Answer Configuration -->
    <div class="mb-6">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Đáp án & Cấu hình</div>
      <div class="bg-white dark:bg-[#1d1d1d] border rounded-xl overflow-hidden">

        <!-- MULTIPLE_CHOICE -->
        <div v-if="question.questionType === 'MULTIPLE_CHOICE'" class="divide-y">
          <div
            v-for="opt in safeMeta.options || []"
            :key="opt.order || opt.text"
            class="flex items-center p-3"
            :class="opt.isCorrect === true ? 'bg-green-50 dark:bg-green-900/20' : ''"
          >
            <span
              class="w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm mr-3 border"
              :class="opt.isCorrect === true ? 'bg-green-500 text-white border-green-500' : 'bg-gray-100 text-gray-500'"
            >
              {{ String.fromCharCode(64 + (opt.order || 1)) }}
            </span>
            <span class="flex-1">{{ opt.text }}</span>
            <el-icon v-if="opt.isCorrect === true" class="text-green-500 text-xl"><Check /></el-icon>
          </div>
        </div>

        <!-- TRUE_FALSE -->
        <div v-else-if="question.questionType === 'TRUE_FALSE'" class="p-4 flex gap-6">
          <div :class="getTrueFalseClass('True')">
            <el-icon class="mr-1"><Select /></el-icon> True
          </div>
          <div :class="getTrueFalseClass('False')">
            <el-icon class="mr-1"><CloseBold /></el-icon> False
          </div>
        </div>

        <!-- FILL_BLANK / VERB_FORM -->
        <div v-else-if="['FILL_BLANK', 'VERB_FORM', 'TEXT_ANSWER'].includes(question.questionType)" class="p-4 space-y-4">
          <!-- Word Bank -->
          <div v-if="hasWordBank" class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border-2 border-blue-200">
            <div class="text-xs font-bold text-blue-700 mb-3 flex items-center gap-2">
              <el-icon><Collection /></el-icon> Word Bank
            </div>
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="word in wordBank" :key="word" size="large" type="primary">{{ word }}</el-tag>
            </div>
          </div>

          <!-- Blank Answers -->
          <div class="p-3 bg-gray-50 rounded border">
            <div class="text-xs font-bold text-gray-600 mb-2">Danh sách đáp án:</div>
            <div class="space-y-2">
              <div v-for="(b, idx) in safeMeta.blanks || []" :key="idx" class="flex items-center gap-2">
                <span class="text-xs font-bold text-gray-500 min-w-[60px]">Vị trí {{ b.position || idx + 1 }}:</span>
                <div class="flex flex-wrap gap-1">
                  <el-tag v-for="ans in getBlankAnswers(b)" :key="ans" type="success" size="small" effect="dark">
                    {{ ans }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- MATCHING -->
        <div v-else-if="question.questionType === 'MATCHING'" class="p-4">
          <div class="space-y-2">
            <div v-for="(pair, idx) in safeMeta.pairs || []" :key="idx" class="flex items-center gap-3 p-2 bg-gray-50 rounded">
              <span class="font-bold text-gray-700">{{ pair.left }}</span>
              <el-icon class="text-blue-500"><Right /></el-icon>
              <span class="text-gray-600">{{ pair.right }}</span>
            </div>
          </div>
        </div>

        <!-- SENTENCE_TRANSFORMATION -->
        <div v-else-if="question.questionType === 'SENTENCE_TRANSFORMATION'" class="p-4">
          <div class="space-y-2">
            <div v-for="ans in safeMeta.correctAnswers || []" :key="ans" class="p-2 bg-green-50 rounded border border-green-200">
              {{ ans }}
            </div>
          </div>
        </div>

        <!-- ERROR_CORRECTION -->
        <div v-else-if="question.questionType === 'ERROR_CORRECTION'" class="p-4">
          <div class="p-3 bg-green-50 rounded border border-green-200">
            <div class="text-xs font-bold text-green-700 mb-1">Câu đúng:</div>
            <div class="text-gray-800">{{ safeMeta.correction }}</div>
          </div>
        </div>

        <!-- SENTENCE_BUILDING -->
        <div v-else-if="question.questionType === 'SENTENCE_BUILDING'" class="p-4">
          <div class="p-3 bg-blue-50 rounded border border-blue-200">
            <div class="text-xs font-bold text-blue-700 mb-1">Câu hoàn chỉnh:</div>
            <div class="text-gray-800">{{ safeMeta.correctSentence }}</div>
          </div>
        </div>

        <!-- Default: No detail -->
        <div v-else class="p-4 text-gray-500 italic text-sm text-center">
          Không có dữ liệu chi tiết cho loại câu hỏi này.
        </div>
      </div>
    </div>

    <!-- Explanation -->
    <div v-if="question.explanation || safeMeta.explanation">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Giải thích</div>
      <div class="bg-blue-50 dark:bg-blue-900/10 p-4 rounded-xl border border-blue-100 text-blue-800 text-sm">
        <el-icon class="mr-2"><InfoFilled /></el-icon>
        {{ question.explanation || safeMeta.explanation }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Check,
  CloseBold,
  Select,
  InfoFilled,
  Collection,
  Right,
} from '@element-plus/icons-vue'

const props = defineProps({
  question: { type: Object, required: true },
})

// ✅ FIX: Correctly extract metadata from 'data' or 'metadata'
const safeMeta = computed(() => {
  if (!props.question) return {}

  // Priority 1: 'data' field (new structure)
  if (props.question.data) {
    if (typeof props.question.data === 'string') {
      try {
        return JSON.parse(props.question.data)
      } catch (e) {
        console.error('Failed to parse question.data:', e)
        return {}
      }
    }
    if (typeof props.question.data === 'object') {
      return props.question.data
    }
  }

  // Priority 2: 'metadata' field (old structure)
  if (props.question.metadata) {
    if (typeof props.question.metadata === 'string') {
      try {
        return JSON.parse(props.question.metadata)
      } catch (e) {
        console.error('Failed to parse question.metadata:', e)
        return {}
      }
    }
    if (typeof props.question.metadata === 'object') {
      return props.question.metadata
    }
  }

  return {}
})

const shouldShowQuestionText = computed(() => {
  const type = props.question?.questionType?.toUpperCase()
  if (type === 'FILL_BLANK') {
    const blanks = safeMeta.value.blanks || []
    return blanks.length <= 1
  }
  return props.question.questionText && props.question.questionText.trim()
})

const hasWordBank = computed(() => {
  const type = props.question?.questionType?.toUpperCase()
  if (!['FILL_BLANK', 'VERB_FORM'].includes(type)) return false
  const blanks = safeMeta.value.blanks || []
  return blanks.length > 1
})

const wordBank = computed(() => {
  if (!hasWordBank.value) return []
  const words = []
  const blanks = safeMeta.value.blanks || []
  blanks.forEach((blank) => {
    if (blank.correctAnswers && blank.correctAnswers.length > 0) {
      words.push(blank.correctAnswers[0])
    } else if (blank.correctAnswer) {
      words.push(blank.correctAnswer)
    }
  })
  return words
})

const getQuestionTypeName = (type) => {
  const map = {
    MULTIPLE_CHOICE: 'Trắc nghiệm',
    TRUE_FALSE: 'Đúng/Sai',
    FILL_BLANK: 'Điền từ',
    VERB_FORM: 'Chia động từ',
    TEXT_ANSWER: 'Trả lời ngắn',
    SENTENCE_TRANSFORMATION: 'Viết lại câu',
    MATCHING: 'Nối từ',
    SENTENCE_BUILDING: 'Sắp xếp câu',
    ERROR_CORRECTION: 'Tìm lỗi sai',
    PRONUNCIATION: 'Phát âm',
    OPEN_ENDED: 'Câu hỏi mở',
    COMPLETE_CONVERSATION: 'Hoàn thành hội thoại',
  }
  return map[type] || type
}

const getTrueFalseClass = (value) => {
  const options = safeMeta.value.options || []
  const correctOpt = options.find((opt) => opt.isCorrect === true)

  if (correctOpt && correctOpt.text === value) {
    return 'text-green-600 font-bold border-b-2 border-green-600 pb-1 flex items-center'
  }
  return 'text-gray-400 flex items-center'
}

const getBlankAnswers = (blank) => {
  if (Array.isArray(blank.correctAnswers)) return blank.correctAnswers
  if (typeof blank.correctAnswers === 'string') return [blank.correctAnswers]
  if (blank.correctAnswer) return [blank.correctAnswer]
  return []
}
</script>
