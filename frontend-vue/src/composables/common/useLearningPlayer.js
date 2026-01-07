import { ref, computed, onUnmounted, nextTick } from 'vue'

export function useLearningPlayer(store) {
  // --- STATE ---
  const viewMode = ref('full')
  const remainingTime = ref(0)
  const timerInterval = ref(null)

  const userAnswers = ref({})
  const submitting = ref(false)
  const showResult = ref(false)
  const resultData = ref({})
  const hasScrolledToBottom = ref(false)

  let observer = null

  // --- COMPUTED ---
  const answeredCount = computed(() => Object.keys(userAnswers.value).length)

  // --- METHODS ---

  const startTimer = (onTimeoutCallback) => {
    stopTimer()

    // ✅ FIX: Thêm safeguard để tránh timer chạy với giá trị âm
    if (remainingTime.value <= 0) {
      remainingTime.value = 0
      if (onTimeoutCallback) onTimeoutCallback()
      return
    }

    timerInterval.value = setInterval(() => {
      if (remainingTime.value > 0) {
        remainingTime.value--
      } else {
        stopTimer()
        if (onTimeoutCallback) onTimeoutCallback()
      }
    }, 1000)
  }

  const stopTimer = () => {
    if (timerInterval.value) {
      clearInterval(timerInterval.value)
      timerInterval.value = null
    }
  }

  // ✅ FIX: Sửa setupScrollObserver để nhận parameter và query đúng element
  const setupScrollObserver = (markerId) => {
    // Cleanup previous observer
    if (observer) {
      observer.disconnect()
      observer = null
    }

    nextTick(() => {
      // ✅ Tìm element theo ID được truyền vào
      const markerElement = document.getElementById(markerId)
      if (!markerElement) {
        console.warn(`⚠️ Element #${markerId} not found for scroll observer`)
        return
      }

      // ✅ Tìm parent scrollable container
      const contentElement = markerElement.closest('.overflow-y-auto')
      if (!contentElement) {
        console.warn('⚠️ No scrollable parent found')
        return
      }

      let debounceTimer = null
      observer = new IntersectionObserver(
        (entries) => {
          if (debounceTimer) clearTimeout(debounceTimer)
          debounceTimer = setTimeout(() => {
            const isVisible = entries.some((entry) => entry.isIntersecting)
            // ✅ CHỈ update khi giá trị thực sự thay đổi
            if (hasScrolledToBottom.value !== isVisible) {
              hasScrolledToBottom.value = isVisible
            }
          }, 200) // Debounce 200ms để tránh loop
        },
        {
          root: contentElement,
          threshold: 0.1, // Chỉ trigger khi ít nhất 10% visible
        },
      )

      observer.observe(markerElement)
      console.log(`✅ Scroll observer setup for #${markerId}`)
    })
  }

  const handleAnswerUpdate = ({ questionId, value }) => {
    // ✅ Ensure questionId is valid
    if (!questionId) return
    userAnswers.value[questionId] = value
  }

  // ✅ FIX: Xử lý Merge đáp án thông minh
  const mergeResultsToQuestions = (resultData, groupedTasks, standaloneQuestions) => {
    // API trả về 'results' (List) chứ không phải 'questionResults'
    if (!resultData.results) return

    const resultsMap = new Map()
    resultData.results.forEach((r) => resultsMap.set(r.questionId, r))

    const updateQ = (q) => {
      const res = resultsMap.get(q.id)
      if (!res) return

      q.isCorrect = res.isCorrect
      q.feedback = res.feedback
      q.score = res.points

      switch (q.questionType) {
        case 'MULTIPLE_CHOICE':
        case 'TRUE_FALSE':
        case 'PRONUNCIATION':
          q.correctAnswer = res.correctAnswer
          if (q.data && q.data.options && res.correctAnswer) {
            q.data.options.forEach((opt) => {
              if (opt.text == res.correctAnswer || opt.value == res.correctAnswer) {
                opt.isCorrect = true
              }
            })
          }
          break

        case 'FILL_BLANK':
        case 'TEXT_ANSWER':
          q.correctAnswer = res.correctAnswer
          if (q.data && q.data.blanks) {
            let parsedAnswers = {}
            try {
              // Nếu backend trả về JSON {"1": "abc"}, parse ra để map đúng vị trí
              if (res.correctAnswer && res.correctAnswer.startsWith('{')) {
                parsedAnswers = JSON.parse(res.correctAnswer)
              }
            } catch (e) {
              console.error('Error parsing fill-in-the-blank answers:', e)
            }

            q.data.blanks.forEach((blank) => {
              // Nếu có map JSON -> lấy theo position, ngược lại dùng raw string
              if (Object.keys(parsedAnswers).length > 0) {
                const key = blank.position || 1
                if (parsedAnswers[key]) blank.correctAnswers = [parsedAnswers[key]]
              } else if (res.correctAnswer) {
                blank.correctAnswers = [res.correctAnswer]
              }
            })
          }
          break

        case 'ERROR_CORRECTION':
          if (q.data) q.data.correction = res.correctAnswer
          break

        case 'SENTENCE_TRANSFORMATION':
        case 'SENTENCE_BUILDING':
        case 'OPEN_ENDED':
          if (q.data) {
            q.data.correctAnswer = res.correctAnswer
            q.data.correctAnswers = [res.correctAnswer]
          }
          break

        case 'MATCHING':
          try {
            q.correctAnswer =
              typeof res.correctAnswer === 'string'
                ? JSON.parse(res.correctAnswer)
                : res.correctAnswer
          } catch (e) {
            console.error('Error parsing matching correct answer:', e)
            q.correctAnswer = res.correctAnswer
          }
          break
      }
    }

    if (groupedTasks) groupedTasks.forEach((t) => t.questions.forEach(updateQ))
    if (standaloneQuestions) standaloneQuestions.forEach(updateQ)
  }

  // ✅ Submit Exam: Đã bao gồm logic map answers và merge results
  const submitExam = async (lessonId, groupedTasks, standaloneQuestions) => {
    submitting.value = true
    try {
      const answers = Object.entries(userAnswers.value).map(([id, val]) => ({
        questionId: parseInt(id),
        textAnswer: typeof val === 'object' ? JSON.stringify(val) : String(val ?? ''),
        selectedOptions: [],
      }))

      const res = await store.submitLesson({
        lessonId: lessonId,
        answers: answers,
      })

      resultData.value = res

      // Tự động merge kết quả để UI hiển thị xanh/đỏ
      mergeResultsToQuestions(res, groupedTasks, standaloneQuestions)

      showResult.value = true
      stopTimer()

      return res
    } catch (error) {
      console.error('Submit error:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  const resetPlayerState = () => {
    stopTimer()

    // ✅ Reset tất cả ref values
    userAnswers.value = {}
    showResult.value = false
    submitting.value = false
    resultData.value = {}
    hasScrolledToBottom.value = false
    viewMode.value = 'full'
    remainingTime.value = 0

    // ✅ Clear observer
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  const formatTime = (s) => {
    if (!s && s !== 0) return '0:00'
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${m}:${sec.toString().padStart(2, '0')}`
  }

  onUnmounted(() => {
    stopTimer()
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    viewMode,
    remainingTime,
    userAnswers,
    submitting,
    showResult,
    resultData,
    hasScrolledToBottom,
    answeredCount,

    startTimer,
    stopTimer,
    setupScrollObserver,
    handleAnswerUpdate,
    submitExam,
    resetPlayerState,
    formatTime,
  }
}
