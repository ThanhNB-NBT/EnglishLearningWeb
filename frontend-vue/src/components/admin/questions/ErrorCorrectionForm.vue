<template>
  <div class="error-correction-form">
    <el-alert title="Cách tạo câu hỏi tìm lỗi sai" type="warning" :closable="false" show-icon class="mb-4">
      <template #default>
        Hệ thống sẽ gạch chân phần sai hoặc yêu cầu người dùng click vào từ sai.<br>
        Nhập chính xác <b>từ/cụm từ sai</b> có trong câu hỏi.
      </template>
    </el-alert>

    <el-form-item label="Chi tiết lỗi sai" required>
      <el-card shadow="never" class="error-card">
        <el-form-item label="Từ/Cụm từ sai (Trong bài)" class="mb-3">
          <el-input v-model="localMetadata.errorText" placeholder="VD: go (trong câu 'He go to school')"
            @input="emitUpdate">
            <template #prefix><el-icon class="text-danger">
                <CloseBold />
              </el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="Sửa lại cho đúng" class="mb-0">
          <el-input v-model="localMetadata.correction" placeholder="VD: goes" @input="emitUpdate">
            <template #prefix><el-icon class="text-success"><Select /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-card>
    </el-form-item>

    <el-form-item label="Giải thích chi tiết">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="3"
        placeholder="Tại sao lại sai? (VD: Chủ ngữ số ít 'He' thì động từ phải thêm 'es')..." @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { CloseBold, Select } from '@element-plus/icons-vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  errorText: props.metadata?.errorText || '',
  correction: props.metadata?.correction || '',
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal) {
    localMetadata.value = {
      errorText: newVal.errorText || '',
      correction: newVal.correction || '',
      explanation: newVal.explanation || ''
    }
  }
}, { deep: true })

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.error-correction-form {
  padding: 10px 0;
}

.error-card {
  background-color: #fff6f6;
  border-color: #fab6b6;
}

.text-danger {
  color: #f56c6c;
}

.text-success {
  color: #67c23a;
}

.mb-3 {
  margin-bottom: 12px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
