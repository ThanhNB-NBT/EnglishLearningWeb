<template>
  <div class="w-full">
    <div
      v-if="questionType === 'FILL_BLANK'"
      class="mb-4 p-3 bg-gray-50 dark:bg-[#2c2c2c] rounded-lg border border-gray-200 dark:border-gray-700 flex justify-between items-center"
    >
      <span class="text-sm font-bold text-gray-700 dark:text-gray-300"
        >Ngân hàng từ (Word Bank)</span
      >
      <el-switch v-model="hasWordBank" active-text="Bật" inactive-text="Tắt" />
    </div>

    <div v-if="hasWordBank && questionType === 'FILL_BLANK'" class="mb-4">
      <el-select
        v-model="localMetadata.wordBank"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        placeholder="Nhập kho từ "
        class="!w-full"
        @change="emitUpdate"
      />
    </div>

    <div class="space-y-3">
      <div
        v-for="(blank, index) in localMetadata.blanks"
        :key="index"
        class="bg-white dark:bg-[#2c2c2c] p-3 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm relative group"
      >
        <div class="flex justify-between items-center mb-2">
          <el-tag effect="dark" size="small" type="success" class="!rounded"
            >Vị trí #{{ blank.position }}</el-tag
          >
          <el-button
            type="danger"
            link
            size="small"
            @click="removeBlank(index)"
            :disabled="localMetadata.blanks.length <= 1"
            >Xóa</el-button
          >
        </div>

        <el-select
          v-model="blank.correctAnswers"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="Nhập đáp án đúng rồi Enter (VD: goes)..."
          class="!w-full"
          @change="emitUpdate"
        />
      </div>

      <el-button type="primary" plain class="w-full mt-2 border-dashed" @click="addBlank">
        + Thêm chỗ trống
      </el-button>
    </div>
  </div>
</template>

<script setup>
// (Giữ nguyên script cũ)
import { ref, watch } from 'vue'
const props = defineProps({ metadata: Object, questionType: String })
const emit = defineEmits(['update:metadata'])
const hasWordBank = ref(false)
const localMetadata = ref({ blanks: [], wordBank: [] })

// Init logic (Simplified for brevity)
const initData = () => {
  const meta = props.metadata || {}
  localMetadata.value.wordBank = meta.wordBank || []
  hasWordBank.value = localMetadata.value.wordBank.length > 0
  // Logic map blanks tương tự file cũ
  if (props.questionType === 'TEXT_ANSWER' && meta.correctAnswer) {
    localMetadata.value.blanks = [{ position: 1, correctAnswers: meta.correctAnswer.split('|') }]
  } else {
    localMetadata.value.blanks = meta.blanks || [{ position: 1, correctAnswers: [] }]
  }
}
watch(() => props.metadata, initData, { immediate: true, deep: true })

const addBlank = () => {
  localMetadata.value.blanks.push({
    position: localMetadata.value.blanks.length + 1,
    correctAnswers: [],
  })
  emitUpdate()
}
const removeBlank = (idx) => {
  localMetadata.value.blanks.splice(idx, 1)
  emitUpdate()
}
const emitUpdate = () => {
  // Logic emit tương tự file cũ
  const payload = {
    blanks: localMetadata.value.blanks,
    wordBank: hasWordBank.value ? localMetadata.value.wordBank : [],
  }
  if (props.questionType === 'TEXT_ANSWER') {
    emit('update:metadata', { correctAnswer: payload.blanks[0]?.correctAnswers.join('|') || '' })
  } else {
    emit('update:metadata', payload)
  }
}
</script>
