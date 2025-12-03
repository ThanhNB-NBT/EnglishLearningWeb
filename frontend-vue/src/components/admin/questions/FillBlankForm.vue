<template>
  <div class="short-answer-form">
    <el-alert title="Hướng dẫn" type="info" :closable="false" class="mb-4" show-icon>
      <template #default>
        <div>
          - Với <b>Điền từ</b>: Nhập các đáp án tương ứng với thứ tự chỗ trống trong đề bài.
          <br />
          - Mỗi chỗ trống có thể có nhiều đáp án đúng (cách nhau bằng phím Enter).
        </div>
      </template>
    </el-alert>

    <el-form-item label="Cấu hình đáp án" required>
      <div class="blanks-container">
        <transition-group name="list">
          <div v-for="(blank, index) in localMetadata.blanks" :key="index" class="blank-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }">
              <div class="blank-header mb-2">
                <el-tag effect="dark" size="small">Vị trí #{{ index + 1 }}</el-tag>
                <el-icon v-if="blank.correctAnswers.length === 0" color="#F56C6C" class="warning-icon">
                    <WarningFilled />
                  </el-icon>
                <el-button type="danger" link :icon="Delete" @click="removeBlank(index)"
                  :disabled="localMetadata.blanks.length <= 1">
                  Xóa
                </el-button>
              </div>

              <el-select v-model="blank.correctAnswers" multiple filterable allow-create default-first-option
                :reserve-keyword="false" placeholder="Nhập đáp án đúng rồi ấn Enter..." style="width: 100%"
                @change="emitUpdate">
                <template #empty>
                  <div class="p-2 text-gray-400 text-xs text-center">
                    Gõ đáp án và ấn Enter để thêm
                  </div>
                </template>
              </el-select>
              <div v-if="blank.correctAnswers.length === 0" class="error-hint">
                Chưa có đáp án nào.  Vui lòng nhập ít nhất 1 đáp án đúng.
              </div>
              <div class="text-xs text-gray-400 mt-1">
                * Chấp nhận nhiều đáp án (VD: "don't", "do not")
              </div>
            </el-card>
          </div>
        </transition-group>

        <el-button type="primary" plain :icon="Plus" class="w-full mt-2 dashed-btn" @click="addBlank">
          Thêm chỗ trống / Câu trả lời
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="Giải thích (Optional)">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2" placeholder="Giải thích chi tiết..."
        @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus, Delete, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

// Init data: Mặc định có 1 chỗ trống
const localMetadata = ref({
  blanks: props.metadata?.blanks || [
    { position: 1, correctAnswers: [] }
  ],
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    localMetadata.value = {
      blanks: newVal.blanks || [{ position: 1, correctAnswers: [] }],
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

// Actions
const addBlank = () => {
  localMetadata.value.blanks.push({
    position: localMetadata.value.blanks.length + 1,
    correctAnswers: []
  })
  emitUpdate()
}

const removeBlank = (index) => {
  if (localMetadata.value.blanks.length <= 1) return
  localMetadata.value.blanks.splice(index, 1)
  // Re-index position
  localMetadata.value.blanks.forEach((b, idx) => b.position = idx + 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.short-answer-form {
  padding: 10px 0;
}

.blank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.mb-3 {
  margin-bottom: 12px;
}

.mt-1 {
  margin-top: 4px;
}

.p-2 {
  padding: 8px;
}

/* Transition */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.error-border {
  border: 2px solid #F56C6C !important;
  animation: shake 0.3s;
}

.error-hint {
  margin-top: 8px;
  padding: 6px 12px;
  background: #FEF0F0;
  border-left: 3px solid #F56C6C;
  color: #F56C6C;
  font-size: 12px;
  border-radius: 4px;
}

.is-error :deep(.el-select__wrapper) {
  border-color: #F56C6C !important;
  box-shadow: 0 0 0 1px #F56C6C inset ! important;
}

.warning-icon {
  animation: pulse 1. 5s infinite;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.flex { display: flex; }
.items-center { align-items: center; }
.gap-2 { gap: 8px; }
</style>
