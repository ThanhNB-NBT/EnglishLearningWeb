<!-- src/components/admin/shared/topic/TopicForm.vue - WITH COMPOSABLE -->
<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? 'Cập nhật Chủ đề' : 'Thêm Chủ đề mới'"
    width="600px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    class="!rounded-xl"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="p-2"
    >
      <!-- Topic Name -->
      <el-form-item label="Tên chủ đề" prop="name" class="!mb-5">
        <el-input
          v-model="form.name"
          placeholder="VD: Present Simple Tense, TOEIC Part 1..."
          size="large"
          clearable
          maxlength="200"
          show-word-limit
          :disabled="topicOps.loading.value"
        />
      </el-form-item>

      <!-- Level and Order -->
      <div class="grid grid-cols-2 gap-5 mb-2">
        <el-form-item label="Trình độ" prop="levelRequired">
          <el-select
            v-model="form.levelRequired"
            placeholder="Chọn trình độ"
            class="!w-full"
            size="large"
            :disabled="topicOps.loading.value"
          >
            <el-option label="A1 - Beginner" value="A1" />
            <el-option label="A2 - Elementary" value="A2" />
            <el-option label="B1 - Intermediate" value="B1" />
            <el-option label="B2 - Upper Intermediate" value="B2" />
            <el-option label="C1 - Advanced" value="C1" />
            <el-option label="C2 - Proficiency" value="C2" />
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
          <el-input-number
            v-model="form.orderIndex"
            :min="0"
            :max="9999"
            class="!w-full"
            size="large"
            controls-position="right"
            placeholder="Tự động"
            :disabled="topicOps.loading.value"
          />
        </el-form-item>
      </div>

      <!-- Description -->
      <el-form-item label="Mô tả ngắn" prop="description" class="!mb-5">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="Mô tả nội dung chính của chủ đề này..."
          resize="none"
          maxlength="500"
          show-word-limit
          :disabled="topicOps.loading.value"
        />
      </el-form-item>

      <!-- Status Toggle -->
      <el-form-item label="Trạng thái">
        <div class="flex items-center gap-3 p-3 border border-gray-200 dark:border-gray-700 rounded-lg w-full">
          <el-switch
            v-model="form.isActive"
            :disabled="topicOps.loading.value"
            style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
          />
          <span class="text-sm font-medium" :class="form.isActive ? 'text-green-600' : 'text-red-500'">
            {{ form.isActive ? '✓ Đang kích hoạt' : '✕ Đang ẩn' }}
          </span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
        <el-button
          @click="handleCancel"
          :disabled="topicOps.loading.value"
          class="!rounded-lg"
        >
          Hủy bỏ
        </el-button>
        <el-button
          type="primary"
          :loading="topicOps.loading.value"
          @click="handleSubmit"
          class="!rounded-lg !font-bold px-6"
        >
          {{ isEdit ? '💾 Lưu thay đổi' : '➕ Tạo mới' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useTopic } from '@/composables/topic/useTopic'

const props = defineProps({
  moduleType: {
    type: String,
    required: true,
    validator: (v) => ['GRAMMAR', 'LISTENING', 'READING'].includes(v),
  },
})

const emit = defineEmits(['success'])

// Composable
const topicOps = useTopic(props.moduleType)

// Refs
const formRef = ref(null)
const visible = ref(false)
const isEdit = ref(false)

// Form state
const form = reactive({
  id: null,
  name: '',
  description: '',
  levelRequired: 'A1',
  orderIndex: null,
  isActive: true,
})

// Validation rules
const rules = {
  name: [
    { required: true, message: 'Vui lòng nhập tên chủ đề', trigger: 'blur' },
    { min: 3, max: 200, message: 'Tên phải từ 3-200 ký tự', trigger: 'blur' },
  ],
  levelRequired: [
    { required: true, message: 'Vui lòng chọn trình độ', trigger: 'change' },
  ],
  description: [
    { max: 500, message: 'Mô tả không quá 500 ký tự', trigger: 'blur' },
  ],
}

// ==================== PUBLIC METHODS ====================

const openCreate = () => {
  console.log('📝 [Form] Opening CREATE')
  isEdit.value = false
  resetFormState()

  // Get next order index
  form.orderIndex = topicOps.getNextOrderIndex()

  visible.value = true
}

const openEdit = (topic) => {
  console.log('✏️ [Form] Opening EDIT:', topic.id)
  isEdit.value = true

  // Copy data
  Object.assign(form, {
    id: topic.id,
    name: topic.name || '',
    description: topic.description || '',
    levelRequired: topic.levelRequired || 'A1',
    orderIndex: topic.orderIndex || 0,
    isActive: topic.isActive ?? true,
  })

  visible.value = true
}

// ==================== PRIVATE METHODS ====================

const resetFormState = () => {
  form.id = null
  form.name = ''
  form.description = ''
  form.levelRequired = 'A1'
  form.orderIndex = null
  form.isActive = true
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.clearValidate()
    formRef.value.resetFields()
  }
}

const handleClose = () => {
  resetForm()
}

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    // Validate
    const isValid = await formRef.value.validate()
    if (!isValid) return

    // Prepare payload
    const payload = {
      name: form.name.trim(),
      description: form.description?.trim() || '',
      levelRequired: form.levelRequired,
      orderIndex: form.orderIndex || null,
      isActive: form.isActive,
    }

    // Call composable (handles success/error messages)
    if (isEdit.value) {
      await topicOps.updateTopic(form.id, payload)
    } else {
      await topicOps.createTopic(payload)
    }

    // Close dialog & emit success
    visible.value = false
    emit('success')
    resetFormState()

  } catch (error) {
    // Error already handled by composable
    console.log('[Form] Error handled by composable: ', error)
  }
}

// ==================== EXPOSE ====================

defineExpose({
  openCreate,
  openEdit,
  reset: resetFormState,
})
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  transition: all 0.2s ease;
}

:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

html.dark :deep(.el-dialog) {
  background-color: #1d1d1d;
}

html.dark :deep(.el-form-item__label) {
  color: #e5e7eb;
}
</style>
