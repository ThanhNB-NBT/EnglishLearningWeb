<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="900px"
    align-center
    :close-on-click-modal="false"
    destroy-on-close
    class="! rounded-2xl"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      class="space-y-4"
      size="large"
    >
      <el-form-item label="Tiêu đề bài đọc" prop="title">
        <el-input
          v-model="formData. title"
          placeholder="Nhập tiêu đề bài đọc (VD: The Benefits of Reading)"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <el-form-item label="Độ khó" prop="difficulty">
          <el-select v-model="formData.difficulty" placeholder="Chọn độ khó" class="! w-full">
            <el-option label="Dễ (Beginner)" value="BEGINNER">
              <span class="flex items-center gap-2">
                <el-icon color="#67c23a"><CircleCheck /></el-icon> Dễ
              </span>
            </el-option>
            <el-option label="Trung bình (Intermediate)" value="INTERMEDIATE">
              <span class="flex items-center gap-2">
                <el-icon color="#e6a23c"><Warning /></el-icon> Trung bình
              </span>
            </el-option>
            <el-option label="Khó (Advanced)" value="ADVANCED">
              <span class="flex items-center gap-2">
                <el-icon color="#f56c6c"><Close /></el-icon> Khó
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
          <el-input-number
            v-model="formData.orderIndex"
            :min="1"
            class="!w-full"
            controls-position="right"
          />
        </el-form-item>

        <el-form-item label="Điểm thưởng" prop="pointsReward">
          <el-input-number
            v-model="formData.pointsReward"
            :min="0"
            :step="10"
            class="!w-full"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <el-form-item label="Thời gian đọc (Giây)" prop="timeLimitSeconds">
        <el-input-number
          v-model="formData.timeLimitSeconds"
          :min="0"
          :step="60"
          class="!w-full"
          controls-position="right"
        />
        <div class="text-xs text-gray-400 mt-1">
          ~ {{ (formData.timeLimitSeconds / 60).toFixed(1) }} phút
        </div>
      </el-form-item>

      <!-- 🆕 Nội dung bài đọc (Tiếng Anh) -->
      <el-form-item label="Nội dung bài đọc (Tiếng Anh)" prop="content" class="! mb-5">
        <div class="w-full border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
          <QuillRichEditor
            v-model="formData.content"
            placeholder="Nhập nội dung bài đọc bằng tiếng Anh..."
            height="350px"
            toolbar="full"
          />
        </div>
        <div class="text-xs text-gray-400 mt-1">
          📖 Nội dung chính của bài đọc (bắt buộc)
        </div>
      </el-form-item>

      <!-- 🆕 Bản dịch tiếng Việt (Optional) -->
      <el-form-item label="Bản dịch tiếng Việt (Tùy chọn)" prop="contentTranslation" class="!mb-5">
        <div class="w-full border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
          <QuillRichEditor
            v-model="formData.contentTranslation"
            placeholder="Nhập bản dịch tiếng Việt (không bắt buộc)..."
            height="350px"
            toolbar="full"
          />
        </div>
        <div class="text-xs text-gray-400 mt-1">
          🇻🇳 Bản dịch này sẽ giúp học viên hiểu rõ hơn (có thể bỏ trống)
        </div>
      </el-form-item>

      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          active-text="Kích hoạt (hiển thị cho học viên)"
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
          class="! rounded-lg ! font-bold px-6"
        >
          {{ submitButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useReadingStore } from '@/stores/reading'
import { useReadingLessonForm } from '@/composables/reading/useReadingLessons'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { CircleCheck, Warning, Close } from '@element-plus/icons-vue'

const emit = defineEmits(['success'])

const store = useReadingStore()
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
  handleSubmit,
} = useReadingLessonForm()

const onSubmit = async () => {
  loading.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      await store.fetchLessons({ size: 1000 })
      closeDialog()
      emit('success')
    }
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  closeDialog()
}

// Expose methods
const openCreate = () => openCreateDialog()
const openEdit = (lesson) => openEditDialog(lesson)

defineExpose({ openCreate, openEdit })
</script>
