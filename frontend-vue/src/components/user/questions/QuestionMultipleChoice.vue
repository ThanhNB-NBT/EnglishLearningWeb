<template>
  <div class="w-full space-y-1 mt-2">
    <div
      v-for="(option, idx) in parsedOptions"
      :key="`opt-${idx}`"
      class="relative flex items-center gap-3 p-3 rounded-md cursor-pointer transition-all group"
      :class="getOptionClass(option)"
      @click="handleSelect(option)"
    >
      <div
        class="w-5 h-5 rounded-full border flex items-center justify-center shrink-0 transition-colors"
        :class="getIconClass(option)"
      >
        <div v-if="isSelected(option)" class="w-2.5 h-2.5 rounded-full bg-current"></div>
      </div>

      <div
        class="flex-1 text-base text-gray-700 dark:text-gray-300 select-none"
        v-html="option.text"
      ></div>

      <div v-if="disabled && showFeedback" class="shrink-0">
        <el-icon v-if="option.isCorrect" class="text-green-600 text-lg"
          ><CircleCheckFilled
        /></el-icon>
        <el-icon v-else-if="isSelected(option) && !option.isCorrect" class="text-red-600 text-lg"
          ><CircleCloseFilled
        /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: [String, Number], default: null },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const parsedOptions = computed(() => {
  if (props.question?.data?.options && Array.isArray(props.question.data.options)) {
    return props.question.data.options
  }
  if (props.question?.options && Array.isArray(props.question.options)) {
    return props.question.options
  }
  return []
})

const isSelected = (option) => {
  // ✅ FIX: Xử lý null/undefined với nullish coalescing
  const val = props.modelValue ?? ''

  if (val === '') return false

  // So sánh text
  if (option.text && String(val).trim() === String(option.text).trim()) return true

  // So sánh value (nếu có)
  if (option.value !== undefined && String(val).trim() === String(option.value).trim()) return true

  return false
}

const handleSelect = (option) => {
  if (props.disabled) return
  // Ưu tiên text vì API trả về text
  emit('update:modelValue', option.text || option.value)
}

// Logic Style: Không dùng border, chỉ dùng nền màu
const getOptionClass = (option) => {
  const selected = isSelected(option)

  // Mode: Xem kết quả
  if (props.disabled && props.showFeedback) {
    if (option.isCorrect)
      return 'bg-green-50 dark:bg-green-900/20 text-green-900 dark:text-green-100' // Đúng
    if (selected && !option.isCorrect)
      return 'bg-red-50 dark:bg-red-900/20 text-red-900 dark:text-red-100' // Sai
    return 'opacity-50 grayscale' // Mờ đi
  }

  // Mode: Đang làm bài
  if (selected) {
    return 'bg-blue-50 dark:bg-blue-900/20 text-blue-900 dark:text-blue-100 font-medium' // Đang chọn
  }
  return 'hover:bg-gray-100 dark:hover:bg-gray-800' // Bình thường
}

const getIconClass = (option) => {
  const selected = isSelected(option)
  if (props.disabled && props.showFeedback) {
    if (option.isCorrect) return 'border-green-600 text-green-600 bg-white dark:bg-gray-800'
    if (selected) return 'border-red-600 text-red-600 bg-white dark:bg-gray-800'
    return 'border-gray-300 dark:border-gray-600'
  }
  // Loại bỏ border xanh khi chưa chọn, chỉ hiện khi chọn
  return selected
    ? 'border-blue-600 text-blue-600 bg-white dark:bg-gray-800'
    : 'border-gray-400 dark:border-gray-600 group-hover:border-gray-500 dark:group-hover:border-gray-500 bg-white dark:bg-gray-800'
}
</script>
