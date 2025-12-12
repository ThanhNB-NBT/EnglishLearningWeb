<template>
  <div class="w-full mt-1">
    <div class="text-base italic text-gray-600 dark:text-gray-400 mb-3 font-serif">
      "{{ transformationData.originalSentence }}"
    </div>

    <div class="flex items-end gap-2 text-lg">
      <span v-if="transformationData.beginningPhrase" class="font-medium whitespace-nowrap mb-1">
        {{ transformationData.beginningPhrase }}
      </span>
      <input
        type="text"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        class="flex-1 bg-transparent border-b border-gray-400 focus:border-black dark:focus:border-white outline-none py-1 px-0 transition-colors font-medium text-blue-800 dark:text-blue-300 placeholder-gray-300"
        placeholder="..."
      />
    </div>
  </div>
</template>

<script setup>
import { useQuestionData } from '@/composables/questions/useQuestionData'
const props = defineProps(['question', 'modelValue'])
defineEmits(['update:modelValue'])
const { transformationData } = useQuestionData(props)
</script>
