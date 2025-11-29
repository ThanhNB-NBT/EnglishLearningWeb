<!-- src/components/admin/grammar/question-previews/MatchingPreview.vue -->
<template>
  <div class="matching-preview">
    <el-alert v-if="!metadata || !metadata.pairs" type="warning" :closable="false">
      No pairs data available
    </el-alert>

    <div v-else>
      <!-- Hint -->
      <div v-if="metadata.hint" class="preview-section">
        <el-text type="info" size="small">
          <el-icon><QuestionFilled /></el-icon>
          Hint: {{ metadata.hint }}
        </el-text>
      </div>

      <!-- Pairs -->
      <div class="pairs-grid">
        <el-card
          v-for="(pair, index) in sortedPairs"
          :key="index"
          shadow="hover"
          class="pair-card"
        >
          <div class="pair-content">
            <div class="pair-left">
              <el-tag type="primary" size="small">{{ index + 1 }}</el-tag>
              <el-text tag="b">{{ pair.left }}</el-text>
            </div>
            <el-icon size="20" color="var(--el-color-primary)">
              <Right />
            </el-icon>
            <div class="pair-right">
              <el-text>{{ pair.right }}</el-text>
            </div>
          </div>
        </el-card>
      </div>

      <!-- Statistics -->
      <el-descriptions :column="1" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="Total Pairs">
          {{ metadata.pairs.length }}
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

const sortedPairs = computed(() => {
  if (!props.metadata.pairs) return []
  return [...props.metadata.pairs].sort((a, b) => a.order - b.order)
})
</script>

<style scoped>
.matching-preview {
  padding: 16px;
}

.preview-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.pairs-grid {
  display: grid;
  gap: 12px;
}

.pair-card {
  border: 2px solid var(--el-border-color);
}

.pair-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pair-left,
.pair-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.pair-right {
  justify-content: flex-end;
}
</style>
