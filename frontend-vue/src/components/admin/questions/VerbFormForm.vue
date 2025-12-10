<template>
  <div class="w-full">
    <div class="space-y-3">
      <div v-for="(blank, index) in localMetadata.blanks" :key="index"
        class="bg-white dark:bg-[#2c2c2c] p-3 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm">
        <div class="flex justify-between mb-2">
          <el-tag effect="dark" size="small">Vị trí #{{ index + 1 }}</el-tag>
          <el-button type="danger" link size="small" @click="removeBlank(index)">Xóa</el-button>
        </div>
        <div class="flex gap-2">
          <el-input v-model="blank.verb" placeholder="Động từ gốc (VD: go)" class="!w-1/3">
            <template #prepend>(V)</template>
          </el-input>
          <el-select v-model="blank.correctAnswers" multiple filterable allow-create default-first-option
            placeholder="Các dạng đúng (VD: went, has gone)" class="!flex-1" @change="emitUpdate" />
        </div>
      </div>
      <el-button type="primary" plain class="w-full mt-2 border-dashed" @click="addBlank">+ Thêm động từ</el-button>
    </div>
  </div>
</template>

<script setup>
// Script tương tự FillBlankForm nhưng có thêm field 'verb'
import { ref, watch } from 'vue'
const props = defineProps({ metadata: Object })
const emit = defineEmits(['update:metadata'])
const localMetadata = ref({ blanks: [] })

watch(() => props.metadata, (val) => {
  localMetadata.value.blanks = val?.blanks || [{ verb: '', correctAnswers: [] }]
}, { immediate: true, deep: true })

const addBlank = () => { localMetadata.value.blanks.push({ verb: '', correctAnswers: [] }); emitUpdate() }
const removeBlank = (idx) => { localMetadata.value.blanks.splice(idx, 1); emitUpdate() }
const emitUpdate = () => emit('update:metadata', { blanks: localMetadata.value.blanks })
</script>
