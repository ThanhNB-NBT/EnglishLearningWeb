<template>
  <div class="text-answer-form">
    <el-alert title="Hướng dẫn" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        Sử dụng cho câu hỏi yêu cầu người dùng nhập chính xác một từ hoặc cụm từ.<br>
        Có thể nhập nhiều đáp án đúng, ngăn cách bằng dấu gạch đứng "<b>|</b>". Ví dụ: <b>color|colour</b>
      </template>
    </el-alert>

    <el-form-item label="Đáp án đúng (Correct Answer)" required>
      <el-input v-model="localMetadata.correctAnswer" placeholder="Nhập đáp án đúng..." @input="emitUpdate">
        <template #append>
          <el-checkbox v-model="localMetadata.caseSensitive" label="Phân biệt hoa/thường" @change="emitUpdate" />
        </template>
      </el-input>
    </el-form-item>

    <el-form-item label="Giải thích đáp án">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="3" placeholder="Giải thích chi tiết..."
        @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  correctAnswer: props.metadata?.correctAnswer || '',
  caseSensitive: props.metadata?.caseSensitive || false,
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value = {
      correctAnswer: newVal.correctAnswer || '',
      caseSensitive: newVal.caseSensitive || false,
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.text-answer-form {
  padding: 10px 0;
}
</style>
