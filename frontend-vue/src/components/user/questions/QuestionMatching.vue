<template>
  <div class="w-full mt-4 text-base text-gray-800 dark:text-gray-200">
    <div class="grid grid-cols-2 gap-x-8 gap-y-4 mb-6">
      <div>
        <h4 class="font-bold mb-3 border-b pb-1">Cột A</h4>
        <div class="space-y-3">
          <div
            v-for="(item, index) in leftItems"
            :key="`left-${index}`"
            class="flex items-start gap-2 p-2 rounded cursor-pointer transition-colors"
            :class="getLeftItemClass(item)"
            @click="!disabled && selectLeft(item)"
          >
            <span class="font-bold min-w-[20px]">{{ index + 1 }}.</span>
            <span v-html="item"></span>
          </div>
        </div>
      </div>

      <div>
        <h4 class="font-bold mb-3 border-b pb-1">Cột B</h4>
        <div class="space-y-3">
          <div
            v-for="(item, index) in rightItems"
            :key="`right-${index}`"
            class="flex items-start gap-2 p-2 rounded cursor-pointer transition-colors"
            :class="getRightItemClass(item)"
            @click="!disabled && selectRight(item)"
          >
            <span class="font-bold min-w-[20px]">{{ getChar(index) }}.</span>
            <span v-html="item"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6 border-t pt-4">
      <div class="font-bold mb-2">Trả lời:</div>
      <div class="flex flex-wrap gap-4">
        <div
          v-for="(item, index) in leftItems"
          :key="`ans-${index}`"
          class="flex items-center gap-1"
        >
          <span class="font-bold text-gray-600 dark:text-gray-400">{{ index + 1 }} - </span>

          <div
            class="w-10 h-8 border-b-2 border-gray-400 flex items-center justify-center font-bold text-blue-700 bg-gray-50 dark:bg-gray-800 dark:text-blue-400 transition-colors relative"
            :class="getAnswerBoxClass()"
          >
            {{ getMatchedChar(item) }}

            <button
              v-if="!disabled && currentMap[item]"
              @click.stop="removeMapping(item)"
              class="absolute -top-2 -right-2 text-gray-400 hover:text-red-500 bg-white rounded-full shadow-sm"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-if="disabled && showFeedback" class="mt-3 font-medium">
        <span v-if="isQuestionCorrect" class="text-green-600">✓ Chính xác</span>
        <span v-else class="text-red-600">✕ Có đáp án sai</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  modelValue: { type: Object, default: () => ({}) },
  disabled: { type: Boolean, default: false },
  showFeedback: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

// State
const selectedLeft = ref(null)
const currentMap = ref({})
const rightItems = ref([]) // Dùng ref để lưu trữ mảng đã shuffle tại frontend

// 1. Cột Trái: Giữ nguyên thứ tự từ API
const leftItems = computed(() => props.question?.data?.leftItems || [])

// 2. Helper chuyển đổi index sang ký tự (0 -> A, 1 -> B...)
const getChar = (index) => String.fromCharCode(65 + index)

// 3. Khởi tạo & Shuffle
const initData = () => {
  // Lấy dữ liệu thô từ API
  const rawRight = props.question?.data?.rightItems || []

  // ✅ SHUFFLE TẠI FRONTEND: Luôn tạo bản sao và xáo trộn ngẫu nhiên
  rightItems.value = [...rawRight].sort(() => Math.random() - 0.5)

  // Khôi phục đáp án đã chọn (nếu có)
  if (props.modelValue) {
    currentMap.value = { ...props.modelValue }
  }
}

onMounted(initData)

// Khi câu hỏi thay đổi (chuyển bài), init lại để shuffle lại
watch(() => props.question, initData)

// Đồng bộ modelValue từ cha (nếu có reset từ bên ngoài)
watch(
  () => props.modelValue,
  (newVal) => {
    if (JSON.stringify(newVal) !== JSON.stringify(currentMap.value)) {
      currentMap.value = { ...newVal }
    }
  },
  { deep: true },
)

// 4. Xử lý chọn (Logic ghép cặp)
const selectLeft = (item) => {
  if (selectedLeft.value === item) {
    selectedLeft.value = null // Bỏ chọn
  } else {
    selectedLeft.value = item
  }
}

const selectRight = (item) => {
  if (!selectedLeft.value) return

  const newMap = { ...currentMap.value }

  // Gỡ item này nếu đã được ghép với câu khác trước đó (1-1 mapping)
  Object.keys(newMap).forEach((key) => {
    if (newMap[key] === item) delete newMap[key]
  })

  newMap[selectedLeft.value] = item
  currentMap.value = newMap
  selectedLeft.value = null

  emit('update:modelValue', newMap)
}

const removeMapping = (leftItem) => {
  const newMap = { ...currentMap.value }
  delete newMap[leftItem]
  currentMap.value = newMap
  emit('update:modelValue', newMap)
}

// 5. Helpers hiển thị
const getMatchedChar = (leftItem) => {
  const rightVal = currentMap.value[leftItem]
  if (!rightVal) return ''
  // Tìm vị trí của giá trị trong mảng rightItems ĐÃ SHUFFLE
  const idx = rightItems.value.indexOf(rightVal)
  return idx !== -1 ? getChar(idx) : '?'
}

const getLeftItemClass = (item) => {
  if (selectedLeft.value === item) return 'bg-blue-100 text-blue-800 font-bold ring-2 ring-blue-400'
  if (currentMap.value[item])
    return 'bg-gray-100 dark:bg-gray-700 text-gray-500 line-through decoration-blue-500'
  return 'hover:bg-gray-50 dark:hover:bg-gray-800 border border-gray-200'
}

const getRightItemClass = (item) => {
  const isMatched = Object.values(currentMap.value).includes(item)
  if (isMatched)
    return 'bg-gray-100 dark:bg-gray-700 text-gray-500 line-through decoration-blue-500'

  if (selectedLeft.value)
    return 'hover:bg-blue-50 dark:hover:bg-blue-900/30 cursor-pointer ring-1 ring-transparent hover:ring-blue-300 border border-gray-200'

  return 'border border-gray-200'
}

const isQuestionCorrect = computed(() => {
  return props.question.isCorrect === true
})

const getAnswerBoxClass = () => {
  if (!props.disabled || !props.showFeedback) return ''
  return isQuestionCorrect.value
    ? 'text-green-600 border-green-600 bg-green-50'
    : 'text-red-500 border-red-500 bg-red-50'
}
</script>
