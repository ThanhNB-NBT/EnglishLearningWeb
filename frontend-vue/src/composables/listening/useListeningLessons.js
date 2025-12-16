// src/composables/listening/useListeningLessons.js
import { ref, computed } from 'vue'
import { useListeningStore } from '@/stores/listening'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useListeningLessonForm() {
  const store = useListeningStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'
  const formRef = ref(null)
  const audioFileList = ref([])

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
    currentAudioUrl: null, // For edit mode
  })

  const formRules = {
    title: [
      { required: true, message: 'Vui lòng nhập tiêu đề bài nghe', trigger: 'blur' },
      { min: 5, max: 200, message: 'Độ dài từ 5-200 ký tự', trigger: 'blur' },
    ],
    transcript: [
      { required: true, message: 'Vui lòng nhập transcript', trigger: 'blur' },
      { min: 10, message: 'Transcript phải có ít nhất 10 ký tự', trigger: 'blur' },
    ],
    difficulty: [{ required: true, message: 'Vui lòng chọn độ khó', trigger: 'change' }],
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
    dialogMode.value === 'create' ? 'Tạo bài nghe mới' : 'Chỉnh sửa bài nghe',
  )

  const submitButtonText = computed(() =>
    dialogMode.value === 'create' ? 'Tạo bài nghe' : 'Cập nhật',
  )

  // ═════════════════════════════════════════════════════════════════
  // DIALOG OPERATIONS
  // ═════════════════════════════════════════════════════════════════

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
      currentAudioUrl: null,
    }

    audioFileList.value = []
    dialogVisible.value = true
  }

  const openEditDialog = async (lesson) => {
    dialogMode.value = 'edit'

    formData.value = {
      id: lesson.id,
      title: lesson.title,
      transcript: lesson.transcript || '',
      transcriptTranslation: lesson.transcriptTranslation || '',
      difficulty: lesson.difficulty || 'BEGINNER',
      orderIndex: lesson.orderIndex,
      timeLimitSeconds: lesson.timeLimitSeconds || 600,
      pointsReward: lesson.pointsReward || 25,
      allowUnlimitedReplay: lesson.allowUnlimitedReplay !== false,
      maxReplayCount: lesson.maxReplayCount || 3,
      isActive: lesson.isActive !== false,
      audioFile: null,
      currentAudioUrl: lesson.audioUrl,
    }

    audioFileList.value = []
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
      currentAudioUrl: null,
    }
    audioFileList.value = []
    formRef.value?.clearValidate()
  }

  // ═════════════════════════════════════════════════════════════════
  // FORM SUBMISSION
  // ═════════════════════════════════════════════════════════════════

  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    return await formEl.validate(async (valid) => {
      if (!valid) {
        ElMessage.warning('Vui lòng kiểm tra lại thông tin')
        return false
      }

      // Validate audio file for create mode
      if (dialogMode.value === 'create' && !formData.value.audioFile) {
        ElMessage.error('Vui lòng upload file âm thanh')
        return false
      }

      try {
        // Build FormData
        const submitData = new FormData()
        submitData.append('title', formData.value.title)
        submitData.append('transcript', formData.value.transcript || '')
        submitData.append('transcriptTranslation', formData.value.transcriptTranslation || '')
        submitData.append('difficulty', formData.value.difficulty)
        submitData.append('orderIndex', formData.value.orderIndex)
        submitData.append('timeLimitSeconds', formData.value.timeLimitSeconds)
        submitData.append('pointsReward', formData.value.pointsReward)
        submitData.append('allowUnlimitedReplay', formData.value.allowUnlimitedReplay)
        submitData.append('maxReplayCount', formData.value.maxReplayCount)
        submitData.append('isActive', formData.value.isActive)

        // Append audio file if exists
        if (formData.value.audioFile) {
          submitData.append('audio', formData.value.audioFile)
        }

        if (dialogMode.value === 'create') {
          await store.createLesson(submitData)
        } else {
          await store.updateLesson(formData.value.id, submitData)
        }

        closeDialog()
        return true
      } catch (error) {
        console.error('Submit error:', error)
        return false
      }
    })
  }

  // ═════════════════════════════════════════════════════════════════
  // AUDIO FILE HANDLING
  // ═════════════════════════════════════════════════════════════════

  const handleAudioChange = (file) => {
    // Validate file type
    const validTypes = ['audio/mp3', 'audio/mpeg', 'audio/wav']
    if (!validTypes.includes(file.raw.type)) {
      ElMessage.error('Chỉ chấp nhận file MP3, WAV')
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

  return {
    // State
    dialogVisible,
    dialogMode,
    formData,
    formRules,
    formRef,
    audioFileList,

    // Computed
    dialogTitle,
    submitButtonText,

    // Methods
    openCreateDialog,
    openEditDialog,
    closeDialog,
    handleSubmit,
    handleAudioChange,
    handleAudioRemove,
  }
}

// ═════════════════════════════════════════════════════════════════
// LESSON LIST COMPOSABLE
// ═════════════════════════════════════════════════════════════════

export function useListeningLessonList() {
  const store = useListeningStore()

  const loading = computed(() => store.lessonsLoading)
  const lessons = computed(() => store.lessons)
  const pagination = computed(() => store.lessonsPagination)

  const searchQuery = ref('')
  const filterDifficulty = ref('')
  const filterStatus = ref('')

  // ═════════════════════════════════════════════════════════════════
  // FILTERING
  // ═════════════════════════════════════════════════════════════════

  const filteredLessons = computed(() => {
    let result = lessons.value

    // Search by title
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter((l) => l.title.toLowerCase().includes(query))
    }

    // Filter by difficulty
    if (filterDifficulty.value) {
      result = result.filter((l) => l.difficulty === filterDifficulty.value)
    }

    // Filter by status
    if (filterStatus.value !== '') {
      result = result.filter((l) => l.isActive === filterStatus.value)
    }

    return result
  })

  // ═════════════════════════════════════════════════════════════════
  // ACTIONS
  // ═════════════════════════════════════════════════════════════════

  const loadLessons = async (params) => {
    await store.fetchLessons(params)
  }

  const deleteLesson = async (lesson) => {
    try {
      await ElMessageBox.confirm(
        `Xóa bài nghe "${lesson.title}"? Hành động này sẽ xóa luôn câu hỏi, progress và audio file.`,
        'Xác nhận xóa',
        {
          confirmButtonText: 'Xóa',
          cancelButtonText: 'Hủy',
          type: 'warning',
          confirmButtonClass: 'el-button--danger',
        },
      )

      await store.deleteLesson(lesson.id)
      await loadLessons()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Delete error:', error)
      }
    }
  }

  const toggleStatus = async (lesson) => {
    try {
      await store.toggleLessonStatus(lesson.id)
    } catch (error) {
      console.error('Toggle status error:', error)
    }
  }

  const reorderLesson = async (lessonId, newOrderIndex) => {
    try {
      await store.reorderLesson(lessonId, newOrderIndex)
    } catch (error) {
      console.error('Reorder error:', error)
    }
  }

  const swapLessons = async (lessonId1, lessonId2) => {
    try {
      await store.swapLessons(lessonId1, lessonId2)
    } catch (error) {
      console.error('Swap error:', error)
    }
  }

  // ═════════════════════════════════════════════════════════════════
  // VALIDATION & HEALTH CHECK
  // ═════════════════════════════════════════════════════════════════

  const validateLessonsOrder = async () => {
    try {
      const result = await store.validateAllLessonsOrder()
      if (result) {
        ElMessage.success(`✅ Validation hoàn tất! Fixed: ${result.fixedCount || 0} lessons`)
        await loadLessons({ size: 1000 })
      }
      return result
    } catch (error) {
      console.error('Validate lessons order failed:', error)
      ElMessage.error('Lỗi khi validate lessons order')
      throw error
    }
  }

  const healthCheck = async () => {
    try {
      const result = await store.healthCheck()
      if (result) {
        ElMessage.success('✅ Health check OK!')
        console.log('Health check result:', result)
      }
      return result
    } catch (error) {
      console.error('Health check failed:', error)
      ElMessage.error('Health check thất bại')
      throw error
    }
  }

  // ═════════════════════════════════════════════════════════════════
  // HELPERS
  // ═════════════════════════════════════════════════════════════════

  const getDifficultyType = (difficulty) => {
    const types = {
      BEGINNER: 'success',
      INTERMEDIATE: 'warning',
      ADVANCED: 'danger',
    }
    return types[difficulty] || 'info'
  }

  const getDifficultyLabel = (difficulty) => {
    const labels = {
      BEGINNER: 'Dễ',
      INTERMEDIATE: 'Trung bình',
      ADVANCED: 'Khó',
    }
    return labels[difficulty] || difficulty
  }

  return {
    // State
    loading,
    lessons,
    pagination,
    searchQuery,
    filterDifficulty,
    filterStatus,

    // Computed
    filteredLessons,

    // Methods
    loadLessons,
    deleteLesson,
    toggleStatus,
    reorderLesson,
    swapLessons,
    validateLessonsOrder,
    healthCheck,
    getDifficultyType,
    getDifficultyLabel,
  }
}
