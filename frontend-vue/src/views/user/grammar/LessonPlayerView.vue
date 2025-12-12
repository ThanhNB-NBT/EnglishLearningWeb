<template>
  <div class="h-screen flex flex-col bg-[#f9f9f9] dark:bg-[#121212] font-sans">
    <header
      class="h-14 bg-white dark:bg-[#1e1e1e] border-b border-gray-300 dark:border-gray-700 flex items-center justify-between px-4 z-30 shrink-0"
    >
      <div class="flex items-center gap-4 flex-1 min-w-0">
        <button
          @click="goBackToList"
          class="text-gray-600 dark:text-gray-400 hover:text-black dark:hover:text-white"
        >
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </button>
        <h1 class="text-base font-bold text-gray-800 dark:text-gray-200 truncate">
          {{ lesson?.title }}
        </h1>
      </div>

      <div class="flex items-center gap-3">
        <span
          v-if="lesson?.lessonType === 'PRACTICE' && !isSubmitted"
          class="font-mono font-bold text-orange-600 dark:text-orange-400 bg-orange-50 dark:bg-orange-900/20 px-2 py-1 rounded"
        >
          {{ formatTime(remainingTime) }}
        </span>
        <button @click="isSidebarOpen = !isSidebarOpen" class="text-gray-600 dark:text-gray-400">
          <el-icon :size="20"><Menu /></el-icon>
        </button>
      </div>
    </header>

    <div class="flex-1 flex overflow-hidden relative">
      <div
        v-if="loading"
        class="absolute inset-0 flex items-center justify-center bg-white dark:bg-[#121212] z-20"
      >
        Loading...
      </div>

      <main
        v-else-if="lesson"
        class="flex-1 flex flex-col lg:flex-row h-full overflow-hidden relative"
      >
        <div
          v-if="lesson.content"
          class="flex-1 lg:w-1/2 h-1/2 lg:h-full overflow-y-auto bg-white dark:bg-[#1e1e1e] border-b lg:border-b-0 lg:border-r border-gray-300 dark:border-gray-700 p-6 md:p-10 custom-scrollbar"
        >
          <div class="max-w-3xl mx-auto prose dark:prose-invert">
            <div
              class="ql-editor !p-0 text-gray-900 dark:text-gray-100 text-lg leading-loose font-serif"
              v-html="lesson.content"
            ></div>

            <div v-if="!hasQuestions && lesson.lessonType === 'THEORY'" class="mt-12 text-center">
              <button
                class="px-6 py-2 border-2 border-black dark:border-white text-black dark:text-white font-bold hover:bg-black hover:text-white dark:hover:bg-white dark:hover:text-black transition-colors disabled:opacity-50"
                :disabled="!canFinishTheory"
                @click="finishTheory"
              >
                {{
                  canFinishTheory
                    ? lesson.isCompleted
                      ? 'Tiếp tục'
                      : 'Hoàn thành'
                    : `Đọc trong ${countdown}s`
                }}
              </button>
            </div>
          </div>
        </div>

        <div
          v-if="hasQuestions"
          class="flex-1 lg:w-1/2 h-1/2 lg:h-full overflow-y-auto bg-[#fff] dark:bg-[#121212] p-6 md:p-10 custom-scrollbar relative"
          id="questions-container"
        >
          <div class="max-w-3xl mx-auto pb-24">
            <div
              class="flex items-center justify-between mb-8 pb-2 border-b-2 border-gray-800 dark:border-gray-200"
            >
              <h2 class="text-xl font-bold text-gray-900 dark:text-white uppercase tracking-tight">
                Bài tập
              </h2>
              <span class="text-sm font-medium text-gray-500"
                >Tổng: {{ lesson.questions.length }} câu</span
              >
            </div>

            <div v-if="!isSubmitted" class="space-y-8">
              <div
                v-for="(q, idx) in lesson.questions"
                :key="q.id"
                class="relative pl-0 md:pl-0"
                :id="'q-' + q.id"
              >
                <div class="flex gap-2 mb-2">
                  <span class="text-lg font-bold text-gray-900 dark:text-gray-100 select-none"
                    >{{ idx + 1 }}.</span
                  >

                  <div class="flex-1">
                    <div
                      v-if="!shouldHideQuestionText(q.questionType)"
                      class="text-lg text-gray-900 dark:text-gray-100 leading-relaxed ql-editor !p-0 font-medium"
                      v-html="q.questionText"
                    ></div>
                  </div>
                </div>

                <div class="mt-2 ml-6">
                  <component
                    :is="getQuestionComponent(q.questionType)"
                    :question="q"
                    v-model="userAnswers[q.id]"
                  />
                </div>
              </div>
            </div>

            <div v-else class="py-10 text-center">
              <h2 class="text-3xl font-bold mb-4">
                {{ resultData.isPassed ? 'ĐẠT' : 'KHÔNG ĐẠT' }}
              </h2>
              <div
                class="text-6xl font-mono font-bold mb-6"
                :class="resultData.isPassed ? 'text-green-600' : 'text-red-600'"
              >
                {{ resultData.score }}/{{
                  resultData.totalQuestions * (lesson.questions[0].points || 1)
                }}
              </div>
              <div class="flex justify-center gap-4">
                <button
                  v-if="resultData.hasUnlockedNext"
                  @click="goToNextLesson(resultData.nextLessonId)"
                  class="px-6 py-2 bg-black text-white font-bold hover:bg-gray-800"
                >
                  Bài tiếp
                </button>
                <button
                  @click="reviewAnswers"
                  class="px-6 py-2 border border-black hover:bg-gray-100 dark:border-white dark:text-white dark:hover:bg-gray-800"
                >
                  Xem lại
                </button>
                <button
                  @click="retryLesson"
                  class="px-6 py-2 border border-black hover:bg-gray-100 dark:border-white dark:text-white dark:hover:bg-gray-800"
                >
                  Làm lại
                </button>
              </div>
            </div>
          </div>

          <div
            v-if="!isSubmitted"
            class="fixed bottom-0 right-0 w-full lg:w-1/2 p-4 bg-white/90 dark:bg-[#121212]/90 border-t border-gray-200 dark:border-gray-800 backdrop-blur flex justify-end"
          >
            <button
              class="px-8 py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-sm tracking-widest uppercase hover:opacity-80 transition-opacity disabled:opacity-50"
              :disabled="submitting"
              @click="handleSubmit(false)"
            >
              {{ submitting ? 'ĐANG NỘP...' : 'NỘP BÀI' }}
            </button>
          </div>
        </div>
      </main>

      <transition name="slide-right">
        <aside
          v-if="isSidebarOpen"
          class="absolute right-0 top-0 bottom-0 w-72 bg-white dark:bg-[#1a1a1a] border-l border-gray-300 dark:border-gray-700 z-40 flex flex-col shadow-2xl"
        >
          <div
            class="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center bg-gray-50 dark:bg-[#252525]"
          >
            <span class="font-bold text-sm uppercase">Mục lục</span>
            <button @click="isSidebarOpen = false">✕</button>
          </div>
          <div class="flex-1 overflow-y-auto">
            <div
              v-for="(item, idx) in siblingLessons"
              :key="item.id"
              class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-[#252525]"
              :class="{ 'bg-gray-100 dark:bg-[#2a2a2a] font-bold': item.id === lesson?.id }"
              @click="switchLesson(item)"
            >
              <div class="flex gap-2">
                <span class="text-gray-500 text-xs mt-0.5">{{ idx + 1 }}.</span>
                <span class="text-sm">{{ item.title }}</span>
                <span v-if="item.isCompleted" class="ml-auto text-green-600">✓</span>
                <span v-else-if="!item.isUnlocked" class="ml-auto text-gray-400">🔒</span>
              </div>
            </div>
          </div>
        </aside>
      </transition>

      <div
        v-if="isSidebarOpen"
        class="absolute inset-0 bg-black/20 z-30 lg:hidden"
        @click="isSidebarOpen = false"
      ></div>
    </div>
  </div>
</template>

<script setup>
// ... (Giữ nguyên toàn bộ script logic từ phiên bản trước) ...
// Import đầy đủ các components và icons
import { ref, computed, onMounted, onUnmounted, defineAsyncComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { grammarUserAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Menu, Timer, Right } from '@element-plus/icons-vue'
import 'quill/dist/quill.snow.css'

// Imports Components
const QuestionMultipleChoice = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionMultipleChoice.vue'),
)
const QuestionFillBlank = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionFillBlank.vue'),
)
const QuestionMatching = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionMatching.vue'),
)
const QuestionSentenceBuilding = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionSentenceBuilding.vue'),
)
const QuestionSentenceTransformation = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionSentenceTransformation.vue'),
)
const QuestionPronunciation = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionPronunciation.vue'),
)
const QuestionErrorCorrection = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionErrorCorrection.vue'),
)
const QuestionOpenEnded = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionOpenEnded.vue'),
)
const QuestionTextAnswer = defineAsyncComponent(
  () => import('@/components/user/questions/QuestionTextAnswer.vue'),
)
// ... (Copy lại toàn bộ logic script từ câu trả lời trước) ...
// Chỉ cần đảm bảo import đúng và đủ
const route = useRoute()
const router = useRouter()
const lesson = ref(null)
const siblingLessons = ref([])
const loading = ref(true)
const submitting = ref(false)
const userAnswers = ref({})
const isSubmitted = ref(false)
const resultData = ref(null)
const isReviewMode = ref(false)
const isSidebarOpen = ref(false)
const readingTime = ref(0)
const countdown = ref(0)
let theoryTimer = null
const remainingTime = ref(0)
let practiceTimer = null

const hasQuestions = computed(() => lesson.value?.questions && lesson.value.questions.length > 0)
const canFinishTheory = computed(() => countdown.value === 0)

const loadLesson = async (lessonId) => {
  loading.value = true
  isSubmitted.value = false
  userAnswers.value = {}
  clearTimers()
  try {
    const res = await grammarUserAPI.getLessonContent(lessonId)
    lesson.value = res.data.data
    if (lesson.value?.topicId) {
      const tRes = await grammarUserAPI.getTopicDetails(lesson.value.topicId)
      siblingLessons.value = tRes.data.data.lessons || []
    }
    if (lesson.value.lessonType === 'THEORY' && !lesson.value.isCompleted) {
      countdown.value = lesson.value.timeLimitSeconds || 30
      theoryTimer = setInterval(() => {
        readingTime.value++
        if (countdown.value > 0) countdown.value--
      }, 1000)
    } else if (lesson.value.lessonType === 'PRACTICE' && !lesson.value.isCompleted) {
      remainingTime.value = lesson.value.timeLimitSeconds || 600
      practiceTimer = setInterval(() => {
        if (remainingTime.value > 0) remainingTime.value--
        else {
          clearInterval(practiceTimer)
          handleSubmit(true)
        }
      }, 1000)
    }
  } catch (e) {
    ElMessage.error('Lỗi tải dữ liệu')
    console.error(e)
  } finally {
    loading.value = false
  }
}
const clearTimers = () => {
  if (theoryTimer) clearInterval(theoryTimer)
  if (practiceTimer) clearInterval(practiceTimer)
}
// Xử lý khi hoàn thành bài Lý thuyết (Theory)
const finishTheory = async () => {
  // Nếu đã hoàn thành rồi thì chỉ cần chuyển bài (hoặc về danh sách)
  if (lesson.value.isCompleted) {
    if (siblingLessons.value) {
      const currentIndex = siblingLessons.value.findIndex((l) => l.id === lesson.value.id)
      const nextLesson = siblingLessons.value[currentIndex + 1]
      if (nextLesson && nextLesson.isUnlocked) {
        goToNextLesson(nextLesson.id)
        return
      }
    }
    goBackToList()
    return
  }

  try {
    // Gọi API submit với thời gian đọc
    const res = await grammarUserAPI.submitLesson({
      lessonId: lesson.value.id,
      readingTimeSecond: readingTime.value,
    })

    ElMessage.success('Đã hoàn thành bài đọc!')

    // Nếu mở khóa bài mới thì chuyển sang, không thì về danh sách
    if (res.data.data.hasUnlockedNext && res.data.data.nextLessonId) {
      goToNextLesson(res.data.data.nextLessonId)
    } else {
      goBackToList()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e.response?.data?.message || 'Lỗi lưu tiến độ')
  }
}

// Xử lý nộp bài tập (Practice)
const handleSubmit = async (auto = false) => {
  if (submitting.value) return

  submitting.value = true
  clearTimers() // Dừng đồng hồ

  if (auto) {
    ElMessage.warning('Hết giờ! Hệ thống đang tự động nộp bài...')
  }

  try {
    // Chuẩn hóa dữ liệu câu trả lời để gửi về Backend
    const payload = Object.entries(userAnswers.value).map(([qid, ans]) => ({
      questionId: parseInt(qid),
      answer: ans,
    }))

    // Gọi API nộp bài
    const res = await grammarUserAPI.submitLesson({
      lessonId: lesson.value.id,
      answers: payload,
    })

    // Lưu kết quả trả về để hiển thị màn hình kết quả
    resultData.value = res.data.data
    isSubmitted.value = true

    // Cập nhật lại Sidebar (để tick xanh bài vừa làm xong)
    if (lesson.value.topicId) {
      const tRes = await grammarUserAPI.getTopicDetails(lesson.value.topicId)
      siblingLessons.value = tRes.data.data.lessons || []
    }

    // Cuộn lên đầu cột câu hỏi để xem kết quả
    document.getElementById('questions-container')?.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    console.error(e)
    ElMessage.error(e.response?.data?.message || 'Nộp bài thất bại. Vui lòng thử lại.')
    // Nếu lỗi, cho phép nộp lại (start lại timer nếu cần thiết, hoặc giữ nguyên state để user bấm lại nút Nộp)
  } finally {
    submitting.value = false
  }
}
const goBackToList = () => router.push({ name: 'user-grammar' })
const goToNextLesson = (id) =>
  router.push({ name: 'user-grammar-lesson', params: { lessonId: id } })
const switchLesson = (item) => {
  if (!item.isUnlocked) return
  router.push({ name: 'user-grammar-lesson', params: { lessonId: item.id } })
  isSidebarOpen.value = false
}
const retryLesson = () => {
  isSubmitted.value = false
  userAnswers.value = {}
  resultData.value = null
  loadLesson(lesson.value.id)
}
const reviewAnswers = () => (isReviewMode.value = true)
const formatTime = (s) =>
  `${Math.floor(s / 60)
    .toString()
    .padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}`
const formatAnswer = (a) => (typeof a === 'object' ? JSON.stringify(a) : a || '(Trống)')
const getQuestionComponent = (type) => {
  const t = type?.toUpperCase()
  if (['MULTIPLE_CHOICE', 'TRUE_FALSE', 'COMPLETE_CONVERSATION'].includes(t))
    return QuestionMultipleChoice
  if (['FILL_BLANK', 'VERB_FORM'].includes(t)) return QuestionFillBlank
  if (['TEXT_ANSWER'].includes(t)) return QuestionTextAnswer
  if (['MATCHING'].includes(t)) return QuestionMatching
  if (['SENTENCE_BUILDING'].includes(t)) return QuestionSentenceBuilding
  if (['SENTENCE_TRANSFORMATION'].includes(t)) return QuestionSentenceTransformation
  if (['PRONUNCIATION'].includes(t)) return QuestionPronunciation
  if (['ERROR_CORRECTION'].includes(t)) return QuestionErrorCorrection
  if (['OPEN_ENDED'].includes(t)) return QuestionOpenEnded
  return QuestionMultipleChoice
}

const shouldHideQuestionText = (type) => {
  return ['FILL_BLANK', 'VERB_FORM'].includes(type?.toUpperCase())
}

watch(
  () => route.params.lessonId,
  (id) => {
    if (id) loadLesson(id)
  },
  { immediate: true },
)
onUnmounted(clearTimers)
</script>

<style scoped>
/* Reset */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}
</style>
