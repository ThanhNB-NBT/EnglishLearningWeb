import { ref, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessage } from 'element-plus'

export function useGrammarQuestionForm() {
  const store = useGrammarStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const currentStep = ref(1)

  const defaultFormData = {
    id: null,
    parentId: null,
    parentType: 'GRAMMAR',
    questionText: '',
    questionType: '',
    points: 5,
    orderIndex: 1,
    metadata: {},
  }

  const formData = ref(JSON.parse(JSON.stringify(defaultFormData)))

  const formRules = {
    questionType: [{ required: true, message: 'Vui lòng chọn loại câu hỏi', trigger: 'change' }],
    questionText: [{ required: true, message: 'Nội dung không được để trống', trigger: 'blur' }],
    points: [{ required: true, message: 'Nhập điểm số', trigger: 'blur' }],
    orderIndex: [{ required: true, message: 'Nhập thứ tự', trigger: 'blur' }],
  }

  // --- Validate Metadata ---
  const validateMetadata = (questionType, metadata) => {
    if (!metadata) return { valid: false, message: 'Metadata không được để trống' }

    switch (questionType) {
      case 'SENTENCE_TRANSFORMATION':
        if (!metadata.originalSentence || !metadata.originalSentence.trim()) {
          return { valid: false, message: 'Chưa nhập câu gốc' }
        }
        if (!metadata.correctAnswers || metadata.correctAnswers.length === 0) {
          return { valid: false, message: 'Cần ít nhất 1 đáp án đúng' }
        }
        if (metadata.correctAnswers.some((ans) => !ans || !ans.trim())) {
          return { valid: false, message: 'Có đáp án đang để trống' }
        }
        break

      case 'FILL_BLANK':
      case 'VERB_FORM':
        if (!metadata.blanks || metadata.blanks.length === 0)
          return { valid: false, message: 'Cần ít nhất 1 chỗ trống' }
        for (let i = 0; i < metadata.blanks.length; i++) {
          if (
            !metadata.blanks[i].correctAnswers ||
            metadata.blanks[i].correctAnswers.length === 0
          ) {
            return { valid: false, message: `Chỗ trống #${i + 1} chưa có đáp án.` }
          }
        }
        break

      case 'MULTIPLE_CHOICE':
        if (!metadata.options || metadata.options.length < 2)
          return { valid: false, message: 'Cần ít nhất 2 đáp án' }
        if (!metadata.options.some((o) => o.isCorrect))
          return { valid: false, message: 'Phải có 1 đáp án đúng' }
        break

      case 'MATCHING':
        if (!metadata.pairs || metadata.pairs.length < 2)
          return { valid: false, message: 'Cần ít nhất 2 cặp' }
        break

      case 'TRUE_FALSE':
        if (metadata.correctAnswer === null || metadata.correctAnswer === undefined)
          return { valid: false, message: 'Chưa chọn đáp án đúng' }
        break
    }
    return { valid: true }
  }

  const questionTypeOptions = [
    {
      value: 'MULTIPLE_CHOICE',
      label: 'Trắc nghiệm (Multiple Choice)',
      description: 'Chọn 1 đáp án đúng',
    },
    {
      value: 'TRUE_FALSE',
      label: 'Đúng / Sai (True/False)',
      description: 'Xác định mệnh đề đúng/sai',
    },
    {
      value: 'FILL_BLANK',
      label: 'Điền từ (Fill in the blank)',
      description: 'Điền từ vào chỗ trống',
    },
    {
      value: 'SENTENCE_TRANSFORMATION',
      label: 'Viết lại câu (Sentence Transformation)',
      description: 'Viết lại câu giữ nguyên nghĩa',
    },
    { value: 'MATCHING', label: 'Nối từ (Matching)', description: 'Ghép cặp' },
    {
      value: 'SENTENCE_BUILDING',
      label: 'Xây dựng câu (Sentence Building)',
      description: 'Sắp xếp từ thành câu',
    },
    { value: 'PRONUNCIATION', label: 'Phát âm (Pronunciation)', description: 'Phân loại phát âm' },
    { value: 'VERB_FORM', label: 'Chia động từ (Verb Form)', description: 'Chia thì/dạng động từ' },
    {
      value: 'ERROR_CORRECTION',
      label: 'Tìm lỗi sai (Error Correction)',
      description: 'Tìm và sửa lỗi',
    },
    {
      value: 'TEXT_ANSWER',
      label: 'Tự luận ngắn (Text Answer)',
      description: 'Trả lời chính xác từ/câu',
    },
    {
      value: 'OPEN_ENDED',
      label: 'Câu hỏi mở (Open Ended)',
      description: 'Trả lời tự do (cần chấm điểm)',
    },
  ]

  const currentQuestionTypeOption = computed(() =>
    questionTypeOptions.find((opt) => opt.value === formData.value.questionType),
  )
  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo câu hỏi mới' : 'Chỉnh sửa câu hỏi',
  )
  const submitButtonText = computed(() => (dialogMode.value === 'create' ? 'Tạo mới' : 'Cập nhật'))

  // --- Actions ---
  const openCreateDialog = async (lessonId) => {
    dialogMode.value = 'create'
    currentStep.value = 1
    formData.value = JSON.parse(JSON.stringify(defaultFormData))
    if (lessonId) {
      formData.value.parentId = lessonId
      formData.value.parentType = 'GRAMMAR'
      try {
        const nextOrder = await store.getNextQuestionOrderIndex(lessonId)
        formData.value.orderIndex = nextOrder || 1
      } catch (error) {
        console.error(error)
      }
    }
    dialogVisible.value = true
  }

  const openEditDialog = (question) => {
    dialogMode.value = 'edit'
    currentStep.value = 1
    const meta = question.metadata ? JSON.parse(JSON.stringify(question.metadata)) : {}
    if (question.explanation) meta.explanation = question.explanation
    formData.value = {
      id: question.id,
      parentId: question.parentId,
      parentType: question.parentType || 'GRAMMAR',
      questionText: question.questionText,
      questionType: question.questionType,
      points: question.points,
      orderIndex: question.orderIndex,
      metadata: meta,
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    currentStep.value = 1
    formData.value = JSON.parse(JSON.stringify(defaultFormData))
  }

  const handleQuestionTypeChange = () => {
    if (formData.value) formData.value.metadata = {}
  }
  const nextStep = () => {
    if (currentStep.value < 3) currentStep.value++
  }
  const prevStep = () => {
    if (currentStep.value > 1) currentStep.value--
  }

  // --- Submit ---
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
      explanation: metadata.explanation || '',
    }

    // Mapping payload specific fields
    switch (formData.value.questionType) {
      case 'SENTENCE_TRANSFORMATION':
        payload.originalSentence = metadata.originalSentence
        payload.beginningPhrase = metadata.beginningPhrase
        payload.correctAnswers = metadata.correctAnswers
        break
      case 'FILL_BLANK':
        payload.blanks = metadata.blanks
        break
      case 'VERB_FORM':
        payload.blanks = metadata.blanks
        break
      case 'MULTIPLE_CHOICE':
        payload.options = metadata.options
        break
      case 'TRUE_FALSE':
        payload.correctAnswer = metadata.correctAnswer
        break
      case 'TEXT_ANSWER':
        payload.correctAnswer = metadata.correctAnswer
        payload.caseSensitive = metadata.caseSensitive
        break
      case 'ERROR_CORRECTION':
        payload.errorText = metadata.errorText
        payload.correction = metadata.correction
        break
      case 'MATCHING':
        payload.pairs = metadata.pairs
        break
      case 'SENTENCE_BUILDING':
        payload.words = metadata.words
        payload.correctSentence = metadata.correctSentence
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
        payload.passage = metadata.passage
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
      if (dialogMode.value === 'create') await store.createQuestion(payload)
      else await store.updateQuestion(formData.value.id, payload)
      return true
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Lỗi lưu câu hỏi')
      throw error
    }
  }

  return {
    dialogVisible,
    dialogMode,
    currentStep,
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
    nextStep,
    prevStep,
    handleSubmit,
  }
}
