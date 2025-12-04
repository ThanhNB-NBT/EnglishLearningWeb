<template>
  <div class="multiple-choice-form">

    <div v-if="isTrueFalseMode" class="true-false-section mb-4">
      <el-alert title="Chế độ Đúng / Sai" type="info" :closable="false" class="mb-3" />

      <el-form-item label="Đáp án đúng là:" required>
        <el-radio-group v-model="trueFalseValue" @change="emitTrueFalseUpdate" size="large">
          <el-radio :label="true" border class="true-radio">
            <el-icon class="mr-1">
              <Check />
            </el-icon> TRUE (Đúng)
          </el-radio>
          <el-radio :label="false" border class="false-radio">
            <el-icon class="mr-1">
              <Close />
            </el-icon> FALSE (Sai)
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </div>

    <el-form-item v-else label="Danh sách đáp án" required class="mb-0">
      <div class="options-grid">
        <div v-for="(option, index) in localMetadata.options" :key="index" class="option-item">
          <el-card shadow="never" :body-style="{ padding: '10px' }" class="option-card">
            <div class="option-row">
              <div class="option-check">
                <el-radio v-model="correctAnswerIndex" :label="index" @change="handleCorrectChange"><span
                    style="display:none">.</span></el-radio>
              </div>

              <div class="option-index">
                <el-tag effect="dark" :type="correctAnswerIndex === index ? 'success' : 'info'" size="small">
                  {{ String.fromCharCode(65 + index) }}
                </el-tag>
              </div>

              <div class="option-content">
                <el-input v-model="option.text" placeholder="Nhập đáp án..." @input="emitUpdate" />
              </div>

              <el-button type="danger" link icon="Close" @click="removeOption(index)"
                :disabled="localMetadata.options.length <= 2" />
            </div>
          </el-card>
        </div>

        <el-button type="primary" plain icon="Plus" @click="addOption" class="add-btn">
          Thêm lựa chọn
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="Giải thích đáp án" class="mt-4">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2"
        placeholder="Giải thích tại sao đáp án này đúng..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { Plus, Close, Check } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) },
  questionType: { type: String, default: 'MULTIPLE_CHOICE' }
})
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({ explanation: '', options: [] })
const correctAnswerIndex = ref(-1)

// State riêng cho True/False
const trueFalseValue = ref(true)

const isTrueFalseMode = computed(() => props.questionType === 'TRUE_FALSE')

const initData = () => {
  const meta = props.metadata || {}
  localMetadata.value.explanation = meta.explanation || ''

  if (isTrueFalseMode.value) {
    // Logic load dữ liệu True/False từ Backend (Backend lưu dưới dạng options)
    // Ta cần tìm xem option 'True' có isCorrect=true không
    if (meta.options && meta.options.length > 0) {
      const trueOpt = meta.options.find(o => o.text === 'True')
      if (trueOpt) trueFalseValue.value = trueOpt.isCorrect
    } else if (meta.correctAnswer !== undefined) {
      // Support dữ liệu legacy
      trueFalseValue.value = meta.correctAnswer
    }
  } else {
    // Logic cũ cho Multiple Choice
    if (meta.options && meta.options.length > 0) {
      localMetadata.value.options = meta.options
    } else {
      localMetadata.value.options = Array.from({ length: 4 }, (_, i) => ({ text: '', isCorrect: false, order: i + 1 }))
    }
    correctAnswerIndex.value = localMetadata.value.options.findIndex(o => o.isCorrect)
  }
}

watch(() => props.metadata, initData, { immediate: true, deep: true })
watch(() => props.questionType, initData)

// --- XỬ LÝ TRUE/FALSE ---
const emitTrueFalseUpdate = () => {
  // Tự động tạo cấu trúc Options chuẩn để gửi về Backend
  const tfOptions = [
    { text: 'True', isCorrect: trueFalseValue.value === true, order: 1 },
    { text: 'False', isCorrect: trueFalseValue.value === false, order: 2 }
  ]

  emit('update:metadata', {
    explanation: localMetadata.value.explanation,
    options: tfOptions,
    // Gửi kèm field legacy để đề phòng
    correctAnswer: trueFalseValue.value
  })
}

// --- XỬ LÝ MULTIPLE CHOICE ---
const addOption = () => {
  localMetadata.value.options.push({ text: '', isCorrect: false, order: localMetadata.value.options.length + 1 })
  emitUpdate()
}
const removeOption = (idx) => {
  localMetadata.value.options.splice(idx, 1)
  if (correctAnswerIndex.value === idx) correctAnswerIndex.value = -1
  else if (correctAnswerIndex.value > idx) correctAnswerIndex.value--
  emitUpdate()
}
const handleCorrectChange = () => {
  localMetadata.value.options.forEach((opt, idx) => { opt.isCorrect = (idx === correctAnswerIndex.value) })
  emitUpdate()
}

const emitUpdate = () => {
  if (isTrueFalseMode.value) {
    emitTrueFalseUpdate()
  } else {
    emit('update:metadata', { ...localMetadata.value })
  }
}
</script>

<style scoped>
.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.option-card {
  width: 100%;
  border: 1px solid #dcdfe6;
  transition: all 0.2s;
}

.option-card:hover {
  border-color: var(--el-color-primary);
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-content {
  flex: 1;
}

.option-check :deep(.el-radio) {
  margin-right: 0;
}

.add-btn {
  grid-column: 1 / -1;
  border-style: dashed;
  margin-top: 5px;
}

/* True/False Styles */
.true-radio :deep(.el-radio__label) {
  color: #67c23a;
  font-weight: bold;
}

.false-radio :deep(.el-radio__label) {
  color: #f56c6c;
  font-weight: bold;
}

:deep(.true-radio.is-checked) {
  background-color: #f0f9eb;
  border-color: #67c23a;
}

:deep(.false-radio.is-checked) {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

@media (max-width: 600px) {
  .options-grid {
    grid-template-columns: 1fr;
  }
}
</style>
