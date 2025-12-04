<template>
  <div class="sentence-transformation-form">
    <el-alert title="Hướng dẫn" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        <div>
          - <b>Câu gốc:</b> Câu đề bài (Học viên sẽ nhìn thấy).<br>
          - <b>Gợi ý:</b> Phần bắt đầu câu viết lại (VD: "I wish...").<br>
          - <b>Đáp án:</b> Nhập các câu viết lại hoàn chỉnh được chấp nhận.
        </div>
      </template>
    </el-alert>

    <el-form-item label="Câu gốc (Original Sentence)" required>
      <el-input v-model="localMetadata.originalSentence" placeholder="VD: It is a pity I didn't see him."
        @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Gợi ý đầu câu (Beginning Phrase)">
      <el-input v-model="localMetadata.beginningPhrase" placeholder="VD: I wish" @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Các đáp án đúng (Correct Answers)" required>
      <div class="answers-container">
        <transition-group name="list">
          <div v-for="(ans, index) in localMetadata.correctAnswers" :key="index" class="answer-row mb-2">
            <el-input v-model="localMetadata.correctAnswers[index]" placeholder="Nhập câu hoàn chỉnh..."
              @input="emitUpdate">
              <template #prefix>
                <el-icon class="text-success">
                  <Check />
                </el-icon>
              </template>
            </el-input>
            <el-button type="danger" icon="Delete" circle plain @click="removeAnswer(index)"
              :disabled="localMetadata.correctAnswers.length <= 1" />
          </div>
        </transition-group>

        <el-button type="primary" plain icon="Plus" class="w-full mt-2 dashed-btn" @click="addAnswer">
          Thêm đáp án chấp nhận
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="Giải thích">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2"
        placeholder="Giải thích cấu trúc ngữ pháp..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  originalSentence: props.metadata?.originalSentence || '',
  beginningPhrase: props.metadata?.beginningPhrase || '',
  correctAnswers: props.metadata?.correctAnswers || [''],
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value = {
      originalSentence: newVal.originalSentence || '',
      beginningPhrase: newVal.beginningPhrase || '',
      correctAnswers: newVal.correctAnswers?.length > 0 ? newVal.correctAnswers : [''],
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

const addAnswer = () => {
  localMetadata.value.correctAnswers.push('')
  emitUpdate()
}

const removeAnswer = (index) => {
  if (localMetadata.value.correctAnswers.length <= 1) return
  localMetadata.value.correctAnswers.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.answers-container {
  padding: 12px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.answer-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.text-success {
  color: #67c23a;
}

.w-full {
  width: 100%;
}

.dashed-btn {
  border-style: dashed;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-2 {
  margin-top: 8px;
}
</style>
