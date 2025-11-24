// composables/useGrammarForm.js
import { ref, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'

export function useGrammarTopicForm() {
  const grammarStore = useGrammarStore()

  const dialogVisible = ref(false)
  const dialogMode = ref('create') // 'create' | 'edit'

  const formData = ref({
    id: null,
    name: '',
    description: '',
    levelRequired: 'BEGINNER',
    orderIndex: 1,
    isActive: true,
  })

  const formRules = {
    name: [
      { required: true, message: 'Vui lòng nhập tên topic', trigger: 'blur' },
      { max: 200, message: 'Tên không được vượt quá 200 ký tự', trigger: 'blur' },
    ],
    levelRequired: [
      { required: true, message: 'Vui lòng chọn level', trigger: 'change' },
    ],
    orderIndex: [
      { required: true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
      { type: 'number', min: 1, message: 'Thứ tự phải lớn hơn 0', trigger: 'blur' },
    ],
  }

  const levelOptions = [
    { value: 'BEGINNER', label: 'Beginner', description: 'A1-A2' },
    { value: 'INTERMEDIATE', label: 'Intermediate', description: 'B1-B2' },
    { value: 'ADVANCED', label: 'Advanced', description: 'C1-C2' },
  ]

  const dialogTitle = computed(() => {
    return dialogMode.value === 'create' ? 'Tạo Topic Mới' : 'Chỉnh Sửa Topic'
  })

  const submitButtonText = computed(() => {
    return dialogMode.value === 'create' ? 'Tạo Mới' : 'Cập Nhật'
  })

  // Open create dialog
  const openCreateDialog = async () => {
    dialogMode.value = 'create'
    const nextOrder = await grammarStore.getNextTopicOrderIndex()
    formData.value = {
      id: null,
      name: '',
      description: '',
      levelRequired: 'BEGINNER',
      orderIndex: nextOrder,
      isActive: true,
    }
    dialogVisible.value = true
  }

  // Open edit dialog
  const openEditDialog = (topic) => {
    dialogMode.value = 'edit'
    formData.value = {
      id: topic.id,
      name: topic.name,
      description: topic.description || '',
      levelRequired: topic.levelRequired,
      orderIndex: topic.orderIndex,
      isActive: topic.isActive ?? true,
    }
    dialogVisible.value = true
  }

  // Submit form - receive formRef from component
  const handleSubmit = async (formRefInstance) => {
    if (!formRefInstance) {
      console.error('Form ref is null')
      return false
    }

    try {
      // Validate form
      await formRefInstance.validate()

      let result
      if (dialogMode.value === 'create') {
        result = await grammarStore.createTopic(formData.value)
      } else {
        result = await grammarStore.updateTopic(formData.value.id, formData.value)
      }

      if (result.success) {
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
    // Don't reset formRef here, only reset form data
    formData.value = {
      id: null,
      name: '',
      description: '',
      levelRequired: 'BEGINNER',
      orderIndex: 1,
      isActive: true,
    }
  }

  return {
    // State (return ref trực tiếp, không .value)
    dialogVisible,
    dialogMode,
    formData,
    formRules,
    levelOptions,

    // Computed (return computed trực tiếp)
    dialogTitle,
    submitButtonText,

    // Methods
    openCreateDialog,
    openEditDialog,
    handleSubmit,
    closeDialog,
  }
}
