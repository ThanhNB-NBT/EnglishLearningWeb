<template>
  <div class="space-y-4">
    <el-alert
      title="Đề bài chính nhập ở phần 'Nội dung câu hỏi' phía trên."
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form-item label="Từ/Cụm từ sai" label-width="130px">
      <el-input v-model="errorText" placeholder="VD: goes" @input="syncToParent" />
    </el-form-item>

    <el-form-item label="Sửa lại đúng" label-width="130px">
      <el-input v-model="correction" placeholder="VD: went" @input="syncToParent" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const errorText = ref('')
const correction = ref('')

const initData = () => {
  if (props.metadata.errorText !== undefined) {
    errorText.value = props.metadata.errorText || ''
    correction.value = props.metadata.correction || ''
  } else {
    errorText.value = ''
    correction.value = ''
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [props.metadata.errorText, props.metadata.correction],
  ([newError, newCorrection]) => {
    if (newError !== errorText.value) {
      errorText.value = newError || ''
    }
    if (newCorrection !== correction.value) {
      correction.value = newCorrection || ''
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    errorText: errorText.value,
    correction: correction.value,
  })
}

const validate = () => {
  if (!errorText.value || !correction.value) {
    ElMessage.warning('Vui lòng nhập phần lỗi và phần sửa lại')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
