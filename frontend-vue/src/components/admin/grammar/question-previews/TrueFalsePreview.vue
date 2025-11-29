<!-- src/components/admin/grammar/question-previews/TrueFalsePreview.vue -->
<template>
  <div class="true-false-preview">
    <el-alert v-if="!metadata" type="warning" :closable="false">
      No metadata available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Answer -->
      <el-card shadow="never" class="answer-card">
        <div class="answer-content">
          <el-icon :color="correctAnswer ? 'var(--el-color-success)' : 'var(--el-color-danger)'" size="40">
            <component :is="correctAnswer ? Select : CloseBold" />
          </el-icon>
          <div class="answer-text">
            <el-text type="info" size="small">Correct Answer:</el-text>
            <el-text size="large" tag="b" :type="correctAnswer ? 'success' : 'danger'">
              {{ correctAnswer ? 'TRUE ✅' : 'FALSE ❌' }}
            </el-text>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionFilled, Select, CloseBold } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})

const correctAnswer = computed(() => {
  // Check options array (từ backend)
  if (props.metadata.options) {
    const trueOption = props.metadata.options.find((o) => o.text === 'True')
    if (trueOption) {
      return trueOption.isCorrect
    }
  }
  // Fallback: correctAnswer field
  if (props.metadata.correctAnswer !== null && props.metadata.correctAnswer !== undefined) {
    return props.metadata.correctAnswer
  }
  return true
})
</script>

<style scoped>
.true-false-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.answer-card {
  border: 2px solid var(--el-border-color);
}

.answer-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
}

.answer-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
