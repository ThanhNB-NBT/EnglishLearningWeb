<template>
  <div class="w-full text-base">
    <div class="flex gap-8 mb-4">
      <div class="flex-1 flex flex-col gap-2">
        <div
          v-for="item in matchingPairs"
          :key="item.left"
          class="flex items-center justify-between p-2 border-b border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-100 dark:hover:bg-[#2a2a2a] transition-colors"
          :class="{
            'font-bold text-blue-600': selectedLeft === item.left,
            'opacity-50 line-through': currentMap[item.left],
          }"
          @click="selectLeft(item.left)"
        >
          <span>{{ item.left }}</span>
          <span v-if="selectedLeft === item.left" class="text-xs text-blue-500">●</span>
        </div>
      </div>

      <div class="flex-1 flex flex-col gap-2">
        <div
          v-for="val in shuffledRight"
          :key="val"
          class="p-2 border-b border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-100 dark:hover:bg-[#2a2a2a] transition-colors"
          :class="{ 'opacity-50 line-through': Object.values(currentMap).includes(val) }"
          @click="selectRight(val)"
        >
          {{ val }}
        </div>
      </div>
    </div>

    <div
      v-if="Object.keys(currentMap).length > 0"
      class="mt-4 p-3 bg-gray-50 dark:bg-[#1a1a1a] rounded text-sm"
    >
      <div class="font-bold mb-2">Đáp án của bạn:</div>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
        <div v-for="(val, key) in currentMap" :key="key" class="flex items-center gap-2">
          <span class="font-medium">{{ key }}</span>
          <span class="text-gray-400">➜</span>
          <span>{{ val }}</span>
          <button @click="removePair(key)" class="text-red-500 hover:font-bold ml-auto">Xóa</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useQuestionData } from '@/composables/questions/useQuestionData'
// ... Logic selectLeft, selectRight, removePair giống bản trước ...
const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])
const { matchingPairs } = useQuestionData(props)
const shuffledRight = ref([])
const selectedLeft = ref(null)
const currentMap = ref(props.modelValue || {})
onMounted(() => {
  shuffledRight.value = matchingPairs.value.map((p) => p.right).sort(() => Math.random() - 0.5)
})
const selectLeft = (k) => {
  if (!currentMap.value[k]) selectedLeft.value = k
}
const selectRight = (v) => {
  if (!selectedLeft.value || Object.values(currentMap.value).includes(v)) return
  currentMap.value[selectedLeft.value] = v
  selectedLeft.value = null
  emit('update:modelValue', { ...currentMap.value })
}
const removePair = (k) => {
  delete currentMap.value[k]
  emit('update:modelValue', { ...currentMap.value })
}
</script>
