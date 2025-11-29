<!-- src/components/admin/grammar/question-previews/TextAnswerPreview.vue -->
<template>
  <div class="text-answer-preview">
    <el-alert v-if="!metadata || !metadata.correctAnswer" type="warning" :closable="false">
      No correct answer data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Correct Answer -->
      <el-card shadow="never" class="answer-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Correct Answer">
            <el-tag type="success" size="large">{{ metadata.correctAnswer }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Case Sensitive">
            <el-tag :type="metadata.caseSensitive ? 'warning' : 'info'" size="small">
              {{ metadata.caseSensitive ? 'Yes' : 'No' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="hasMultipleAnswers" label="Accepted Answers">
            <el-space wrap>
              <el-tag
                v-for="(answer, index) in acceptedAnswers"
                :key="index"
                type="success"
                size="small"
              >
                {{ answer }}
              </el-tag>
            </el-space>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})

const acceptedAnswers = computed(() => {
  if (!props.metadata.correctAnswer) return []
  return props.metadata.correctAnswer.split('|').map((a) => a.trim())
})

const hasMultipleAnswers = computed(() => acceptedAnswers.value.length > 1)
</script>

<style scoped>
.text-answer-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.answer-card {
  border: 2px solid var(--el-color-success);
}
</style>
