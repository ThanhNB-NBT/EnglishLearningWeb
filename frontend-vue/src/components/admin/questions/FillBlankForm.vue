<template>
  <div class="fill-blank-form">
    <div v-if="questionType === 'FILL_BLANK'" class="word-bank-box mb-4">
        <div class="header">
            <span class="title">Ngân hàng từ (Word Bank)</span>
            <el-switch v-model="hasWordBank" active-text="Bật" inactive-text="Tắt" />
        </div>
        <div v-if="hasWordBank" class="content">
            <el-select
                v-model="localMetadata.wordBank"
                multiple filterable allow-create default-first-option
                :reserve-keyword="false"
                placeholder="Nhập các từ để chọn..."
                style="width: 100%"
                @change="emitUpdate"
            />
        </div>
    </div>

    <el-form-item label="Cấu hình đáp án" required>
      <div class="blanks-container">
        <transition-group name="list">
          <div v-for="(blank, index) in localMetadata.blanks" :key="index" class="blank-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }" class="blank-card">
              <div class="blank-header mb-2">
                <el-tag effect="dark" size="small">Vị trí #{{ blank.position }}</el-tag>
                <el-button type="danger" link icon="Delete" @click="removeBlank(index)" :disabled="localMetadata.blanks.length <= 1">Xóa</el-button>
              </div>

              <div v-if="questionType === 'VERB_FORM'" class="mb-2">
                 <el-input v-model="blank.hint" placeholder="Động từ gốc (VD: go)" size="small">
                    <template #prepend>Gợi ý (V)</template>
                 </el-input>
              </div>

              <el-select
                v-model="blank.correctAnswers"
                multiple filterable allow-create default-first-option
                placeholder="Nhập đáp án đúng rồi ấn Enter..."
                style="width: 100%"
                @change="emitUpdate"
              />
            </el-card>
          </div>
        </transition-group>
        <el-button type="primary" plain icon="Plus" class="w-full mt-2 dashed-btn" @click="addBlank">Thêm chỗ trống</el-button>
      </div>
    </el-form-item>

    <el-form-item label="Giải thích">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2" @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
    metadata: { type: Object, default: () => ({}) },
    questionType: { type: String, default: 'FILL_BLANK' }
})
const emit = defineEmits(['update:metadata'])

const hasWordBank = ref(false)
const localMetadata = ref({ blanks: [], wordBank: [], explanation: '' })

const initData = () => {
    const meta = props.metadata || {}
    localMetadata.value.explanation = meta.explanation || ''
    localMetadata.value.wordBank = meta.wordBank || []
    hasWordBank.value = localMetadata.value.wordBank.length > 0

    if (props.questionType === 'TEXT_ANSWER' && meta.correctAnswer) {
        localMetadata.value.blanks = [{ position: 1, correctAnswers: meta.correctAnswer.split('|') }]
    } else if (props.questionType === 'VERB_FORM') {
        localMetadata.value.blanks = (meta.blanks || []).map(b => ({ position: b.position, correctAnswers: b.correctAnswers, hint: b.verb || b.hint }))
    } else {
        localMetadata.value.blanks = meta.blanks || [{ position: 1, correctAnswers: [] }]
    }
}

watch(() => props.metadata, initData, { immediate: true, deep: true })
watch(() => props.questionType, initData)

const addBlank = () => { localMetadata.value.blanks.push({ position: localMetadata.value.blanks.length + 1, correctAnswers: [], hint: '' }); emitUpdate() }
const removeBlank = (idx) => { localMetadata.value.blanks.splice(idx, 1); localMetadata.value.blanks.forEach((b, i) => b.position = i + 1); emitUpdate() }

const emitUpdate = () => {
  const payload = { explanation: localMetadata.value.explanation, blanks: localMetadata.value.blanks }
  if (props.questionType === 'FILL_BLANK' && hasWordBank.value) payload.wordBank = localMetadata.value.wordBank
  if (props.questionType === 'TEXT_ANSWER') {
      emit('update:metadata', { correctAnswer: payload.blanks[0]?.correctAnswers.join('|') || '', explanation: payload.explanation, caseSensitive: false })
  } else {
      emit('update:metadata', payload)
  }
}
</script>

<style scoped>
/* Word Bank Box Style */
.word-bank-box {
  background-color: var(--el-fill-color-light); /* Màu nền tự động */
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 12px;
}
.word-bank-box .header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
}
.word-bank-box .title {
  font-weight: bold; font-size: 14px; color: var(--el-text-color-primary);
}

.dashed-btn { border-style: dashed; }
.w-full { width: 100%; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.blank-header { display: flex; justify-content: space-between; align-items: center; }

/* Dark Mode Overrides */
html.dark .word-bank-box {
  background-color: var(--el-bg-color-overlay);
  border-color: var(--el-border-color-darker);
}
</style>
