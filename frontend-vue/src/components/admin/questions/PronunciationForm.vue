<template>
  <div class="pronunciation-form">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="Từ vựng cần phát âm" required>
          <el-input v-model="localMetadata.targetWord" placeholder="VD: schedule" @input="emitUpdate" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="Phiên âm IPA (Optional)">
          <el-input v-model="localMetadata.ipa" placeholder="VD: /ˈʃedʒ.uːl/" @input="emitUpdate" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="Link Audio mẫu (Optional)">
      <el-input v-model="localMetadata.audioUrl" placeholder="https://..." @input="emitUpdate">
        <template #prepend>
          <el-icon>
            <Microphone />
          </el-icon>
        </template>
      </el-input>
      <div class="text-xs text-gray-400 mt-1">
        Nếu không có audio, hệ thống sẽ sử dụng Text-to-Speech mặc định.
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Microphone } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  targetWord: props.metadata?.targetWord || '',
  ipa: props.metadata?.ipa || '',
  audioUrl: props.metadata?.audioUrl || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) localMetadata.value = { ...newVal }
}, { deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.pronunciation-form {
  padding: 10px 0;
}
</style>
