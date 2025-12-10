<template>
  <div class="w-full">
    <div class="grid grid-cols-2 gap-4 md:gap-8">

      <div class="flex flex-col gap-3">
        <div class="text-xs font-bold text-gray-400 uppercase mb-1">Cột A</div>
        <div v-for="(item, idx) in pairs" :key="'left-' + idx"
          class="p-4 rounded-xl border-2 cursor-pointer transition-all text-center font-medium select-none shadow-sm"
          :class="getLeftClass(item.key)" @click="selectLeft(item.key)">
          {{ item.key }}
        </div>
      </div>

      <div class="flex flex-col gap-3">
        <div class="text-xs font-bold text-gray-400 uppercase mb-1">Cột B</div>
        <div v-for="(item, idx) in shuffledValues" :key="'right-' + idx"
          class="p-4 rounded-xl border-2 cursor-pointer transition-all text-center font-medium select-none shadow-sm"
          :class="getRightClass(item)" @click="selectRight(item)">
          {{ item }}
        </div>
      </div>

    </div>

    <div v-if="Object.keys(currentMap).length > 0"
      class="mt-6 p-4 bg-gray-50 dark:bg-[#262626] rounded-xl border border-gray-200 dark:border-gray-700">
      <div class="text-sm font-bold text-gray-500 mb-2 uppercase">Các cặp đã nối:</div>
      <div class="flex flex-wrap gap-2">
        <div v-for="(val, key) in currentMap" :key="key"
          class="flex items-center gap-2 px-3 py-1.5 bg-white dark:bg-[#333] border border-blue-200 dark:border-gray-600 rounded-lg shadow-sm">
          <span class="font-bold text-blue-600 dark:text-blue-400">{{ key }}</span>
          <el-icon>
            <Link />
          </el-icon>
          <span class="text-gray-800 dark:text-gray-200">{{ val }}</span>
          <button @click="removePair(key)" class="ml-1 text-gray-400 hover:text-red-500 transition-colors">
            <el-icon>
              <Close />
            </el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Link, Close } from '@element-plus/icons-vue'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])

const pairs = computed(() => props.question.metadata?.matchingPairs || [])
const shuffledValues = ref([])
const selectedLeft = ref(null)
const currentMap = ref(props.modelValue || {})

// Shuffle values on mount
onMounted(() => {
  shuffledValues.value = pairs.value.map(p => p.value).sort(() => Math.random() - 0.5)
})

const selectLeft = (key) => {
  // Nếu đã nối rồi thì không chọn lại
  if (currentMap.value[key]) return
  selectedLeft.value = key
}

const selectRight = (val) => {
  if (!selectedLeft.value) return
  // Check nếu value này đã được nối với key khác
  if (Object.values(currentMap.value).includes(val)) return

  // Tạo cặp
  currentMap.value[selectedLeft.value] = val
  selectedLeft.value = null
  emit('update:modelValue', { ...currentMap.value })
}

const removePair = (key) => {
  delete currentMap.value[key]
  emit('update:modelValue', { ...currentMap.value })
}

// Styling
const getLeftClass = (key) => {
  if (currentMap.value[key]) return 'bg-green-50 dark:bg-green-900/20 border-green-500 text-green-700 dark:text-green-400 opacity-60'
  if (selectedLeft.value === key) return 'bg-blue-600 border-blue-600 text-white shadow-md scale-105'
  return 'bg-white dark:bg-[#262626] border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-200 hover:border-blue-400'
}

const getRightClass = (val) => {
  const isMatched = Object.values(currentMap.value).includes(val)
  if (isMatched) return 'bg-gray-100 dark:bg-[#333] border-gray-300 dark:border-gray-600 text-gray-400 cursor-not-allowed'
  if (selectedLeft.value) return 'bg-white dark:bg-[#262626] border-blue-300 dark:border-blue-700 border-dashed text-gray-800 dark:text-white hover:bg-blue-50 dark:hover:bg-blue-900/20'
  return 'bg-white dark:bg-[#262626] border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-200 opacity-50'
}
</script>
