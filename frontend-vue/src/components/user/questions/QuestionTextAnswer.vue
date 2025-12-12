<template>
  <div class="w-full mt-2">
    <input
      type="text"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      class="w-full bg-transparent border-b border-gray-400 focus:border-blue-600 outline-none py-2 px-0 text-base text-blue-800 dark:text-blue-300 placeholder-gray-400 transition-colors"
      placeholder="Nhập câu trả lời của bạn..."
    />
  </div>
</template>

<script setup>
defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
</script>
