// src/composables/reading/useReadingLessons.js
import { ref, computed } from 'vue'
import { useReadingStore } from '@/stores/reading'
import { ElMessage } from 'element-plus'

export function useReadingLessonForm() {
  const store = useReadingStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'

  const formData = ref({
    id: null,
    title: '',
    content: '',
    contentTranslation: '', // 🆕 Thêm field này
    difficulty: 'INTERMEDIATE',
    orderIndex: 1,
    timeLimitSeconds: 600,
    pointsReward: 100,
    isActive: true,
  })

  const formRules = {
    title: [
      { required: true, message: 'Vui lòng nhập tiêu đề bài đọc', trigger: 'blur' },
      { min: 5, max: 200, message:  'Độ dài từ 5-200 ký tự', trigger: 'blur' },
    ],
    content: [{ required: true, message: 'Vui lòng nhập nội dung bài đọc', trigger: 'blur' }],
    // contentTranslation không bắt buộc
    difficulty: [{ required: true, message: 'Vui lòng chọn độ khó', trigger:  'change' }],
    orderIndex:  [
      { required: true, message:  'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải >= 1', trigger: 'blur' },
    ],
    timeLimitSeconds: [
      { required: true, message: 'Vui lòng nhập thời gian', trigger: 'blur' },
      { type: 'number', min: 0, message: 'Thời gian phải >= 0', trigger: 'blur' },
    ],
    pointsReward: [
      { required: true, message: 'Vui lòng nhập điểm thưởng', trigger: 'blur' },
      { type: 'number', min: 0, message: 'Điểm phải >= 0', trigger: 'blur' },
    ],
  }

  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo bài đọc mới' : 'Chỉnh sửa bài đọc'
  )

  const submitButtonText = computed(() =>
    dialogMode.value === 'create' ? 'Tạo bài đọc' : 'Cập nhật'
  )

  const openCreateDialog = async () => {
    dialogMode.value = 'create'
    const nextOrder = await store.getNextLessonOrderIndex()
    formData.value = {
      id: null,
      title: '',
      content:  '',
      contentTranslation:  '', // 🆕 Reset field
      difficulty: 'INTERMEDIATE',
      orderIndex: nextOrder,
      timeLimitSeconds: 600,
      pointsReward: 100,
      isActive: true,
    }
    dialogVisible.value = true
  }

  const openEditDialog = (lesson) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: lesson.id,
      title: lesson.title,
      content: lesson.content || '',
      contentTranslation:  lesson.contentTranslation || '', // 🆕 Load field
      difficulty: lesson.difficulty,
      orderIndex: lesson.orderIndex,
      timeLimitSeconds: lesson.timeLimitSeconds || 600,
      pointsReward: lesson.pointsReward || 100,
      isActive:  lesson.isActive ??  true,
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    formData.value = {
      id: null,
      title: '',
      content:  '',
      contentTranslation:  '', // 🆕 Reset field
      difficulty: 'INTERMEDIATE',
      orderIndex: 1,
      timeLimitSeconds: 600,
      pointsReward: 100,
      isActive: true,
    }
  }

  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    await formEl.validate()

    const payload = {
      title: formData.value.title,
      content: formData.value.content,
      contentTranslation: formData.value.contentTranslation || null, // 🆕 Thêm vào payload
      difficulty: formData.value.difficulty,
      orderIndex: formData.value.orderIndex,
      timeLimitSeconds: formData.value.timeLimitSeconds,
      pointsReward:  formData.value.pointsReward,
      isActive: formData.value.isActive,
    }

    try {
      if (dialogMode.value === 'create') {
        await store.createLesson(payload)
      } else {
        await store.updateLesson(formData.value.id, payload)
      }
      return true
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Lỗi khi lưu bài đọc')
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
