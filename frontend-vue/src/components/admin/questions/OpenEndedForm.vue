<template>
  <div class="open-ended-form">
    <el-alert title="Câu hỏi mở" type="info" :closable="false" class="mb-4" show-icon>
      <template #default>
        Dạng câu hỏi này không có đáp án đúng sai tuyệt đối (VD: Speaking, Interview).<br>
        Bạn có thể cung cấp gợi ý trả lời để người học tham khảo.
      </template>
    </el-alert>

    <el-form-item label="Gợi ý trả lời (Suggested Answer)">
      <el-input v-model="localMetadata.suggestedAnswer" type="textarea" :rows="4"
        placeholder="Nhập các ý chính hoặc câu trả lời mẫu..." @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Ghi chú cho người chấm (Optional)">
      <el-input v-model="localMetadata.gradingNote" type="textarea" :rows="2" placeholder="Lưu ý khi chấm điểm..."
        @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  suggestedAnswer: props.metadata?.suggestedAnswer || '',
  gradingNote: props.metadata?.gradingNote || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) localMetadata.value = { ...newVal }
}, { deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.open-ended-form {
  padding: 10px 0;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
