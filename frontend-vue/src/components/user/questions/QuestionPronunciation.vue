<template>
  <div class="w-full mt-2">
    <div class="flex flex-wrap gap-3 mb-4 text-sm">
      <span
        v-for="word in unclassified"
        :key="word"
        class="cursor-pointer hover:text-blue-600 border-b border-dashed border-gray-400"
        :class="selectedWord === word ? 'text-blue-600 border-blue-600 font-bold' : ''"
        @click="selectedWord = word"
      >
        {{ word }}
      </span>
      <span v-if="unclassified.length === 0" class="text-gray-400 italic">Hoàn thành!</span>
    </div>

    <div class="flex gap-4">
      <div
        v-for="cat in pronunciationData.categories"
        :key="cat"
        class="flex-1 border border-gray-300 dark:border-gray-700 rounded p-2 min-h-[60px] cursor-pointer hover:bg-gray-50 dark:hover:bg-white/5"
        @click="addToCategory(cat)"
      >
        <div class="text-xs font-bold text-gray-500 mb-1 text-center border-b pb-1">{{ cat }}</div>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="w in getWords(cat)"
            :key="w"
            class="text-xs bg-gray-100 dark:bg-[#333] px-1 rounded flex items-center"
          >
            {{ w }}
            <span
              @click.stop="remove(w)"
              class="ml-1 text-red-400 hover:text-red-600 cursor-pointer"
              >×</span
            >
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useQuestionData } from '@/composables/questions/useQuestionData'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])
const { pronunciationData } = useQuestionData(props)
const userMap = ref(props.modelValue || {})
const selectedWord = ref(null)

const unclassified = computed(() => pronunciationData.value.words.filter((w) => !userMap.value[w]))
const getWords = (cat) =>
  Object.entries(userMap.value)
    .filter(([_, c]) => c === cat)
    .map(([w]) => w)
const addToCategory = (cat) => {
  if (!selectedWord.value) return
  userMap.value[selectedWord.value] = cat
  selectedWord.value = null
  emit('update:modelValue', { ...userMap.value })
}
const remove = (w) => {
  delete userMap.value[w]
  emit('update:modelValue', { ...userMap.value })
}
</script>
