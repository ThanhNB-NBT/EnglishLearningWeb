<!-- src/components/admin/grammar/question-previews/SentenceBuildingPreview.vue -->
<template>
  <div class="sentence-building-preview">
    <el-alert v-if="!metadata || !metadata.words" type="warning" :closable="false">
      No words data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Scrambled Words -->
      <el-card shadow="never" class="words-card">
        <template #header>
          <el-text tag="b">Scrambled Words</el-text>
        </template>
        <el-space wrap>
          <el-tag
            v-for="(word, index) in metadata.words"
            :key="index"
            type="warning"
            size="large"
          >
            {{ word }}
          </el-tag>
        </el-space>
      </el-card>

      <!-- Correct Sentence -->
      <el-card shadow="never" class="answer-card">
        <template #header>
          <el-text tag="b">Correct Sentence</el-text>
        </template>
        <el-text size="large" type="success">{{ metadata.correctSentence }}</el-text>
      </el-card>

      <!-- Statistics -->
      <el-descriptions :column="1" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Total Words">
          {{ metadata.words.length }}
        </el-descriptions-item>
        <el-descriptions-item label="Sentence Length">
          {{ metadata.correctSentence ? metadata.correctSentence.split(' ').length : 0 }} words
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
</script>

<style scoped>
.sentence-building-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.words-card,
.answer-card {
  margin-bottom: 16px;
  border: 2px solid var(--el-border-color);
}

.answer-card {
  border-color: var(--el-color-success);
}
</style>
