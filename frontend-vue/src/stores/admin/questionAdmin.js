// src/stores/admin/questionAdmin.js - UPDATED VERSION
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { grammarAdminAPI, listeningAdminAPI, readingAdminAPI, taskGroupAPI } from '@/api'
import { ElMessage } from 'element-plus'

// ==================== HELPER: TRANSFORM FORM -> DTO ====================
function toQuestionDTO(formData, lessonId, parentType) {
  console.log('🔍 Input formData:', formData)
  let questionData = {
    explanation: formData.explanation || null, // Use null instead of empty string
  }

  console.log('🔍 Constructed questionData:', questionData)

  switch (formData.questionType) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
    case 'COMPLETE_CONVERSATION':
      questionData.options = formData.options || []
      break

    case 'FILL_BLANK':
    case 'VERB_FORM':
    case 'TEXT_ANSWER':
      questionData.blanks = formData.blanks || []
      questionData.wordBank = formData.wordBank || []
      break

    case 'MATCHING':
      questionData.pairs = formData.pairs || []
      break

    case 'SENTENCE_BUILDING':
      questionData.words = formData.words || []
      questionData.correctSentence = formData.correctSentence || ''
      break

    case 'SENTENCE_TRANSFORMATION':
      questionData.originalSentence = formData.originalSentence || ''
      questionData.beginningPhrase = formData.beginningPhrase || ''
      questionData.correctAnswers = formData.correctAnswers || []
      break

    case 'ERROR_CORRECTION':
      questionData.errorText = formData.errorText || ''
      questionData.correction = formData.correction || ''
      break

    case 'PRONUNCIATION':
      questionData.words = formData.words || []
      questionData.categories = formData.categories || []
      questionData.classifications = formData.classifications || []
      break

    case 'OPEN_ENDED':
      questionData.suggestedAnswer = formData.suggestedAnswer || ''
      questionData.minWord = formData.minWord || null
      questionData.maxWord = formData.maxWord || null
      questionData.timeLimitSeconds = formData.timeLimitSeconds || 0
      break
  }

  const payload = {
    parentId: lessonId,
    parentType: parentType,
    questionType: formData.questionType,
    questionText: formData.questionText || null, // ✅ Allow null
    points: formData.points || 1,
    orderIndex: formData.orderIndex || 0,
    taskGroupId: formData.taskGroupId || null,
    data: questionData, // ✅ Already structured correctly
  }

  if (formData.id) payload.id = formData.id

  console.log('📤 Payload gửi lên backend:', JSON.stringify(payload, null, 2))

  return payload
}

function getApi(parentType) {
  switch (parentType) {
    case 'LISTENING':
      return listeningAdminAPI
    case 'READING':
      return readingAdminAPI
    case 'GRAMMAR':
      return grammarAdminAPI
    default:
      throw new Error(`Unknown parent type: ${parentType}`)
  }
}

// ==================== STORE ====================

export const useQuestionStore = defineStore('admin-question', () => {
  const loading = ref(false)
  const questions = ref([])
  const groupedQuestions = ref({})
  const currentLessonId = ref(null)

  // ✅ NEW: TaskGroup state
  const taskGroups = ref([])
  const taskGroupsLoading = ref(false)

  // --- TASK GROUP ACTIONS ---

  const fetchTaskGroups = async (parentType, lessonId) => {
    taskGroupsLoading.value = true
    try {
      const response = await taskGroupAPI.getTaskGroupsByLesson(parentType, lessonId)
      taskGroups.value = response.data.data || []
      console.log('✅ Fetched TaskGroups:', taskGroups.value.length)
      return taskGroups.value
    } catch (error) {
      console.error('❌ Fetch TaskGroups error:', error)
      ElMessage.error('Lỗi tải danh sách Task Groups')
      throw error
    } finally {
      taskGroupsLoading.value = false
    }
  }

  const createTaskGroup = async (parentType, lessonId, data) => {
    try {
      const response = await taskGroupAPI.createTaskGroup(parentType, lessonId, data)
      if (response.data.success) {
        ElMessage.success('Tạo Task Group thành công')
        // Refresh list
        await fetchTaskGroups(parentType, lessonId)
        return response.data.data
      }
    } catch (error) {
      console.error('❌ Create TaskGroup error:', error)
      ElMessage.error(error.response?.data?.message || 'Tạo Task Group thất bại')
      throw error
    }
  }

  const updateTaskGroup = async (taskGroupId, data) => {
    try {
      const response = await taskGroupAPI.updateTaskGroup(taskGroupId, data)
      if (response.data.success) {
        ElMessage.success('Cập nhật Task Group thành công')
        return response.data.data
      }
    } catch (error) {
      console.error('❌ Update TaskGroup error:', error)
      ElMessage.error('Cập nhật Task Group thất bại')
      throw error
    }
  }

  const deleteTaskGroup = async (taskGroupId, parentType, lessonId) => {
    try {
      await taskGroupAPI.deleteTaskGroup(taskGroupId)
      ElMessage.success('Đã xóa Task Group và tất cả câu hỏi bên trong')
      // Refresh both lists
      await fetchTaskGroups(parentType, lessonId)
      await fetchQuestions(parentType, lessonId)
    } catch (error) {
      console.error('❌ Delete TaskGroup error:', error)
      ElMessage.error('Xóa Task Group thất bại')
      throw error
    }
  }

  // --- QUESTION ACTIONS ---

  const fetchQuestions = async (parentType, lessonId) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      const response = await api.getQuestionsByLesson(lessonId)

      const responseData = response.data.data

      if (responseData && typeof responseData === 'object' && 'hasTaskStructure' in responseData) {
        console.log('✅ Received grouped structure:', responseData)

        groupedQuestions.value = responseData

        let allQuestions = []
        if (responseData.standaloneQuestions && Array.isArray(responseData.standaloneQuestions)) {
          allQuestions.push(...responseData.standaloneQuestions)
        }
        if (responseData.tasks && Array.isArray(responseData.tasks)) {
          responseData.tasks.forEach((task) => {
            if (task.questions && Array.isArray(task.questions)) {
              allQuestions.push(...task.questions)
            }
          })
        }

        questions.value = allQuestions
        console.log('✅ Flattened questions:', allQuestions.length)
      } else if (Array.isArray(responseData)) {
        console.log('✅ Received flat array:', responseData.length)
        questions.value = responseData
        groupedQuestions.value = null
      } else {
        console.warn('⚠️ Unexpected response format:', responseData)
        questions.value = []
        groupedQuestions.value = null
      }

      currentLessonId.value = lessonId
      return questions.value
    } catch (error) {
      console.error('❌ Fetch error:', error)
      ElMessage.error('Lỗi tải danh sách câu hỏi')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchNextOrder = async (parentType, lessonId) => {
    if (!lessonId) return 1
    try {
      const api = getApi(parentType)
      const response = await api.getNextQuestionOrderIndex(lessonId)
      return response.data.data?.nextOrderIndex || 1
    } catch (error) {
      console.error('Fetch order error:', error)
      if (questions.value.length > 0) {
        const max = Math.max(...questions.value.map((q) => q.orderIndex || 0))
        return max + 1
      }
      return 1
    }
  }

  const createQuestion = async (parentType, lessonId, formData) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      const payload = toQuestionDTO(formData, lessonId, parentType)
      const response = await api.createQuestion(lessonId, payload)
      ElMessage.success('Tạo câu hỏi thành công')
      await fetchQuestions(parentType, lessonId)
      return response.data.data
    } catch (error) {
      console.error('Create error:', error)
      ElMessage.error(error.response?.data?.message || 'Tạo câu hỏi thất bại')
      throw error
    } finally {
      loading.value = false
    }
  }

  const updateQuestion = async (parentType, lessonId, questionId, formData) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      const payload = toQuestionDTO(formData, lessonId, parentType)
      const response = await api.updateQuestion(questionId, payload)
      ElMessage.success('Cập nhật thành công')
      await fetchQuestions(parentType, lessonId)
      return response.data.data
    } catch (error) {
      console.error('Update error:', error)
      ElMessage.error(error.response?.data?.message || 'Cập nhật thất bại')
      throw error
    } finally {
      loading.value = false
    }
  }

  const deleteQuestion = async (parentType, lessonId, questionId) => {
    try {
      const api = getApi(parentType)
      await api.deleteQuestion(questionId)
      ElMessage.success('Xóa thành công')
      await fetchQuestions(parentType, lessonId)
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Xóa thất bại')
      throw error
    }
  }

  const createQuestionsBulk = async (parentType, lessonId, questionsList) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      const payloadList = questionsList.map((q) => toQuestionDTO(q, lessonId, parentType))
      await api.createQuestionsBulk(lessonId, payloadList)
      ElMessage.success(`Đã tạo ${questionsList.length} câu hỏi`)
      await fetchQuestions(parentType, lessonId)
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'Lỗi tạo hàng loạt')
      throw error
    } finally {
      loading.value = false
    }
  }

  const deleteQuestionsBulk = async (parentType, lessonId, ids) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      await api.bulkDeleteQuestions(ids)
      ElMessage.success(`Đã xóa ${ids.length} câu hỏi`)
      await fetchQuestions(parentType, lessonId)
    } catch (error) {
      ElMessage.error('Xóa hàng loạt thất bại')
      throw error
    } finally {
      loading.value = false
    }
  }

  const fixOrderIndexes = async (parentType, lessonId) => {
    loading.value = true
    try {
      const api = getApi(parentType)
      await api.fixQuestionOrder(lessonId)
      ElMessage.success('Đã sắp xếp lại thứ tự')
      await fetchQuestions(parentType, lessonId)
    } catch (error) {
      console.error('Fix order error:', error)
      ElMessage.error('Sắp xếp thất bại')
    } finally {
      loading.value = false
    }
  }

  const fetchQuestionById = async (parentType, questionId) => {
    try {
      const api = getApi(parentType)
      const response = await api.getQuestionById(questionId)
      return response.data.data
    } catch (error) {
      console.error('❌ Fetch question by id error:', error)
      throw error
    }
  }

  return {
    loading,
    questions,
    groupedQuestions,

    // TaskGroup exports
    taskGroups,
    taskGroupsLoading,
    fetchTaskGroups,
    createTaskGroup,
    updateTaskGroup,
    deleteTaskGroup,

    // Question exports
    fetchQuestions,
    fetchQuestionById,
    fetchNextOrder,
    createQuestion,
    updateQuestion,
    deleteQuestion,
    createQuestionsBulk,
    deleteQuestionsBulk,
    fixOrderIndexes,
  }
})
