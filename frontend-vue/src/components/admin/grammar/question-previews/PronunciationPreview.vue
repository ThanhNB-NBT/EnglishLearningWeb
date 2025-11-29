<!-- src/components/admin/grammar/question-previews/PronunciationPreview.vue -->
<template>
  <div class="pronunciation-preview">
    <el-alert v-if="!metadata || !metadata.words" type="warning" :closable="false">
      No pronunciation data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Words to Classify -->
      <el-card shadow="never" class="words-card">
        <template #header>
          <el-text tag="b">Words to Classify</el-text>
        </template>
        <el-space wrap>
          <el-tag
            v-for="(word, index) in metadata.words"
            :key="index"
            type="primary"
            size="large"
          >
            {{ word }}
          </el-tag>
        </el-space>
      </el-card>

      <!-- Categories -->
      <el-card shadow="never" class="categories-card">
        <template #header>
          <el-text tag="b">Pronunciation Categories</el-text>
        </template>
        <el-space wrap>
          <el-tag
            v-for="(category, index) in metadata.categories"
            :key="index"
            type="success"
            size="large"
          >
            {{ category }}
          </el-tag>
        </el-space>
      </el-card>

      <!-- Correct Classifications -->
      <el-card shadow="never" class="classifications-card">
        <template #header>
          <el-text tag="b">Correct Classifications</el-text>
        </template>
        <el-space direction="vertical" fill style="width: 100%">
          <div
            v-for="(classification, index) in sortedClassifications"
            :key="index"
            class="classification-row"
          >
            <el-tag type="primary" size="large">{{ classification.word }}</el-tag>
            <el-icon size="20"><Right /></el-icon>
            <el-tag type="success" size="large">{{ classification.category }}</el-tag>
          </div>
        </el-space>
      </el-card>

      <!-- Statistics -->
      <el-descriptions :column="2" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Total Words">
          {{ metadata.words.length }}
        </el-descriptions-item>
        <el-descriptions-item label="Total Categories">
          {{ metadata.categories.length }}
        </el-descriptions-item>
        <el-descriptions-item label="Classifications">
          {{ metadata.classifications ? metadata.classifications.length : 0 }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionFilled, Right } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    required: true,
  },
})

const sortedClassifications = computed(() => {
  if (!props.metadata.classifications) return []
  return [...props.metadata.classifications].sort((a, b) =>
    a.word.localeCompare(b.word)
  )
})
</script>

<style scoped>
.pronunciation-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.words-card,
.categories-card,
.classifications-card {
  margin-bottom: 16px;
  border: 2px solid var(--el-border-color);
}

.classification-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}
</style>
