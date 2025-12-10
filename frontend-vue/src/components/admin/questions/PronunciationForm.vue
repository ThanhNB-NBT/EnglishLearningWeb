<template>
  <div class="w-full">
    <el-alert
      title="Hướng dẫn"
      type="info"
      :closable="false"
      show-icon
      class="!mb-5 !bg-cyan-50 dark:!bg-cyan-900/20 !border-cyan-200 dark:!border-cyan-800 text-cyan-800 dark:text-cyan-300"
    >
      <template #default>
        <ul class="list-disc pl-4 mt-1 text-xs space-y-1">
          <li>Nhập <b>các nhóm âm</b> (VD: /i:/, /ɪ/).</li>
          <li>Nhập <b>danh sách từ</b> cần phân loại.</li>
          <li>Hệ thống sẽ tự động ghép nối để tạo bài tập phân loại.</li>
        </ul>
      </template>
    </el-alert>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-4">
      <div class="bg-gray-50 dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-200 dark:border-gray-700">
        <div class="text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">1. Danh sách từ (Words)</div>
        <el-select
          v-model="localMetadata.words"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="Gõ từ và ấn Enter..."
          class="!w-full"
          @change="handleWordsChange"
        >
          <template #empty>
            <div class="p-3 text-center text-gray-400 text-xs">Gõ từ vựng (VD: sheep, ship) rồi Enter</div>
          </template>
        </el-select>
        <div class="mt-2 text-xs text-gray-400">
          * Đã thêm {{ localMetadata.words.length }} từ
        </div>
      </div>

      <div class="bg-gray-50 dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-200 dark:border-gray-700">
        <div class="text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">2. Các nhóm âm (Categories)</div>
        <el-select
          v-model="localMetadata.categories"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="Gõ ký hiệu âm và ấn Enter..."
          class="!w-full"
          @change="emitUpdate"
        >
          <template #empty>
            <div class="p-3 text-center text-gray-400 text-xs">Gõ ký hiệu (VD: /i:/) rồi Enter</div>
          </template>
        </el-select>
        <div class="mt-2 text-xs text-gray-400">
          * Đã thêm {{ localMetadata.categories.length }} nhóm
        </div>
      </div>
    </div>

    <el-form-item
      v-if="localMetadata.words.length > 0 && localMetadata.categories.length > 0"
      label="3. Phân loại đáp án đúng"
      required
    >
      <div class="w-full border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
        <div
          v-for="(word, idx) in localMetadata.words"
          :key="word"
          class="flex items-center gap-4 p-3 border-b border-gray-100 dark:border-gray-800 last:border-0 bg-white dark:bg-[#2c2c2c] hover:bg-gray-50 dark:hover:bg-[#333] transition-colors"
        >
          <div class="w-8 h-8 rounded-lg bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-xs shrink-0">
            {{ idx + 1 }}
          </div>

          <div class="min-w-[120px] font-bold text-gray-800 dark:text-white">{{ word }}</div>

          <el-icon class="text-gray-400"><Right /></el-icon>

          <el-select
            :model-value="getClassification(word)"
            @change="(val) => setClassification(word, val)"
            placeholder="Chọn nhóm âm..."
            class="flex-1"
            size="small"
          >
            <el-option
              v-for="cat in localMetadata.categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </div>
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Right } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  words: [],
  categories: [],
  classifications: [],
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value = {
      words: newVal.words || [],
      categories: newVal.categories || [],
      classifications: newVal.classifications || [],
    }
  }
}, { immediate: true, deep: true })

const getClassification = (word) => {
  const found = localMetadata.value.classifications.find(c => c.word === word)
  return found ? found.category : ''
}

const setClassification = (word, category) => {
  const index = localMetadata.value.classifications.findIndex(c => c.word === word)
  if (index !== -1) {
    localMetadata.value.classifications[index].category = category
  } else {
    localMetadata.value.classifications.push({ word, category })
  }
  emitUpdate()
}

const handleWordsChange = () => {
  // Sync classifications with words list
  localMetadata.value.classifications = localMetadata.value.classifications.filter(c => localMetadata.value.words.includes(c.word))
  emitUpdate()
}

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>
