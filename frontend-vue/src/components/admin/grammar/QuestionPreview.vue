<template>
  <div class="question-preview">
    <div class="preview-header">
      <div class="meta-info">
        <el-tag :type="typeColor" effect="dark">{{ question.questionType }}</el-tag>
        <span class="order">Thứ tự: #{{ question.orderIndex }}</span>
        <span class="points">+{{ question.points }} điểm</span>
      </div>
    </div>

    <div class="preview-body">
      <div class="label">Câu hỏi:</div>
      <div class="question-text ql-editor" v-html="question.questionText"></div>

      <div class="answer-section mt-4">
        <div class="label">Đáp án / Chi tiết:</div>

        <div v-if="question.questionType === 'MULTIPLE_CHOICE'" class="options-grid">
          <div
            v-for="(opt, idx) in question.metadata.options"
            :key="idx"
            class="option-item"
            :class="{ 'correct': opt.isCorrect }"
          >
            <div class="opt-marker">
              <el-icon v-if="opt.isCorrect"><Check /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="opt-text">{{ opt.text }}</div>
          </div>
        </div>

        <div v-else-if="question.questionType === 'TRUE_FALSE'" class="tf-display">
          <el-alert
            :title="question.metadata.correctAnswer ? 'ĐÁP ÁN: TRUE' : 'ĐÁP ÁN: FALSE'"
            :type="question.metadata.correctAnswer ? 'success' : 'error'"
            :closable="false"
            show-icon
          />
        </div>

        <div v-else-if="question.questionType === 'FILL_BLANK'">
          <el-table :data="question.metadata.blanks" size="small" border>
            <el-table-column label="Vị trí" prop="position" width="80" align="center" />
            <el-table-column label="Đáp án đúng" prop="correctAnswers">
              <template #default="{ row }">
                <el-tag v-for="ans in row.correctAnswers" :key="ans" class="mr-1">{{ ans }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else-if="question.questionType === 'MATCHING'">
          <el-table :data="question.metadata.pairs" size="small" border>
            <el-table-column label="Vế A" prop="item1" />
            <el-table-column label="" width="40" align="center"><el-icon><Switch /></el-icon></el-table-column>
            <el-table-column label="Vế B" prop="item2" />
          </el-table>
        </div>

        <div v-else class="json-dump">
          <pre>{{ JSON.stringify(question.metadata, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="question.explanation" class="explanation-section mt-4">
        <div class="label">Giải thích:</div>
        <div class="explanation-text" v-html="question.explanation"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check, Switch } from '@element-plus/icons-vue'

const props = defineProps({
  question: { type: Object, required: true }
})

const typeColor = computed(() => {
  const map = {
    'MULTIPLE_CHOICE': 'primary',
    'TRUE_FALSE': 'warning',
    'FILL_BLANK': 'success',
    'MATCHING': 'info',
    'ERROR_CORRECTION': 'danger'
  }
  return map[props.question.questionType] || 'info'
})
</script>

<style scoped>
.question-preview { padding: 0 10px; }
.preview-header { border-bottom: 1px solid #eee; padding-bottom: 12px; margin-bottom: 16px; }
.meta-info { display: flex; align-items: center; gap: 16px; font-size: 13px; color: #666; }
.points { font-weight: bold; color: #e6a23c; }

.label { font-weight: 600; margin-bottom: 8px; color: #303133; font-size: 14px; }
.mt-4 { margin-top: 16px; }

/* Question Text Style */
.question-text {
  background: #f9faFc; padding: 12px; border-radius: 6px; border: 1px solid #e4e7ed; font-size: 15px;
}

/* Options Grid */
.options-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; }
.option-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border: 1px solid #dcdfe6; border-radius: 6px;
}
.option-item.correct {
  background-color: #f0f9eb; border-color: #67c23a; color: #67c23a; font-weight: 500;
}
.opt-marker {
  width: 24px; height: 24px; border-radius: 50%; background: #eee;
  display: flex; align-items: center; justify-content: center; font-size: 12px;
}
.option-item.correct .opt-marker { background: #67c23a; color: white; }

.explanation-section {
  background: #fff6f6; padding: 12px; border-left: 3px solid #f56c6c; border-radius: 4px;
}
.json-dump { background: #f4f4f5; padding: 10px; border-radius: 4px; font-size: 12px; }
</style>
