<template>
  <div class="w-full">
    <div
      class="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-xl border border-blue-100 dark:border-blue-800 mb-5 flex gap-3"
    >
      <el-icon class="text-blue-600 mt-0.5"><InfoFilled /></el-icon>
      <div class="text-sm text-blue-800 dark:text-blue-300">
        <p class="font-bold mb-1">Cách tạo câu hỏi Sắp xếp câu:</p>
        <ul class="list-disc pl-4 space-y-1 text-xs">
          <li><b>Bước 1:</b> Nhập câu hoàn chỉnh vào ô "Câu đáp án".</li>
          <li><b>Bước 2:</b> Bấm "Tách từ" để hệ thống tự động chia nhỏ câu.</li>
          <li><b>Bước 3:</b> Bạn có thể chỉnh sửa từng từ, xóa bớt hoặc thêm từ gây nhiễu.</li>
        </ul>
      </div>
    </div>

    <div class="mb-6">
      <label class="block text-xs font-bold text-gray-500 uppercase mb-2"
        >Câu hoàn chỉnh (Đáp án đúng)</label
      >
      <div class="flex gap-2">
        <el-input
          v-model="fullSentence"
          placeholder="VD: I go to school by bus"
          clearable
          @change="generateWords"
          class="flex-1"
        >
          <template #prefix
            ><el-icon><EditPen /></el-icon
          ></template>
        </el-input>
        <el-button type="primary" @click="generateWords" class="!h-9">
          <el-icon class="mr-2"><MagicStick /></el-icon> Tách từ
        </el-button>
      </div>
    </div>

    <div
      class="bg-gray-50 dark:bg-[#252525] p-5 rounded-xl border border-dashed border-gray-300 dark:border-gray-600 relative"
    >
      <label class="text-xs font-bold text-gray-500 uppercase mb-3 block">Các từ thành phần</label>

      <div class="flex flex-wrap gap-2 min-h-[50px]">
        <div
          v-for="(word, index) in localMetadata.words"
          :key="index"
          class="group flex items-center bg-white dark:bg-[#333] border border-gray-200 dark:border-gray-600 rounded-lg shadow-sm pl-3 pr-1 py-1 hover:border-blue-400 transition-all cursor-default"
        >
          <span
            contenteditable="true"
            class="outline-none min-w-[20px] cursor-text text-gray-800 dark:text-gray-200 font-medium focus:text-blue-600 dark:focus:text-blue-400"
            @blur="(e) => updateWord(index, e.target.innerText)"
            @keydown.enter.prevent="(e) => e.target.blur()"
            >{{ word }}</span
          >

          <button
            class="ml-2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-full transition-all"
            @click.stop="removeWord(index)"
            type="button"
            title="Xóa từ này"
          >
            <el-icon :size="14"><Close /></el-icon>
          </button>
        </div>

        <div class="flex items-center">
          <input
            v-if="inputVisible"
            ref="InputRef"
            v-model="inputValue"
            class="w-24 h-8 px-2 rounded border border-blue-400 outline-none text-sm bg-white dark:bg-[#333] text-gray-800 dark:text-white"
            placeholder="Nhập từ..."
            @keyup.enter="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <el-button v-else size="small" round @click="showInput" class="!h-8">
            + Thêm từ
          </el-button>
        </div>
      </div>

      <div class="mt-3 text-xs text-gray-400 flex items-center gap-1">
        <el-icon><InfoFilled /></el-icon> Click trực tiếp vào chữ để sửa.
      </div>
    </div>

    <div class="mt-6">
      <label class="block text-xs font-bold text-gray-500 uppercase mb-2"
        >Giải thích (Optional)</label
      >
      <el-input
        v-model="localMetadata.explanation"
        type="textarea"
        :rows="2"
        placeholder="Giải thích cấu trúc ngữ pháp..."
        resize="none"
        @input="emitUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { InfoFilled, EditPen, MagicStick, Close } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const fullSentence = ref('')
const inputVisible = ref(false)
const inputValue = ref('')
const InputRef = ref(null)

// Init Local Data
const localMetadata = ref({
  words: [],
  correctSentence: '',
  explanation: '',
})

// Sync from Props
watch(
  () => props.metadata,
  (newVal) => {
    if (newVal) {
      // Clone array để tránh mutate trực tiếp prop
      localMetadata.value = {
        words: newVal.words ? [...newVal.words] : [],
        correctSentence: newVal.correctSentence || '',
        explanation: newVal.explanation || '',
      }
      if (newVal.correctSentence) fullSentence.value = newVal.correctSentence
    }
  },
  { immediate: true, deep: true },
)

const generateWords = () => {
  if (!fullSentence.value.trim()) return
  // Tách từ theo khoảng trắng, bỏ qua khoảng trắng thừa
  const words = fullSentence.value
    .trim()
    .split(/\s+/)
    .filter((w) => w)
  localMetadata.value.words = words
  localMetadata.value.correctSentence = fullSentence.value
  emitUpdate()
}

const removeWord = (index) => {
  localMetadata.value.words.splice(index, 1)
  emitUpdate()
}

const updateWord = (index, newValue) => {
  // Nếu xóa hết text thì coi như xóa từ đó
  if (!newValue || !newValue.trim()) {
    removeWord(index)
  } else {
    localMetadata.value.words[index] = newValue.trim()
    emitUpdate()
  }
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    InputRef.value?.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value.trim()) {
    localMetadata.value.words.push(inputValue.value.trim())
    emitUpdate()
  }
  inputVisible.value = false
  inputValue.value = ''
}

const emitUpdate = () => {
  // Sync lại correctSentence nếu cần thiết
  localMetadata.value.correctSentence = fullSentence.value
  // Gửi bản copy deep để đảm bảo reactivity
  emit('update:metadata', JSON.parse(JSON.stringify(localMetadata.value)))
}
</script>
