<template>
  <div class="pronunciation-form">
    <el-alert title="Hướng dẫn tạo câu hỏi phát âm" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        1. Nhập danh sách từ cần phân loại<br>
        2. Nhập các nhóm âm (categories) - VD: /eɪ/, /æ/, /i:/<br>
        3. Phân loại từng từ vào nhóm âm tương ứng
      </template>
    </el-alert>

    <!-- Danh sách từ -->
    <el-form-item label="Danh sách từ cần phân loại" required>
      <el-select
        v-model="localMetadata.words"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        placeholder="Nhập từ và ấn Enter... (VD: bread, great, eat)"
        style="width: 100%"
        @change="handleWordsChange"
      >
        <template #empty>
          <div class="p-2 text-gray-400 text-xs text-center">
            Gõ từ và ấn Enter để thêm
          </div>
        </template>
      </el-select>
      <div class="text-xs text-gray-400 mt-1">
        * Cần ít nhất 2 từ
      </div>
    </el-form-item>

    <!-- Các nhóm âm -->
    <el-form-item label="Các nhóm âm (Categories)" required>
      <el-select
        v-model="localMetadata.categories"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        placeholder="Nhập ký hiệu âm và ấn Enter... (VD: /eɪ/, /æ/, /i:/)"
        style="width: 100%"
        @change="emitUpdate"
      >
        <template #empty>
          <div class="p-2 text-gray-400 text-xs text-center">
            Gõ ký hiệu âm và ấn Enter
          </div>
        </template>
      </el-select>
      <div class="text-xs text-gray-400 mt-1">
        * Cần ít nhất 2 nhóm âm
      </div>
    </el-form-item>

    <!-- Phân loại (hiển thị khi đã có words và categories) -->
    <el-form-item
      v-if="localMetadata.words.length > 0 && localMetadata.categories.length > 0"
      label="Phân loại các từ"
      required
    >
      <el-card shadow="never" class="classification-card">
        <div v-for="word in localMetadata.words" :key="word" class="classification-row">
          <div class="word-label">
            <el-tag type="primary" effect="plain">{{ word }}</el-tag>
          </div>
          <el-icon class="arrow-icon"><Right /></el-icon>
          <el-select
            :model-value="getClassification(word)"
            @change="(val) => setClassification(word, val)"
            placeholder="Chọn nhóm âm"
            style="flex: 1"
          >
            <el-option
              v-for="cat in localMetadata.categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </div>
      </el-card>
    </el-form-item>

    <!-- Giải thích -->
    <el-form-item label="Giải thích quy tắc phát âm">
      <el-input
        v-model="localMetadata.explanation"
        type="textarea"
        :rows="3"
        placeholder="VD: Tổ hợp 'ea' có 3 cách phát âm chính: /i:/ (eat), /e/ (bread), /eɪ/ (great)..."
        @input="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Right } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  words: props.metadata?.words || [],
  categories: props.metadata?.categories || [],
  classifications: props.metadata?.classifications || [],
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    localMetadata.value = {
      words: newVal.words || [],
      categories: newVal.categories || [],
      classifications: newVal.classifications || [],
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

// Helper: Get classification for a word
const getClassification = (word) => {
  const found = localMetadata.value.classifications.find(c => c.word === word)
  return found ? found.category : ''
}

// Helper: Set classification for a word
const setClassification = (word, category) => {
  const index = localMetadata.value.classifications.findIndex(c => c.word === word)

  if (index !== -1) {
    localMetadata.value.classifications[index].category = category
  } else {
    localMetadata.value.classifications.push({ word, category })
  }

  emitUpdate()
}

// Handle words change - sync classifications
const handleWordsChange = () => {
  // Remove classifications for deleted words
  localMetadata.value.classifications = localMetadata.value.classifications.filter(
    c => localMetadata.value.words.includes(c.word)
  )

  // Add empty classifications for new words
  localMetadata.value.words.forEach(word => {
    if (!localMetadata.value.classifications.find(c => c.word === word)) {
      localMetadata.value.classifications.push({ word, category: '' })
    }
  })

  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.pronunciation-form {
  padding: 10px 0;
}

.classification-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.classification-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.classification-row:last-child {
  border-bottom: none;
}

.word-label {
  min-width: 100px;
}

.arrow-icon {
  color: #909399;
  font-size: 16px;
}

.mb-4 { margin-bottom: 16px; }
.mt-1 { margin-top: 4px; }
</style>
