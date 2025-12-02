<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="500px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      label-position="top"
    >
      <el-form-item label="Tên chủ đề" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="Ví dụ: Present Simple"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <div class="flex-row">
        <el-form-item label="Trình độ" prop="levelRequired" style="flex: 1">
          <el-select v-model="formData.levelRequired" placeholder="Chọn trình độ" style="width: 100%">
            <el-option label="Beginner (A1-A2)" value="BEGINNER" />
            <el-option label="Intermediate (B1-B2)" value="INTERMEDIATE" />
            <el-option label="Advanced (C1-C2)" value="ADVANCED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự" prop="orderIndex" style="width: 120px">
          <el-input-number v-model="formData.orderIndex" :min="1" style="width: 100%" />
        </el-form-item>
      </div>

      <el-form-item label="Mô tả" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="Mô tả ngắn gọn về chủ đề này..."
        />
      </el-form-item>

      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          active-text="Kích hoạt"
          inactive-text="Ẩn"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Hủy</el-button>
        <el-button type="primary" :loading="loading" @click="onSubmit">
          {{ submitButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useGrammarStore } from '@/stores/grammar' // Import store trực tiếp
import { useGrammarTopicForm } from '@/composables/grammar/useGrammarTopics'

const emit = defineEmits(['success'])
const store = useGrammarStore() // Khởi tạo store

defineExpose({
  openCreate: () => openCreateDialog(),
  openEdit: (topic) => openEditDialog(topic)
})

const formRef = ref(null)
const loading = ref(false)

// Lấy các state và function từ composable
// LƯU Ý: Không dùng handleSubmit của composable nữa vì nó bị lỗi logic return
const {
  dialogVisible,
  dialogMode, // Lấy thêm dialogMode để biết đang Create hay Edit
  formData,
  formRules,
  dialogTitle,
  submitButtonText,
  openCreateDialog,
  openEditDialog,
  closeDialog
} = useGrammarTopicForm()

const handleClose = () => {
  closeDialog()
  if (formRef.value) formRef.value.resetFields()
}

// Viết lại hàm Submit xử lý trực tiếp tại đây
const onSubmit = async () => {
  if (!formRef.value) return

  // 1. Validate Form
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 2. Gọi API thông qua Store
        if (dialogMode.value === 'create') {
          await store.createTopic(formData.value)
        } else {
          // Khi edit, formData đã có ID từ openEditDialog
          await store.updateTopic(formData.value.id, formData.value)
        }

        // 3. Nếu không có lỗi (Store tự bắn ElMessage success), chạy tiếp:
        handleClose()   // Đóng dialog
        emit('success') // Báo ra ngoài để reload list

      } catch (error) {
        console.error('Submit error:', error)
        // Store đã xử lý hiện lỗi (ElMessage.error) nên không cần làm gì thêm
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.flex-row {
  display: flex;
  gap: 16px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Mobile responsive */
@media (max-width: 480px) {
  .flex-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
