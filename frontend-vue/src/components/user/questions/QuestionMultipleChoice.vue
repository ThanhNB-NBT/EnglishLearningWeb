<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 mt-2">
    <div
      v-for="(opt, idx) in options" :key="idx"
      class="flex items-start gap-2 cursor-pointer group"
      @click="$emit('update:modelValue', opt.text)"
    >
      <div
        class="w-6 h-6 rounded-full border flex items-center justify-center text-sm font-bold shrink-0 transition-colors"
        :class="modelValue === opt.text
          ? 'bg-black border-black text-white dark:bg-white dark:border-white dark:text-black'
          : 'border-gray-400 text-gray-600 dark:border-gray-600 dark:text-gray-400 group-hover:border-black dark:group-hover:border-white'"
      >
        {{ String.fromCharCode(65 + idx) }}
      </div>

      <div
        class="text-base leading-snug pt-0.5"
        :class="modelValue === opt.text ? 'font-bold text-black dark:text-white' : 'text-gray-800 dark:text-gray-300'"
      >
        {{ opt.text }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { useQuestionData } from '@/composables/questions/useQuestionData'
const props = defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
const { options } = useQuestionData(props)
</script>
