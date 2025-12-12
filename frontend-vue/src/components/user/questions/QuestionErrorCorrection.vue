<template>
  <div class="w-full mt-2 flex flex-wrap gap-4 items-center">
    <div class="flex-1 min-w-[120px]">
      <input
        v-model="answer.errorText"
        type="text"
        class="w-full bg-transparent border-b border-red-300 focus:border-red-600 outline-none py-1 text-red-600 placeholder-red-200"
        placeholder="Từ sai"
      />
    </div>
    <span class="text-gray-400">➜</span>
    <div class="flex-1 min-w-[120px]">
      <input
        v-model="answer.correction"
        type="text"
        class="w-full bg-transparent border-b border-green-300 focus:border-green-600 outline-none py-1 text-green-600 placeholder-green-200"
        placeholder="Sửa thành"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps(['question', 'modelValue'])
const emit = defineEmits(['update:modelValue'])
const answer = ref(
  props.modelValue && typeof props.modelValue === 'object'
    ? props.modelValue
    : { errorText: '', correction: '' },
)
watch(answer, (val) => emit('update:modelValue', { ...val }), { deep: true })
</script>
