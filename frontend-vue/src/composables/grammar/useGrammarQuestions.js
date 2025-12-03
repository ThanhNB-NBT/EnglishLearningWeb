import { ref, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessage } from 'element-plus'

export function useGrammarQuestionForm() {
  const store = useGrammarStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const currentStep = ref(1)

  // Định nghĩa giá trị mặc định rõ ràng
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

  // Khởi tạo formData là REF
  const formData = ref(JSON.parse(JSON.stringify(defaultFormData)))

  // Form Rules
  const formRules = {
    questionType: [{ required: true, message: 'Vui lòng chọn loại câu hỏi', trigger: 'change' }],
    questionText: [{ required: true, message: 'Nội dung không được để trống', trigger: 'blur' }],
    points: [{ required: true, message: 'Nhập điểm số', trigger: 'blur' }],
    orderIndex: [{ required: true, message: 'Nhập thứ tự', trigger: 'blur' }],
  }

  // Thêm sau dòng 24 (sau formRules)
  const validateMetadata = (questionType, metadata) => {
    if (!metadata) {
      return { valid: false, message: 'Metadata không được để trống' }
    }

    switch (questionType) {
      case 'FILL_BLANK':
      case 'VERB_FORM':
        if (!metadata.blanks || metadata.blanks.length === 0) {
          return { valid: false, message: 'Cần có ít nhất 1 chỗ trống' }
        }

        for (let i = 0; i < metadata.blanks.length; i++) {
          const blank = metadata.blanks[i]
          if (!blank.correctAnswers || blank.correctAnswers.length === 0) {
            return {
              valid: false,
              message: `Chỗ trống #${i + 1} chưa có đáp án đúng.  Vui lòng nhập ít nhất 1 đáp án. `,
            }
          }

          // Kiểm tra đáp án có rỗng không
          const hasEmptyAnswer = blank.correctAnswers.some((ans) => !ans || ans.trim() === '')
          if (hasEmptyAnswer) {
            return {
              valid: false,
              message: `Chỗ trống #${i + 1} có đáp án rỗng.  Vui lòng xóa hoặc điền đầy đủ. `,
            }
          }
        }
        break

      case 'MULTIPLE_CHOICE': {
        if (!metadata.options || metadata.options.length < 2) {
          return { valid: false, message: 'Cần có ít nhất 2 đáp án' }
        }

        const hasCorrect = metadata.options.some((opt) => opt.isCorrect === true)
        if (!hasCorrect) {
          return { valid: false, message: 'Phải có ít nhất 1 đáp án đúng' }
        }

        const hasEmptyOption = metadata.options.some((opt) => !opt.text || opt.text.trim() === '')
        if (hasEmptyOption) {
          return { valid: false, message: 'Có đáp án chưa điền nội dung' }
        }
        break
      }

      case 'MATCHING': {
        if (!metadata.pairs || metadata.pairs.length < 2) {
          return { valid: false, message: 'Cần có ít nhất 2 cặp' }
        }

        const hasEmptyPair = metadata.pairs.some(
          (p) => !p.left || !p.right || p.left.trim() === '' || p.right.trim() === '',
        )
        if (hasEmptyPair) {
          return { valid: false, message: 'Có cặp chưa điền đầy đủ' }
        }
        break
      }

      case 'TRUE_FALSE':
        if (metadata.correctAnswer === null || metadata.correctAnswer === undefined) {
          return { valid: false, message: 'Chưa chọn đáp án đúng (True/False)' }
        }
        break
    }

    return { valid: true }
  }

  // Options
  const questionTypeOptions = [
    {
      value: 'MULTIPLE_CHOICE',
      label: 'Trắc nghiệm (Multiple Choice)',
      description: 'Chọn 1 đáp án đúng từ danh sách',
    },
    {
      value: 'TRUE_FALSE',
      label: 'Đúng / Sai (True/False)',
      description: 'Xác định mệnh đề là đúng hay sai',
    },
    {
      value: 'FILL_BLANK',
      label: 'Điền từ (Fill in the blank)',
      description: 'Điền từ còn thiếu vào chỗ trống',
    },
    {
      value: 'MATCHING',
      label: 'Nối từ (Matching)',
      description: 'Ghép cặp các mục tương ứng với nhau',
    },
    {
      value: 'SENTENCE_BUILDING',
      label: 'Xây dựng câu (Sentence Building)',
      description: 'Sắp xếp các từ thành câu hoàn chỉnh',
    },
    {
      value: 'CONVERSATION',
      label: 'Hội thoại (Conversation)',
      description: 'Hoàn thành hoặc sắp xếp đoạn hội thoại',
    },
    {
      value: 'PRONUNCIATION',
      label: 'Phát âm (Pronunciation)',
      description: 'Kiểm tra phát âm từ vựng',
    },
    {
      value: 'VERB_FORM',
      label: 'Chia động từ (Verb Form)',
      description: 'Chia dạng đúng của động từ trong ngoặc',
    },
    {
      value: 'ERROR_CORRECTION',
      label: 'Tìm lỗi sai (Error Correction)',
      description: 'Tìm và sửa lỗi sai trong câu',
    },
    {
      value: 'READING_COMPREHENSION',
      label: 'Đọc hiểu (Reading)',
      description: 'Đọc đoạn văn và trả lời câu hỏi',
    },
    {
      value: 'TEXT_ANSWER',
      label: 'Tự luận (Text Answer)',
      description: 'Trả lời câu hỏi',
    },
    {
      value: 'OPEN_ENDED',
      label: 'Câu hỏi mở (Open Ended)',
      description: 'Câu hỏi không có đáp án cố định',
    },
  ]

  const currentQuestionTypeOption = computed(() =>
    questionTypeOptions.find((opt) => opt.value === formData.value.questionType),
  )

  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo câu hỏi mới' : 'Chỉnh sửa câu hỏi',
  )
  const submitButtonText = computed(() => (dialogMode.value === 'create' ? 'Tạo mới' : 'Cập nhật'))

  // --- ACTIONS ---

  const openCreateDialog = async (lessonId) => {
    dialogMode.value = 'create'
    currentStep.value = 1

    // Reset form data an toàn bằng JSON parse/stringify để deep copy
    formData.value = JSON.parse(JSON.stringify(defaultFormData))

    // Nếu có lessonId, gán vào form và lấy OrderIndex
    if (lessonId) {
      if (formData.value) {
        formData.value.parentId = lessonId
        // Mặc định parentType là GRAMMAR, nếu cần đổi thì set ở đây
        formData.value.parentType = 'GRAMMAR'
      }

      try {
        const nextOrder = await store.getNextQuestionOrderIndex(lessonId)
        if (formData.value) {
          formData.value.orderIndex = nextOrder || 1
        }
      } catch (error) {
        console.error('Failed to get next order:', error)
        if (formData.value) formData.value.orderIndex = 1
      }
    }

    dialogVisible.value = true
  }

  const openEditDialog = (question) => {
    dialogMode.value = 'edit'
    currentStep.value = 1

    const meta = question.metadata ? JSON.parse(JSON.stringify(question.metadata)) : {}
    if (question.explanation) {
      meta.explanation = question.explanation
    }

    // Clone data để edit
    formData.value = {
      id: question.id,
      parentId: question.parentId || question.lessonId,
      parentType: question.parentType || 'GRAMMAR',
      questionText: question.questionText,
      questionType: question.questionType,
      points: question.points,
      orderIndex: question.orderIndex,
      // Metadata clone
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

  // --- SUBMIT ---
  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    let isValid = false
    await formEl.validate((valid) => {
      isValid = valid
    })
    if (!isValid) {
      ElMessage.warning('Vui lòng kiểm tra lại các trường bắt buộc')
      return false
    }

    const metadataValidation = validateMetadata(
      formData.value.questionType,
      formData.value.metadata,
    )
    if (!metadataValidation.valid) {
      ElMessage.error({
        message: metadataValidation.message,
        duration: 5000,
        showClose: true,
      })
      return false
    }

    const metadata = { ...formData.value.metadata }
    const explanation = metadata.explanation || ''

    // ✅ BASE PAYLOAD - LUÔN CÓ questionType
    const payload = {
      parentId: formData.value.parentId,
      parentType: formData.value.parentType,
      questionType: formData.value.questionType, // ← QUAN TRỌNG
      questionText: formData.value.questionText,
      points: formData.value.points,
      orderIndex: formData.value.orderIndex,
      explanation: explanation,
    }

    // Thêm field theo từng loại
    switch (formData.value.questionType) {
      case 'FILL_BLANK':
        // Validate trước khi gửi
        if (!metadata.blanks || metadata.blanks.length === 0) {
          ElMessage.error('Cần có ít nhất 1 chỗ trống')
          return false
        }

        // Validate từng blank
        for (let i = 0; i < metadata.blanks.length; i++) {
          const blank = metadata.blanks[i]
          if (!blank.correctAnswers || blank.correctAnswers.length === 0) {
            ElMessage.error(
              `Chỗ trống #${i + 1} chưa có đáp án đúng.  Vui lòng nhập ít nhất 1 đáp án. `,
            )
            return false
          }

          // Kiểm tra đáp án rỗng
          const hasEmpty = blank.correctAnswers.some((ans) => !ans || ans.trim() === '')
          if (hasEmpty) {
            ElMessage.error(`Chỗ trống #${i + 1} có đáp án rỗng.  Vui lòng xóa hoặc điền đầy đủ.`)
            return false
          }
        }

        payload.blanks = metadata.blanks
        break

      case 'VERB_FORM':
        if (!metadata.blanks || metadata.blanks.length === 0) {
          ElMessage.error('Cần có ít nhất 1 động từ cần chia')
          return false
        }

        for (let i = 0; i < metadata.blanks.length; i++) {
          const blank = metadata.blanks[i]
          if (!blank.correctAnswers || blank.correctAnswers.length === 0) {
            ElMessage.error(`Động từ #${i + 1} chưa có dạng chia đúng`)
            return false
          }
        }

        payload.blanks = metadata.blanks
        break

      case 'TEXT_ANSWER':
        if (!metadata.correctAnswer || metadata.correctAnswer.trim() === '') {
          ElMessage.error('Chưa nhập đáp án đúng')
          return false
        }
        payload.correctAnswer = metadata.correctAnswer
        payload.caseSensitive = metadata.caseSensitive || false
        break

      case 'MULTIPLE_CHOICE': {
        if (!metadata.options || metadata.options.length < 2) {
          ElMessage.error('Cần có ít nhất 2 đáp án')
          return false
        }

        const hasCorrect = metadata.options.some((opt) => opt.isCorrect === true)
        if (!hasCorrect) {
          ElMessage.error('Phải có ít nhất 1 đáp án đúng')
          return false
        }

        payload.options = metadata.options
        break
      }

      case 'TRUE_FALSE':
        if (metadata.correctAnswer === null || metadata.correctAnswer === undefined) {
          ElMessage.error('Chưa chọn đáp án đúng (True/False)')
          return false
        }
        payload.correctAnswer = metadata.correctAnswer
        break

      case 'ERROR_CORRECTION':
        if (!metadata.errorText || metadata.errorText.trim() === '') {
          ElMessage.error('Chưa nhập từ/cụm từ sai')
          return false
        }
        if (!metadata.correction || metadata.correction.trim() === '') {
          ElMessage.error('Chưa nhập đáp án sửa lại')
          return false
        }
        payload.errorText = metadata.errorText
        payload.correction = metadata.correction
        break

      case 'MATCHING': {
        if (!metadata.pairs || metadata.pairs.length < 2) {
          ElMessage.error('Cần có ít nhất 2 cặp')
          return false
        }

        const hasEmptyPair = metadata.pairs.some(
          (p) => !p.left || !p.right || p.left.trim() === '' || p.right.trim() === '',
        )
        if (hasEmptyPair) {
          ElMessage.error('Có cặp chưa điền đầy đủ')
          return false
        }

        payload.pairs = metadata.pairs
        break
      }

      case 'SENTENCE_BUILDING':
        if (!metadata.words || metadata.words.length < 2) {
          ElMessage.error('Cần có ít nhất 2 từ')
          return false
        }
        if (!metadata.correctSentence || metadata.correctSentence.trim() === '') {
          ElMessage.error('Chưa nhập câu hoàn chỉnh')
          return false
        }
        payload.words = metadata.words
        payload.correctSentence = metadata.correctSentence
        break

      case 'COMPLETE_CONVERSATION':
        if (!metadata.conversationContext || metadata.conversationContext.trim() === '') {
          ElMessage.error('Chưa nhập ngữ cảnh hội thoại')
          return false
        }
        if (!metadata.options || metadata.options.length < 2) {
          ElMessage.error('Cần ít nhất 2 lựa chọn')
          return false
        }
        if (!metadata.correctAnswer || metadata.correctAnswer.trim() === '') {
          ElMessage.error('Chưa chọn đáp án đúng')
          return false
        }
        payload.conversationContext = metadata.conversationContext
        payload.options = metadata.options
        payload.correctAnswer = metadata.correctAnswer
        break

      case 'PRONUNCIATION': {
        if (!metadata.words || metadata.words.length < 2) {
          ElMessage.error('Cần ít nhất 2 từ')
          return false
        }
        if (!metadata.categories || metadata.categories.length < 2) {
          ElMessage.error('Cần ít nhất 2 nhóm âm')
          return false
        }
        if (!metadata.classifications || metadata.classifications.length === 0) {
          ElMessage.error('Chưa phân loại các từ')
          return false
        }

        // Check all words are classified
        const unclassified = metadata.words.filter(
          (word) => !metadata.classifications.find((c) => c.word === word && c.category),
        )
        if (unclassified.length > 0) {
          ElMessage.error(`Các từ chưa được phân loại: ${unclassified.join(', ')}`)
          return false
        }

        payload.words = metadata.words
        payload.categories = metadata.categories
        payload.classifications = metadata.classifications
        break
      }

      case 'READING_COMPREHENSION': {
        if (!metadata.passage || metadata.passage.trim() === '') {
          ElMessage.error('Chưa nhập đoạn văn')
          return false
        }
        if (!metadata.blanks || metadata.blanks.length === 0) {
          ElMessage.error('Cần ít nhất 1 câu hỏi')
          return false
        }

        // Validate each blank
        for (let i = 0; i < metadata.blanks.length; i++) {
          const blank = metadata.blanks[i]
          if (!blank.options || blank.options.length < 2) {
            ElMessage.error(`Câu ${i + 1}: Cần ít nhất 2 lựa chọn`)
            return false
          }
          if (!blank.correctAnswer || blank.correctAnswer.trim() === '') {
            ElMessage.error(`Câu ${i + 1}: Chưa chọn đáp án đúng`)
            return false
          }
        }

        payload.passage = metadata.passage
        payload.blanks = metadata.blanks
        break
      }

      case 'OPEN_ENDED':
        payload.suggestedAnswer = metadata.suggestedAnswer
        payload.timeLimitSeconds = metadata.timeLimitSeconds
        payload.minWord = metadata.minWord
        payload.maxWord = metadata.maxWord
        break
    }

    // LOG CUỐI CÙNG
    console.log('Final Payload:', JSON.stringify(payload, null, 2))

    try {
      if (dialogMode.value === 'create') {
        await store.createQuestion(payload)
      } else {
        await store.updateQuestion(formData.value.id, payload)
      }
      return true
    } catch (error) {
      console.error('Submit Error:', error)
      console.error('Response:', error.response?.data)

      // Hiển thị lỗi chi tiết
      const errorMsg = error.response?.data?.message || 'Có lỗi xảy ra khi lưu câu hỏi'
      ElMessage.error({
        message: errorMsg,
        duration: 5000,
        showClose: true,
      })

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
