<template>
  <div>
    <el-form-item label="Câu hoàn chỉnh" label-width="120px" class="mb-6">
      <el-input
        v-model="correctSentence"
        placeholder="Nhập câu đúng hoàn chỉnh (VD: I go to school)"
        @blur="autoSplitWords"
        @input="syncToParent"
      />
      <div class="text-xs text-gray-400 mt-1">Nhập xong nhấn ra ngoài để tự động tách từ.</div>
    </el-form-item>

    <el-form-item label="Các từ (Words)" label-width="120px">
      <!-- ✅ FIX: Add :reserve-keyword="false" -->
      <el-select
        v-model="words"
        multiple
        filterable
        allow-create
        default-first-option
        :reserve-keyword="false"
        placeholder="Nhập các từ rời rạc..."
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

const correctSentence = ref('')
const words = ref([])

const initData = () => {
  if (props.metadata.correctSentence) {
    correctSentence.value = props.metadata.correctSentence
    words.value = props.metadata.words ? [...props.metadata.words] : []
  } else {
    correctSentence.value = ''
    words.value = []
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [props.metadata.correctSentence, props.metadata.words],
  ([newSentence, newWords]) => {
    if (newSentence !== correctSentence.value) {
      correctSentence.value = newSentence || ''
    }
    if (newWords && JSON.stringify(newWords) !== JSON.stringify(words.value)) {
      words.value = [...newWords]
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    correctSentence: correctSentence.value,
    words: words.value,
  })
}

const autoSplitWords = () => {
  if (correctSentence.value && (!words.value || words.value.length === 0)) {
    const splitWords = correctSentence.value.trim().split(/\s+/)
    words.value = splitWords
    syncToParent()
  }
}

const validate = () => {
  if (!correctSentence.value) {
    ElMessage.warning('Chưa nhập câu hoàn chỉnh')
    return false
  }
  if (!words.value || words.value.length < 2) {
    ElMessage.warning('Cần ít nhất 2 từ để sắp xếp')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
