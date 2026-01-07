<template>
  <div>
    <el-alert
      title="Định nghĩa các chỗ trống (Blank) trong câu hỏi."
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <div
      v-for="(blank, index) in localBlanks"
      :key="index"
      class="p-4 border rounded mb-3 bg-white relative"
    >
      <div class="absolute top-2 right-2">
        <el-button type="danger" link :icon="Delete" @click="removeBlank(index)" />
      </div>

      <div class="font-bold mb-2 text-blue-600">Chỗ trống số [{{ blank.position }}]</div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="text-xs text-gray-500 mb-1 block">Đáp án chấp nhận (Enter để thêm)</label>
          <el-select
            v-model="blank.correctAnswers"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="Nhập đáp án..."
            class="w-full"
            @change="syncToParent"
          />
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-1 block">Gợi ý (Hint)</label>
          <el-input v-model="blank.hint" placeholder="VD: to be..." @input="syncToParent" />
        </div>
      </div>
    </div>

    <div class="flex gap-2 mb-6">
      <el-button type="primary" plain size="small" @click="addBlank">+ Thêm chỗ trống</el-button>
    </div>

    <el-divider />

    <div class="bg-gray-50 p-4 rounded">
      <h4 class="text-sm font-bold mb-2">Word Bank (Từ gợi ý)</h4>
      <el-select
        v-model="localWordBank"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="Nhập các từ gây nhiễu hoặc đáp án..."
        class="w-full"
        @change="syncToParent"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localBlanks = ref([])
const localWordBank = ref([])

const initData = () => {
  if (props.metadata.blanks && props.metadata.blanks.length > 0) {
    localBlanks.value = JSON.parse(JSON.stringify(props.metadata.blanks))
    localWordBank.value = props.metadata.wordBank ? [...props.metadata.wordBank] : []
  } else {
    localBlanks.value = [{ position: 1, correctAnswers: [], hint: '' }]
    localWordBank.value = []
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [props.metadata.blanks, props.metadata.wordBank],
  ([newBlanks, newWordBank]) => {
    if (newBlanks && JSON.stringify(newBlanks) !== JSON.stringify(localBlanks.value)) {
      localBlanks.value = JSON.parse(JSON.stringify(newBlanks))
    }
    if (newWordBank && JSON.stringify(newWordBank) !== JSON.stringify(localWordBank.value)) {
      localWordBank.value = [...newWordBank]
    }
  },
  { deep: true }
)

// ✅ FIX: Emit metadata
const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    blanks: localBlanks.value,
    wordBank: localWordBank.value,
  })
}

const addBlank = () => {
  const nextPos = localBlanks.value.length + 1
  localBlanks.value.push({ position: nextPos, correctAnswers: [], hint: '' })
  syncToParent()
}

const removeBlank = (index) => {
  localBlanks.value.splice(index, 1)
  localBlanks.value.forEach((b, i) => (b.position = i + 1))
  syncToParent()
}

const validate = () => {
  if (!localBlanks.value.length) {
    ElMessage.warning('Phải có ít nhất 1 chỗ trống')
    return false
  }
  for (const b of localBlanks.value) {
    if (!b.correctAnswers || b.correctAnswers.length === 0) {
      ElMessage.warning(`Chỗ trống số ${b.position} chưa có đáp án đúng`)
      return false
    }
  }
  return true
}

defineExpose({ validate })
</script>
