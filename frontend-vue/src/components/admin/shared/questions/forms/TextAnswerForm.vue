<template>
  <div>
    <el-alert
      title="Nhập các đáp án chính xác cho câu hỏi ngắn này."
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <el-form-item label="Đáp án chấp nhận" label-width="0">
      <div class="w-full">
        <el-select
          v-model="correctAnswers"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="Nhập đáp án rồi nhấn Enter (VD: Hà Nội, Ha Noi)..."
          class="w-full"
          size="large"
          @change="syncToParent"
        >
          <template #empty>
            <div class="p-2 text-gray-400 text-xs">Gõ đáp án và nhấn Enter</div>
          </template>
        </el-select>

        <div class="text-xs text-gray-500 mt-2">
          <el-icon class="mr-1"><InfoFilled /></el-icon>
          Học viên cần nhập chính xác một trong các từ/cụm từ trên để được tính điểm.
        </div>
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:metadata'])

const correctAnswers = ref([])

const initData = () => {
  if (props.metadata.blanks && props.metadata.blanks[0]?.correctAnswers) {
    correctAnswers.value = [...props.metadata.blanks[0].correctAnswers]
  } else {
    correctAnswers.value = []
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => props.metadata.blanks,
  (newBlanks) => {
    if (newBlanks && newBlanks[0]?.correctAnswers) {
      const newAnswers = newBlanks[0].correctAnswers
      if (JSON.stringify(newAnswers) !== JSON.stringify(correctAnswers.value)) {
        correctAnswers.value = [...newAnswers]
      }
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    blanks: [{ position: 1, correctAnswers: correctAnswers.value, hint: '' }],
    wordBank: [],
  })
}

const validate = () => {
  if (!correctAnswers.value || correctAnswers.value.length === 0) {
    ElMessage.warning('Vui lòng nhập ít nhất một đáp án đúng')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
