<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="900px"
    align-center
    :close-on-click-modal="false"
    destroy-on-close
    class="!rounded-2xl"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      class="space-y-4"
      size="large"
    >
      <el-form-item label="Tiêu đề bài nghe" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="Nhập tiêu đề (VD: Daily Conversation)"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <!-- Audio Upload -->
      <el-form-item
        :label="dialogMode === 'create' ? 'File âm thanh' : 'File âm thanh (tùy chọn)'"
        prop="audioFile"
      >
        <el-upload
          class="w-full"
          drag
          :auto-upload="false"
          :limit="1"
          accept="audio/mp3,audio/wav,audio/mpeg"
          :on-change="handleAudioChange"
          :on-remove="handleAudioRemove"
          :file-list="audioFileList"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">Kéo thả file hoặc <em>click để chọn</em></div>
          <template #tip>
            <div class="el-upload__tip text-xs">
              Chấp nhận: MP3, WAV (tối đa 50MB)
              <span v-if="formData.currentAudioUrl" class="text-blue-500 ml-2">✓ Đã có audio</span>
            </div>
          </template>
        </el-upload>
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
            :min="1"
            :max="100"
            class="!w-full"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <!-- Transcript -->
      <el-form-item label="Transcript (Bản gốc)" prop="transcript">
        <el-input
          v-model="formData.transcript"
          type="textarea"
          :rows="6"
          placeholder="Nhập nội dung transcript tiếng Anh..."
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="Bản dịch Transcript (Tiếng Việt)">
        <el-input
          v-model="formData.transcriptTranslation"
          type="textarea"
          :rows="4"
          placeholder="Nhập bản dịch tiếng Việt (tùy chọn)..."
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>

      <!-- Advanced Settings -->
      <el-divider content-position="left">
        <span class="text-sm text-gray-500">Cài đặt nâng cao</span>
      </el-divider>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <el-form-item label="Thời gian làm bài (giây)" prop="timeLimitSeconds">
          <el-input-number
            v-model="formData.timeLimitSeconds"
            :min="60"
            :max="3600"
            :step="60"
            class="!w-full"
            controls-position="right"
          />
          <div class="text-xs text-gray-500 mt-1">
            ≈ {{ Math.floor(formData.timeLimitSeconds / 60) }} phút
          </div>
        </el-form-item>

        <el-form-item label="Số lần phát lại tối đa" prop="maxReplayCount">
          <div class="flex flex-col gap-2 w-full">
            <el-switch
              v-model="formData.allowUnlimitedReplay"
              active-text="Không giới hạn"
              inactive-text="Giới hạn"
            />
            <el-input-number
              v-if="! formData.allowUnlimitedReplay"
              v-model="formData.maxReplayCount"
              :min="1"
              :max="10"
              class="!w-full"
              controls-position="right"
            />
          </div>
        </el-form-item>
      </div>

      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          active-text="Kích hoạt"
          inactive-text="Ẩn"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-2">
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
import { UploadFilled, CircleCheck, Warning, Close } from '@element-plus/icons-vue'
import { useListeningLessonForm } from '@/composables/listening/useListeningLessons'
import { useListeningStore } from '@/stores/listening'

// Store
const store = useListeningStore()

// Composables
const {
  dialogVisible,
  dialogMode,
  formData,
  formRules,
  formRef,
  audioFileList,
  dialogTitle,
  submitButtonText,
  openCreateDialog,
  openEditDialog,
  closeDialog,
  handleSubmit,
  handleAudioChange,
  handleAudioRemove,
} = useListeningLessonForm()

// Local state
const loading = ref(false)

// Emits
const emit = defineEmits(['success'])

// Methods
const onSubmit = async () => {
  loading.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      await store.fetchLessons({ page: 0, size: 1000, sort: 'orderIndex,asc' })
      closeDialog()
      audioFileList.value = []
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
  audioFileList.value = []
}

// Expose methods
const openCreate = () => openCreateDialog()
const openEdit = (lesson) => openEditDialog(lesson)

defineExpose({
  openCreate,
  openEdit,
})
</script>
