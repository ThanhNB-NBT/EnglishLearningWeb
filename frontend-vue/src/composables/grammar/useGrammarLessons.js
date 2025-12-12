// src/composables/useGrammarLessons.js
import { ref, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'

export function useGrammarLessonForm() {
  const grammarStore = useGrammarStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'

  const formData = ref({
    id: null,
    topicId: null,
    title: '',
    lessonType: 'THEORY',
    content: '',
    orderIndex: 1,
    pointsReward: 10,
    timeLimitSeconds: 10,
    isActive: true,
  })

  const formRules = {
    topicId: [
      { required: true, message: 'Vui lòng chọn topic', trigger: 'change' },
    ],
    title: [
      { required: true, message: 'Vui lòng nhập tiêu đề', trigger: 'blur' },
      { max: 200, message: 'Tiêu đề không được vượt quá 200 ký tự', trigger: 'blur' },
    ],
    lessonType: [
      { required: true, message: 'Vui lòng chọn loại bài học', trigger: 'change' },
    ],
    orderIndex: [
      { required: true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải lớn hơn 0', trigger: 'blur' },
    ],
    pointsReward: [
      { required: true, message: 'Vui lòng nhập điểm thưởng', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Điểm thưởng phải lớn hơn 0', trigger: 'blur' },
    ],
    timeLimitSeconds: [
      { required: true, message: 'Vui lòng nhập thời gian ước tính', trigger: 'blur' },
      { type: 'number', min: 10, message: 'Thời gian phải >= 10 giây', trigger: 'blur' },
    ],
  }

  const lessonTypeOptions = [
    { value: 'THEORY', label: 'Theory (Lý thuyết)', icon: '📖' },
    { value: 'PRACTICE', label: 'Practice (Thực hành)', icon: '✍️' },
  ]

  const dialogTitle = computed(() => {
    return dialogMode.value === 'create' ? 'Tạo Lesson Mới' : 'Chỉnh Sửa Lesson'
  })

  const submitButtonText = computed(() => {
    return dialogMode.value === 'create' ? 'Tạo Mới' : 'Cập Nhật'
  })

  // Open create dialog
  const openCreateDialog = async (topicId) => {
    dialogMode.value = 'create'
    const nextOrder = await grammarStore.getNextLessonOrderIndex(topicId)

    formData.value = {
      id: null,
      topicId: topicId,
      title: '',
      lessonType: 'THEORY',
      content: '',
      orderIndex: nextOrder,
      pointsReward: 10,
      timeLimitSeconds: 10,
      isActive: true,
    }
    dialogVisible.value = true
  }

  // Open edit dialog
  const openEditDialog = (lesson) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: lesson.id,
      topicId: lesson.topicId,
      title: lesson.title,
      lessonType: lesson.lessonType,
      content: lesson.content || '',
      orderIndex: lesson.orderIndex,
      pointsReward: lesson.pointsReward || 10,
      timeLimitSeconds: lesson.timeLimitSeconds || 10,
      isActive: lesson.isActive ?? true,
    }
    dialogVisible.value = true
  }

  // Submit form
  const handleSubmit = async (formRefInstance) => {
    if (!formRefInstance) {
      console.error('Form ref is null')
      return false
    }

    try {
      await formRefInstance.validate()

      let result
      if (dialogMode.value === 'create') {
        result = await grammarStore.createLesson(formData.value)
      } else {
        result = await grammarStore.updateLesson(formData.value.id, formData.value)
      }

      if (result) {
        closeDialog()
        return true
      }
      return false
    } catch (error) {
      console.error('Form validation failed:', error)
      return false
    }
  }

  // Close dialog
  const closeDialog = () => {
    dialogVisible.value = false
    formData.value = {
      id: null,
      topicId: null,
      title: '',
      lessonType: 'THEORY',
      content: '',
      orderIndex: 1,
      pointsReward: 10,
      timeLimitSeconds: 10,
      isActive: true,
    }
  }

  // Helper: Update lesson type and adjust defaults
  const handleLessonTypeChange = (type) => {
    if (type === 'THEORY') {
      formData.value.pointsReward = 10
      formData.value.timeLimitSeconds = 10
    } else if (type === 'PRACTICE') {
      formData.value.pointsReward = 15
      formData.value.timeLimitSeconds = 30
    }
  }

  return {
    // State
    dialogVisible,
    dialogMode,
    formData,
    formRules,
    lessonTypeOptions,

    // Computed
    dialogTitle,
    submitButtonText,

    // Methods
    openCreateDialog,
    openEditDialog,
    handleSubmit,
    closeDialog,
    handleLessonTypeChange,
  }
}
