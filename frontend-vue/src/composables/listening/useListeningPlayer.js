// src/composables/listening/useListeningPlayer.js
import { ref, computed, watch, onUnmounted } from 'vue'
import { listeningUserAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useListeningPlayer() {
  // ==================== STATE ====================
  const loadingLesson = ref(false)
  const currentLesson = ref(null)
  const userAnswers = ref({})
  const isSubmitted = ref(false)
  const submitting = ref(false)
  const resultData = ref(null)
  const remainingTime = ref(0)
  const timerInterval = ref(null)
  const hasPlayedOnce = ref(false)

  // ==================== COMPUTED ====================
  const totalQuestions = computed(() => {
    return currentLesson.value?.questions?.length || 0
  })

  const answeredCount = computed(() => {
    if (!currentLesson.value?.questions) return 0

    // ✅ ĐƠN GIẢN - chỉ đếm keys có giá trị
    const count = Object.keys(userAnswers.value).filter(key => {
      const answer = userAnswers.value[key]
      if (!answer) return false

      // Nếu là object
      if (typeof answer === 'object' && !Array.isArray(answer)) {
        // Check các giá trị string trong object (Fill Blank, Matching)
        const values = Object.values(answer).filter(v => typeof v === 'string')
        return values.some(v => v && v.trim())
      }

      // Nếu là string trực tiếp
      if (typeof answer === 'string') {
        return answer.trim() !== ''
      }

      return false
    }).length

    console.log('🔢 Answered:', count, '/', totalQuestions.value)
    return count
  })

  const timerClass = computed(() => {
    if (remainingTime.value <= 60) return 'text-red-600 dark:text-red-400 animate-pulse'
    if (remainingTime.value <= 300) return 'text-orange-600 dark:text-orange-400'
    return 'text-gray-800 dark:text-gray-200'
  })

  // ==================== METHODS ====================

  const initializeAnswers = () => {
    userAnswers.value = {}
    if (!currentLesson.value?.questions) return

    console.log('🔧 Initializing answers for', currentLesson.value.questions.length, 'questions')

    // ✅ GIỐNG HỆT READING - ĐƠN GIẢN
    currentLesson.value.questions.forEach((question) => {
      const type = question.questionType?.toUpperCase()

      if (['FILL_BLANK', 'LISTENING_COMPREHENSION'].includes(type)) {
        userAnswers.value[question.id] = {}  // Object rỗng, component tự xử lý
      } else if (type === 'MATCHING') {
        userAnswers.value[question.id] = {}  // Object cho matching pairs
      } else {
        userAnswers.value[question.id] = ''  // String cho Multiple Choice, Text Answer
      }
    })

    console.log('✅ Initialized answers:', JSON.parse(JSON.stringify(userAnswers.value)))
  }

  const loadLesson = async (lessonId) => {
    console.log('🔥 Loading lesson:', lessonId)
    loadingLesson.value = true

    try {
      const response = await listeningUserAPI.getLessonDetail(lessonId)
      console.log('✅ Lesson response:', response.data)

      if (response.data.success) {
        currentLesson.value = response.data.data
        console.log('📖 Current lesson:', currentLesson.value)

        initializeAnswers()
        startTimer()
      } else {
        throw new Error('Failed to load lesson')
      }
    } catch (error) {
      console.error('❌ Failed to load lesson:', error)
      ElMessage.error(error.response?.data?.message || 'Không thể tải bài nghe')
    } finally {
      loadingLesson.value = false
    }
  }

  const startTimer = () => {
    if (timerInterval.value) {
      clearInterval(timerInterval.value)
    }

    remainingTime.value = currentLesson.value?.timeLimitSeconds || 600
    console.log('⏱️ Timer started:', remainingTime.value, 'seconds')

    timerInterval.value = setInterval(() => {
      if (remainingTime.value > 0 && !isSubmitted.value) {
        remainingTime.value--
      } else if (remainingTime.value === 0 && !isSubmitted.value) {
        handleSubmit(true)
      }
    }, 1000)
  }

  const handlePlay = async () => {
    if (!currentLesson.value) return

    try {
      await listeningUserAPI.trackPlay(currentLesson.value.id)
      console.log('🎵 Play tracked')

      // ✅ CHỈ update playCount và transcriptUnlocked, KHÔNG reload toàn bộ lesson
      const response = await listeningUserAPI.getLessonDetail(currentLesson.value.id)
      if (response.data.success) {
        // Chỉ update các field cần thiết, GIỮ NGUYÊN userAnswers
        currentLesson.value.playCount = response.data.data.playCount
        currentLesson.value.transcriptUnlocked = response.data.data.transcriptUnlocked
      }
    } catch (error) {
      console.error('Failed to track play:', error)
    }
  }

  const handleViewTranscript = async () => {
    if (!currentLesson.value) return

    try {
      await listeningUserAPI.viewTranscript(currentLesson.value.id)
      ElMessage.success('Đã mở khóa transcript')

      // ✅ CHỈ update transcriptUnlocked, KHÔNG reload
      const response = await listeningUserAPI.getLessonDetail(currentLesson.value.id)
      if (response.data.success) {
        currentLesson.value.transcriptUnlocked = response.data.data.transcriptUnlocked
        currentLesson.value.transcript = response.data.data.transcript
      }
    } catch (error) {
      ElMessage.error('Chưa thể xem transcript')
      console.error('Failed to view transcript:', error)
    }
  }

  const handleSubmit = async (isTimeout = false) => {
    if (submitting.value || isSubmitted.value) return

    if (!isTimeout && answeredCount.value < totalQuestions.value) {
      try {
        await ElMessageBox.confirm(
          `Bạn mới trả lời ${answeredCount.value}/${totalQuestions.value} câu. Bạn có chắc muốn nộp bài?`,
          'Xác nhận nộp bài',
          {
            confirmButtonText: 'Nộp bài',
            cancelButtonText: 'Kiểm tra lại',
            type: 'warning',
          }
        )
      } catch {
        return
      }
    }

    submitting.value = true

    try {
      // ✅ Build payload - GIỮ NGUYÊN TEXT như Reading
      const payload = {
        answers: Object.entries(userAnswers.value).map(([k, v]) => ({
          questionId: parseInt(k),
          answer: v,  // Gửi trực tiếp TEXT, KHÔNG convert sang index
        })),
      }

      // ✅ Log chi tiết TỪNG câu hỏi để debug
      console.log('📤 ========== SUBMITTING LISTENING LESSON ==========')
      console.log('Total questions:', currentLesson.value.questions.length)

      payload.answers.forEach((item, index) => {
        const question = currentLesson.value.questions.find(q => q.id === item.questionId)
        console.log(`\n[${index + 1}] Question ${item.questionId}:`)
        console.log('  Type:', question?.questionType)
        console.log('  Answer type:', typeof item.answer)
        console.log('  Is array:', Array.isArray(item.answer))
        console.log('  Is object:', typeof item.answer === 'object' && !Array.isArray(item.answer))
        console.log('  Value:', JSON.stringify(item.answer))

        // Log metadata nếu là Fill Blank
        if (['FILL_BLANK', 'LISTENING_COMPREHENSION'].includes(question?.questionType?.toUpperCase())) {
          console.log('  Blanks count:', question.metadata?.blanks?.length || 0)
        }
      })

      console.log('\n📦 Full Payload:', JSON.stringify(payload, null, 2))
      console.log('================================================\n')

      const response = await listeningUserAPI.submitLesson(currentLesson.value.id, payload)

      console.log('✅ Response received:', response)
      console.log('✅ Response data:', response.data)

      if (response.data.success) {
        resultData.value = response.data.data
        isSubmitted.value = true

        if (timerInterval.value) {
          clearInterval(timerInterval.value)
          timerInterval.value = null
        }

        window.scrollTo({ top: 0, behavior: 'smooth' })

        if (isTimeout) {
          ElMessage.warning('Hết giờ! Bài làm đã được tự động nộp.')
        } else {
          ElMessage.success('Đã nộp bài thành công!')
        }
      }
    } catch (error) {
      console.error('❌ Submit failed:', error)
      console.error('❌ Error response:', error.response?.data)
      ElMessage.error(error.response?.data?.message || 'Lỗi khi nộp bài')
    } finally {
      submitting.value = false
    }
  }

  const isCorrect = (questionId) => {
    if (!resultData.value?.results) return false
    const result = resultData.value.results.find((r) => r.questionId === questionId)
    return result?.isCorrect || false
  }

  const retryLesson = () => {
    isSubmitted.value = false
    resultData.value = null
    hasPlayedOnce.value = false
    loadLesson(currentLesson.value.id)
    ElMessage.info('Bắt đầu làm lại bài nghe')
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const cleanup = () => {
    if (timerInterval.value) {
      clearInterval(timerInterval.value)
      timerInterval.value = null
    }
  }

  const reset = () => {
    isSubmitted.value = false
    resultData.value = null
    hasPlayedOnce.value = false
  }

  // ==================== WATCHERS ====================

  watch(
    userAnswers,
    (newVal, oldVal) => {
      console.log('👀 UserAnswers changed:')

      // Tìm key nào thay đổi
      Object.keys(newVal).forEach(key => {
        if (JSON.stringify(newVal[key]) !== JSON.stringify(oldVal?.[key])) {
          const question = currentLesson.value?.questions?.find(q => q.id == key)
          console.log(`  📝 Q${key} (${question?.questionType}):`, {
            old: oldVal?.[key],
            new: newVal[key],
            type: typeof newVal[key],
            isObject: typeof newVal[key] === 'object' && !Array.isArray(newVal[key])
          })
        }
      })
    },
    { deep: true }
  )

  // ==================== LIFECYCLE ====================

  onUnmounted(() => {
    cleanup()
  })

  // ==================== RETURN ====================

  return {
    // State
    loadingLesson,
    currentLesson,
    userAnswers,
    isSubmitted,
    submitting,
    resultData,
    remainingTime,
    hasPlayedOnce,

    // Computed
    totalQuestions,
    answeredCount,
    timerClass,

    // Methods
    loadLesson,
    handleSubmit,
    handlePlay,
    handleViewTranscript,
    isCorrect,
    retryLesson,
    formatTime,
    cleanup,
    reset,
  }
}
