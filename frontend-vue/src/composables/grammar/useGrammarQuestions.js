// src/composables/useGrammarQuestions.js
import { ref, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'

export function useGrammarQuestionForm() {
  const grammarStore = useGrammarStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'
  const currentStep = ref(1) // Step trong form wizard (1: Basic Info, 2: Metadata, 3: Preview)

  const formData = ref({
    id: null,
    parentId: null, // lessonId
    parentType: 'GRAMMAR',
    questionText: '',
    questionType: 'MULTIPLE_CHOICE',
    explanation: '',
    points: 5,
    orderIndex: 1,

    // Metadata (dynamic per question type)
    metadata: {},
  })

  const formRules = {
    questionText: [
      { required: true, message: 'Vui lòng nhập câu hỏi', trigger: 'blur' },
      { max: 1000, message: 'Câu hỏi không được vượt quá 1000 ký tự', trigger: 'blur' },
    ],
    questionType: [
      { required: true, message: 'Vui lòng chọn loại câu hỏi', trigger: 'change' },
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
    {
      value: 'MULTIPLE_CHOICE',
      label: 'Multiple Choice',
      description: 'Chọn 1 đáp án đúng từ nhiều lựa chọn',
      defaultPoints: 5,
    },
    {
      value: 'TRUE_FALSE',
      label: 'True/False',
      description: 'Câu hỏi đúng/sai',
      defaultPoints: 5,
    },
    {
      value: 'FILL_BLANK',
      label: 'Fill in the Blank',
      description: 'Điền từ vào chỗ trống',
      defaultPoints: 5,
    },
    {
      value: 'SHORT_ANSWER',
      label: 'Short Answer',
      description: 'Trả lời ngắn (text)',
      defaultPoints: 5,
    },
    {
      value: 'VERB_FORM',
      label: 'Verb Form',
      description: 'Chia động từ đúng dạng',
      defaultPoints: 5,
    },
    {
      value: 'ERROR_CORRECTION',
      label: 'Error Correction',
      description: 'Sửa lỗi sai trong câu',
      defaultPoints: 7,
    },
    {
      value: 'MATCHING',
      label: 'Matching',
      description: 'Nối cặp tương ứng',
      defaultPoints: 10,
    },
    {
      value: 'SENTENCE_BUILDING',
      label: 'Sentence Building',
      description: 'Sắp xếp từ thành câu',
      defaultPoints: 8,
    },
    {
      value: 'COMPLETE_CONVERSATION',
      label: 'Complete Conversation',
      description: 'Hoàn thành đoạn hội thoại',
      defaultPoints: 7,
    },
    {
      value: 'PRONUNCIATION',
      label: 'Pronunciation',
      description: 'Phân loại phát âm',
      defaultPoints: 10,
    },
    {
      value: 'READING_COMPREHENSION',
      label: 'Reading Comprehension',
      description: 'Đọc hiểu với chỗ trống',
      defaultPoints: 15,
    },
    {
      value: 'OPEN_ENDED',
      label: 'Open Ended',
      description: 'Câu trả lời tự do (cần AI đánh giá)',
      defaultPoints: 20,
    },
  ]

  const dialogTitle = computed(() => {
    if (dialogMode.value === 'create') {
      const typeOption = questionTypeOptions.find(opt => opt.value === formData.value.questionType)
      return `Tạo Question: ${typeOption?.label || 'New'}`
    }
    return 'Chỉnh Sửa Question'
  })

  const submitButtonText = computed(() => {
    return dialogMode.value === 'create' ? 'Tạo Mới' : 'Cập Nhật'
  })

  const currentQuestionTypeOption = computed(() => {
    return questionTypeOptions.find(opt => opt.value === formData.value.questionType)
  })

  // Open create dialog
  const openCreateDialog = async (lessonId) => {
    dialogMode.value = 'create'
    const nextOrder = await grammarStore.getNextQuestionOrderIndex(lessonId)

    formData.value = {
      id: null,
      parentId: lessonId,
      parentType: 'GRAMMAR',
      questionText: '',
      questionType: 'MULTIPLE_CHOICE',
      explanation: '',
      points: 5,
      orderIndex: nextOrder,
      metadata: {},
    }

    currentStep.value = 1
    dialogVisible.value = true
  }

  // Open edit dialog
  const openEditDialog = (question) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: question.id,
      parentId: question.parentId,
      parentType: question.parentType,
      questionText: question.questionText,
      questionType: question.questionType,
      explanation: question.explanation || '',
      points: question.points || 5,
      orderIndex: question.orderIndex,
      metadata: question.metadata || {},
    }

    currentStep.value = 1
    dialogVisible.value = true
  }

  // Build final DTO based on question type
  const buildQuestionDTO = () => {
    const baseDTO = {
      parentId: formData.value.parentId,
      parentType: formData.value.parentType,
      questionText: formData.value.questionText,
      questionType: formData.value.questionType,
      explanation: formData.value.explanation,
      points: formData.value.points,
      orderIndex: formData.value.orderIndex,
    }

    // Attach metadata based on question type
    const metadata = formData.value.metadata

    switch (formData.value.questionType) {
      case 'MULTIPLE_CHOICE':
        return {
          ...baseDTO,
          hint: metadata.hint,
          options: (metadata.options || []).map(opt => ({
            text: opt.text,
            isCorrect: opt.isCorrect === true,
            order: opt.order
          }))
        }

      case 'TRUE_FALSE':
        return {
          ...baseDTO,
          hint: metadata.hint,
          correctAnswer: metadata.correctAnswer,
        }

      case 'FILL_BLANK':
      case 'SHORT_ANSWER':
      case 'VERB_FORM':
      case 'ERROR_CORRECTION':
        return {
          ...baseDTO,
          hint: metadata.hint,
          correctAnswer: metadata.correctAnswer,
          caseSensitive: Boolean(metadata.caseSensitive || false),
          type: formData.value.questionType,
        }

      case 'MATCHING':
        return {
          ...baseDTO,
          hint: metadata.hint,
          pairs: metadata.pairs,
        }

      case 'SENTENCE_BUILDING':
        return {
          ...baseDTO,
          hint: metadata.hint,
          words: metadata.words || [],
          correctSentence: metadata.correctSentence,
        }

      case 'COMPLETE_CONVERSATION':
        return {
          ...baseDTO,
          hint: metadata.hint,
          conversationContext: metadata.conversationContext,
          options: metadata.options || [],
          correctAnswer: metadata.correctAnswer,
        }

      case 'PRONUNCIATION':
        return {
          ...baseDTO,
          hint: metadata.hint,
          words: metadata.words,
          categories: metadata.categories,
          classifications: (metadata.classifications || []).map(cls => ({
            word: cls.word,
            category: cls.category
          }))
        }

      case 'READING_COMPREHENSION':
        return {
          ...baseDTO,
          hint: metadata.hint,
          passage: metadata.passage,
          blanks: (metadata.blanks || []).map(blank => ({
            position: blank.position,
            options: blank.options || [],
            correctAnswer: blank.correctAnswer
          }))
        }

      case 'OPEN_ENDED':
        return {
          ...baseDTO,
          hint: metadata.hint,
          suggestedAnswer: metadata.suggestedAnswer,
          timeLimitSeconds: metadata.timeLimitSeconds,
          minWord: metadata.minWord,
          maxWord: metadata.maxWord,
        }

      default:
        return baseDTO
    }
  }

  // Submit form
  const handleSubmit = async (formRefInstance) => {
    if (!formRefInstance) {
      console.error('Form ref is null')
      return false
    }

    try {
      await formRefInstance.validate()

      const dto = buildQuestionDTO()
      console.log('📤 Submitting question DTO:', dto)

      let result
      if (dialogMode.value === 'create') {
        result = await grammarStore.createQuestion(dto)
      } else {
        result = await grammarStore.updateQuestion(formData.value.id, dto)
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
    currentStep.value = 1
    formData.value = {
      id: null,
      parentId: null,
      parentType: 'GRAMMAR',
      questionText: '',
      questionType: 'MULTIPLE_CHOICE',
      explanation: '',
      points: 5,
      orderIndex: 1,
      metadata: {},
    }
  }

  // Handle question type change (reset metadata)
  const handleQuestionTypeChange = (newType) => {
    console.log('Question type changed to:', newType)
    formData.value.metadata = {}

    // Set default points based on type
    const typeOption = questionTypeOptions.find(opt => opt.value === newType)
    if (typeOption) {
      formData.value.points = typeOption.defaultPoints
    }
  }

  // Navigate steps
  const goToStep = (step) => {
    if (step >= 1 && step <= 3) {
      currentStep.value = step
    }
  }

  const nextStep = () => {
    if (currentStep.value < 3) {
      currentStep.value += 1
    }
  }

  const prevStep = () => {
    if (currentStep.value > 1) {
      currentStep.value -= 1
    }
  }

  return {
    // State
    dialogVisible,
    dialogMode,
    currentStep,
    formData,
    formRules,
    questionTypeOptions,

    // Computed
    dialogTitle,
    submitButtonText,
    currentQuestionTypeOption,

    // Methods
    openCreateDialog,
    openEditDialog,
    handleSubmit,
    closeDialog,
    handleQuestionTypeChange,
    buildQuestionDTO,
    goToStep,
    nextStep,
    prevStep,
  }
}
