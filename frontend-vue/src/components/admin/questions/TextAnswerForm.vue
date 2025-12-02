<template>
  <div class="text-answer-form">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="Số từ tối thiểu">
          <el-input-number v-model="localMetadata.minWords" :min="0" style="width: 100%" @change="emitUpdate" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="Số từ tối đa">
          <el-input-number v-model="localMetadata.maxWords" :min="0" style="width: 100%" @change="emitUpdate" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="Bài mẫu tham khảo (Sample Answer)">
      <el-input v-model="localMetadata.sampleAnswer" type="textarea" :rows="6"
        placeholder="Nhập bài viết mẫu để người học tham khảo sau khi nộp bài..." @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Tiêu chí chấm điểm (Guideline)">
      <el-input v-model="localMetadata.gradingCriteria" type="textarea" :rows="3"
        placeholder="Các tiêu chí để chấm điểm (VD: Ngữ pháp, Từ vựng, Bố cục)..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  minWords: props.metadata?.minWords || 0,
  maxWords: props.metadata?.maxWords || 500,
  sampleAnswer: props.metadata?.sampleAnswer || '',
  gradingCriteria: props.metadata?.gradingCriteria || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) localMetadata.value = { ...newVal }
}, { deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.text-answer-form {
  padding: 10px 0;
}
</style>
