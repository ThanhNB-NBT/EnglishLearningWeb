<template>
  <div class="w-full">
    <div class="grid grid-cols-1 gap-3">
      <div v-for="opt in options" :key="opt.order"
        class="group flex items-center p-4 rounded-xl border-2 cursor-pointer transition-all duration-200 relative overflow-hidden"
        :class="getOptionClass(opt.text)" @click="selectOption(opt.text)">
        <div
          class="w-10 h-10 rounded-lg flex items-center justify-center font-bold text-lg mr-4 border transition-colors shrink-0"
          :class="getLetterClass(opt.text)">
          {{ getLabel(opt.order) }}
        </div>

        <div class="flex-1 text-base font-medium z-10"
          :class="modelValue === opt.text ? 'text-blue-700 dark:text-blue-300' : 'text-gray-700 dark:text-gray-200'">
          {{ opt.text }}
        </div>

        <div v-if="modelValue === opt.text" class="absolute right-4 text-blue-600 dark:text-blue-400">
          <el-icon :size="24">
            <CircleCheckFilled />
          </el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])

const options = computed(() => {
  return (props.question.metadata?.options || []).sort((a, b) => a.order - b.order)
})

const getLabel = (order) => String.fromCharCode(64 + order) // 1->A, 2->B

const selectOption = (val) => {
  emit('update:modelValue', val)
}

// Styling Logic
const getOptionClass = (val) => {
  if (props.modelValue === val) {
    return 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 dark:border-blue-500 shadow-md'
  }
  return 'border-gray-200 dark:border-gray-700 bg-white dark:bg-[#262626] hover:border-blue-300 dark:hover:border-blue-700 hover:bg-gray-50 dark:hover:bg-[#333]'
}

const getLetterClass = (val) => {
  if (props.modelValue === val) {
    return 'bg-blue-600 border-blue-600 text-white'
  }
  return 'bg-gray-100 dark:bg-[#333] border-gray-200 dark:border-gray-600 text-gray-500 dark:text-gray-400 group-hover:bg-white dark:group-hover:bg-[#444] group-hover:border-blue-200'
}
</script>
