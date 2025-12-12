<template>
  <textarea
    :value="modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
    rows="3"
    class="w-full mt-2 p-2 bg-transparent border border-gray-300 focus:border-blue-500 rounded outline-none text-sm dark:border-gray-600 dark:text-white"
    placeholder="Nhập câu trả lời..."
  ></textarea>
</template>
<script setup>
defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
</script>
