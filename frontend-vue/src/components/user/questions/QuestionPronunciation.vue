<template>
  <div class="w-full mt-4">
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div
        v-for="group in groups"
        :key="group"
        class="border border-gray-300 dark:border-gray-700 rounded-lg p-3 min-h-[100px]"
      >
        <div
          class="font-bold text-center border-b pb-2 mb-2 bg-gray-50 dark:bg-gray-800 -mx-3 -mt-3 pt-2 rounded-t-lg"
        >
          /{{ group }}/
        </div>

        <div class="flex flex-wrap gap-2 min-h-[50px]">
          <div
            v-for="word in getWordsInGroup(group)"
            :key="word"
            class="px-2 py-1 bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-100 rounded cursor-pointer hover:bg-red-100 hover:text-red-800 transition-colors"
            @click="!disabled && unassignWord(word)"
            title="Bấm để bỏ chọn"
          >
            {{ word }}
          </div>

          <div
            v-if="!disabled && selectedWord"
            class="w-full h-8 border-2 border-dashed border-blue-300 rounded flex items-center justify-center text-blue-400 text-xs cursor-pointer hover:bg-blue-50"
            @click="assignSelectedTo(group)"
          >
            Chọn vào đây
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="unclassifiedWords.length > 0"
      class="mt-4 p-3 bg-gray-50 dark:bg-gray-800/50 rounded border border-gray-200"
    >
      <div class="text-xs text-gray-500 mb-2 uppercase font-bold">Từ cần phân loại:</div>
      <div class="flex flex-wrap gap-3">
        <div
          v-for="word in unclassifiedWords"
          :key="word"
          class="px-3 py-1.5 border rounded cursor-pointer transition-all select-none"
          :class="
            selectedWord === word
              ? 'bg-blue-600 text-white border-blue-600 shadow-md transform -translate-y-0.5'
              : 'bg-white dark:bg-gray-700 border-gray-300 hover:border-blue-400'
          "
          @click="!disabled && selectWord(word)"
        >
          {{ word }}
        </div>
      </div>
    </div>
    <div v-else-if="!disabled" class="mt-4 text-center text-green-600 text-sm italic">
      Đã phân loại hết các từ!
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) }, // Format: { "word": "groupName" }
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const userMap = ref({})
const selectedWord = ref(null)

// Parse dữ liệu từ question object
// Giả định structure: question.data.groups = ["id", "ea"] và question.data.words = ["meat", "head", ...]
const groups = computed(() => {
  return props.question?.data?.groups || props.question?.metadata?.groups || ['Group 1', 'Group 2']
})

const allWords = computed(() => {
  return props.question?.data?.words || props.question?.metadata?.words || []
})

onMounted(() => {
  userMap.value = { ...props.modelValue }
})

const getWordsInGroup = (groupName) => {
  return Object.keys(userMap.value).filter((word) => userMap.value[word] === groupName)
}

const unclassifiedWords = computed(() => {
  return allWords.value.filter((w) => !userMap.value[w])
})

const selectWord = (word) => {
  if (selectedWord.value === word) {
    selectedWord.value = null // Toggle off
  } else {
    selectedWord.value = word
  }
}

const assignSelectedTo = (group) => {
  if (!selectedWord.value) return

  userMap.value = {
    ...userMap.value,
    [selectedWord.value]: group,
  }
  selectedWord.value = null
  emit('update:modelValue', userMap.value)
}

const unassignWord = (word) => {
  const newMap = { ...userMap.value }
  delete newMap[word]
  userMap.value = newMap
  emit('update:modelValue', newMap)
}
</script>
}
