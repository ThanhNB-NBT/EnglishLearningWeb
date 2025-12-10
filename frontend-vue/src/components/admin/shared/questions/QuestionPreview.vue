<template>
  <div class="w-full">
    <!-- Header Info -->
    <div class="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-4 mb-4">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-blue-50 dark:bg-blue-900/20 rounded-lg flex items-center justify-center font-bold text-blue-600 dark:text-blue-400">
          Q{{ question.orderIndex }}
        </div>
        <div>
          <div class="text-xs text-gray-500 uppercase font-bold tracking-wider">Loại câu hỏi</div>
          <div class="font-bold text-gray-800 dark:text-white">{{ getQuestionTypeName(question.questionType) }}</div>
        </div>
      </div>
      <div class="text-right">
        <div class="text-xs text-gray-500 uppercase font-bold tracking-wider">Điểm số</div>
        <div class="font-bold text-orange-500 text-lg">+{{ question.points }}</div>
      </div>
    </div>

    <!-- Question Text -->
    <div class="mb-6" v-if="question.questionText && question.questionText.trim()">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Nội dung câu hỏi</div>
      <div class="bg-gray-50 dark:bg-[#1a1a1a] p-5 rounded-xl border border-gray-200 dark:border-gray-700 min-h-[60px]">
        <div class="ql-editor !p-0 text-base text-gray-800 dark:text-gray-200" v-html="question.questionText"></div>
      </div>
    </div>

    <!-- Answer & Config Section -->
    <div class="mb-6">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Đáp án & Cấu hình</div>
      <div class="bg-white dark:bg-[#1d1d1d] border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">

        <!-- 1. MULTIPLE_CHOICE -->
        <div v-if="question.questionType === 'MULTIPLE_CHOICE'" class="divide-y divide-gray-100 dark:divide-gray-800">
          <div
            v-for="opt in safeMeta.options || []"
            :key="opt.order || opt.text"
            class="flex items-center p-3"
            :class="opt.isCorrect === true ? 'bg-green-50 dark:bg-green-900/20' : ''"
          >
            <span
              class="w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm mr-3 border"
              :class="opt.isCorrect === true
                ? 'bg-green-500 text-white border-green-500'
                : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-300'"
            >
              {{ String.fromCharCode(64 + (opt.order || 1)) }}
            </span>
            <span class="flex-1 text-gray-700 dark:text-gray-300">{{ opt.text }}</span>
            <el-icon v-if="opt.isCorrect === true" class="text-green-500 text-xl"><Check /></el-icon>
          </div>
        </div>

        <!-- 2. TRUE_FALSE -->
        <div v-else-if="question.questionType === 'TRUE_FALSE'" class="p-4 flex gap-6">
          <div :class="getTrueFalseClass('True')">
            <el-icon class="mr-1"><Select /></el-icon> True
          </div>
          <div :class="getTrueFalseClass('False')">
            <el-icon class="mr-1"><CloseBold /></el-icon> False
          </div>
        </div>

        <!-- 3. FILL_BLANK -->
        <div v-else-if="question.questionType === 'FILL_BLANK'" class="p-4">
          <!-- Word Bank (nếu có) -->
          <div v-if="safeMeta.wordBank && safeMeta.wordBank.length > 0" class="mb-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800">
            <div class="text-xs font-bold text-blue-700 dark:text-blue-300 mb-2">Ngân hàng từ:</div>
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="word in safeMeta.wordBank" :key="word" size="small">{{ word }}</el-tag>
            </div>
          </div>

          <!-- Blanks -->
          <div v-if="safeMeta.blanks && safeMeta.blanks.length > 0">
            <div v-for="(b, idx) in safeMeta.blanks" :key="idx" class="mb-3 last:mb-0">
              <span class="font-bold text-gray-600 dark:text-gray-300 mr-2">Vị trí #{{ b.position }}:</span>
              <div class="inline-flex flex-wrap gap-2 mt-1">
                <el-tag v-for="ans in getBlankAnswers(b)" :key="ans" type="success" size="large" effect="dark">
                  {{ ans }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. VERB_FORM -->
        <div v-else-if="question.questionType === 'VERB_FORM'" class="p-4">
          <div v-if="safeMeta.blanks && safeMeta.blanks.length > 0">
            <div v-for="(b, idx) in safeMeta.blanks" :key="idx" class="mb-3 p-3 bg-gray-50 dark:bg-[#252525] rounded-lg border border-gray-200 dark:border-gray-700">
              <div class="flex items-center gap-2 mb-2">
                <el-tag size="small" type="warning">Vị trí #{{ b.position || idx + 1 }}</el-tag>
                <span class="font-bold text-purple-600 dark:text-purple-400">
                  <el-icon><Edit /></el-icon> Động từ: {{ b.verb || b.hint || '?' }}
                </span>
              </div>
              <div class="flex flex-wrap gap-2">
                <el-tag v-for="ans in getBlankAnswers(b)" :key="ans" type="success" effect="dark">
                  {{ ans }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 5. TEXT_ANSWER -->
        <div v-else-if="question.questionType === 'TEXT_ANSWER'" class="p-4">
          <p class="text-sm text-gray-500 mb-2">Đáp án chấp nhận:</p>
          <div class="flex flex-wrap gap-2">
            <el-tag v-for="ans in getCorrectAnswers(safeMeta)" :key="ans" type="success" size="large" effect="dark">
              {{ ans }}
            </el-tag>
          </div>
        </div>

        <!-- 6. SENTENCE_TRANSFORMATION -->
        <div v-else-if="question.questionType === 'SENTENCE_TRANSFORMATION'" class="p-4 space-y-4">
          <div class="p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg border border-orange-100 dark:border-orange-800">
            <div class="text-xs text-orange-500 font-bold uppercase mb-1">Câu gốc</div>
            <div class="text-base font-medium text-gray-800 dark:text-gray-200">{{ safeMeta.originalSentence }}</div>
          </div>

          <div v-if="safeMeta.beginningPhrase" class="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800">
            <div class="text-xs text-blue-500 font-bold uppercase mb-1">Gợi ý đầu câu</div>
            <div class="text-base font-medium text-blue-700 dark:text-blue-300">{{ safeMeta.beginningPhrase }}</div>
          </div>

          <div class="p-3 bg-green-50 dark:bg-green-900/20 rounded-lg border border-green-100 dark:border-green-800">
            <div class="text-xs text-green-500 font-bold uppercase mb-2">Đáp án đúng</div>
            <div class="space-y-2">
              <div v-for="(ans, idx) in safeMeta.correctAnswers || []" :key="idx" class="flex items-start gap-2">
                <el-icon class="text-green-600 mt-0.5"><Check /></el-icon>
                <span class="text-sm text-gray-800 dark:text-gray-200">{{ ans }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 7. MATCHING -->
        <div v-else-if="question.questionType === 'MATCHING'" class="p-4">
          <div class="grid grid-cols-1 gap-2">
            <div
              v-for="(pair, index) in safeMeta.pairs || []"
              :key="index"
              class="flex items-center justify-between bg-gray-50 dark:bg-[#252525] p-3 rounded border border-gray-200 dark:border-gray-700"
            >
              <span class="font-bold text-blue-600 dark:text-blue-400">{{ pair.left }}</span>
              <el-icon class="text-gray-400 mx-3"><Right /></el-icon>
              <span class="font-bold text-green-600 dark:text-green-400">{{ pair.right }}</span>
            </div>
          </div>
        </div>

        <!-- 8. SENTENCE_BUILDING -->
        <div v-else-if="question.questionType === 'SENTENCE_BUILDING'" class="p-4 space-y-3">
          <div>
            <p class="text-sm text-gray-500 mb-2">Các từ thành phần:</p>
            <div class="flex flex-wrap gap-2 p-3 bg-gray-50 dark:bg-[#252525] rounded-lg border border-gray-200 dark:border-gray-700">
              <el-tag v-for="word in safeMeta.words || []" :key="word" size="large">{{ word }}</el-tag>
            </div>
          </div>

          <div>
            <p class="text-sm text-gray-500 mb-2">Câu đúng:</p>
            <div class="p-3 bg-green-50 dark:bg-green-900/20 rounded border border-green-200 dark:border-green-800 font-medium text-gray-800 dark:text-gray-200">
              <el-icon class="text-green-600 mr-2"><Check /></el-icon>{{ safeMeta.correctSentence }}
            </div>
          </div>
        </div>

        <!-- 9. ERROR_CORRECTION -->
        <div v-else-if="question.questionType === 'ERROR_CORRECTION'" class="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="p-3 bg-red-50 dark:bg-red-900/20 rounded border border-red-100 dark:border-red-800">
            <div class="text-xs text-red-500 font-bold uppercase mb-1 flex items-center gap-1">
              <el-icon><CloseBold /></el-icon> Sai (Error)
            </div>
            <div class="text-lg font-medium text-red-700 dark:text-red-300">{{ safeMeta.errorText }}</div>
          </div>
          <div class="p-3 bg-green-50 dark:bg-green-900/20 rounded border border-green-100 dark:border-green-800">
            <div class="text-xs text-green-500 font-bold uppercase mb-1 flex items-center gap-1">
              <el-icon><Select /></el-icon> Sửa (Correction)
            </div>
            <div class="text-lg font-medium text-green-700 dark:text-green-300">{{ safeMeta.correction }}</div>
          </div>
        </div>

        <!-- 10. PRONUNCIATION -->
        <div v-else-if="question.questionType === 'PRONUNCIATION'" class="p-4 space-y-4">
          <div>
            <div class="text-sm font-bold text-gray-600 dark:text-gray-300 mb-2">Các nhóm âm:</div>
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="cat in safeMeta.categories || []" :key="cat" type="info" size="large">{{ cat }}</el-tag>
            </div>
          </div>

          <div>
            <div class="text-sm font-bold text-gray-600 dark:text-gray-300 mb-2">Phân loại đáp án:</div>
            <div class="space-y-2">
              <div
                v-for="cls in safeMeta.correctClassifications || safeMeta.classifications || []"
                :key="cls.word"
                class="flex items-center gap-3 p-2 bg-gray-50 dark:bg-[#252525] rounded border border-gray-200 dark:border-gray-700"
              >
                <span class="font-bold text-gray-800 dark:text-white min-w-[100px]">{{ cls.word }}</span>
                <el-icon class="text-gray-400"><Right /></el-icon>
                <el-tag type="success" size="small">{{ cls.category }}</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 11. OPEN_ENDED -->
        <div v-else-if="question.questionType === 'OPEN_ENDED'" class="p-4 space-y-3">
          <el-alert type="info" :closable="false" show-icon>
            <template #title>Câu hỏi mở - Cần chấm điểm thủ công</template>
          </el-alert>

          <div v-if="safeMeta.suggestedAnswer" class="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800">
            <div class="text-xs text-blue-500 font-bold uppercase mb-2">Câu trả lời gợi ý:</div>
            <div class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ safeMeta.suggestedAnswer }}</div>
          </div>

          <div v-if="safeMeta.timeLimitSeconds" class="flex items-center gap-2 text-sm text-gray-500">
            <el-icon><Clock /></el-icon> Thời gian: {{ safeMeta.timeLimitSeconds }}s
          </div>
        </div>

        <!-- FALLBACK: Raw JSON for unsupported types -->
        <div v-else class="p-4 bg-gray-900 text-green-400 font-mono text-xs overflow-auto max-h-60 rounded scrollbar-thin">
          <div class="text-yellow-400 mb-2">⚠️ Chưa có template hiển thị cho loại: {{ question.questionType }}</div>
          <pre>{{ JSON.stringify(safeMeta, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- Explanation -->
    <div v-if="question.explanation || safeMeta.explanation">
      <div class="text-sm font-bold text-gray-500 uppercase mb-2">Giải thích</div>
      <div class="bg-blue-50 dark:bg-blue-900/10 p-4 rounded-xl border border-blue-100 dark:border-blue-900/30 text-blue-800 dark:text-blue-300 text-sm">
        <el-icon class="mr-2"><InfoFilled /></el-icon>
        {{ question.explanation || safeMeta.explanation }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { Check, Right, CloseBold, Select, Edit, Clock, InfoFilled } from '@element-plus/icons-vue'
import 'quill/dist/quill.snow.css'
import { computed } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
})

const safeMeta = computed(() => {
  if (!props.question || !props.question.metadata) return {}

  const meta = props.question.metadata

  if (typeof meta === 'string') {
    try {
      return JSON.parse(meta)
    } catch (e) {
      console.error('Failed to parse metadata:', e)
      return {}
    }
  }

  return meta
})

// Helper: Get question type display name
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
  }
  return map[type] || type
}

// Helper: Get TRUE_FALSE styling
const getTrueFalseClass = (value) => {
  const options = safeMeta.value.options || []
  const correctOpt = options.find(opt => opt.isCorrect === true)

  if (correctOpt && correctOpt.text === value) {
    return 'text-green-600 font-bold border-b-2 border-green-600 pb-1 flex items-center'
  }
  return 'text-gray-400 flex items-center'
}

// Helper: Get blank answers (handle both array and string)
const getBlankAnswers = (blank) => {
  if (Array.isArray(blank.correctAnswers)) {
    return blank.correctAnswers
  }
  if (typeof blank.correctAnswers === 'string') {
    return [blank.correctAnswers]
  }
  return []
}

// Helper: Get correct answers for FILL_BLANK, TEXT_ANSWER, etc.
const getCorrectAnswers = (meta) => {
  // Priority 1: Blanks array (new format)
  if (meta.blanks && meta.blanks.length > 0) {
    return meta.blanks.flatMap(b => getBlankAnswers(b))
  }

  // Priority 2: correctAnswers array (Sentence Transformation)
  if (Array.isArray(meta.correctAnswers)) {
    return meta.correctAnswers
  }

  // Priority 3: correctAnswer string (legacy)
  if (meta.correctAnswer) {
    return meta.correctAnswer.split('|')
  }

  return []
}
</script>
