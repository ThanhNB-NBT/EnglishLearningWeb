<template>
  <div class="space-y-4">
    <el-alert type="info" :closable="false" show-icon>
      <template #title>
        <span class="font-bold">Câu hỏi trả lời ngắn (Text Answer)</span>
      </template>
      <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">
        Học viên nhập câu trả lời văn bản ngắn. Hỗ trợ nhiều đáp án đúng (nhấn <kbd class="px-1. 5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded text-xs font-bold">Enter</kbd> để thêm).
      </div>
    </el-alert>

    <!-- Input for adding answers -->
    <el-form-item label="Đáp án đúng" required>
      <el-input
        v-model="newAnswer"
        placeholder="Nhập đáp án và nhấn Enter (VD: London)"
        clearable
        @keyup.enter="addAnswer"
      >
        <template #append>
          <el-button :icon="Plus" @click="addAnswer" :disabled="!newAnswer.trim()">
            Thêm
          </el-button>
        </template>
      </el-input>
      <div class="text-xs text-gray-500 mt-1">
        Nhấn <kbd class="px-1 py-0.5 bg-gray-100 dark:bg-gray-700 rounded">Enter</kbd> hoặc click "Thêm" để thêm đáp án
      </div>
    </el-form-item>

    <!-- List of correct answers -->
    <div v-if="correctAnswers.length > 0" class="space-y-2">
      <div class="text-sm font-bold text-gray-700 dark:text-gray-300">
        📋 Đáp án được chấp nhận ({{ correctAnswers.length }}):
      </div>
      <div class="space-y-2">
        <div
          v-for="(answer, index) in correctAnswers"
          :key="index"
          class="flex items-center gap-2 bg-green-50 dark:bg-green-900/20 px-3 py-2 rounded-lg border border-green-200 dark:border-green-800"
        >
          <el-icon class="text-green-600 dark:text-green-400"><CircleCheck /></el-icon>
          <span class="flex-1 text-gray-800 dark:text-gray-200">{{ answer }}</span>
          <el-button
            type="danger"
            :icon="Delete"
            size="small"
            circle
            @click="removeAnswer(index)"
          />
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <el-empty
      v-else
      description="Chưa có đáp án nào"
      :image-size="80"
      class="!my-4"
    >
      <template #description>
        <span class="text-sm text-gray-500">Nhập ít nhất 1 đáp án đúng</span>
      </template>
    </el-empty>

    <!-- Case Sensitive -->
    <el-divider content-position="left">
      <span class="text-xs text-gray-500">Tùy chọn nâng cao</span>
    </el-divider>

    <el-form-item>
      <div class="flex items-center gap-2">
        <el-switch
          v-model="localData.caseSensitive"
          active-text="Phân biệt chữ hoa/thường"
          inactive-text="Không phân biệt"
          @change="emitUpdate"
        />
        <el-tooltip placement="top">
          <template #content>
            <div class="max-w-xs">
              <strong>Bật: </strong> "London" ≠ "london"<br />
              <strong>Tắt:</strong> "London" = "london" = "LONDON"
            </div>
          </template>
          <el-icon class="text-gray-400 cursor-help"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>
    </el-form-item>

    <!-- Min/Max Length -->
    <div class="grid grid-cols-2 gap-4">
      <el-form-item label="Độ dài tối thiểu (ký tự)">
        <el-input-number
          v-model="localData.minLength"
          :min="0"
          :max="localData.maxLength || 1000"
          placeholder="Không giới hạn"
          class="! w-full"
          controls-position="right"
          @change="emitUpdate"
        />
      </el-form-item>

      <el-form-item label="Độ dài tối đa (ký tự)">
        <el-input-number
          v-model="localData.maxLength"
          :min="localData.minLength || 1"
          :max="1000"
          placeholder="Không giới hạn"
          class="!w-full"
          controls-position="right"
          @change="emitUpdate"
        />
      </el-form-item>
    </div>

    <!-- Placeholder -->
    <el-form-item label="Gợi ý cho học viên (Placeholder)">
      <el-input
        v-model="localData.placeholder"
        placeholder="VD:  Nhập tên thủ đô của Anh..."
        clearable
        @input="emitUpdate"
      />
    </el-form-item>

    <!-- Preview -->
    <el-divider content-position="left">
      <span class="text-xs text-gray-500">👁️ Xem trước</span>
    </el-divider>

    <div class="bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-950/30 dark:to-purple-950/30 p-5 rounded-xl border-2 border-dashed border-blue-200 dark:border-blue-800">
      <div class="text-sm font-bold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2">
        <el-icon><View /></el-icon>
        Học viên sẽ thấy:
      </div>
      <el-input
        :placeholder="localData.placeholder || 'Nhập câu trả lời... '"
        disabled
        class="!bg-white dark:!bg-gray-900"
      />
      <div class="flex items-center gap-4 mt-3 text-xs text-gray-600 dark:text-gray-400">
        <div v-if="localData.caseSensitive" class="flex items-center gap-1">
          <el-icon class="text-orange-500"><Warning /></el-icon>
          <span>Phân biệt HOA/thường</span>
        </div>
        <div v-if="localData.minLength || localData.maxLength" class="flex items-center gap-1">
          <el-icon class="text-blue-500"><Ruler /></el-icon>
          <span v-if="localData.minLength && localData.maxLength">
            {{ localData.minLength }} - {{ localData.maxLength }} ký tự
          </span>
          <span v-else-if="localData.minLength">Tối thiểu {{ localData.minLength }} ký tự</span>
          <span v-else-if="localData. maxLength">Tối đa {{ localData.maxLength }} ký tự</span>
        </div>
      </div>
    </div>

    <!-- Validation Warning -->
    <el-alert v-if="! isValid" type="error" :closable="false" show-icon class="mt-4">
      <template #title>
        <span class="font-bold">Lỗi cấu hình</span>
      </template>
      <div class="text-sm">Chưa nhập đáp án đúng.  Vui lòng thêm ít nhất 1 đáp án. </div>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Plus,
  Delete,
  CircleCheck,
  QuestionFilled,
  View,
  Warning,
  Ruler,
} from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
  questionType: {
    type: String,
    default: 'TEXT_ANSWER',
  },
})

const emit = defineEmits(['update:metadata'])

// Local state
const newAnswer = ref('')
const correctAnswers = ref([])

const localData = ref({
  caseSensitive:  false,
  minLength: null,
  maxLength: null,
  placeholder: '',
})

// Initialize from props
const initializeData = () => {
  const meta = props.metadata || {}

  // Extract correct answers from blanks format (backend format)
  if (meta.blanks && meta.blanks.length > 0) {
    const blank = meta.blanks[0]
    correctAnswers.value = Array.isArray(blank. correctAnswers)
      ? [... blank.correctAnswers]
      : blank.correctAnswers
      ? [blank.correctAnswers]
      : []
  } else {
    correctAnswers.value = []
  }

  // Extract other settings
  localData.value = {
    caseSensitive: meta.caseSensitive || false,
    minLength: meta.minLength || null,
    maxLength: meta.maxLength || null,
    placeholder: meta.placeholder || '',
  }
}

// Watch props changes
watch(
  () => props.metadata,
  () => {
    initializeData()
  },
  { deep: true, immediate: true }
)

// Computed
const isValid = computed(() => correctAnswers.value.length > 0)

// Methods
const addAnswer = () => {
  const answer = newAnswer.value.trim()
  if (!answer) return

  // Check duplicate
  if (correctAnswers.value.includes(answer)) {
    ElMessage.warning('Đáp án này đã tồn tại')
    return
  }

  correctAnswers.value.push(answer)
  newAnswer.value = ''
  emitUpdate()
}

const removeAnswer = (index) => {
  correctAnswers.value.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  // Format theo backend:  blanks array
  const metadata = {
    blanks: [
      {
        position: 1,
        correctAnswers: [... correctAnswers.value],
      },
    ],
    caseSensitive: localData.value.caseSensitive,
    minLength: localData.value.minLength,
    maxLength:  localData.value.maxLength,
    placeholder: localData.value.placeholder,
  }

  emit('update:metadata', metadata)
}
</script>

<style scoped>
kbd {
  font-family: monospace;
  font-weight: bold;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
</style>
