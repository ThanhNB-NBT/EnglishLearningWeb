// src/composables/reading/useReadingQuestions.js
import { ref, computed } from 'vue'
import { useReadingStore } from '@/stores/reading'
import { ElMessage } from 'element-plus'

// Validation helper (giống Grammar)
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
    case 'VERB_FORM':
      if (!metadata.blanks || metadata.blanks.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 chỗ trống' }
      }
      for (const blank of metadata.blanks) {
        if (!blank. correctAnswers || blank.correctAnswers. length === 0) {
          return { valid: false, message:  `Chỗ trống #${blank.position} chưa có đáp án` }
        }
      }
      break
    case 'MATCHING':
      if (!metadata.pairs || metadata.pairs.length < 2) {
        return { valid: false, message: 'Cần ít nhất 2 cặp để ghép' }
      }
      break
    case 'SENTENCE_BUILDING':
      if (!metadata.words || metadata.words.length < 2) {
        return { valid: false, message: 'Cần ít nhất 2 từ để sắp xếp' }
      }
      if (!metadata.correctSentence || metadata.correctSentence.trim() === '') {
        return { valid: false, message: 'Câu đúng không được để trống' }
      }
      break
    case 'SENTENCE_TRANSFORMATION':
      if (!metadata.originalSentence || metadata.originalSentence.trim() === '') {
        return { valid: false, message: 'Câu gốc không được để trống' }
      }
      if (! metadata.correctAnswers || metadata.correctAnswers.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 đáp án đúng' }
      }
      break
    case 'ERROR_CORRECTION':
      if (!metadata.errorText || metadata.errorText.trim() === '') {
        return { valid: false, message:  'Câu có lỗi không được để trống' }
      }
      if (!metadata.correction || metadata.correction.trim() === '') {
        return { valid:  false, message: 'Câu sửa lỗi không được để trống' }
      }
      break
    case 'TEXT_ANSWER':
      if (! metadata.correctAnswer || metadata.correctAnswer.trim() === '') {
        return { valid: false, message: 'Đáp án không được để trống' }
      }
      break
    case 'PRONUNCIATION':
      if (!metadata.words || metadata.words.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 từ' }
      }
      if (!metadata.categories || metadata.categories.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 nhóm phân loại' }
      }
      if (!metadata.classifications || metadata.classifications.length === 0) {
        return { valid: false, message: 'Cần phân loại cho các từ' }
      }
      break
    case 'READING_COMPREHENSION':
      if (! metadata.passage || metadata.passage.trim() === '') {
        return { valid:  false, message: 'Đoạn văn không được để trống' }
      }
      if (!metadata.blanks || metadata.blanks.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 chỗ trống' }
      }
      break
    case 'OPEN_ENDED':
      // Optional validation
      break
  }
  return { valid: true }
}

export function useReadingQuestionForm() {
  const store = useReadingStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create')

  const formData = ref({
    id: null,
    parentId:  null,
    parentType: 'READING',
    questionType: 'MULTIPLE_CHOICE',
    questionText: '',
    points: 10,
    orderIndex: 1,
    explanation: '',
    metadata: {},
  })

  const formRules = {
    questionText: [
      { required: true, message: 'Vui lòng nhập nội dung câu hỏi', trigger: 'blur' },
      { min: 5, message: 'Tối thiểu 5 ký tự', trigger: 'blur' },
    ],
    points: [
      { required: true, message: 'Vui lòng nhập điểm', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Điểm phải >= 1', trigger: 'blur' },
    ],
    orderIndex: [
      { required:  true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải >= 1', trigger: 'blur' },
    ],
  }

  const questionTypeOptions = [
    { label: 'Trắc nghiệm (Multiple Choice)', value: 'MULTIPLE_CHOICE', group: 'Cơ bản' },
    { label: 'Đúng / Sai (True/False)', value: 'TRUE_FALSE', group: 'Cơ bản' },
    { label: 'Điền từ (Fill Blank)', value: 'FILL_BLANK', group: 'Cơ bản' },
    { label: 'Chia động từ (Verb Form)', value: 'VERB_FORM', group: 'Cơ bản' },
    { label: 'Trả lời ngắn (Text Answer)', value: 'TEXT_ANSWER', group: 'Cơ bản' },
    { label: 'Nối từ (Matching)', value: 'MATCHING', group: 'Nâng cao' },
    { label: 'Sắp xếp câu (Sentence Building)', value: 'SENTENCE_BUILDING', group: 'Nâng cao' },
    {
      label: 'Viết lại câu (Transformation)',
      value: 'SENTENCE_TRANSFORMATION',
      group:  'Nâng cao',
    },
    { label:  'Tìm lỗi sai (Error Correction)', value: 'ERROR_CORRECTION', group: 'Nâng cao' },
    { label: 'Phát âm (Pronunciation)', value: 'PRONUNCIATION', group: 'Nâng cao' },
    {
      label: 'Đọc hiểu (Reading Comprehension)',
      value: 'READING_COMPREHENSION',
      group: 'Nâng cao',
    },
    { label: 'Câu hỏi mở (Open Ended)', value: 'OPEN_ENDED', group: 'Nâng cao' },
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
      parentType: 'READING',
      questionType: 'MULTIPLE_CHOICE',
      questionText: '',
      points: 10,
      orderIndex: nextOrder,
      explanation:  '',
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
      explanation: question. explanation || '',
      metadata: JSON.parse(JSON.stringify(question.metadata || {})),
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    formData.value = {
      id: null,
      parentId:  null,
      parentType: 'READING',
      questionType: 'MULTIPLE_CHOICE',
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

    const metaValid = validateMetadata(formData. value.questionType, formData. value.metadata)
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
      points: formData. value.points,
      orderIndex: formData.value.orderIndex,
      explanation: formData. value.explanation || '',
    }

    // Map metadata fields based on question type
    switch (formData.value.questionType) {
      case 'SENTENCE_TRANSFORMATION':
        payload.originalSentence = metadata.originalSentence
        payload.beginningPhrase = metadata.beginningPhrase
        payload.correctAnswers = metadata.correctAnswers
        break
      case 'FILL_BLANK':
        payload. blanks = metadata.blanks
        payload.wordBank = metadata.wordBank
        break
      case 'VERB_FORM':
        payload. blanks = metadata.blanks
        break
      case 'MULTIPLE_CHOICE':
        payload. options = metadata.options
        break
      case 'TRUE_FALSE':
        payload.correctAnswer = metadata.correctAnswer
        break
      case 'TEXT_ANSWER':
        payload. correctAnswer = metadata.correctAnswer
        payload.caseSensitive = metadata.caseSensitive
        break
      case 'ERROR_CORRECTION':
        payload. errorText = metadata.errorText
        payload.correction = metadata.correction
        break
      case 'MATCHING':
        payload.pairs = metadata.pairs
        break
      case 'SENTENCE_BUILDING':
        payload. words = metadata.words
        payload.correctSentence = metadata. correctSentence
        break
      case 'CONVERSATION':
        payload.conversationContext = metadata.conversationContext
        payload.options = metadata.options
        payload.correctAnswer = metadata.correctAnswer
        break
      case 'PRONUNCIATION':
        payload.words = metadata.words
        payload.categories = metadata.categories
        payload.classifications = metadata.classifications
        break
      case 'READING_COMPREHENSION':
        payload. passage = metadata.passage
        payload.blanks = metadata.blanks
        break
      case 'OPEN_ENDED':
        payload.suggestedAnswer = metadata.suggestedAnswer
        payload.timeLimitSeconds = metadata.timeLimitSeconds
        payload.minWord = metadata.minWord
        payload.maxWord = metadata.maxWord
        break
    }

    try {
      if (dialogMode.value === 'create') {
        await store. createQuestion(payload)
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
