<!-- src/components/admin/questions/ErrorCorrectionForm.vue -->
<template>
  <div class="error-correction-form">
    <!-- Hint (Optional) -->
    <el-form-item label="Hint (Optional)">
      <el-input
        v-model="localMetadata.hint"
        type="textarea"
        :rows="2"
        placeholder="Enter a hint to help users..."
        maxlength="200"
        show-word-limit
        @input="emitUpdate"
      />
    </el-form-item>

    <!-- Correct Answer (Corrected Sentence) -->
    <el-form-item label="Corrected Sentence" required>
      <el-input
        v-model="localMetadata.correctAnswer"
        type="textarea"
        :rows="3"
        placeholder="Enter the corrected sentence (e.g., She goes to school every day.)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Enter the grammatically correct version of the sentence in the question.
        </el-text>
      </template>
    </el-form-item>

    <!-- Case Sensitive -->
    <el-form-item label="Case Sensitive">
      <el-switch
        v-model="localMetadata.caseSensitive"
        active-text="Yes"
        inactive-text="No"
        @change="emitUpdate"
      />
    </el-form-item>

    <!-- Example Usage -->
    <el-alert
      title="Example"
      type="info"
      :closable="false"
      style="margin-top: 12px"
    >
      <template #default>
        <p><strong>Question:</strong> Find and correct the error: "She go to school every day."</p>
        <p><strong>Correct Answer:</strong> She goes to school every day.</p>
      </template>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  correctAnswer: props.metadata.correctAnswer || '',
  caseSensitive: props.metadata.caseSensitive ?? false,
})

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        correctAnswer: newVal.correctAnswer || '',
        caseSensitive: newVal.caseSensitive ?? false,
      }
    }
  },
  { deep: true }
)

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.error-correction-form {
  padding: 8px 0;
}
</style>
