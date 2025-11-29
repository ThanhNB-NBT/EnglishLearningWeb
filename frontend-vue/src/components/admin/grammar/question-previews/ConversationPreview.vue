<!-- src/components/admin/grammar/question-previews/ConversationPreview.vue -->
<template>
  <div class="conversation-preview">
    <el-alert v-if="!metadata || !metadata.conversationContext" type="warning" :closable="false">
      No conversation data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Conversation Context -->
      <el-card shadow="never" class="conversation-card">
        <template #header>
          <el-text tag="b">Conversation Context</el-text>
        </template>
        <div class="conversation-text">
          {{ metadata.conversationContext }}
        </div>
      </el-card>

      <!-- Options -->
      <el-card shadow="never" class="options-card">
        <template #header>
          <el-text tag="b">Response Options</el-text>
        </template>
        <el-space direction="vertical" fill style="width: 100%">
          <el-card
            v-for="(option, index) in metadata.options"
            :key="index"
            shadow="hover"
            :class="{ 'correct-option': option === metadata.correctAnswer }"
            class="option-item"
          >
            <div class="option-content">
              <el-tag :type="option === metadata.correctAnswer ? 'success' : 'info'" size="small">
                {{ String.fromCharCode(65 + index) }}
              </el-tag>
              <el-text>{{ option }}</el-text>
              <el-icon v-if="option === metadata.correctAnswer" color="var(--el-color-success)" size="20">
                <Select />
              </el-icon>
            </div>
          </el-card>
        </el-space>
      </el-card>

      <!-- Correct Answer -->
      <el-descriptions :column="1" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Correct Answer">
          <el-tag type="success">{{ metadata.correctAnswer }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Total Options">
          {{ metadata.options ? metadata.options.length : 0 }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { QuestionFilled, Select } from '@element-plus/icons-vue'

defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})
</script>

<style scoped>
.conversation-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.conversation-card,
.options-card {
  margin-bottom: 16px;
  border: 2px solid var(--el-border-color);
}

.conversation-text {
  white-space: pre-wrap;
  line-height: 1.8;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.option-item {
  border: 2px solid var(--el-border-color);
}

.option-item.correct-option {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.option-content {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
