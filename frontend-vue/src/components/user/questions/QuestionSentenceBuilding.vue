<template>
  <div class="w-full">
    <div class="min-h-[60px] p-4 rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-[#262626] flex flex-wrap gap-2 mb-6 items-center">
      <div
        v-for="(word, idx) in userOrder"
        :key="idx"
        class="px-3 py-1.5 bg-white dark:bg-[#333] border border-gray-200 dark:border-gray-600 rounded-lg shadow-sm text-gray-800 dark:text-gray-100 font-medium cursor-pointer hover:text-red-500 hover:border-red-300 transition-all"
        @click="removeWord(idx)"
      >
        {{ word }}
      </div>
      <div v-if="userOrder.length === 0" class="text-gray-400 italic text-sm w-full text-center">
        Chọn các từ bên dưới để ghép thành câu...
      </div>
    </div>

    <div class="flex flex-wrap justify-center gap-3">
      <button
        v-for="(word, idx) in shuffledWords"
        :key="idx"
        class="px-4 py-2 rounded-lg font-medium transition-all shadow-sm border-b-4 active:border-b-0 active:translate-y-1"
        :class="isUsed(idx)
          ? 'bg-gray-100 dark:bg-[#333] text-gray-300 dark:text-gray-600 border-transparent cursor-not-allowed'
          : 'bg-indigo-500 hover:bg-indigo-600 text-white border-indigo-700'"
        :disabled="isUsed(idx)"
        @click="addWord(word, idx)"
      >
        {{ word }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])

const correctOrder = props.question.metadata?.correctOrder || []
const shuffledWords = ref([])
const usedIndices = ref(new Set())
const userOrder = ref([])

onMounted(() => {
  // Clone và shuffle
  shuffledWords.value = [...correctOrder].sort(() => Math.random() - 0.5)
})

const isUsed = (idx) => usedIndices.value.has(idx)

const addWord = (word, idx) => {
  userOrder.value.push(word)
  usedIndices.value.add(idx)
  emitVal()
}

const removeWord = (idxInUserOrder) => {
  const wordToRemove = userOrder.value[idxInUserOrder]
  userOrder.value.splice(idxInUserOrder, 1)

  // Tìm index gốc trong shuffledWords để restore trạng thái
  // Logic đơn giản: tìm index đầu tiên của từ đó trong shuffled mà đang bị used
  for (let i = 0; i < shuffledWords.value.length; i++) {
    if (shuffledWords.value[i] === wordToRemove && usedIndices.value.has(i)) {
      usedIndices.value.delete(i)
      break // Chỉ xóa 1 instance
    }
  }
  emitVal()
}

const emitVal = () => {
  // Gửi lên chuỗi câu hoàn chỉnh
  emit('update:modelValue', userOrder.value.join(' '))
}
</script>
