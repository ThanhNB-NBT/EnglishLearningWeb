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
      <el-form-item :label="dialogMode === 'create' ? 'File âm thanh' : 'File âm thanh (tùy chọn)'" prop="audioFile">
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
          <div class="el-upload__text">
            Kéo thả file hoặc <em>click để chọn</em>
          </div>
          <template #tip>
            <div class="el-upload__tip text-xs">
              Chấp nhận: MP3, WAV (tối đa 50MB)
              <span v-if="formData.currentAudioUrl" class="text-blue-500 ml-2">
                ✓ Đã có audio
              </span>
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <el-form-item label="Độ khó" prop="difficulty">
          <el-select v-model="formData.difficulty" placeholder="Chọn độ khó" class="!w-full">
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
            :step="5"
            class="!w-full"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <el-form-item label="Thời gian làm bài (Giây)" prop="timeLimitSeconds">
        <el-input-number
          v-model="formData.timeLimitSeconds"
          :min="60"
          :step="60"
          class="!w-full"
          controls-position="right"
        />
        <div class="text-xs text-gray-400 mt-1">
          ~ {{ (formData.timeLimitSeconds / 60).toFixed(1) }} phút
        </div>
      </el-form-item>

      <!-- Replay Settings -->
      <div class="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg border border-blue-200 dark:border-blue-800">
        <h4 class="font-bold text-blue-600 dark:text-blue-400 mb-3 flex items-center gap-2">
          <el-icon><Headset /></el-icon> Cài đặt phát lại
        </h4>

        <el-form-item label="Cho phép phát lại không giới hạn" class="!mb-3">
          <el-switch
            v-model="formData.allowUnlimitedReplay"
            active-text="Không giới hạn"
            inactive-text="Giới hạn"
            style="--el-switch-on-color: #13ce66"
          />
        </el-form-item>

        <el-form-item v-if="!formData.allowUnlimitedReplay" label="Số lần phát lại tối đa" prop="maxReplayCount">
          <el-input-number
            v-model="formData.maxReplayCount"
            :min="1"
            :max="10"
            class="!w-full"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <!-- Transcript -->
      <el-form-item label="Transcript (Bản ghi âm)" prop="transcript">
        <el-input
          v-model="formData.transcript"
          type="textarea"
          :rows="5"
          placeholder="Nhập nội dung transcript..."
        />
      </el-form-item>

      <!-- Translation -->
      <el-form-item label="Bản dịch tiếng Việt" prop="transcriptTranslation">
        <el-input
          v-model="formData.transcriptTranslation"
          type="textarea"
          :rows="5"
          placeholder="Nhập bản dịch..."
        />
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
import { useListeningStore } from '@/stores/listening'
import { useListeningLessonForm } from '@/composables/listening/useListeningLessons'
import { CircleCheck, Warning, Close, UploadFilled, Headset } from '@element-plus/icons-vue'

const emit = defineEmits(['success'])

const store = useListeningStore()
const formRef = ref(null)
const loading = ref(false)
const audioFileList = ref([])

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
} = useListeningLessonForm()

const handleAudioChange = (file) => {
  // Validate file type
  const validTypes = ['audio/mp3', 'audio/mpeg', 'audio/wav']
  if (!validTypes.includes(file.raw.type)) {
    ElMessage.error('Chỉ chấp nhận file MP3 hoặc WAV')
    audioFileList.value = []
    return
  }

  // Validate file size (50MB)
  const maxSize = 50 * 1024 * 1024
  if (file.raw.size > maxSize) {
    ElMessage.error('File không được vượt quá 50MB')
    audioFileList.value = []
    return
  }

  formData.value.audioFile = file.raw
  audioFileList.value = [file]
}

const handleAudioRemove = () => {
  formData.value.audioFile = null
  audioFileList.value = []
}

const onSubmit = async () => {
  loading.value = true
  try {
    const success = await handleSubmit(formRef.value)
    if (success) {
      await store.fetchLessons()
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

defineExpose({ openCreate, openEdit })
</script>
