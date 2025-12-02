<template>
  <div class="true-false-form">
    <el-form-item label="Đáp án đúng là gì?">
      <div class="tf-selector">
        <el-radio-group v-model="localMetadata.correctAnswer" size="large" @change="emitUpdate">
          <el-radio :label="true" border class="tf-radio true-radio">
            <el-icon class="mr-1">
              <Check />
            </el-icon> TRUE (Đúng)
          </el-radio>

          <el-radio :label="false" border class="tf-radio false-radio">
            <el-icon class="mr-1">
              <Close />
            </el-icon> FALSE (Sai)
          </el-radio>
        </el-radio-group>
      </div>
    </el-form-item>

    <el-form-item label="Gợi ý / Giải thích nhanh (Optional)">
      <el-input v-model="localMetadata.explanation" placeholder="Nhập giải thích ngắn gọn..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

// Default is TRUE
const localMetadata = ref({
  correctAnswer: props.metadata?.correctAnswer ?? true,
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value.correctAnswer = newVal.correctAnswer ?? true
    localMetadata.value.explanation = newVal.explanation || ''
  }
}, { deep: true })

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.tf-selector {
  display: flex;
  gap: 20px;
  width: 100%;
}

.tf-radio {
  flex: 1;
  margin-right: 0 !important;
  text-align: center;
  transition: all 0.2s;
}

/* Custom colors for True/False */
:deep(.true-radio.is-checked) {
  background-color: #f0f9eb;
  border-color: #67c23a;
}

:deep(.true-radio.is-checked .el-radio__label) {
  color: #67c23a;
  font-weight: bold;
}

:deep(.false-radio.is-checked) {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

:deep(.false-radio.is-checked .el-radio__label) {
  color: #f56c6c;
  font-weight: bold;
}

.mr-1 {
  margin-right: 4px;
}
</style>
