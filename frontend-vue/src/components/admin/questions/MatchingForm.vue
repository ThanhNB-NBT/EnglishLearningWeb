<template>
  <div class="w-full">
    <div
      class="bg-yellow-50 dark:bg-yellow-900/10 p-3 rounded-lg border border-yellow-200 dark:border-yellow-800 mb-4 text-sm text-yellow-800 dark:text-yellow-400">
      <el-icon class="mr-1">
        <InfoFilled />
      </el-icon> Hệ thống sẽ tự động xáo trộn cột phải khi hiển thị.
    </div>

    <div class="space-y-3">
      <div v-for="(pair, index) in localMetadata.pairs" :key="index"
        class="flex gap-2 items-center bg-white dark:bg-[#2c2c2c] p-2 rounded-lg border border-gray-200 dark:border-gray-700">
        <div class="w-6 h-6 rounded bg-gray-200 text-gray-500 flex items-center justify-center text-xs font-bold">{{
          index + 1 }}</div>

        <div class="flex-1 grid grid-cols-2 gap-2">
          <el-input v-model="pair.left" placeholder="Vế Trái (Key)" @input="emitUpdate">
            <template #prefix><span class="text-gray-400 text-xs">A</span></template>
          </el-input>
          <el-input v-model="pair.right" placeholder="Vế Phải (Value)" @input="emitUpdate">
            <template #prefix><span class="text-gray-400 text-xs">B</span></template>
          </el-input>
        </div>

        <el-button type="danger" circle plain text @click="removePair(index)"
          :disabled="localMetadata.pairs.length <= 2">
          <el-icon>
            <Delete />
          </el-icon>
        </el-button>
      </div>

      <el-button type="primary" plain class="w-full mt-2 border-dashed" @click="addPair">
        + Thêm cặp từ
      </el-button>
    </div>
  </div>
</template>

<script setup>
// (Giữ nguyên script cũ)
import { ref, watch } from 'vue'
import { Delete, InfoFilled } from '@element-plus/icons-vue'
const props = defineProps({ metadata: Object })
const emit = defineEmits(['update:metadata'])
const localMetadata = ref({ pairs: [{ left: '', right: '' }] })

watch(() => props.metadata, (val) => {
  if (val?.matchingPairs) localMetadata.value.pairs = val.matchingPairs // Backend dùng key matchingPairs
  else if (val?.pairs) localMetadata.value.pairs = val.pairs
}, { immediate: true, deep: true })

const addPair = () => { localMetadata.value.pairs.push({ left: '', right: '' }); emitUpdate() }
const removePair = (idx) => { localMetadata.value.pairs.splice(idx, 1); emitUpdate() }
const emitUpdate = () => emit('update:metadata', { matchingPairs: localMetadata.value.pairs })
</script>
