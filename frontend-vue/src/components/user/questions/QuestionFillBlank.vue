<template>
  <div class="w-full text-lg leading-loose text-gray-900 dark:text-gray-100 font-serif">
    <template v-for="(part, index) in parsedContent" :key="index">
      <span v-if="part.isBlank" class="inline-block mx-1 relative">
        <input
          type="text"
          v-model="userAnswers[part.blankIndex]"
          @input="emitAnswer"
          class="border-b border-gray-400 focus:border-black dark:focus:border-white outline-none bg-transparent text-center px-1 py-0 text-blue-700 dark:text-blue-300 font-bold min-w-[60px] h-6 text-lg font-sans"
          :style="{ width: getInputWidth(userAnswers[part.blankIndex]) }"
        />
        <span
          class="absolute -bottom-4 left-0 w-full text-center text-[9px] text-gray-400 font-sans"
          >({{ part.blankIndex + 1 }})</span
        >
      </span>

      <span v-else v-html="part.text"></span>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])
const userAnswers = ref({})

const initAnswers = () => {
  if (typeof props.modelValue === 'object' && props.modelValue !== null)
    userAnswers.value = { ...props.modelValue }
  else if (typeof props.modelValue === 'string') userAnswers.value = { 0: props.modelValue }
}
initAnswers()

const parsedContent = computed(() => {
  const rawText = props.question.questionText || ''
  const regex = /(___|\[\d+\]|\(\d+\)|\.{3,})/g
  const parts = rawText.split(regex)
  const matches = rawText.match(regex) || []
  let blankCounter = 0
  const result = []
  parts.forEach((text, idx) => {
    if (text) result.push({ isBlank: false, text })
    if (idx < matches.length) result.push({ isBlank: true, blankIndex: blankCounter++ })
  })
  return result
})
const getInputWidth = (val) => Math.max(60, (val?.length || 0) * 10 + 10) + 'px'
const emitAnswer = () => emit('update:modelValue', { ...userAnswers.value })
watch(() => props.modelValue, initAnswers)
</script>
<style scoped>
input {
  border-top: none;
  border-left: none;
  border-right: none;
  border-radius: 0;
}
</style>
