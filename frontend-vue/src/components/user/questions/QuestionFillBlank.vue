<template>
  <div class="w-full">
    <div class="mb-2 text-sm text-gray-500 dark:text-gray-400">Nhập đáp án vào ô trống:</div>

    <div class="relative">
      <input type="text" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)"
        class="w-full p-4 pl-5 rounded-xl border-2 border-gray-300 dark:border-gray-600 bg-white dark:bg-[#262626] text-gray-900 dark:text-white placeholder-gray-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition-all font-medium text-lg"
        placeholder="Gõ câu trả lời của bạn..." />
      <div class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400">
        <el-icon :size="20">
          <EditPen />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { EditPen } from '@element-plus/icons-vue'
defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
</script>
