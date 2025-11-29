<!-- src/components/admin/grammar/question-previews/MultipleChoicePreview.vue -->
<template>
  <div class="multiple-choice-preview">
    <el-alert v-if="!metadata || !metadata.options" type="warning" :closable="false">
      No options data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Options -->
      <div class="options-grid">
        <el-card
          v-for="(option, index) in sortedOptions"
          :key="index"
          shadow="hover"
          :class="{ 'correct-option': option.isCorrect }"
          class="option-card"
        >
          <div class="option-content">
            <el-tag :type="option.isCorrect ? 'success' : 'info'" size="small">
              {{ String.fromCharCode(65 + index) }}
            </el-tag>
            <el-text>{{ option.text }}</el-text>
            <el-icon v-if="option.isCorrect" color="var(--el-color-success)" size="20">
              <Select />
            </el-icon>
          </div>
        </el-card>
      </div>

      <!-- Statistics -->
      <el-descriptions :column="2" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Total Options">
          {{ metadata.options.length }}
        </el-descriptions-item>
        <el-descriptions-item label="Correct Answer">
          {{ correctAnswerText }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionFilled, Select } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})

const sortedOptions = computed(() => {
  if (!props.metadata.options) return []
  return [...props.metadata.options].sort((a, b) => a.order - b.order)
})

const correctAnswerText = computed(() => {
  const correct = props.metadata.options?.find((o) => o.isCorrect)
  return correct ? correct.text : 'N/A'
})
</script>

<style scoped>
.multiple-choice-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.options-grid {
  display: grid;
  gap: 12px;
}

.option-card {
  border: 2px solid var(--el-border-color);
  transition: all 0.3s;
}

.option-card.correct-option {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.option-content {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
