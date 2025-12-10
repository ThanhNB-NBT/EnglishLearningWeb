<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="800px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    @close="handleClose"
    class="!rounded-xl"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="p-2">
      <el-form-item label="Tiêu đề bài học" prop="title" class="!mb-5">
        <el-input
          v-model="formData.title"
          placeholder="Nhập tiêu đề bài học..."
          maxlength="200"
          show-word-limit
          size="large"
        />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2">
        <el-form-item label="Loại bài học" prop="lessonType">
          <el-select v-model="formData.lessonType" class="!w-full" size="large">
            <el-option label="Lý thuyết (Theory)" value="THEORY">
              <span class="flex items-center gap-2"
                ><el-icon><Reading /></el-icon> Lý thuyết</span
              >
            </el-option>
            <el-option label="Thực hành (Practice)" value="PRACTICE">
              <span class="flex items-center gap-2"
                ><el-icon><EditPen /></el-icon> Thực hành</span
              >
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
          <el-input-number
            v-model="formData.orderIndex"
            :min="1"
            class="!w-full"
            size="large"
            controls-position="right"
          />
        </el-form-item>

        <el-form-item label="Thời gian ước tính (Giây)" prop="timeLimitSeconds">
          <el-input-number
            v-model="formData.timeLimitSeconds"
            :min="0"
            :step="10"
            class="!w-full"
            size="large"
            controls-position="right"
          />
          <div class="text-xs text-gray-400 mt-1">
            ~ {{ (formData.timeLimitSeconds / 60).toFixed(1) }} phút
          </div>
        </el-form-item>

        <el-form-item label="Điểm thưởng" prop="pointsReward">
          <el-input-number
            v-model="formData.pointsReward"
            :min="0"
            class="!w-full"
            size="large"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <el-form-item label="Nội dung bài học" prop="content" class="!mb-5">
        <div class="w-full border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
          <QuillRichEditor
            v-model="formData.content"
            placeholder="Soạn thảo nội dung bài học..."
            height="300px"
            :toolbar="formData.lessonType === 'THEORY' ? 'full' : 'basic'"
          />
        </div>
      </el-form-item>

      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          active-text="Kích hoạt"
          inactive-text="Ẩn"
          style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
        <el-button @click="handleClose" class="!rounded-lg">Hủy</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="onSubmit"
          class="!rounded-lg !font-bold px-6"
        >
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
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { Reading, EditPen } from '@element-plus/icons-vue'

const props = defineProps({
  topicId: { type: Number, default: null },
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
  closeDialog,
} = useGrammarLessonForm()

const handleOpenCreate = async () => {
  await openCreateDialog(props.topicId)
}

defineExpose({
  openCreate: handleOpenCreate,
  openEdit: (lesson) => openEditDialog(lesson),
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
