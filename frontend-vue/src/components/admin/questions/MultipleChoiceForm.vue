<template>
  <div class="multiple-choice-form">
    <el-form-item label="Giải thích đáp án">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2" placeholder="Giải thích chi tiết..."
        @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Danh sách đáp án" required class="mb-0">
      <div class="options-container">
        <transition-group name="list">
          <div v-for="(option, index) in localMetadata.options" :key="index" class="option-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }">
              <div class="option-row">
                <div class="option-index"><el-tag effect="plain" size="small">{{ String.fromCharCode(65 + index)
                    }}</el-tag></div>
                <div class="option-content">
                  <el-input v-model="option.text" placeholder="Nhập đáp án..." @input="emitUpdate">
                    <template #prefix><el-icon class="text-gray-400">
                        <EditPen />
                      </el-icon></template>
                  </el-input>
                </div>
                <div class="option-actions">
                  <el-checkbox v-model="option.isCorrect" label="Đúng" border
                    :class="{ 'is-checked-success': option.isCorrect }" @change="handleCorrectChange(index)" />
                  <el-button type="danger" plain icon="Delete" circle @click="removeOption(index)"
                    :disabled="localMetadata.options.length <= 2" />
                </div>
              </div>
            </el-card>
          </div>
        </transition-group>
        <el-button type="primary" plain icon="Plus" @click="addOption" class="w-full mt-2 dashed-btn">Thêm đáp
          án</el-button>
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { EditPen } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  explanation: props.metadata?.explanation || '',
  options: props.metadata?.options || [
    { text: '', isCorrect: true, order: 1 },
    { text: '', isCorrect: false, order: 2 },
    { text: '', isCorrect: false, order: 3 },
    { text: '', isCorrect: false, order: 4 }
  ]
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value.explanation = newVal.explanation || ''
    localMetadata.value.options = newVal.options || localMetadata.value.options
  }
}, { deep: true })

const addOption = () => {
  localMetadata.value.options.push({ text: '', isCorrect: false, order: localMetadata.value.options.length + 1 })
  emitUpdate()
}

const removeOption = (index) => {
  if (localMetadata.value.options.length <= 2) return
  localMetadata.value.options.splice(index, 1)
  localMetadata.value.options.forEach((opt, idx) => opt.order = idx + 1)
  emitUpdate()
}

const handleCorrectChange = (index) => {
  if (localMetadata.value.options[index].isCorrect) {
    localMetadata.value.options.forEach((opt, idx) => { if (idx !== index) opt.isCorrect = false })
  }
  emitUpdate()
}

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.multiple-choice-form {
  padding: 10px 0;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-content {
  flex: 1;
}

.option-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.w-full {
  width: 100%;
}

.mt-2 {
  margin-top: 8px;
}

.mb-3 {
  margin-bottom: 12px;
}

:deep(.is-checked-success .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #67c23a;
  border-color: #67c23a;
}

:deep(.is-checked-success .el-checkbox__label) {
  color: #67c23a;
  font-weight: bold;
}

.dashed-btn {
  border-style: dashed;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
