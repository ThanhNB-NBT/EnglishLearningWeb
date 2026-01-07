<template>
  <div>
    <el-alert
      title="Nhập các lựa chọn và tích chọn đáp án đúng."
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <div
      v-for="(item, index) in localOptions"
      :key="index"
      class="flex items-start gap-3 mb-3"
    >
      <div class="pt-1">
        <el-radio
          v-model="selectedIndex"
          :label="index"
          border
          class="!mr-0 bg-white"
          @change="updateCorrectAnswer(index)"
        >
          {{ getLabel(index) }}
        </el-radio>
      </div>

      <el-input
        v-model="item.text"
        placeholder="Nhập nội dung đáp án..."
        type="textarea"
        autosize
        @input="syncToParent"
      />

      <el-button
        type="danger"
        :icon="Delete"
        circle
        plain
        @click="removeOption(index)"
        :disabled="localOptions.length <= 2"
      />
    </div>

    <el-button type="primary" plain size="small" @click="addOption" class="mt-2">
      + Thêm lựa chọn
    </el-button>
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

const localOptions = ref([])
const selectedIndex = ref(0)

// Initialize
const initOptions = () => {
  if (props.metadata.options && props.metadata.options.length > 0) {
    localOptions.value = JSON.parse(JSON.stringify(props.metadata.options))
    const idx = localOptions.value.findIndex((o) => o.isCorrect)
    selectedIndex.value = idx !== -1 ? idx : 0
  } else {
    localOptions.value = [
      { text: '', isCorrect: true, order: 1 },
      { text: '', isCorrect: false, order: 2 },
      { text: '', isCorrect: false, order: 3 },
      { text: '', isCorrect: false, order: 4 },
    ]
    selectedIndex.value = 0
    syncToParent()
  }
}

onMounted(() => {
  initOptions()
})

// Watch parent changes
watch(
  () => props.metadata.options,
  (newOptions) => {
    if (newOptions && JSON.stringify(newOptions) !== JSON.stringify(localOptions.value)) {
      localOptions.value = JSON.parse(JSON.stringify(newOptions))
      const idx = localOptions.value.findIndex((o) => o.isCorrect)
      selectedIndex.value = idx !== -1 ? idx : 0
    }
  },
  { deep: true }
)

// ✅ FIX: Sync to parent with correct structure
const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    options: localOptions.value,
  })
}

const getLabel = (index) => String.fromCharCode(65 + index)

const updateCorrectAnswer = (idx) => {
  localOptions.value.forEach((opt, i) => {
    opt.isCorrect = i === idx
  })
  syncToParent()
}

const addOption = () => {
  localOptions.value.push({
    text: '',
    isCorrect: false,
    order: localOptions.value.length + 1,
  })
  syncToParent()
}

const removeOption = (index) => {
  localOptions.value.splice(index, 1)
  if (selectedIndex.value === index) {
    selectedIndex.value = 0
  } else if (selectedIndex.value > index) {
    selectedIndex.value--
  }
  updateCorrectAnswer(selectedIndex.value)
  localOptions.value.forEach((opt, i) => (opt.order = i + 1))
  syncToParent()
}

const validate = () => {
  if (localOptions.value.length < 2) {
    ElMessage.warning('Cần ít nhất 2 đáp án')
    return false
  }
  const hasEmpty = localOptions.value.some((o) => !o.text || !o.text.trim())
  if (hasEmpty) {
    ElMessage.warning('Nội dung đáp án không được để trống')
    return false
  }
  const hasCorrect = localOptions.value.some((o) => o.isCorrect === true)
  if (!hasCorrect) {
    ElMessage.warning('Chưa chọn đáp án đúng')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
