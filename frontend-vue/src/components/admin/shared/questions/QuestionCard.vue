<!-- src/components/admin/shared/questions/QuestionCard.vue - NEW -->
<template>
  <div class="question-card">
    <!-- Question Type & Points -->
    <div class="flex justify-between items-start mb-2">
      <span
        class="font-bold text-[10px] uppercase tracking-wider text-gray-500 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded"
      >
        {{ question.questionType }}
      </span>
      <span class="bg-indigo-100 text-indigo-800 text-[10px] px-2 py-1 rounded font-bold">
        {{ question.points }} điểm
      </span>
    </div>

    <!-- Question Text -->
    <p
      class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-2 line-clamp-3"
      :title="getPlainText(question.questionText)"
      v-html="question.questionText"
    ></p>

    <!-- Answer Preview -->
    <div
      class="bg-gray-50 dark:bg-gray-800 p-2 rounded text-xs text-gray-600 dark:text-gray-400 border border-gray-100 dark:border-gray-700"
    >
      <!-- MULTIPLE_CHOICE / TRUE_FALSE -->
      <div v-if="question.data?.options" class="grid grid-cols-1 gap-1">
        <div
          v-for="(op, i) in question.data.options"
          :key="i"
          :class="{ 'text-green-600 font-bold bg-green-50 px-1 rounded': op.isCorrect }"
        >
          • {{ op.text }} <span v-if="op.isCorrect">✓</span>
        </div>
      </div>

      <!-- FILL_BLANK / TEXT_ANSWER -->
      <div v-else-if="question.data?.blanks">
        <span class="font-bold">Đáp án:</span>
        <span
          v-for="(blank, i) in question.data.blanks"
          :key="i"
          class="bg-green-100 text-green-800 px-1 rounded ml-1"
        >
          {{ getBlankAnswers(blank) }}
        </span>
      </div>

      <!-- MATCHING -->
      <div v-else-if="question.data?.pairs">
        <div v-for="(p, i) in question.data.pairs.slice(0, 2)" :key="i" class="flex gap-2">
          <span>{{ p.left }}</span> → <span>{{ p.right }}</span>
        </div>
        <div v-if="question.data.pairs.length > 2" class="text-gray-400 italic text-xs mt-1">
          +{{ question.data.pairs.length - 2 }} more pairs...
        </div>
      </div>

      <!-- ERROR_CORRECTION -->
      <div v-else-if="question.data?.correction">
        Sai: <span class="line-through text-red-400">{{ question.data.errorText }}</span> ➜
        <span class="text-green-600 font-bold">{{ question.data.correction }}</span>
      </div>

      <!-- SENTENCE_TRANSFORMATION -->
      <div v-else-if="question.data?.correctAnswers">
        <span class="font-bold">Đáp án:</span>
        <div class="mt-1">
          <div
            v-for="(ans, i) in question.data.correctAnswers.slice(0, 2)"
            :key="i"
            class="bg-green-100 text-green-800 px-2 py-0.5 rounded text-xs"
          >
            {{ ans }}
          </div>
        </div>
      </div>

      <!-- SENTENCE_BUILDING -->
      <div v-else-if="question.data?.correctSentence">
        <span class="font-bold">Câu đúng:</span>
        <div class="text-green-700 mt-1">{{ question.data.correctSentence }}</div>
      </div>

      <!-- Fallback -->
      <div v-else class="text-gray-400 italic">Không có thông tin đáp án</div>
    </div>

    <!-- Task Info (if provided) -->
    <div
      v-if="showTaskInfo && question.taskGroupId"
      class="mt-2 text-xs text-blue-600 dark:text-blue-400"
    >
      <el-icon class="mr-1"><Collection /></el-icon>
      Task: {{ question.taskGroupName || 'Unknown' }}
    </div>
  </div>
</template>

<script setup>
import { Collection } from '@element-plus/icons-vue'

defineProps({
  question: {
    type: Object,
    required: true,
  },
  showTaskInfo: {
    type: Boolean,
    default: true,
  },
})

const getPlainText = (html) => {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || div.innerText || ''
}

const getBlankAnswers = (blank) => {
  if (Array.isArray(blank.correctAnswers)) return blank.correctAnswers.join(' / ')
  if (typeof blank.correctAnswers === 'string') return blank.correctAnswers
  if (blank.correctAnswer) return blank.correctAnswer
  return 'N/A'
}
</script>

<style scoped>
.question-card :deep(p) {
  margin: 0;
}

.question-card :deep(ul),
.question-card :deep(ol) {
  padding-left: 1.5rem;
}
</style>
