<template>
  <div class="reading-comprehension-form">
    <el-alert title="Hướng dẫn" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        1. Nhập đoạn văn vào bên dưới<br>
        2. Thêm các câu hỏi trắc nghiệm về đoạn văn (Question text ở Bước 1)<br>
        3. Mỗi câu hỏi có 4 lựa chọn A-B-C-D
      </template>
    </el-alert>

    <!-- Đoạn văn -->
    <el-form-item label="Đoạn văn (Passage)" required>
      <el-input
        v-model="localMetadata.passage"
        type="textarea"
        :rows="8"
        placeholder="Paste đoạn văn tiếng Anh vào đây..."
        @input="emitUpdate"
      />
    </el-form-item>

    <!-- Các câu hỏi -->
    <el-form-item label="Câu hỏi (Blanks)" required>
      <div class="text-xs text-info mb-2">
        ⚠️ Lưu ý: Nội dung câu hỏi (VD: "What is the main idea?") nên nhập ở <b>Bước 1 - Question Text</b>
      </div>

      <div class="blanks-container">
        <transition-group name="list">
          <div v-for="(blank, index) in localMetadata.blanks" :key="index" class="blank-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }">
              <div class="blank-header mb-2">
                <el-tag effect="dark" size="small">Câu {{ blank.position }}</el-tag>
                <el-button
                  type="danger"
                  link
                  icon="Delete"
                  @click="removeBlank(index)"
                  :disabled="localMetadata.blanks.length <= 1"
                >
                  Xóa
                </el-button>
              </div>

              <!-- 4 Options -->
              <div class="options-grid">
                <div v-for="optIdx in 4" :key="optIdx" class="option-input-row">
                  <el-tag size="small" effect="plain">{{ String.fromCharCode(64 + optIdx) }}</el-tag>
                  <el-input
                    v-model="blank.options[optIdx - 1]"
                    placeholder="Nhập lựa chọn..."
                    size="small"
                    @input="emitUpdate"
                  />
                  <el-radio
                    v-model="blank.correctAnswer"
                    :label="blank.options[optIdx - 1]"
                    :disabled="!blank.options[optIdx - 1] || !blank.options[optIdx - 1].trim()"
                    @change="emitUpdate"
                  >
                    Đúng
                  </el-radio>
                </div>
              </div>
            </el-card>
          </div>
        </transition-group>

        <el-button type="primary" plain icon="Plus" class="w-full mt-2" @click="addBlank">
          Thêm câu hỏi
        </el-button>
      </div>
    </el-form-item>

    <!-- Giải thích -->
    <el-form-item label="Giải thích / Dịch đoạn văn">
      <el-input
        v-model="localMetadata.explanation"
        type="textarea"
        :rows="3"
        placeholder="Dịch nghĩa đoạn văn hoặc giải thích đáp án..."
        @input="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  passage: props.metadata?.passage || '',
  blanks: props.metadata?.blanks || [
    { position: 1, options: ['', '', '', ''], correctAnswer: '' }
  ],
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    localMetadata.value = {
      passage: newVal.passage || '',
      blanks: newVal.blanks || [{ position: 1, options: ['', '', '', ''], correctAnswer: '' }],
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

const addBlank = () => {
  const nextPosition = localMetadata.value.blanks.length + 1
  localMetadata.value.blanks.push({
    position: nextPosition,
    options: ['', '', '', ''],
    correctAnswer: ''
  })
  emitUpdate()
}

const removeBlank = (index) => {
  if (localMetadata.value.blanks.length <= 1) return
  localMetadata.value.blanks.splice(index, 1)

  // Re-index positions
  localMetadata.value.blanks.forEach((blank, idx) => {
    blank.position = idx + 1
  })

  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.reading-comprehension-form {
  padding: 10px 0;
}

.blanks-container {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}

.blank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.options-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-input-row .el-tag {
  min-width: 28px;
  text-align: center;
}

.w-full { width: 100%; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mt-2 { margin-top: 8px; }

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
