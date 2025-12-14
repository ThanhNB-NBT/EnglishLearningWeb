// src/composables/listening/useListeningLessons.js
import { ref, computed } from 'vue'
import { useListeningStore } from '@/stores/listening'
import { ElMessage } from 'element-plus'

export function useListeningLessonForm() {
  const store = useListeningStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'

  const formData = ref({
    id: null,
    title: '',
    transcript: '',
    transcriptTranslation: '',
    difficulty: 'BEGINNER',
    orderIndex: 1,
    timeLimitSeconds: 600,
    pointsReward: 25,
    allowUnlimitedReplay: true,
    maxReplayCount: 3,
    isActive: true,
    audioFile: null, // File object
  })

  const formRules = {
    title: [
      { required: true, message: 'Vui lòng nhập tiêu đề bài nghe', trigger: 'blur' },
      { min: 5, max: 200, message: 'Độ dài từ 5-200 ký tự', trigger: 'blur' },
    ],
    audioFile: [
      { required: true, message: 'Vui lòng upload file audio', trigger: 'change' },
    ],
    transcript: [
      { required: true, message: 'Vui lòng nhập transcript', trigger: 'blur' },
    ],
    transcriptTranslation: [
      { required: true, message: 'Vui lòng nhập bản dịch', trigger: 'blur' },
    ],
    difficulty: [
      { required: true, message: 'Vui lòng chọn độ khó', trigger: 'change' },
    ],
    orderIndex: [
      { required: true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải >= 1', trigger: 'blur' },
    ],
    timeLimitSeconds: [
      { required: true, message: 'Vui lòng nhập thời gian', trigger: 'blur' },
      { type: 'number', min: 60, message: 'Thời gian phải >= 60 giây', trigger: 'blur' },
    ],
    pointsReward: [
      { required: true, message: 'Vui lòng nhập điểm thưởng', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Điểm phải >= 1', trigger: 'blur' },
    ],
    maxReplayCount: [
      { type: 'number', min: 1, max: 10, message: 'Số lần replay từ 1-10', trigger: 'blur' },
    ],
  }

  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo bài nghe mới' : 'Chỉnh sửa bài nghe'
  )

  const submitButtonText = computed(() =>
    dialogMode.value === 'create' ? 'Tạo bài nghe' : 'Cập nhật'
  )

  const openCreateDialog = async () => {
    dialogMode.value = 'create'
    const nextOrder = await store.getNextLessonOrderIndex()
    formData.value = {
      id: null,
      title: '',
      transcript: '',
      transcriptTranslation: '',
      difficulty: 'BEGINNER',
      orderIndex: nextOrder,
      timeLimitSeconds: 600,
      pointsReward: 25,
      allowUnlimitedReplay: true,
      maxReplayCount: 3,
      isActive: true,
      audioFile: null,
    }
    dialogVisible.value = true
  }

  const openEditDialog = (lesson) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: lesson.id,
      title: lesson.title,
      transcript: lesson.transcript || '',
      transcriptTranslation: lesson.transcriptTranslation || '',
      difficulty: lesson.difficulty,
      orderIndex: lesson.orderIndex,
      timeLimitSeconds: lesson.timeLimitSeconds || 600,
      pointsReward: lesson.pointsReward || 25,
      allowUnlimitedReplay: lesson.allowUnlimitedReplay ?? true,
      maxReplayCount: lesson.maxReplayCount || 3,
      isActive: lesson.isActive ?? true,
      audioFile: null, // File sẽ optional khi edit
      currentAudioUrl: lesson.audioUrl, // Keep track of current audio
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    formData.value = {
      id: null,
      title: '',
      transcript: '',
      transcriptTranslation: '',
      difficulty: 'BEGINNER',
      orderIndex: 1,
      timeLimitSeconds: 600,
      pointsReward: 25,
      allowUnlimitedReplay: true,
      maxReplayCount: 3,
      isActive: true,
      audioFile: null,
    }
  }

  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    await formEl.validate()

    // Create FormData for multipart/form-data
    const formDataToSend = new FormData()

    // Add request JSON part
    const requestJson = {
      title: formData.value.title,
      transcript: formData.value.transcript,
      transcriptTranslation: formData.value.transcriptTranslation,
      difficulty: formData.value.difficulty,
      orderIndex: formData.value.orderIndex,
      timeLimitSeconds: formData.value.timeLimitSeconds,
      pointsReward: formData.value.pointsReward,
      allowUnlimitedReplay: formData.value.allowUnlimitedReplay,
      maxReplayCount: formData.value.maxReplayCount,
      isActive: formData.value.isActive,
    }

    formDataToSend.append('request', new Blob([JSON.stringify(requestJson)], { type: 'application/json' }))

    // Add audio file if present
    if (formData.value.audioFile) {
      formDataToSend.append('audio', formData.value.audioFile)
    } else if (dialogMode.value === 'create') {
      ElMessage.error('Vui lòng upload file audio')
      return false
    }

    try {
      if (dialogMode.value === 'create') {
        await store.createLesson(formDataToSend)
      } else {
        await store.updateLesson(formData.value.id, formDataToSend)
      }
      return true
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Lỗi khi lưu bài nghe')
      throw error
    }
  }

  return {
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
  }
}
