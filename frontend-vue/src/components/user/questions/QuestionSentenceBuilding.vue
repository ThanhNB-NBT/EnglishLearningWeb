<template>
  <div class="w-full">
    <div class="text-base text-gray-600 dark:text-gray-400 mb-3 font-serif italic">
      <span v-for="(word, idx) in shuffledWords" :key="idx">
        {{ word }}<span v-if="idx < shuffledWords.length - 1" class="mx-1">/</span>
      </span>
    </div>

    <input
      type="text"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      class="w-full bg-transparent border-b border-gray-400 focus:border-black dark:focus:border-white outline-none py-1 px-0 text-lg font-medium text-blue-800 dark:text-blue-300 placeholder-gray-300"
      placeholder="Viết lại câu hoàn chỉnh..."
    />
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useQuestionData } from '@/composables/questions/useQuestionData'
const props = defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
const { buildingWords } = useQuestionData(props)
const shuffledWords = ref([])
onMounted(() => { shuffledWords.value = [...buildingWords.value] }) // Giữ nguyên thứ tự để gợi ý như đề bài
</script>
