<template>
  <div class="reading-comprehension-form">
    <el-tabs type="border-card" class="mb-4">
      <el-tab-pane label="Nội dung bài đọc">
        <el-alert title="Lưu ý" type="info" :closable="false" class="mb-2">
          Nội dung bài đọc chính nên nhập ở Bước 1 ("Nội dung câu hỏi").
          Nếu bài đọc quá dài hoặc muốn tách biệt, bạn có thể nhập thêm vào đây.
        </el-alert>

        <el-input v-model="localMetadata.passage" type="textarea" :rows="10"
          placeholder="Paste nội dung bài đọc vào đây..." @input="emitUpdate" />
      </el-tab-pane>

      <el-tab-pane label="Câu hỏi phụ (Sub-questions)">
        <div class="sub-questions-container">
          <div v-for="(question, qIndex) in localMetadata.subQuestions" :key="qIndex" class="sub-question-item mb-4">
            <el-card shadow="hover">
              <template #header>
                <div class="flex justify-between items-center">
                  <span class="font-bold">Câu hỏi #{{ qIndex + 1 }}</span>
                  <el-button type="danger" icon="Delete" circle size="small" @click="removeSubQuestion(qIndex)" />
                </div>
              </template>

              <el-input v-model="question.questionText" placeholder="Nội dung câu hỏi..." class="mb-2"
                @input="emitUpdate" />

              <div class="options-list pl-4 border-l-2 border-gray-200">
                <div v-for="(opt, oIndex) in question.options" :key="oIndex" class="flex gap-2 mb-2 items-center">
                  <el-radio v-model="question.correctAnswerIndex" :label="oIndex" @change="emitUpdate">
                    {{ String.fromCharCode(65 + oIndex) }}
                  </el-radio>
                  <el-input v-model="opt.text" placeholder="Đáp án..." size="small" @input="emitUpdate" />
                </div>
              </div>
            </el-card>
          </div>

          <el-button type="primary" plain class="w-full" icon="Plus" @click="addSubQuestion">Thêm câu hỏi
            phụ</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  passage: props.metadata?.passage || '',
  subQuestions: props.metadata?.subQuestions || []
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value.passage = newVal.passage || ''
    localMetadata.value.subQuestions = newVal.subQuestions || []
  }
}, { deep: true })

const addSubQuestion = () => {
  localMetadata.value.subQuestions.push({
    questionText: '',
    options: [{ text: '' }, { text: '' }, { text: '' }, { text: '' }],
    correctAnswerIndex: 0
  })
  emitUpdate()
}

const removeSubQuestion = (index) => {
  localMetadata.value.subQuestions.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.reading-comprehension-form {
  padding: 10px 0;
}

.w-full {
  width: 100%;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.pl-4 {
  padding-left: 16px;
}

.border-l-2 {
  border-left-width: 2px;
}

.flex {
  display: flex;
}

.justify-between {
  justify-content: space-between;
}

.items-center {
  align-items: center;
}

.gap-2 {
  gap: 8px;
}

.font-bold {
  font-weight: bold;
}
</style>
