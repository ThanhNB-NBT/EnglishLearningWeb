<!-- src/components/admin/grammar/question-previews/ReadingComprehensionPreview.vue -->
<template>
  <div class="reading-comprehension-preview">
    <el-alert v-if="!metadata || !metadata.passage" type="warning" :closable="false">
      No passage data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Passage -->
      <el-card shadow="never" class="passage-card">
        <template #header>
          <el-text tag="b">Reading Passage</el-text>
        </template>
        <div class="passage-text">
          {{ metadata.passage }}
        </div>
      </el-card>

      <!-- Blanks -->
      <el-card shadow="never" class="blanks-card">
        <template #header>
          <el-text tag="b">Blanks Configuration</el-text>
        </template>
        <el-space direction="vertical" fill style="width: 100%">
          <el-card
            v-for="(blank, index) in sortedBlanks"
            :key="index"
            shadow="hover"
            class="blank-item"
          >
            <div class="blank-header">
              <el-tag type="warning" size="small">Blank {{ blank.position }}</el-tag>
            </div>

            <el-descriptions :column="1" border size="small" style="margin-top: 8px">
              <el-descriptions-item label="Options">
                <el-space wrap>
                  <el-tag
                    v-for="(option, optIdx) in blank.options"
                    :key="optIdx"
                    :type="option === blank.correctAnswer ? 'success' : 'info'"
                    size="small"
                  >
                    {{ option }}
                  </el-tag>
                </el-space>
              </el-descriptions-item>
              <el-descriptions-item label="Correct Answer">
                <el-tag type="success">{{ blank.correctAnswer }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-space>
      </el-card>

      <!-- Statistics -->
      <el-descriptions :column="2" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Total Blanks">
          {{ metadata.blanks ? metadata.blanks.length : 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="Passage Length">
          {{ metadata.passage ? metadata.passage.split(' ').length : 0 }} words
        </el-descriptions-item>
      </el-descriptions>
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

const sortedBlanks = computed(() => {
  if (!props.metadata.blanks) return []
  return [...props.metadata.blanks].sort((a, b) => a.position - b.position)
})
</script>

<style scoped>
.reading-comprehension-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.passage-card,
.blanks-card {
  margin-bottom: 16px;
  border: 2px solid var(--el-border-color);
}

.passage-text {
  white-space: pre-wrap;
  line-height: 1.8;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  font-size: 15px;
}

.blank-item {
  border: 2px solid var(--el-border-color);
}

.blank-header {
  margin-bottom: 8px;
}
</style>
