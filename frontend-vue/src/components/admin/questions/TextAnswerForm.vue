<!-- TextAnswerForm.vue - Dùng cho FILL_BLANK, SHORT_ANSWER, VERB_FORM, ERROR_CORRECTION -->
<template>
  <div class="text-answer-form">
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

    <!-- Correct Answer -->
    <el-form-item label="Correct Answer" required>
      <el-input
        v-model="localMetadata.correctAnswer"
        placeholder="Enter the correct answer (use | for multiple accepted answers)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Use "|" to separate multiple accepted answers. Example: "go|goes"
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
.text-answer-form {
  padding: 8px 0;
}
</style>
