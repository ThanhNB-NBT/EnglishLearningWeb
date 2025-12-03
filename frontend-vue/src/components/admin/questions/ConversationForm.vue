<template>
  <div class="conversation-form">
    <el-alert title="Hướng dẫn tạo câu hỏi hội thoại" type="success" :closable="false" class="mb-4">
      Nhập ngữ cảnh hội thoại, các lựa chọn để hoàn thành câu, và chọn đáp án đúng.
    </el-alert>

    <!-- Ngữ cảnh hội thoại -->
    <el-form-item label="Ngữ cảnh hội thoại" required>
      <el-input
        v-model="localMetadata.conversationContext"
        type="textarea"
        :rows="4"
        placeholder="VD: A: Hi, how are you?&#10;B: I'm fine, thanks. _____&#10;A: That's great!"
        @input="emitUpdate"
      >
        <template #prepend>
          <el-icon><ChatDotRound /></el-icon>
        </template>
      </el-input>
      <div class="text-xs text-gray-400 mt-1">
        * Dùng <b>_____</b> để đánh dấu chỗ trống cần điền
      </div>
    </el-form-item>

    <!-- Các lựa chọn -->
    <el-form-item label="Các lựa chọn (Options)" required>
      <div class="options-container">
        <transition-group name="list">
          <div v-for="(opt, idx) in localMetadata.options" :key="idx" class="option-row mb-2">
            <el-tag effect="plain" class="option-label">{{ String.fromCharCode(65 + idx) }}</el-tag>
            <el-input
              v-model="localMetadata.options[idx]"
              placeholder="Nhập lựa chọn..."
              @input="emitUpdate"
            />
            <el-button
              type="danger"
              icon="Delete"
              circle
              plain
              size="small"
              @click="removeOption(idx)"
              :disabled="localMetadata.options.length <= 2"
            />
          </div>
        </transition-group>

        <el-button type="primary" plain icon="Plus" class="w-full mt-2" @click="addOption">
          Thêm lựa chọn
        </el-button>
      </div>
    </el-form-item>

    <!-- Đáp án đúng -->
    <el-form-item label="Đáp án đúng" required>
      <el-select
        v-model="localMetadata.correctAnswer"
        placeholder="Chọn đáp án đúng"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option
          v-for="(opt, idx) in localMetadata.options"
          :key="idx"
          :label="`${String.fromCharCode(65 + idx)}. ${opt}`"
          :value="opt"
          :disabled="!opt.trim()"
        />
      </el-select>
    </el-form-item>

    <!-- Giải thích -->
    <el-form-item label="Giải thích / Dịch nghĩa">
      <el-input
        v-model="localMetadata.explanation"
        type="textarea"
        :rows="2"
        placeholder="Giải thích đáp án hoặc dịch đoạn hội thoại..."
        @input="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  conversationContext: props.metadata?.conversationContext || '',
  options: props.metadata?.options || ['', '', '', ''],
  correctAnswer: props.metadata?.correctAnswer || '',
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    localMetadata.value = {
      conversationContext: newVal.conversationContext || '',
      options: newVal.options || ['', '', '', ''],
      correctAnswer: newVal.correctAnswer || '',
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

const addOption = () => {
  localMetadata.value.options.push('')
  emitUpdate()
}

const removeOption = (index) => {
  if (localMetadata.value.options.length <= 2) return

  const removedOption = localMetadata.value.options[index]
  localMetadata.value.options.splice(index, 1)

  // Nếu xóa đáp án đang được chọn, reset correctAnswer
  if (localMetadata.value.correctAnswer === removedOption) {
    localMetadata.value.correctAnswer = ''
  }

  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.conversation-form {
  padding: 10px 0;
}

.options-container {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-label {
  min-width: 32px;
  text-align: center;
  font-weight: bold;
}

.w-full { width: 100%; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }

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
