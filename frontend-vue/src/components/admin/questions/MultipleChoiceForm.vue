<template>
  <div class="multiple-choice-form">
    <el-form-item label="Gợi ý (Không bắt buộc)">
      <el-input v-model="localMetadata.hint" type="textarea" :rows="2"
        placeholder="Nhập gợi ý giúp người học trả lời câu hỏi..." maxlength="200" show-word-limit
        @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Danh sách đáp án" required class="mb-0">
      <div class="options-container">
        <transition-group name="list">
          <div v-for="(option, index) in localMetadata.options" :key="index" class="option-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }" class="option-card">
              <div class="option-row">
                <div class="option-index">
                  <el-tag effect="plain" size="small" type="info">{{ String.fromCharCode(65 + index) }}</el-tag>
                </div>

                <div class="option-content">
                  <el-input v-model="option.text" placeholder="Nhập nội dung đáp án..." @input="emitUpdate">
                    <template #prefix>
                      <el-icon class="text-gray-400">
                        <EditPen />
                      </el-icon>
                    </template>
                  </el-input>
                </div>

                <div class="option-actions">
                  <el-tooltip content="Đánh dấu là đáp án đúng" placement="top">
                    <el-checkbox v-model="option.isCorrect" label="Đúng" border
                      :class="{ 'is-checked-success': option.isCorrect }" @change="handleCorrectChange(index)" />
                  </el-tooltip>

                  <el-tooltip content="Xóa đáp án" placement="top">
                    <el-button type="danger" plain :icon="Delete" circle @click="removeOption(index)"
                      :disabled="localMetadata.options.length <= 2" />
                  </el-tooltip>
                </div>
              </div>
            </el-card>
          </div>
        </transition-group>

        <el-button type="primary" plain :icon="Plus" @click="addOption" class="w-full mt-2 dashed-btn">
          Thêm đáp án khác
        </el-button>
      </div>
    </el-form-item>

    <transition name="el-fade-in">
      <el-alert v-if="validationError" :title="validationError" type="error" show-icon :closable="false" class="mt-4" />
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Delete, EditPen } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:metadata'])

// Init data
const localMetadata = ref({
  hint: props.metadata?.hint || '',
  options: props.metadata?.options?.length > 0 ? props.metadata.options : [
    { text: '', isCorrect: true, order: 1 }, // Default cái đầu đúng cho nhanh
    { text: '', isCorrect: false, order: 2 },
    { text: '', isCorrect: false, order: 3 },
    { text: '', isCorrect: false, order: 4 },
  ],
})

watch(() => props.metadata, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    localMetadata.value = {
      hint: newVal.hint || '',
      options: newVal.options || localMetadata.value.options,
    }
  }
}, { deep: true })

// Validation Logic
const validationError = computed(() => {
  const options = localMetadata.value.options
  if (!options || options.length < 2) return 'Cần ít nhất 2 đáp án.'
  if (!options.some(opt => opt.isCorrect)) return 'Phải chọn ít nhất một đáp án đúng.'
  if (options.some(opt => !opt.text || opt.text.trim() === '')) return 'Nội dung đáp án không được để trống.'
  return null
})

// Actions
const addOption = () => {
  localMetadata.value.options.push({
    text: '',
    isCorrect: false,
    order: localMetadata.value.options.length + 1,
  })
  emitUpdate()
}

const removeOption = (index) => {
  if (localMetadata.value.options.length <= 2) return
  localMetadata.value.options.splice(index, 1)
  // Re-index order
  localMetadata.value.options.forEach((opt, idx) => opt.order = idx + 1)
  emitUpdate()
}

const handleCorrectChange = (index) => {
  // Logic Radio: Chỉ 1 đáp án đúng
  const clicked = localMetadata.value.options[index]
  if (clicked.isCorrect) {
    localMetadata.value.options.forEach((opt, idx) => {
      if (idx !== index) opt.isCorrect = false
    })
  }
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
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

.mt-4 {
  margin-top: 16px;
}

.mb-3 {
  margin-bottom: 12px;
}

/* Custom Checkbox Style for Correct Answer */
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

/* Animation List */
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
