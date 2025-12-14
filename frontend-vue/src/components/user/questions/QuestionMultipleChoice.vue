<template>
  <div class="question-multiple-choice w-full">
    <div class="flex flex-col gap-3 mt-2 w-full">
      <div
        v-for="(opt, idx) in options"
        :key="idx"
        class="option-item group flex items-start p-3 rounded-lg border cursor-pointer transition-all hover:shadow-sm"
        :class="
          modelValue === opt.text
            ? 'bg-blue-50 border-blue-600 dark:bg-blue-900/30 dark:border-blue-400'
            : 'bg-white border-gray-200 hover:border-blue-300 dark:bg-[#1e1e1e] dark:border-gray-700 dark:hover:border-gray-500'
        "
        @click="! disabled && $emit('update:modelValue', opt.text)"
      >
        <!-- Option Label (A, B, C, D) -->
        <div
          class="flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold shrink-0 mr-3 border transition-colors"
          :class="
            modelValue === opt.text
              ?  'bg-blue-600 text-white border-blue-600'
              : 'bg-gray-50 text-gray-500 border-gray-200 group-hover:bg-white group-hover:text-blue-600 group-hover:border-blue-300 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-400'
          "
        >
          {{ String. fromCharCode(65 + idx) }}
        </div>

        <!-- Option Text -->
        <div
          class="flex-1 text-base leading-snug pt-1 min-w-0 break-words"
          :class="
            modelValue === opt.text
              ? 'font-semibold text-blue-900 dark:text-blue-100'
              : 'text-gray-800 dark:text-gray-200'
          "
        >
          {{ opt.text }}
        </div>

        <!-- Checkmark Icon -->
        <div v-if="modelValue === opt.text" class="ml-2 text-blue-600 dark:text-blue-400 self-center shrink-0">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-6 w-6"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clip-rule="evenodd"
            />
          </svg>
        </div>

        <!-- Feedback (if submitted) -->
        <div
          v-if="showFeedback && disabled"
          class="absolute top-1 right-1"
        >
          <el-icon
            v-if="opt.isCorrect"
            class="text-green-500"
            :size="20"
          >
            <CircleCheckFilled />
          </el-icon>
          <el-icon
            v-else-if="modelValue === opt.text && ! opt.isCorrect"
            class="text-red-500"
            :size="20"
          >
            <CircleCloseFilled />
          </el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { useQuestionData } from '@/composables/questions/useQuestionData'

const props = defineProps({
  question: Object,
  modelValue: [String, Number],
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const { options } = useQuestionData(props)
</script>

<style scoped>
.question-multiple-choice {
  display: block;
  position: relative;
  width: 100%;
}

.option-item {
  position: relative; /* Để feedback icon absolute work */
  display: flex;
  width: 100%;
}
</style>
