<!-- src/components/admin/questions/ShortAnswerForm.vue -->
<template>
  <TextAnswerForm v-model:metadata="localMetadata" :question-text="questionText" />
</template>

<script setup>
import { ref, watch } from 'vue'
import TextAnswerForm from './TextAnswerForm.vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
  questionText: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref(props.metadata)

watch(
  () => props.metadata,
  (newVal) => {
    localMetadata.value = newVal
  },
  { deep: true }
)

watch(
  localMetadata,
  (newVal) => {
    emit('update:metadata', newVal)
  },
  { deep: true }
)
</script>
