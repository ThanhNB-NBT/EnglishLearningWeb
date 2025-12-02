<template>
  <div class="sentence-building-form">
    <el-alert title="Cách hoạt động" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        1. Nhập câu hoàn chỉnh vào ô bên dưới.<br>
        2. Hệ thống sẽ tự động tách câu thành các từ/cụm từ.<br>
        3. Bạn có thể chỉnh sửa, xóa hoặc thêm các từ gây nhiễu (distractors).
      </template>
    </el-alert>

    <el-form-item label="Câu hoàn chỉnh (Đáp án đúng)" required>
      <el-input v-model="fullSentence" placeholder="VD: I go to school by bus" clearable @change="generateWords">
        <template #append>
          <el-button @click="generateWords">Tách từ tự động</el-button>
        </template>
      </el-input>
    </el-form-item>

    <el-form-item label="Các thành phần (Từ/Cụm từ)" required>
      <div class="words-container">
        <el-tag v-for="(word, index) in localMetadata.words" :key="index" closable size="large" effect="plain"
          class="word-tag" @close="removeWord(index)">
          <span contenteditable @blur="(e) => updateWord(index, e.target.innerText)">
            {{ word }}
          </span>
        </el-tag>

        <el-input v-if="inputVisible" ref="InputRef" v-model="inputValue" class="input-new-word" size="small"
          @keyup.enter="handleInputConfirm" @blur="handleInputConfirm" />
        <el-button v-else class="button-new-word" size="small" @click="showInput">
          + Thêm từ
        </el-button>
      </div>
      <div class="text-xs text-gray-400 mt-2">
        * Mẹo: Click vào từ để sửa nội dung. Thêm các từ sai để tăng độ khó.
      </div>
    </el-form-item>

    <el-form-item label="Giải thích (Optional)">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2"
        placeholder="Giải thích cấu trúc ngữ pháp..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

const fullSentence = ref('')
const inputVisible = ref(false)
const inputValue = ref('')
const InputRef = ref(null)

const localMetadata = ref({
  words: props.metadata?.words || [],
  correctSentence: props.metadata?.correctSentence || '',
  explanation: props.metadata?.explanation || ''
})

// Sync props
watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value.words = newVal.words || []
    localMetadata.value.explanation = newVal.explanation || ''
    // Nếu có câu đúng lưu sẵn thì hiện lại
    if (newVal.correctSentence) fullSentence.value = newVal.correctSentence
  }
}, { deep: true })

// Logic tách từ
const generateWords = () => {
  if (!fullSentence.value.trim()) return

  // Tách theo khoảng trắng, lọc bỏ rỗng
  const words = fullSentence.value.trim().split(/\s+/).filter(w => w)
  localMetadata.value.words = words
  localMetadata.value.correctSentence = fullSentence.value
  emitUpdate()
}

// Logic Tag Input
const removeWord = (index) => {
  localMetadata.value.words.splice(index, 1)
  emitUpdate()
}

const updateWord = (index, newValue) => {
  if (newValue.trim()) {
    localMetadata.value.words[index] = newValue.trim()
  } else {
    removeWord(index)
  }
  emitUpdate()
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => InputRef.value.input.focus())
}

const handleInputConfirm = () => {
  if (inputValue.value) {
    localMetadata.value.words.push(inputValue.value)
    emitUpdate()
  }
  inputVisible.value = false
  inputValue.value = ''
}

const emitUpdate = () => {
  // Cập nhật lại correctSentence nếu user sửa input
  localMetadata.value.correctSentence = fullSentence.value
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.sentence-building-form {
  padding: 10px 0;
}

.words-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  min-height: 60px;
  background-color: var(--el-fill-color-blank);
}

.word-tag {
  cursor: pointer;
  user-select: none;
}

.input-new-word {
  width: 100px;
}

.button-new-word {
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
