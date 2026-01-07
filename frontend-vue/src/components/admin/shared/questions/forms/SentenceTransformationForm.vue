<template>
  <div class="space-y-4">
    <el-form-item label="Câu gốc" label-width="100px">
      <el-input
        v-model="originalSentence"
        placeholder="VD: It is a pity I didn't see him."
        @input="syncToParent"
      />
    </el-form-item>

    <el-form-item label="Gợi ý đầu câu" label-width="100px">
      <el-input
        v-model="beginningPhrase"
        placeholder="VD: I wish"
        @input="syncToParent"
      />
    </el-form-item>

    <el-form-item label="Đáp án đúng" label-width="100px">
      <el-select
        v-model="correctAnswers"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="Nhập các câu trả lời chấp nhận được (Enter để thêm)"
        class="w-full"
        @change="syncToParent"
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

const originalSentence = ref('')
const beginningPhrase = ref('')
const correctAnswers = ref([])

const initData = () => {
  if (props.metadata.originalSentence !== undefined) {
    originalSentence.value = props.metadata.originalSentence || ''
    beginningPhrase.value = props.metadata.beginningPhrase || ''
    correctAnswers.value = props.metadata.correctAnswers ? [...props.metadata.correctAnswers] : []
  } else {
    originalSentence.value = ''
    beginningPhrase.value = ''
    correctAnswers.value = []
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [props.metadata.originalSentence, props.metadata.beginningPhrase, props.metadata.correctAnswers],
  ([newOriginal, newBeginning, newAnswers]) => {
    if (newOriginal !== originalSentence.value) {
      originalSentence.value = newOriginal || ''
    }
    if (newBeginning !== beginningPhrase.value) {
      beginningPhrase.value = newBeginning || ''
    }
    if (newAnswers && JSON.stringify(newAnswers) !== JSON.stringify(correctAnswers.value)) {
      correctAnswers.value = [...newAnswers]
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    originalSentence: originalSentence.value,
    beginningPhrase: beginningPhrase.value,
    correctAnswers: correctAnswers.value,
  })
}

const validate = () => {
  if (!originalSentence.value) {
    ElMessage.warning('Chưa nhập câu gốc')
    return false
  }
  if (!correctAnswers.value?.length) {
    ElMessage.warning('Phải có ít nhất 1 đáp án đúng')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
