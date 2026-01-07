<template>
  <div class="space-y-4">
    <el-alert
      title="Cấu hình cho câu hỏi tự luận/viết (Open Ended)."
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
    />

    <div class="grid grid-cols-2 gap-6">
      <el-form-item label="Số từ tối thiểu" label-width="140px">
        <el-input-number
          v-model="minWord"
          :min="0"
          class="w-full"
          placeholder="VD: 50"
          @change="syncToParent"
        />
      </el-form-item>

      <el-form-item label="Số từ tối đa" label-width="140px">
        <el-input-number
          v-model="maxWord"
          :min="0"
          class="w-full"
          placeholder="VD: 200"
          @change="syncToParent"
        />
      </el-form-item>
    </div>

    <el-form-item label="Thời gian (giây)" label-width="140px">
      <el-input-number
        v-model="timeLimitSeconds"
        :min="0"
        :step="60"
        class="w-full"
        @change="syncToParent"
      />
      <div class="text-xs text-gray-400 mt-1">Để 0 nếu không giới hạn thời gian.</div>
    </el-form-item>

    <el-form-item label="Bài mẫu / Gợi ý" label-width="140px">
      <el-input
        v-model="suggestedAnswer"
        type="textarea"
        rows="4"
        placeholder="Nhập dàn ý hoặc bài mẫu tham khảo..."
        @input="syncToParent"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const suggestedAnswer = ref('')
const minWord = ref(null)
const maxWord = ref(null)
const timeLimitSeconds = ref(0)

const initData = () => {
  if (props.metadata.suggestedAnswer !== undefined || props.metadata.minWord !== undefined) {
    suggestedAnswer.value = props.metadata.suggestedAnswer || ''
    minWord.value = props.metadata.minWord ?? null
    maxWord.value = props.metadata.maxWord ?? null
    timeLimitSeconds.value = props.metadata.timeLimitSeconds ?? 0
  } else {
    suggestedAnswer.value = ''
    minWord.value = null
    maxWord.value = null
    timeLimitSeconds.value = 0
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [
    props.metadata.suggestedAnswer,
    props.metadata.minWord,
    props.metadata.maxWord,
    props.metadata.timeLimitSeconds,
  ],
  ([newSuggested, newMin, newMax, newTime]) => {
    if (newSuggested !== suggestedAnswer.value) {
      suggestedAnswer.value = newSuggested || ''
    }
    if (newMin !== minWord.value) {
      minWord.value = newMin ?? null
    }
    if (newMax !== maxWord.value) {
      maxWord.value = newMax ?? null
    }
    if (newTime !== timeLimitSeconds.value) {
      timeLimitSeconds.value = newTime ?? 0
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    suggestedAnswer: suggestedAnswer.value,
    minWord: minWord.value,
    maxWord: maxWord.value,
    timeLimitSeconds: timeLimitSeconds.value,
  })
}

const validate = () => {
  if (minWord.value && maxWord.value && minWord.value > maxWord.value) {
    ElMessage.warning('Số từ tối thiểu không được lớn hơn tối đa')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
