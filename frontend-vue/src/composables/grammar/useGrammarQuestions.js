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
    explanation: '',
    points: 1, // Mặc định 1 điểm
    orderIndex: 1,
    metadata: {}
  }

  // Khởi tạo formData là REF
  const formData = ref(JSON.parse(JSON.stringify(defaultFormData)))

  // Form Rules
  const formRules = {
    questionType: [{ required: true, message: 'Vui lòng chọn loại câu hỏi', trigger: 'change' }],
    questionText: [{ required: true, message: 'Nội dung không được để trống', trigger: 'blur' }],
    points: [{ required: true, message: 'Nhập điểm số', trigger: 'blur' }],
    orderIndex: [{ required: true, message: 'Nhập thứ tự', trigger: 'blur' }]
  }

  // Options
  const questionTypeOptions = [
    { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm (Multiple Choice)', description: 'Chọn 1 đáp án đúng từ danh sách' },
    { value: 'TRUE_FALSE', label: 'Đúng / Sai (True/False)', description: 'Xác định mệnh đề là đúng hay sai' },
    { value: 'FILL_BLANK', label: 'Điền từ (Fill in the blank)', description: 'Điền từ còn thiếu vào chỗ trống' },
    { value: 'SHORT_ANSWER', label: 'Trả lời ngắn (Short Answer)', description: 'Trả lời bằng một từ hoặc cụm từ ngắn' },
    { value: 'MATCHING', label: 'Nối từ (Matching)', description: 'Ghép cặp các mục tương ứng với nhau' },
    { value: 'SENTENCE_BUILDING', label: 'Xây dựng câu (Sentence Building)', description: 'Sắp xếp các từ thành câu hoàn chỉnh' },
    { value: 'CONVERSATION', label: 'Hội thoại (Conversation)', description: 'Hoàn thành hoặc sắp xếp đoạn hội thoại' },
    { value: 'PRONUNCIATION', label: 'Phát âm (Pronunciation)', description: 'Kiểm tra phát âm từ vựng' },
    { value: 'VERB_FORM', label: 'Chia động từ (Verb Form)', description: 'Chia dạng đúng của động từ trong ngoặc' },
    { value: 'ERROR_CORRECTION', label: 'Tìm lỗi sai (Error Correction)', description: 'Tìm và sửa lỗi sai trong câu' },
    { value: 'READING_COMPREHENSION', label: 'Đọc hiểu (Reading)', description: 'Đọc đoạn văn và trả lời câu hỏi' },
    { value: 'TEXT_ANSWER', label: 'Tự luận (Text Answer)', description: 'Viết đoạn văn hoặc bài luận' },
    { value: 'OPEN_ENDED', label: 'Câu hỏi mở (Open Ended)', description: 'Câu hỏi không có đáp án cố định' }
  ]

  const currentQuestionTypeOption = computed(() =>
    questionTypeOptions.find(opt => opt.value === formData.value.questionType)
  )

  const dialogTitle = computed(() => dialogMode.value === 'create' ? 'Tạo câu hỏi mới' : 'Chỉnh sửa câu hỏi')
  const submitButtonText = computed(() => dialogMode.value === 'create' ? 'Tạo mới' : 'Cập nhật')

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

    // Clone data để edit
    formData.value = {
      id: question.id,
      parentId: question.parentId || question.lessonId,
      parentType: question.parentType || 'GRAMMAR',
      questionText: question.questionText,
      questionType: question.questionType,
      explanation: question.explanation,
      points: question.points,
      orderIndex: question.orderIndex,
      // Metadata clone
      metadata: question.metadata ? JSON.parse(JSON.stringify(question.metadata)) : {}
    }
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    currentStep.value = 1
    formData.value = JSON.parse(JSON.stringify(defaultFormData))
  }

  const handleQuestionTypeChange = () => {
    if(formData.value) formData.value.metadata = {}
  }

  const nextStep = () => { if (currentStep.value < 3) currentStep.value++ }
  const prevStep = () => { if (currentStep.value > 1) currentStep.value-- }

  // --- SUBMIT ---
  const handleSubmit = async (formEl) => {
    if (!formEl) return false

    // Validate
    let isValid = false
    await formEl.validate((valid) => { isValid = valid })
    if (!isValid) {
      ElMessage.warning('Vui lòng kiểm tra lại các trường bắt buộc')
      return false
    }

    // Flatten Data cho Backend
    const payload = {
      parentId: formData.value.parentId,
      parentType: formData.value.parentType,
      questionText: formData.value.questionText,
      questionType: formData.value.questionType,
      explanation: formData.value.explanation,
      points: formData.value.points,
      orderIndex: formData.value.orderIndex,
      ...formData.value.metadata // Bung metadata ra root
    }

    try {
      if (dialogMode.value === 'create') {
        await store.createQuestion(payload)
      } else {
        await store.updateQuestion(formData.value.id, payload)
      }
      return true
    } catch (error) {
      console.error('API Error:', error)
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
    handleSubmit
  }
}
