<template>
  <div class="w-full">
    <div class="p-4 bg-yellow-50 dark:bg-yellow-900/10 border border-yellow-200 dark:border-yellow-800 rounded-xl mb-4 text-sm text-yellow-800 dark:text-yellow-400 flex items-start gap-2">
      <el-icon class="mt-0.5 text-lg"><InfoFilled /></el-icon>
      <span>Tìm từ/cụm từ sai trong câu hỏi và nhập từ sửa lại đúng.</span>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="space-y-2">
        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300">1. Từ sai (Error)</label>
        <input
          v-model="answer.errorText"
          type="text"
          class="w-full p-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#262626] focus:border-red-500 focus:ring-2 focus:ring-red-500/20 outline-none transition-all"
          placeholder="Nhập từ bị sai..."
        />
      </div>

      <div class="space-y-2">
        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300">2. Sửa lại thành (Correction)</label>
        <input
          v-model="answer.correction"
          type="text"
          class="w-full p-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#262626] focus:border-green-500 focus:ring-2 focus:ring-green-500/20 outline-none transition-all"
          placeholder="Nhập từ đúng..."
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])

const answer = ref({ errorText: '', correction: '' })

watch(answer, (val) => {
  // Gửi string correction hoặc object tùy backend.
  // Ở đây gửi correction string cho đơn giản theo logic cũ,
  // hoặc gửi cả object nếu backend chấm cả errorText.
  emit('update:modelValue', val.correction)
}, { deep: true })
</script>
