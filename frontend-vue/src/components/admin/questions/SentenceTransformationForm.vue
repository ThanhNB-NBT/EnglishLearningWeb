<template>
  <div class="w-full space-y-4">
    <el-form-item label="Câu gốc (Original Sentence)">
      <el-input v-model="localMetadata.originalSentence" placeholder="VD: It is a pity I didn't see him."
        @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Gợi ý đầu câu (Beginning Phrase)">
      <el-input v-model="localMetadata.beginningPhrase" placeholder="VD: I wish" @input="emitUpdate" />
    </el-form-item>

    <el-form-item label="Các đáp án đúng">
      <div v-for="(ans, index) in localMetadata.correctAnswers" :key="index" class="flex gap-2 mb-2">
        <el-input v-model="localMetadata.correctAnswers[index]" placeholder="Nhập câu hoàn chỉnh..."
          @input="emitUpdate">
          <template #prefix><el-icon class="text-green-500">
              <Check />
            </el-icon></template>
        </el-input>
        <el-button type="danger" circle plain icon="Delete" @click="removeAnswer(index)"
          :disabled="localMetadata.correctAnswers.length <= 1" />
      </div>
      <el-button link type="primary" @click="addAnswer">+ Thêm đáp án khác</el-button>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Check, Delete } from '@element-plus/icons-vue'
const props = defineProps({ metadata: Object })
const emit = defineEmits(['update:metadata'])
const localMetadata = ref({ originalSentence: '', beginningPhrase: '', correctAnswers: [''] })

watch(() => props.metadata, (val) => {
  if (val) localMetadata.value = {
    originalSentence: val.originalSentence || '',
    beginningPhrase: val.beginningPhrase || '',
    correctAnswers: val.correctAnswers?.length ? val.correctAnswers : ['']
  }
}, { immediate: true })

const addAnswer = () => localMetadata.value.correctAnswers.push('')
const removeAnswer = (idx) => { localMetadata.value.correctAnswers.splice(idx, 1); emitUpdate() }
const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>
