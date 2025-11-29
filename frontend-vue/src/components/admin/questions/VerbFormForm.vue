<!-- src/components/admin/questions/VerbFormForm.vue -->
<template>
  <div class="verb-form-form">
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

    <!-- Correct Answer (Verb Form) -->
    <el-form-item label="Correct Verb Form" required>
      <el-input
        v-model="localMetadata.correctAnswer"
        placeholder="Enter the correct verb form (e.g., went, has gone, will go)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Use "|" to separate multiple accepted forms. Example: "went|gone"
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
        <p><strong>Question:</strong> She _____ (go) to school yesterday.</p>
        <p><strong>Correct Answer:</strong> went</p>
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
.verb-form-form {
  padding: 8px 0;
}
</style>
