<template>
  <div class="question-preview">
    <div class="preview-header">
      <div class="meta-info">
        <el-tag :type="typeColor" effect="dark">{{ question.questionType }}</el-tag>
        <span class="order">#{{ question.orderIndex }}</span>
        <span class="points">+{{ question.points }} điểm</span>
      </div>
    </div>

    <div class="preview-body">
      <div class="label">Nội dung câu hỏi:</div>

      <div v-if="question.metadata?.wordBank?.length" class="word-bank-preview mb-3">
        <span class="text-muted text-xs mr-2">Word Bank:</span>
        <el-space wrap>
          <el-tag v-for="word in question.metadata.wordBank" :key="word" type="info" effect="plain">{{ word }}</el-tag>
        </el-space>
      </div>

      <div class="question-text ql-editor" v-html="question.questionText"></div>

      <div class="answer-section mt-4">
        <div class="label">Đáp án & Cấu hình chi tiết:</div>

        <div v-if="['MULTIPLE_CHOICE', 'COMPLETE_CONVERSATION'].includes(question.questionType)" class="options-grid">
          <div v-for="(opt, idx) in question.metadata.options" :key="idx" class="option-item"
            :class="{ 'correct': opt.isCorrect }">
            <div class="opt-marker">
              <el-icon v-if="opt.isCorrect">
                <Check />
              </el-icon>
              <span v-else>{{ String.fromCharCode(65 + idx) }}</span>
            </div>
            <div class="opt-text">{{ opt.text }}</div>
          </div>
        </div>

        <div v-else-if="question.questionType === 'TRUE_FALSE'">
          <el-alert :title="getTrueFalseResult(question.metadata) ? 'ĐÁP ÁN: TRUE (Đúng)' : 'ĐÁP ÁN: FALSE (Sai)'"
            :type="getTrueFalseResult(question.metadata) ? 'success' : 'error'" :closable="false" show-icon />
        </div>

        <div v-else-if="['FILL_BLANK', 'TEXT_ANSWER', 'VERB_FORM'].includes(question.questionType)">
          <el-table :data="question.metadata.blanks" size="small" border>
            <el-table-column label="#" prop="position" width="50" align="center" />

            <el-table-column v-if="hasHint" label="Gợi ý (V)" prop="hint" width="120">
              <template #default="{ row }">
                <span v-if="row.hint || row.verb" class="font-mono text-primary">{{ row.hint || row.verb }}</span>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>

            <el-table-column label="Đáp án chấp nhận">
              <template #default="{ row }">
                <el-space wrap>
                  <el-tag v-for="ans in row.correctAnswers" :key="ans" type="success" effect="light">{{ ans }}</el-tag>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else-if="question.questionType === 'MATCHING'">
          <el-table :data="question.metadata.pairs" size="small" border stripe>
            <el-table-column label="Vế A (Left)" prop="left" />
            <el-table-column width="50" align="center"><template #default><el-icon>
                  <Switch />
                </el-icon></template></el-table-column>
            <el-table-column label="Vế B (Right)" prop="right" />
          </el-table>
        </div>

        <div v-else-if="question.questionType === 'SENTENCE_BUILDING'">
          <div class="mb-2">
            <span class="text-sm font-bold text-gray-600">Từ khóa:</span>
            <el-space wrap class="ml-2">
              <el-tag v-for="w in question.metadata.words" :key="w" type="warning">{{ w }}</el-tag>
            </el-space>
          </div>
          <div class="p-2 bg-success-light border-success rounded">
            <span class="font-bold text-success">Đáp án câu hoàn chỉnh:</span>
            <div class="mt-1">{{ question.metadata.correctSentence }}</div>
          </div>
        </div>

        <div v-else-if="question.questionType === 'SENTENCE_TRANSFORMATION'">
          <div class="mb-2"><b>Câu gốc:</b> {{ question.metadata.originalSentence }}</div>
          <div class="mb-2" v-if="question.metadata.beginningPhrase">
            <b>Gợi ý mở đầu:</b> <el-tag size="small">{{ question.metadata.beginningPhrase }} ...</el-tag>
          </div>
          <div class="p-2 bg-success-light border-success rounded">
            <b>Đáp án chấp nhận:</b>
            <ul class="pl-4 mt-1 m-0">
              <li v-for="ans in question.metadata.correctAnswers" :key="ans" class="text-success">{{ ans }}</li>
            </ul>
          </div>
        </div>

        <div v-else-if="question.questionType === 'ERROR_CORRECTION'">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Từ/Cụm từ sai (Error)">
              <span class="text-danger font-bold">{{ question.metadata.errorText }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Sửa lại đúng (Correction)">
              <span class="text-success font-bold">{{ question.metadata.correction }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-else-if="question.questionType === 'PRONUNCIATION'">
          <div class="mb-2">
            <span class="text-gray-500">Các nhóm âm:</span>
            <el-tag v-for="cat in question.metadata.categories" :key="cat" class="ml-2" effect="dark">{{ cat }}</el-tag>
          </div>
          <el-table :data="question.metadata.classifications" size="small" border>
            <el-table-column label="Từ vựng" prop="word" />
            <el-table-column label="Thuộc nhóm âm" prop="category">
              <template #default="{ row }">
                <el-tag type="success" effect="plain">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else-if="question.questionType === 'OPEN_ENDED'">
          <div class="p-3 bg-gray-50 border rounded mb-2">
            <div class="text-xs text-gray-500 mb-1 font-bold">Gợi ý trả lời (Suggested Answer):</div>
            <div style="white-space: pre-wrap;">{{ question.metadata.suggestedAnswer || '(Không có gợi ý)' }}</div>
          </div>
          <div class="flex gap-2 mt-2">
            <el-tag type="warning" size="small" v-if="question.metadata.timeLimitSeconds">
              <el-icon>
                <Timer />
              </el-icon> {{ question.metadata.timeLimitSeconds }}s
            </el-tag>
            <el-tag type="info" size="small" v-if="question.metadata.minWord">Min: {{ question.metadata.minWord }}
              từ</el-tag>
            <el-tag type="info" size="small" v-if="question.metadata.maxWord">Max: {{ question.metadata.maxWord }}
              từ</el-tag>
          </div>
        </div>

        <div v-else class="json-dump">
          <pre>{{ JSON.stringify(question.metadata, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="question.explanation" class="explanation-section mt-4">
        <div class="label">Giải thích chi tiết:</div>
        <div class="explanation-text">{{ question.explanation }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check, Switch, EditPen, Connection, Timer } from '@element-plus/icons-vue'

const props = defineProps({ question: { type: Object, required: true } })

// Helper check True/False
const getTrueFalseResult = (meta) => {
  if (meta.options && Array.isArray(meta.options)) {
    const trueOpt = meta.options.find(o => o.text === 'True')
    if (trueOpt) return trueOpt.isCorrect
  }
  return meta.correctAnswer !== undefined ? meta.correctAnswer : false
}

const hasHint = computed(() => {
  const blanks = props.question.metadata?.blanks
  return blanks && blanks.some(b => b.hint || b.verb)
})

const typeColor = computed(() => {
  const map = {
    'MULTIPLE_CHOICE': 'primary', 'TRUE_FALSE': 'primary',
    'FILL_BLANK': 'success', 'VERB_FORM': 'success', 'TEXT_ANSWER': 'success',
    'MATCHING': 'warning', 'SENTENCE_BUILDING': 'warning', 'PRONUNCIATION': 'warning',
    'ERROR_CORRECTION': 'danger', 'SENTENCE_TRANSFORMATION': 'info', 'OPEN_ENDED': 'info'
  }
  return map[props.question.questionType] || 'info'
})
</script>

<style scoped>
.question-preview {
  padding: 0 10px;
}

.preview-header {
  border-bottom: 1px solid var(--el-border-color);
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.points {
  font-weight: bold;
  color: #e6a23c;
}

.label {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.mt-4 {
  margin-top: 16px;
}

.mb-3 {
  margin-bottom: 12px;
}

/* --- STYLES --- */
.question-text {
  background: var(--el-fill-color);
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  font-size: 15px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
}

.word-bank-preview {
  padding: 10px;
  background: var(--el-fill-color-light);
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
}

.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
}

.option-item.correct {
  background-color: var(--el-color-success-light-9);
  border-color: var(--el-color-success);
  color: var(--el-color-success);
  font-weight: 600;
}

.opt-marker {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--el-fill-color-darker);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.option-item.correct .opt-marker {
  background: var(--el-color-success);
  color: white;
}

.explanation-section {
  background: var(--el-color-warning-light-9);
  padding: 12px;
  border-left: 3px solid var(--el-color-warning);
  border-radius: 4px;
  color: var(--el-text-color-primary);
}

.bg-success-light {
  background-color: var(--el-color-success-light-9);
}

.border-success {
  border: 1px solid var(--el-color-success);
}

.text-success {
  color: var(--el-color-success);
}

.text-danger {
  color: var(--el-color-danger);
}

.text-warning {
  color: var(--el-color-warning);
}

.text-gray {
  color: var(--el-text-color-secondary);
}

.bg-gray-50 {
  background-color: var(--el-fill-color-lighter);
}

.border {
  border: 1px solid var(--el-border-color);
}

.rounded {
  border-radius: 4px;
}

.json-dump {
  background: var(--el-fill-color);
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  overflow: auto;
}

@media (max-width: 600px) {
  .options-grid {
    grid-template-columns: 1fr;
  }
}
</style>
