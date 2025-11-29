<!-- src/components/admin/grammar/question-previews/OpenEndedPreview.vue -->
<template>
  <div class="open-ended-preview">
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

      <!-- Info Alert -->
      <el-alert
        title="Open-Ended Question"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        This question requires AI or teacher evaluation. Students can submit free-form text answers.
      </el-alert>

      <!-- Suggested Answer -->
      <el-card v-if="metadata.suggestedAnswer" shadow="never" class="answer-card">
        <template #header>
          <el-text tag="b">Suggested Answer (For AI/Teacher Reference)</el-text>
        </template>
        <div class="answer-text">
          {{ metadata.suggestedAnswer }}
        </div>
      </el-card>

      <!-- Constraints -->
      <el-descriptions :column="2" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Time Limit">
          <el-tag v-if="metadata.timeLimitSeconds" type="warning">
            {{ formatTime(metadata.timeLimitSeconds) }}
          </el-tag>
          <el-text v-else type="info">No limit</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="Word Count Range">
          <el-tag v-if="metadata.minWord || metadata.maxWord" type="info">
            {{ formatWordRange(metadata.minWord, metadata.maxWord) }}
          </el-tag>
          <el-text v-else type="info">No restriction</el-text>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { QuestionFilled } from '@element-plus/icons-vue'

defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})

// Helper functions
const formatTime = (seconds) => {
  if (!seconds) return 'No limit'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return minutes > 0 ? `${minutes}m ${secs}s` : `${secs}s`
}

const formatWordRange = (min, max) => {
  if (min && max) return `${min} - ${max} words`
  if (min) return `Min: ${min} words`
  if (max) return `Max: ${max} words`
  return 'No restriction'
}
</script>

<style scoped>
.open-ended-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.answer-card {
  margin-bottom: 16px;
  border: 2px solid var(--el-color-warning);
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.8;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}
</style>
