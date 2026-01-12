<template>
  <div>
    <el-alert
      :title="
        isTrueFalse
          ? 'Chọn đáp án đúng: True hoặc False'
          : 'Nhập các lựa chọn và tích chọn đáp án đúng.'
      "
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <!-- TRUE/FALSE Special UI -->
    <div v-if="isTrueFalse" class="flex gap-4 justify-center p-4">
      <div
        @click="selectTrueFalse('True')"
        class="flex-1 max-w-xs p-6 border-2 rounded-lg cursor-pointer transition-all hover:shadow-lg"
        :class="getTrueFalseClass('True')"
      >
        <div class="text-center">
          <el-icon :size="40" class="mb-2">
            <Select />
          </el-icon>
          <div class="text-xl font-bold">True</div>
        </div>
      </div>

      <div
        @click="selectTrueFalse('False')"
        class="flex-1 max-w-xs p-6 border-2 rounded-lg cursor-pointer transition-all hover:shadow-lg"
        :class="getTrueFalseClass('False')"
      >
        <div class="text-center">
          <el-icon :size="40" class="mb-2">
            <CloseBold />
          </el-icon>
          <div class="text-xl font-bold">False</div>
        </div>
      </div>
    </div>

    <!-- Regular Multiple Choice UI -->
    <div v-else>
      <div v-for="(item, index) in localOptions" :key="index" class="flex items-start gap-3 mb-3">
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
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { Delete, Select, CloseBold } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
  questionType: {
    type: String,
    default: 'MULTIPLE_CHOICE',
  },
})

const emit = defineEmits(['update:metadata'])

const localOptions = ref([])
const selectedIndex = ref(0)

// Check if this is TRUE_FALSE type
const isTrueFalse = computed(() => props.questionType === 'TRUE_FALSE')

// Initialize
const initOptions = () => {
  if (props.metadata.options && props.metadata.options.length > 0) {
    localOptions.value = JSON.parse(JSON.stringify(props.metadata.options))
    const idx = localOptions.value.findIndex((o) => o.isCorrect)
    selectedIndex.value = idx !== -1 ? idx : 0
  } else {
    // Initialize based on type
    if (isTrueFalse.value) {
      localOptions.value = [
        { text: 'True', isCorrect: true, order: 1 },
        { text: 'False', isCorrect: false, order: 2 },
      ]
    } else {
      localOptions.value = [
        { text: '', isCorrect: true, order: 1 },
        { text: '', isCorrect: false, order: 2 },
        { text: '', isCorrect: false, order: 3 },
        { text: '', isCorrect: false, order: 4 },
      ]
    }
    selectedIndex.value = 0
    syncToParent()
  }
}

onMounted(() => {
  initOptions()
})

// Watch for type changes
watch(
  () => props.questionType,
  (newType) => {
    if (newType === 'TRUE_FALSE' && localOptions.value.length !== 2) {
      localOptions.value = [
        { text: 'True', isCorrect: true, order: 1 },
        { text: 'False', isCorrect: false, order: 2 },
      ]
      selectedIndex.value = 0
      syncToParent()
    }
  },
)

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
  { deep: true },
)

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

// TRUE/FALSE specific methods
const selectTrueFalse = (value) => {
  const idx = localOptions.value.findIndex((opt) => opt.text === value)
  if (idx !== -1) {
    selectedIndex.value = idx
    updateCorrectAnswer(idx)
  }
}

const getTrueFalseClass = (value) => {
  const option = localOptions.value.find((opt) => opt.text === value)
  const isSelected = option?.isCorrect === true

  if (isSelected) {
    return 'border-green-500 bg-green-50 dark:bg-green-900/20'
  }
  return 'border-gray-300 bg-white dark:bg-[#252525] hover:border-blue-400'
}

// Regular multiple choice methods
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

  // TRUE_FALSE doesn't need text validation
  if (!isTrueFalse.value) {
    const hasEmpty = localOptions.value.some((o) => !o.text || !o.text.trim())
    if (hasEmpty) {
      ElMessage.warning('Nội dung đáp án không được để trống')
      return false
    }
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
