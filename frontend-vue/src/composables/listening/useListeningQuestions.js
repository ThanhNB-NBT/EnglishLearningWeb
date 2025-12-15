// src/composables/listening/useListeningQuestions.js
import { ref, computed } from 'vue'
import { useListeningStore } from '@/stores/listening'
import { ElMessage, ElMessageBox } from 'element-plus'

// ═════════════════════════════════════════════════════════════════
// VALIDATION HELPER
// ═════════════════════════════════════════════════════════════════

const validateMetadata = (questionType, metadata) => {
  switch (questionType) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      if (!metadata.options || metadata.options.length < 2) {
        return { valid: false, message: 'Cần ít nhất 2 đáp án' }
      }
      if (! metadata.options.some((o) => o.isCorrect)) {
        return { valid: false, message: 'Phải có ít nhất 1 đáp án đúng' }
      }
      break
    case 'FILL_BLANK':
      if (!metadata.blanks || metadata.blanks.length === 0) {
        return { valid: false, message: 'Cần ít nhất 1 chỗ trống' }
      }
      for (const blank of metadata.blanks) {
        if (! blank.correctAnswers || blank.correctAnswers. length === 0) {
          return { valid: false, message:  `Chỗ trống #${blank.position} chưa có đáp án` }
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

// ═════════════════════════════════════════════════════════════════
// QUESTION FORM COMPOSABLE
// ═════════════════════════════════════════════════════════════════

export function useListeningQuestionForm() {
  const store = useListeningStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const formRef = ref(null)

  const formData = ref({
    id:  null,
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
      { required: false, message: 'Nội dung câu hỏi (có thể để trống cho LISTENING)', trigger: 'blur' },
    ],
    points: [
      { required: true, message: 'Vui lòng nhập điểm', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Điểm phải >= 1', trigger: 'blur' },
    ],
    orderIndex: [
      { required:  true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type:  'number', min: 1, message: 'Thứ tự phải >= 1', trigger: 'blur' },
    ],
  }

  const questionTypeOptions = [
    {
      label: 'Nghe hiểu (Listening Comprehension)',
      value: 'LISTENING_COMPREHENSION',
      group: 'Listening',
    },
    { label: 'Trắc nghiệm (Multiple Choice)', value: 'MULTIPLE_CHOICE', group: 'Cơ bản' },
    { label:  'Đúng / Sai (True/False)', value: 'TRUE_FALSE', group:  'Cơ bản' },
    { label: 'Điền từ (Fill Blank)', value: 'FILL_BLANK', group: 'Cơ bản' },
    { label: 'Trả lời ngắn (Text Answer)', value: 'TEXT_ANSWER', group: 'Cơ bản' },
  ]

  const dialogTitle = computed(() =>
    dialogMode.value === 'create' ? 'Tạo câu hỏi mới' : 'Chỉnh sửa câu hỏi'
  )

  // ═════════════════════════════════════════════════════════════════
  // DIALOG OPERATIONS
  // ═════════════════════════════════════════════════════════════════

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
      metadata:  {},
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
      questionText: question.questionText || '',
      points: question.points,
      orderIndex: question.orderIndex,
      explanation: question. explanation || '',
      metadata: question.metadata || {},
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

      // Validate metadata
      const metadataValidation = validateMetadata(formData.value.questionType, formData.value.metadata)
      if (!metadataValidation.valid) {
        ElMessage.error(metadataValidation.message)
        return false
      }

      try {
        const submitData = {
          parentId: formData.value.parentId,
          parentType: formData.value.parentType,
          questionType: formData.value.questionType,
          questionText: formData.value.questionText,
          points: formData. value.points,
          orderIndex: formData.value.orderIndex,
          explanation: formData. value.explanation,
          metadata: formData.value.metadata,
        }

        if (dialogMode.value === 'create') {
          await store.createQuestion(submitData)
        } else {
          await store.updateQuestion(formData.value.id, submitData)
        }

        closeDialog()
        return true
      } catch (error) {
        console.error('Submit error:', error)
        return false
      }
    })
  }

  return {
    // State
    dialogVisible,
    dialogMode,
    formData,
    formRules,
    formRef,
    questionTypeOptions,

    // Computed
    dialogTitle,

    // Methods
    openCreateDialog,
    openEditDialog,
    closeDialog,
    handleSubmit,
    validateMetadata,
  }
}

// ═════════════════════════════════════════════════════════════════
// QUESTION LIST COMPOSABLE
// ═════════════════════════════════════════════════════════════════

export function useListeningQuestionList() {
  const store = useListeningStore()

  const currentLessonId = ref(null)
  const loading = computed(() => store.questionsLoading)
  const questions = computed(() => store.questions)
  const pagination = computed(() => store.questionsPagination)

  const searchQuery = ref('')
  const filterType = ref('')
  const selectedRows = ref([])

  // ═════════════════════════════════════════════════════════════════
  // FILTERING
  // ═════════════════════════════════════════════════════════════════

  const filteredQuestions = computed(() => {
    let result = questions.value

    // Search by question text
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter((q) => q.questionText?.toLowerCase().includes(query))
    }

    // Filter by type
    if (filterType.value) {
      result = result.filter((q) => q.questionType === filterType.value)
    }

    return result
  })

  // ═════════════════════════════════════════════════════════════════
  // ACTIONS
  // ═════════════════════════════════════════════════════════════════

  const loadQuestions = async (lessonId, params) => {
    if (! lessonId) return
    currentLessonId.value = lessonId
    await store.fetchQuestions(lessonId, params)
  }

  const deleteQuestion = async (question) => {
    try {
      await ElMessageBox. confirm(`Xóa câu hỏi này? `, 'Xác nhận xóa', {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      })

      await store.deleteQuestion(question.id)
      await loadQuestions(currentLessonId.value)
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Delete error:', error)
      }
    }
  }

  const bulkDeleteQuestions = async (questionIds) => {
    try {
      await ElMessageBox.confirm(`Xóa ${questionIds.length} câu hỏi?`, 'Xác nhận xóa', {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      })

      await store.bulkDeleteQuestions(questionIds)
      selectedRows.value = []
      await loadQuestions(currentLessonId.value)
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Bulk delete error:', error)
      }
    }
  }

  const handleSelectionChange = (selection) => {
    selectedRows.value = selection.map((row) => row.id)
  }

  // ═════════════════════════════════════════════════════════════════
  // HELPERS
  // ═════════════════════════════════════════════════════════════════

  const getQuestionTypeLabel = (type) => {
    const labels = {
      MULTIPLE_CHOICE: 'Trắc nghiệm',
      TRUE_FALSE:  'Đúng/Sai',
      FILL_BLANK: 'Điền từ',
      TEXT_ANSWER: 'Trả lời ngắn',
    }
    return labels[type] || type
  }

  const getQuestionTypeColor = (type) => {
    const colors = {
      MULTIPLE_CHOICE: 'success',
      TRUE_FALSE: 'warning',
      FILL_BLANK: 'info',
      TEXT_ANSWER: 'danger',
    }
    return colors[type] || ''
  }

  return {
    // State
    currentLessonId,
    loading,
    questions,
    pagination,
    searchQuery,
    filterType,
    selectedRows,

    // Computed
    filteredQuestions,

    // Methods
    loadQuestions,
    deleteQuestion,
    bulkDeleteQuestions,
    handleSelectionChange,
    getQuestionTypeLabel,
    getQuestionTypeColor,
  }
}

// ═════════════════════════════════════════════════════════════════
// BULK CREATE COMPOSABLE
// ═════════════════════════════════════════════════════════════════

export function useListeningBulkCreate() {
  const store = useListeningStore()

  const visible = ref(false)
  const currentLesson = ref(null)
  const questionList = ref([])
  const showContent = ref(true)

  const open = (lesson) => {
    currentLesson.value = lesson
    questionList.value = []
    showContent.value = true
    visible.value = true
  }

  const close = () => {
    visible.value = false
    currentLesson.value = null
    questionList.value = []
  }

  const addNewQuestion = async () => {
    const nextOrder = questionList.value.length + 1

    questionList.value.push({
      parentId: currentLesson.value. id,
      parentType: 'LISTENING',
      questionType: 'LISTENING_COMPREHENSION',
      questionText: '',
      points: 10,
      orderIndex: nextOrder,
      explanation: '',
      metadata:  {},
    })
  }

  const removeQuestion = (index) => {
    questionList.value.splice(index, 1)
    // Re-index
    questionList.value.forEach((q, i) => {
      q.orderIndex = i + 1
    })
  }

  const cloneQuestion = (index) => {
    const cloned = JSON.parse(JSON.stringify(questionList.value[index]))
    cloned.orderIndex = questionList.value.length + 1
    questionList.value.push(cloned)
  }

  const updateQuestion = (index, data) => {
    questionList.value[index] = data
  }

  const handleSubmit = async () => {
    // Validate all questions
    for (let i = 0; i < questionList.value.length; i++) {
      const q = questionList.value[i]
      const validation = validateMetadata(q.questionType, q.metadata)
      if (!validation.valid) {
        ElMessage.error(`Câu ${i + 1}:${validation.message}`)
        return
      }
    }

    if (questionList.value.length === 0) {
      ElMessage.warning('Chưa có câu hỏi nào')
      return
    }

    try {
      await store.bulkCreateQuestions(currentLesson.value.id, questionList.value)
      close()
    } catch (error) {
      console.error('Bulk create error:', error)
    }
  }

  return {
    // State
    visible,
    currentLesson,
    questionList,
    showContent,

    // Methods
    open,
    close,
    addNewQuestion,
    removeQuestion,
    cloneQuestion,
    updateQuestion,
    handleSubmit,
  }
}
