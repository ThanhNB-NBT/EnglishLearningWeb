<template>
  <div class="w-full">
    <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">Câu trả lời của bạn:</label>
    <el-input
      v-model="text"
      type="textarea"
      :rows="6"
      placeholder="Nhập câu trả lời..."
      maxlength="2000"
      show-word-limit
      resize="none"
      class="!text-base"
      @input="updateVal"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])
const text = ref(props.modelValue || '')
const updateVal = () => emit('update:modelValue', text.value)
</script>
