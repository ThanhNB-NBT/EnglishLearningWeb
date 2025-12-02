<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="800px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    @close="handleClose"
    class="lesson-dialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      label-position="top"
    >
      <el-form-item label="Tiêu đề bài học" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="Nhập tiêu đề bài học..."
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <div class="form-row">
        <el-form-item label="Loại bài học" prop="lessonType" style="flex: 1">
          <el-select v-model="formData.lessonType" style="width: 100%">
            <el-option label="Lý thuyết (Theory)" value="THEORY" />
            <el-option label="Thực hành (Practice)" value="PRACTICE" />
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự" prop="orderIndex" style="width: 120px">
          <el-input-number v-model="formData.orderIndex" :min="1" style="width: 100%" />
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="Thời gian ước tính (Giây)" prop="estimatedDuration" style="flex: 1">
          <el-input-number
            v-model="formData.estimatedDuration"
            :min="0"
            :step="10"
            style="width: 100%"
          />
          <div class="help-text">
            ~ {{ (formData.estimatedDuration / 60).toFixed(1) }} phút
          </div>
        </el-form-item>

        <el-form-item label="Điểm thưởng" prop="pointsReward" style="flex: 1">
          <el-input-number v-model="formData.pointsReward" :min="0" style="width: 100%" />
        </el-form-item>
      </div>

      <el-form-item label="Nội dung bài học" prop="content">
        <QuillRichEditor
          v-model="formData.content"
          placeholder="Soạn thảo nội dung bài học..."
          height="300px"
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
import { useGrammarStore } from '@/stores/grammar'
import { useGrammarLessonForm } from '@/composables/grammar/useGrammarLessons'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue' // Import Editor

const props = defineProps({
  topicId: { type: Number, default: null }
})

const emit = defineEmits(['success'])
const store = useGrammarStore()
const formRef = ref(null)
const loading = ref(false)

const {
  dialogVisible,
  dialogMode,
  formData,
  formRules,
  dialogTitle,
  submitButtonText,
  openCreateDialog,
  openEditDialog,
  closeDialog
} = useGrammarLessonForm()

const handleOpenCreate = async () => {
  await openCreateDialog(props.topicId)
}

defineExpose({
  openCreate: handleOpenCreate,
  openEdit: (lesson) => openEditDialog(lesson)
})

const handleClose = () => {
  closeDialog()
  if (formRef.value) formRef.value.resetFields()
}

const onSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (dialogMode.value === 'create') {
          if (!formData.value.topicId && props.topicId) {
            formData.value.topicId = props.topicId
          }
          await store.createLesson(formData.value)
        } else {
          await store.updateLesson(formData.value.id, formData.value)
        }
        handleClose()
        emit('success')
      } catch (error) {
        console.error('Submit error:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.form-row {
  display: flex;
  gap: 16px;
}
.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  font-style: italic;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 600px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
