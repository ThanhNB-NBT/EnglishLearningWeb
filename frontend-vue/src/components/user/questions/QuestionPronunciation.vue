<template>
  <div class="w-full">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
      <div
        v-for="cat in categories"
        :key="cat"
        class="bg-white dark:bg-[#1d1d1d] border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden shadow-sm flex flex-col"
      >
        <div class="bg-blue-50 dark:bg-blue-900/20 px-4 py-2 border-b border-blue-100 dark:border-blue-800 font-bold text-blue-700 dark:text-blue-300 text-center">
          {{ cat }}
        </div>

        <div class="p-4 min-h-[100px] flex flex-wrap content-start gap-2 bg-gray-50/50 dark:bg-[#222]">
          <div
            v-for="word in getWordsInCategory(cat)"
            :key="word"
            class="px-3 py-1.5 bg-white dark:bg-[#333] border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm flex items-center gap-2 group animate-pop-in"
          >
            <span class="text-gray-800 dark:text-white font-medium">{{ word }}</span>
            <button @click="removeWord(word)" class="text-gray-400 hover:text-red-500 transition-colors">
              <el-icon><CircleClose /></el-icon>
            </button>
          </div>

          <div v-if="getWordsInCategory(cat).length === 0" class="w-full text-center text-gray-400 text-xs py-4">
            Trống
          </div>
        </div>

        <div class="p-2 border-t border-gray-100 dark:border-gray-700 bg-white dark:bg-[#1d1d1d]">
           <div class="text-xs text-gray-400 mb-2 text-center">Thêm từ vào nhóm này:</div>
           <div class="flex flex-wrap gap-1 justify-center">
             <button
               v-for="word in unclassifiedWords"
               :key="word"
               class="px-2 py-1 bg-gray-100 dark:bg-[#333] hover:bg-blue-100 dark:hover:bg-blue-900 rounded text-xs transition-colors border border-transparent hover:border-blue-300"
               @click="addWordToCategory(word, cat)"
             >
               {{ word }}
             </button>
           </div>
        </div>
      </div>
    </div>

    <div class="bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm text-center">
      <div class="text-sm font-bold text-gray-500 uppercase mb-3">Từ cần phân loại</div>
      <div class="flex flex-wrap justify-center gap-2">
        <div
          v-for="word in availableWords"
          :key="word"
          class="px-4 py-2 rounded-lg font-medium transition-all"
          :class="isClassified(word)
            ? 'bg-gray-100 dark:bg-[#2c2c2c] text-gray-400 line-through decoration-2 decoration-gray-300'
            : 'bg-indigo-50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 shadow-sm'"
        >
          {{ word }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { CircleClose } from '@element-plus/icons-vue'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])

const userMap = ref(props.modelValue || {})
const words = computed(() => props.question.metadata?.words || [])
const categories = computed(() => props.question.metadata?.categories || [])

const availableWords = computed(() => words.value)
const unclassifiedWords = computed(() => words.value.filter(w => !userMap.value[w]))

const isClassified = (word) => !!userMap.value[word]

const getWordsInCategory = (cat) => {
  return Object.entries(userMap.value)
    .filter(([_, c]) => c === cat)
    .map(([w, _]) => w)
}

const addWordToCategory = (word, cat) => {
  userMap.value[word] = cat
  emit('update:modelValue', { ...userMap.value })
}

const removeWord = (word) => {
  delete userMap.value[word]
  emit('update:modelValue', { ...userMap.value })
}
</script>

<style scoped>
.animate-pop-in {
  animation: popIn 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes popIn {
  from { transform: scale(0.8); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
