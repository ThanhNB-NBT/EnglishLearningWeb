// src/composables/reading/useReadingPlayer.js
import { ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { readingUserAPI } from '@/api'

export function useReadingPlayer() {
  const route = useRoute()
  const router = useRouter()

  // --- STATE ---
  const loadingLesson = ref(false)
  const allLessons = ref([])
  const currentLesson = ref(null)
  const userAnswers = ref({})
  const isSubmitted = ref(false)
  const submitting = ref(false)
  const resultData = ref(null)
  const remainingTime = ref(0)
  const timerInterval = ref(null) // FIX: Đổi từ let sang ref
  const isComponentMounted = ref(false) // FIX: Đổi từ true sang false ban đầu

  // UI State
  const isSidebarOpen = ref(true)
  const showTranslation = ref(false)
  const fontSize = ref(18)

  // --- COMPUTED ---
  const totalQuestions = computed(() => currentLesson.value?.questions?.length || 0)
  const answeredCount = computed(() => Object.keys(userAnswers.value).length)
  const timerClass = computed(() =>
    remainingTime.value < 60 ? 'text-red-500 animate-pulse' : 'text-gray-600 dark:text-gray-300',
  )

  // --- METHODS ---
  const init = async () => {
    try {
      isComponentMounted.value = true // Đánh dấu component đã mount
      const res = await readingUserAPI.getLessons()
      allLessons.value = res.data.data || []

      const targetId = route.params.lessonId
      if (!targetId) {
        const next = allLessons.value.find((l) => !l.isCompleted && l.isAccessible)
        const target = next || allLessons.value[0]
        if (target) {
          router.replace({ name: 'user-reading-detail', params: { lessonId: target.id } })
          return
        }
      } else {
        await loadLesson(targetId)
      }
    } catch (e) {
      ElMessage.error('Không thể tải danh sách bài học')
      console.error(e)
    }
  }

  const cleanup = () => {
    stopTimer()
    isComponentMounted.value = false
  }

  const loadLesson = async (id) => {
    if (currentLesson.value?.id == id && !isSubmitted.value) return

    loadingLesson.value = true
    stopTimer()
    isSubmitted.value = false
    userAnswers.value = {}
    resultData.value = null
    showTranslation.value = false

    try {
      const res = await readingUserAPI.getLessonDetail(id)
      currentLesson.value = res.data.data

      if (currentLesson.value?.questions) {
        const initialAnswers = {}
        currentLesson.value.questions.forEach((q) => {
          // Khởi tạo theo loại câu hỏi
          const type = q.questionType?.toUpperCase()

          if (['FILL_BLANK', 'VERB_FORM'].includes(type)) {
            initialAnswers[q.id] = {} // Object cho multiple blanks
          } else if (type === 'MATCHING') {
            initialAnswers[q.id] = {} // Object cho matching pairs
          } else {
            initialAnswers[q.id] = '' // String cho các loại khác
          }
        })
        userAnswers.value = initialAnswers
      }

      if (currentLesson.value && !currentLesson.value.isCompleted && !isSubmitted.value) {
        remainingTime.value = currentLesson.value.timeLimitSeconds || 600
        startTimer()
      }

      if (window.innerWidth < 1280) isSidebarOpen.value = false
    } catch (e) {
      ElMessage.error(e.response?.data?.message || 'Lỗi tải bài học')
      if (!allLessons.value.length) router.push({ name: 'user-home' })
    } finally {
      loadingLesson.value = false
    }
  }

  const handleSelectLesson = (lesson) => {
    if (!lesson.isAccessible && !lesson.isCompleted) {
      ElMessage.warning('Bạn cần hoàn thành bài trước để mở khóa!')
      return
    }
    router.push({ name: 'user-reading-detail', params: { lessonId: lesson.id } })
    if (window.innerWidth < 1024) isSidebarOpen.value = false
  }

  const handleSubmit = async (auto = false) => {
    if (submitting.value) return

    if (!auto) {
      try {
        await ElMessageBox.confirm('Nộp bài ngay?', 'Xác nhận', {
          confirmButtonText: 'Nộp',
          cancelButtonText: 'Hủy',
        })
      } catch {
        return
      }
    }

    submitting.value = true
    stopTimer()

    try {
      const payload = {
        answers: Object.entries(userAnswers.value).map(([k, v]) => ({
          questionId: parseInt(k),
          answer: v,
        })),
      }
      const res = await readingUserAPI.submitLesson(currentLesson.value.id, payload)
      resultData.value = res.data.data
      isSubmitted.value = true

      // Cập nhật trạng thái list local
      const item = allLessons.value.find((l) => l.id === currentLesson.value.id)
      if (item && resultData.value.isPassed) {
        item.isCompleted = true
        const idx = allLessons.value.findIndex((l) => l.id === currentLesson.value.id)
        if (idx < allLessons.value.length - 1) allLessons.value[idx + 1].isAccessible = true
      }

      // Scroll to result
      document.getElementById('questions-area')?.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (e) {
      ElMessage.error('Nộp bài thất bại')
      console.error(e)
    } finally {
      submitting.value = false
    }
  }

  // --- TIMER UTILS ---
  const startTimer = () => {
    timerInterval.value = setInterval(() => {
      // Wrap trong nextTick để đợi render cycle
      nextTick(() => {
        if (!isComponentMounted.value) return

        if (remainingTime.value > 0) {
          remainingTime.value--
        } else {
          stopTimer()
          handleSubmit(true)
        }
      })
    }, 1000)
  }

  const stopTimer = () => {
    if (timerInterval.value) {
      clearInterval(timerInterval.value)
      timerInterval.value = null
    }
  }

  const formatTime = (s) =>
    `${Math.floor(s / 60)
      .toString()
      .padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}`

  // --- QUESTION UTILS ---
  const getQuestionType = (type) => {
    const t = type?.toUpperCase()
    if (['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(t)) return 'MULTIPLE_CHOICE'
    if (['MATCHING'].includes(t)) return 'MATCHING'
    if (['FILL_BLANK', 'VERB_FORM'].includes(t)) return 'FILL_BLANK'
    return 'TEXT_ANSWER'
  }

  const isCorrect = (qid) =>
    resultData.value?.questionResults?.find((r) => r.questionId === qid)?.isCorrect

  // --- UI UTILS ---
  const toggleSidebar = () => (isSidebarOpen.value = !isSidebarOpen.value)
  const adjustFontSize = (d) => (fontSize.value = Math.max(14, Math.min(26, fontSize.value + d)))
  const goBack = () => router.push({ name: 'user-home' })
  const retryLesson = () => loadLesson(currentLesson.value.id)
  const goToNextLesson = () => {
    if (resultData.value?.nextLessonId) {
      router.push({
        name: 'user-reading-detail',
        params: { lessonId: resultData.value.nextLessonId },
      })
    }
  }

  const getDifficultyColor = (diff) => {
    if (diff === 'BEGINNER') return 'text-green-600'
    if (diff === 'INTERMEDIATE') return 'text-yellow-600'
    return 'text-red-600'
  }

  return {
    // State
    loadingLesson,
    allLessons,
    currentLesson,
    userAnswers,
    isSubmitted,
    submitting,
    resultData,
    remainingTime,
    isSidebarOpen,
    showTranslation,
    fontSize,

    // Computed
    totalQuestions,
    answeredCount,
    timerClass,

    // Methods
    init,
    cleanup,
    loadLesson,
    handleSelectLesson,
    handleSubmit,
    stopTimer,
    formatTime,
    getQuestionType,
    isCorrect,
    toggleSidebar,
    adjustFontSize,
    goBack,
    retryLesson,
    goToNextLesson,
    getDifficultyColor,
  }
}
