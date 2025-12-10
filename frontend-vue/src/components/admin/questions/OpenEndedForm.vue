<template>
  <div class="w-full">
    <el-alert
      title="Câu hỏi mở (Speaking/Writing)"
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
    />

    <div class="bg-gray-50 dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-200 dark:border-gray-700 space-y-4">
      <el-form-item label="Gợi ý câu trả lời (Suggested Answer)">
        <el-input
          v-model="localMetadata.suggestedAnswer"
          type="textarea"
          :rows="4"
          placeholder="Nhập dàn ý hoặc câu trả lời mẫu để người học tham khảo..."
          @input="emitUpdate"
        />
      </el-form-item>

      <el-form-item label="Lưu ý chấm điểm">
        <el-input
          v-model="localMetadata.explanation"
          type="textarea"
          :rows="2"
          placeholder="Các tiêu chí đánh giá (Grammar, Vocabulary...)"
          @input="emitUpdate"
        />
      </el-form-item>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({ suggestedAnswer: '', explanation: '' })

watch(() => props.metadata, (newVal) => {
  if (newVal) localMetadata.value = { ...newVal }
}, { immediate: true, deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>
