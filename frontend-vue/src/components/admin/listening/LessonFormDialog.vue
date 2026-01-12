<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? 'Thêm bài nghe mới' : 'Cập nhật bài nghe'"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="150px"
      class="mt-4"
      status-icon
    >
      <el-form-item label="Tiêu đề bài học" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="VD: Daily Conversation - At the airport"
          clearable
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
            <el-input-number
              v-model="formData.orderIndex"
              :min="1"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Thời gian làm bài" prop="timeLimitSeconds">
            <el-input-number
              v-model="formData.timeLimitSeconds"
              :min="60"
              :step="30"
              class="!w-full"
              placeholder="Giây"
            >
              <template #suffix>giây</template>
            </el-input-number>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Điểm thưởng">
            <el-input-number v-model="formData.pointsReward" :min="0" :step="5" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Trạng thái">
            <el-switch
              v-model="formData.isActive"
              inline-prompt
              active-text="Hiện"
              inactive-text="Ẩn"
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg mb-4 mx-0">
        <el-col :span="12">
          <el-form-item label="Nghe lại vô hạn?" class="!mb-0">
            <el-switch v-model="formData.allowUnlimitedReplay" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Số lần tối đa" prop="maxReplayCount" class="!mb-0">
            <el-input-number
              v-model="formData.maxReplayCount"
              :disabled="formData.allowUnlimitedReplay"
              :min="1"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="File Audio" prop="audioFile">
        <div class="w-full">
          <input
            type="file"
            ref="fileInput"
            accept="audio/mp3,audio/wav,audio/mpeg"
            @change="handleFileChange"
            class="hidden"
            id="audio-upload"
          />

          <label
            for="audio-upload"
            class="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer bg-gray-50 dark:hover:bg-gray-800 dark:bg-gray-700 hover:bg-gray-100 dark:border-gray-600 dark:hover:border-gray-500 transition-all"
            :class="{ '!border-blue-500 bg-blue-50': selectedFile }"
          >
            <div
              v-if="!selectedFile && !formData.audioUrl"
              class="flex flex-col items-center justify-center pt-5 pb-6"
            >
              <el-icon class="text-3xl text-gray-400 mb-2"><UploadFilled /></el-icon>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                <span class="font-semibold">Click để tải lên</span>
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400">MP3, WAV (Max 50MB)</p>
            </div>

            <div v-else-if="selectedFile" class="flex flex-col items-center text-blue-600">
              <el-icon class="text-3xl mb-2"><Headset /></el-icon>
              <p class="font-bold text-sm">{{ selectedFile.name }}</p>
              <p class="text-xs">Size: {{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
            </div>

            <div v-else class="flex flex-col items-center text-green-600">
              <el-icon class="text-3xl mb-2"><VideoPlay /></el-icon>
              <p class="font-bold text-sm">Đang sử dụng Audio hiện tại</p>
              <p class="text-xs text-gray-400 break-all px-4 text-center">
                {{ formData.audioUrl }}
              </p>
              <p class="text-xs text-orange-500 mt-1">(Click để thay file khác)</p>
            </div>
          </label>
        </div>
      </el-form-item>

      <el-tabs type="border-card" class="!rounded-lg !border-gray-200 shadow-sm">
        <el-tab-pane label="Transcript (Tiếng Anh)">
          <el-input
            v-model="formData.transcript"
            type="textarea"
            :rows="6"
            placeholder="Nhập lời thoại của bài nghe..."
            resize="none"
          />
        </el-tab-pane>
        <el-tab-pane label="Dịch nghĩa (Tiếng Việt)">
          <el-input
            v-model="formData.transcriptTranslation"
            type="textarea"
            :rows="6"
            placeholder="Nhập bản dịch tiếng Việt..."
            resize="none"
          />
        </el-tab-pane>
      </el-tabs>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="visible = false">Hủy bỏ</el-button>
        <el-button type="primary" :loading="isLoading" @click="handleSubmit" class="!px-6">
          {{ mode === 'create' ? 'Tạo bài nghe' : 'Lưu thay đổi' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, VideoPlay, Headset } from '@element-plus/icons-vue'
import { useListeningAdminStore } from '@/stores/admin/listeningAdmin'

const props = defineProps({ topicId: Number })
const emit = defineEmits(['success']) // Đổi tên emit cho chuẩn
const listeningAdminStore = useListeningAdminStore()

const visible = ref(false)
const mode = ref('create')
const isLoading = ref(false)
const formRef = ref(null)
const selectedFile = ref(null)
const fileInput = ref(null)

const formData = ref({
  id: null,
  title: '',
  audioUrl: '',
  transcript: '',
  transcriptTranslation: '',
  orderIndex: 1,
  timeLimitSeconds: 600,
  pointsReward: 25,
  allowUnlimitedReplay: true,
  maxReplayCount: 3,
  isActive: true,
})

const rules = reactive({
  title: [{ required: true, message: 'Vui lòng nhập tiêu đề', trigger: 'blur' }],
  orderIndex: [{ required: true, message: 'Nhập thứ tự', trigger: 'change' }],
})

// Mở form tạo mới
const openCreate = async () => {
  mode.value = 'create'
  selectedFile.value = null

  // Reset form data
  formData.value = {
    id: null,
    title: '',
    audioUrl: '',
    transcript: '',
    transcriptTranslation: '',
    orderIndex: 1, // Sẽ update sau khi gọi API
    timeLimitSeconds: 600,
    pointsReward: 25,
    allowUnlimitedReplay: true,
    maxReplayCount: 3,
    isActive: true,
  }

  // Tự động lấy orderIndex tiếp theo
  if (props.topicId) {
    try {
      const nextOrder = await listeningAdminStore.getNextOrderIndex(props.topicId)
      formData.value.orderIndex = nextOrder || 1
    } catch (e) {
      console.warn('Cannot fetch next order index', e)
    }
  }

  visible.value = true
}

// Mở form chỉnh sửa
const openEdit = (row) => {
  mode.value = 'edit'
  selectedFile.value = null
  // Clone object để không dính binding trực tiếp vào row của table
  formData.value = { ...row }
  visible.value = true
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // Validate file type & size
    const validTypes = ['audio/mpeg', 'audio/wav', 'audio/mp3', 'audio/x-wav']
    if (!validTypes.includes(file.type)) {
      ElMessage.warning('Chỉ hỗ trợ file MP3 hoặc WAV')
      return
    }
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.warning('File quá lớn (Max 50MB)')
      return
    }
    selectedFile.value = file
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (!props.topicId && mode.value === 'create') {
    ElMessage.error('Lỗi: Không xác định được chủ đề (Topic ID missing)')
    return
  }

  isLoading.value = true
  try {
    // ✅ FIX: Tạo FormData đơn giản
    const submitData = new FormData()

    // Required fields
    submitData.append('topicId', props.topicId || formData.value.topicId)
    submitData.append('title', formData.value.title)
    submitData.append('orderIndex', formData.value.orderIndex)
    submitData.append('timeLimitSeconds', formData.value.timeLimitSeconds)
    submitData.append('pointsReward', formData.value.pointsReward)
    submitData.append('allowUnlimitedReplay', formData.value.allowUnlimitedReplay)
    submitData.append('maxReplayCount', formData.value.maxReplayCount || 3)
    submitData.append('isActive', formData.value.isActive)
    submitData.append('transcript', formData.value.transcript || '')
    submitData.append('transcriptTranslation', formData.value.transcriptTranslation || '')

    // Audio file (chỉ khi có chọn file mới)
    if (selectedFile.value) {
      submitData.append('audio', selectedFile.value)
    }

    // ✅ FIX: Gọi store đúng cách
    if (mode.value === 'create') {
      if (!selectedFile.value) {
        ElMessage.warning('Vui lòng chọn file Audio cho bài học mới')
        isLoading.value = false
        return
      }
      await listeningAdminStore.createLesson(submitData)
    } else {
      // Update - FormData đã chứa cả text và file
      await listeningAdminStore.updateLesson(formData.value.id, submitData)
    }

    visible.value = false
    emit('success')
  } catch (err) {
    console.error('Submit error:', err)
    // Error đã được handle ở store
  } finally {
    isLoading.value = false
  }
}

defineExpose({ openCreate, openEdit })
</script>
