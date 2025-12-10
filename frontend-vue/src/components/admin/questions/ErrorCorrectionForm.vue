<template>
  <div class="w-full">
    <div class="bg-red-50 dark:bg-red-900/10 border border-red-100 dark:border-red-900/30 rounded-xl p-4 mb-5">
      <div class="flex items-center gap-2 text-red-700 dark:text-red-400 font-bold text-sm mb-2">
        <el-icon><Warning /></el-icon> Cấu hình lỗi sai
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Từ/Cụm từ sai</label>
          <el-input
            v-model="localMetadata.errorText"
            placeholder="VD: go (trong câu 'He go')"
            @input="emitUpdate"
          >
            <template #prefix><el-icon class="text-red-500"><CloseBold /></el-icon></template>
          </el-input>
        </div>

        <div>
          <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Sửa lại đúng</label>
          <el-input
            v-model="localMetadata.correction"
            placeholder="VD: goes"
            @input="emitUpdate"
          >
            <template #prefix><el-icon class="text-green-500"><Select /></el-icon></template>
          </el-input>
        </div>
      </div>
    </div>

    <el-form-item label="Giải thích chi tiết">
      <el-input
        v-model="localMetadata.explanation"
        type="textarea"
        :rows="3"
        placeholder="Giải thích ngữ pháp: Tại sao lại sai? Quy tắc đúng là gì?"
        @input="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { CloseBold, Select, Warning } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({ errorText: '', correction: '', explanation: '' })

watch(() => props.metadata, (newVal) => {
  if (newVal) localMetadata.value = { ...newVal }
}, { immediate: true, deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>
