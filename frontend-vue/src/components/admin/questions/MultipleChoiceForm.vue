<template>
  <div class="w-full">
    <div
      v-if="isTrueFalseMode"
      class="mb-4 bg-blue-50 dark:bg-blue-900/10 p-4 rounded-lg border border-blue-100 dark:border-blue-800"
    >
      <div class="text-sm font-bold text-blue-800 dark:text-blue-300 mb-3">Đáp án đúng là:</div>
      <el-radio-group
        v-model="trueFalseValue"
        @change="emitTrueFalseUpdate"
        class="!flex !flex-col !items-start gap-3"
      >
        <el-radio
          :label="true"
          border
          class="!ml-0 !w-full !mr-0 !bg-white dark:!bg-[#2c2c2c] !border-green-300"
        >
          <span class="text-green-600 font-bold">TRUE (Đúng)</span>
        </el-radio>
        <el-radio
          :label="false"
          border
          class="!ml-0 !w-full !bg-white dark:!bg-[#2c2c2c] !border-red-300"
        >
          <span class="text-red-600 font-bold">FALSE (Sai)</span>
        </el-radio>
      </el-radio-group>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="(opt, index) in localMetadata.options"
        :key="index"
        class="flex items-center gap-3 bg-white dark:bg-[#2c2c2c] p-2 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm group"
      >
        <div class="flex items-center justify-center w-10 shrink-0">
          <el-radio
            v-model="correctAnswerIndex"
            :label="index"
            @change="handleCorrectChange"
            size="large"
          >
            <span class="hidden">.</span>
          </el-radio>
        </div>

        <div
          class="w-8 h-8 rounded bg-gray-100 dark:bg-gray-700 flex items-center justify-center font-bold text-gray-500 dark:text-gray-300 text-sm shrink-0"
        >
          {{ String.fromCharCode(65 + index) }}
        </div>

        <el-input
          v-model="opt.text"
          placeholder="Nhập nội dung đáp án..."
          class="flex-1"
          @input="emitUpdate"
        />

        <el-button
          type="danger"
          circle
          plain
          text
          @click="removeOption(index)"
          :disabled="localMetadata.options.length <= 2"
          class="!p-2 opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <el-button type="primary" plain class="w-full mt-2 border-dashed" @click="addOption">
        + Thêm lựa chọn
      </el-button>
    </div>
  </div>
</template>

<script setup>
// (Giữ nguyên logic script cũ của bạn, chỉ thay đổi template class)
import { ref, watch, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) },
  questionType: { type: String, default: 'MULTIPLE_CHOICE' },
})
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({ options: [] })
const correctAnswerIndex = ref(-1)
const trueFalseValue = ref(true)
const isTrueFalseMode = computed(() => props.questionType === 'TRUE_FALSE')

const initData = () => {
  const meta = props.metadata || {}
  if (isTrueFalseMode.value) {
    if (meta.options?.length) {
      const trueOpt = meta.options.find((o) => o.text === 'True')
      if (trueOpt) trueFalseValue.value = trueOpt.isCorrect
    } else if (meta.correctAnswer !== undefined) {
      trueFalseValue.value = meta.correctAnswer === 'True' || meta.correctAnswer === true
    }
  } else {
    localMetadata.value.options = meta.options?.length
      ? meta.options
      : Array.from({ length: 4 }, (_, i) => ({ text: '', isCorrect: false, order: i + 1 }))
    correctAnswerIndex.value = localMetadata.value.options.findIndex((o) => o.isCorrect)
  }
}

watch(() => props.metadata, initData, { immediate: true, deep: true })

const emitTrueFalseUpdate = () => {
  const tfOptions = [
    { text: 'True', isCorrect: trueFalseValue.value === true, order: 1 },
    { text: 'False', isCorrect: trueFalseValue.value === false, order: 2 },
  ]
  emit('update:metadata', {
    options: tfOptions,
    correctAnswer: trueFalseValue.value ? 'True' : 'False',
  })
}

const addOption = () => {
  localMetadata.value.options.push({
    text: '',
    isCorrect: false,
    order: localMetadata.value.options.length + 1,
  })
  emitUpdate()
}
const removeOption = (idx) => {
  localMetadata.value.options.splice(idx, 1)
  if (correctAnswerIndex.value === idx) correctAnswerIndex.value = -1
  emitUpdate()
}
const handleCorrectChange = () => {
  localMetadata.value.options.forEach((opt, idx) => {
    opt.isCorrect = idx === correctAnswerIndex.value
  })
  // Cũng update luôn correctAnswer string cho đồng bộ
  const correctOpt = localMetadata.value.options[correctAnswerIndex.value]
  emit('update:metadata', {
    ...localMetadata.value,
    correctAnswer: correctOpt ? correctOpt.text : '',
  })
}
const emitUpdate = () => {
  if (!isTrueFalseMode.value) emit('update:metadata', { ...localMetadata.value })
}
</script>
