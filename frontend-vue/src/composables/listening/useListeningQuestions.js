// src/composables/listening/useListeningQuestions.js
import { ref, computed } from 'vue'
import { useListeningStore } from '@/stores/listening'
import { ElMessage } from 'element-plus'

// Validation helper (giống Reading)
const validateMetadata = (questionType, metadata) => {
  switch (questionType) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      if (!metadata.options || metadata.options.length < 2) {
        return { valid: false, message: 'Cần ít nhất 2 đáp án' }
      }
      if (!metadata.options.some((o) => o.isCorrect)) {
        return { valid: false, message: 'Phải có ít nhất 1 đáp án đúng' }
      }
      break
    case 'FILL_BLANK':
      if (!metadata.blanks || metadata.blanks.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 chỗ trống' }
      }
      for (const blank of metadata.blanks) {
        if (!blank.correctAnswers || blank.correctAnswers.length === 0) {
          return { valid: false, message: `Chỗ trống #${blank.position} chưa có đáp án` }
        }
      }
      break
    case 'TEXT_ANSWER':
      if (!metadata.correctAnswer || metadata.correctAnswer.trim() === '') {
        return { valid: false, message: 'Đáp án không được để trống' }
      }
      break
  }
  return { valid: true }
}

export function useListeningQuestionForm() {
  const store = useListeningStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create')

  const formData = ref({
    id: null,
    parentId: null,
    parentType: 'LISTENING',
    questionType: 'LISTENING_COMPREHENSION',
    questionText: '',
    points: 10,
    orderIndex: 1,
    explanation: '',
    metadata: {},
  })

  const formRules = {
    questionText: [
      { required: false, message: 'Nội dung câu hỏi (có thể để trống)', trigger: 'blur' },
    ],
    points: [
      { required: true, message: 'Vui lòng nhập điểm', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Điểm phải >= 1', trigger: 'blur' },
    ],
    orderIndex: [
      { required: true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải >= 1', trigger: 'blur' },
    ],
  }

  const questionTypeOptions = [
    { label: 'Nghe hiểu (Listening Comprehension)', value: 'LISTENING_COMPREHENSION', group: 'Listening' },
    { label: 'Trắc nghiệm (Multiple Choice)', value: 'MULTIPLE_CHOICE', group: 'Cơ bản' },
    { label: 'Đúng / Sai (True/False)', value: 'TRUE_FALSE', group: 'Cơ bản' },
    { label: 'Điền từ (Fill Blank)', value: 'FILL_BLANK', group: 'Cơ bản' },
    { label: 'Trả lời ngắn (Text Answer)', value: 'TEXT_ANSWER', group: 'Cơ bản' },
  ]

  const currentQuestionTypeOption = computed(() =>
    questionTypeOptions.find((opt) => opt.value === formData.value.questionType)
  )

  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo câu hỏi mới' : 'Chỉnh sửa câu hỏi'
  )

  const submitButtonText = computed(() =>
    dialogMode.value === 'create' ? 'Tạo câu hỏi' : 'Cập nhật'
  )

  const openCreateDialog = async (lessonId) => {
    dialogMode.value = 'create'
    const nextOrder = await store.getNextQuestionOrderIndex(lessonId)
    formData.value = {
      id: null,
      parentId: lessonId,
      parentType: 'LISTENING',
      questionType: 'LISTENING_COMPREHENSION',
      questionText: '',
      points: 10,
      orderIndex: nextOrder,
      explanation: '',
      metadata: {},
    }
    dialogVisible.value = true
  }

  const openEditDialog = (question) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: question.id,
      parentId: question.parentId,
      parentType: question.parentType,
      questionType: question.questionType,
      questionText: question.questionText,
      points: question.points,
      orderIndex: question.orderIndex,
      explanation: question.explanation || '',
      metadata: JSON.parse(JSON.stringify(question.metadata || {})),
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    formData.value = {
      id: null,
      parentId: null,
      parentType: 'LISTENING',
      questionType: 'LISTENING_COMPREHENSION',
      questionText: '',
      points: 10,
      orderIndex: 1,
      explanation: '',
      metadata: {},
    }
  }

  const handleQuestionTypeChange = () => {
    if (formData.value) formData.value.metadata = {}
  }

  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    await formEl.validate()

    const metaValid = validateMetadata(formData.value.questionType, formData.value.metadata)
    if (!metaValid.valid) {
      ElMessage.error(metaValid.message)
      return false
    }

    const metadata = { ...formData.value.metadata }
    const payload = {
      parentId: formData.value.parentId,
      parentType: formData.value.parentType,
      questionType: formData.value.questionType,
      questionText: formData.value.questionText,
      points: formData.value.points,
      orderIndex: formData.value.orderIndex,
      explanation: formData.value.explanation || '',
    }

    // Map metadata fields based on question type
    switch (formData.value.questionType) {
      case 'LISTENING_COMPREHENSION':
      case 'MULTIPLE_CHOICE':
      case 'TRUE_FALSE':
        payload.options = metadata.options
        break
      case 'FILL_BLANK':
        payload.blanks = metadata.blanks
        payload.wordBank = metadata.wordBank
        break
      case 'TEXT_ANSWER':
        payload.correctAnswer = metadata.correctAnswer
        payload.caseSensitive = metadata.caseSensitive
        break
    }

    try {
      if (dialogMode.value === 'create') {
        await store.createQuestion(payload)
      } else {
        await store.updateQuestion(formData.value.id, payload)
      }
      return true
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Lỗi lưu câu hỏi')
      throw error
    }
  }

  return {
    dialogVisible,
    dialogMode,
    formData,
    formRules,
    questionTypeOptions,
    currentQuestionTypeOption,
    dialogTitle,
    submitButtonText,
    openCreateDialog,
    openEditDialog,
    closeDialog,
    handleQuestionTypeChange,
    handleSubmit,
  }
}
