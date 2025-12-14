<template>
  <div class="h-screen flex flex-col bg-gray-50 dark:bg-[#0a0a0a] font-sans">
    <!-- Header -->
    <header
      class="h-14 bg-white dark:bg-[#1e1e1e] border-b border-gray-200 dark:border-gray-800 flex items-center justify-between px-4 z-30 shrink-0 shadow-sm"
    >
      <div class="flex items-center gap-3 flex-1 min-w-0">
        <button
          @click="goBackToList"
          class="p-2 -ml-2 rounded-full hover:bg-gray-100 dark:hover:bg-[#333] text-gray-600 dark:text-gray-400 transition-colors"
        >
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </button>

        <div class="flex flex-col">
          <h1 class="text-base font-bold text-gray-900 dark:text-white leading-tight truncate max-w-md">
            {{ lesson?.title }}
          </h1>
          <span class="text-xs text-gray-500 font-medium uppercase tracking-wider">
            {{ lesson?.lessonType === 'THEORY' ? 'Lý thuyết' : 'Bài tập' }}
          </span>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <div
          v-if="lesson?.lessonType === 'PRACTICE' && ! isSubmitted"
          class="flex items-center gap-2 px-3 py-1.5 bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-400 rounded border border-orange-200 dark:border-orange-800 font-mono font-bold text-sm"
        >
          <el-icon><Timer /></el-icon>
          <span>{{ formatTime(remainingTime) }}</span>
        </div>

        <div
          v-if="lesson?. lessonType === 'THEORY' && ! canFinishTheory"
          class="flex items-center gap-2 px-3 py-1.5 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 rounded border border-blue-200 dark:border-blue-800 font-mono font-bold text-sm"
        >
          <el-icon class="animate-pulse"><Reading /></el-icon>
          <span>{{ countdown }}s</span>
        </div>

        <button
          class="text-gray-600 dark:text-gray-400 hover:text-black dark:hover:text-white transition-colors p-2"
          @click="isSidebarOpen = !isSidebarOpen"
        >
          <el-icon :size="22"><Menu /></el-icon>
        </button>
      </div>
    </header>

    <div class="flex-1 flex overflow-hidden relative">
      <div
        v-if="loading"
        class="absolute inset-0 flex items-center justify-center bg-white dark:bg-[#121212] z-20"
      >
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>

      <main v-else-if="lesson" class="flex-1 flex h-full overflow-hidden relative">
        <!-- Content Panel -->
        <div
          v-if="lesson.content"
          class="flex-1 h-full overflow-y-auto bg-white dark:bg-[#1e1e1e] border-r border-gray-200 dark:border-gray-800 custom-scrollbar"
        >
          <div class="max-w-4xl mx-auto p-8">
            <div class="prose dark:prose-invert max-w-none">
              <div
                class="ql-editor !p-0 text-gray-900 dark:text-gray-100 text-base leading-loose font-serif"
                v-html="lesson.content"
              ></div>

              <div v-if="! hasQuestions && lesson.lessonType === 'THEORY'" class="mt-12 text-center">
                <button
                  class="px-8 py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-sm tracking-widest uppercase hover:opacity-80 transition-opacity disabled:opacity-50"
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
        </div>

        <!-- Questions Panel -->
        <div
          v-if="hasQuestions"
          class="flex-1 h-full overflow-y-auto bg-white dark:bg-[#121212] custom-scrollbar relative"
          id="questions-container"
        >
          <div class="max-w-4xl mx-auto p-8 pb-24">
            <div class="flex items-center justify-between mb-6 pb-3 border-b-2 border-gray-800 dark:border-gray-200">
              <h2 class="text-xl font-bold text-gray-900 dark:text-white uppercase tracking-tight">
                Bài tập
              </h2>
              <span class="text-sm font-medium text-gray-500">
                Tổng:  {{ lesson. questions.length }} câu
              </span>
            </div>

            <!-- Not Submitted -->
            <div v-if="!isSubmitted" class="space-y-6">
              <div
                v-for="(q, idx) in lesson.questions"
                :key="q.id"
                class="question-item"
                :id="'q-' + q.id"
              >
                <div class="flex gap-2 mb-2">
                  <span class="text-base font-bold text-gray-900 dark:text-gray-100 shrink-0">
                    {{ idx + 1 }}.
                  </span>
                  <div class="flex-1">
                    <div
                      v-if="!shouldHideQuestionText(q)"
                      class="text-base text-gray-900 dark:text-gray-100 leading-relaxed ql-editor ! p-0 font-medium"
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

            <!-- Result -->
            <div v-else class="py-10 text-center">
              <h2 class="text-3xl font-bold mb-4 text-gray-900 dark:text-white">
                {{ resultData.isPassed ? 'ĐẠT' : 'KHÔNG ĐẠT' }}
              </h2>
              <div
                class="text-6xl font-mono font-bold mb-6"
                :class="resultData.isPassed ? 'text-green-600' :  'text-red-600'"
              >
                {{ resultData.score }}/{{
                  resultData.totalQuestions * (lesson.questions[0]. points || 1)
                }}
              </div>
              <div class="flex justify-center gap-4">
                <button
                  v-if="resultData.hasUnlockedNext"
                  @click="goToNextLesson(resultData.nextLessonId)"
                  class="px-6 py-2 bg-black dark:bg-white text-white dark:text-black font-bold hover:opacity-80"
                >
                  Bài tiếp
                </button>
                <button
                  @click="reviewAnswers"
                  class="px-6 py-2 border-2 border-black dark:border-white text-black dark:text-white font-bold hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  Xem lại
                </button>
                <button
                  @click="retryLesson"
                  class="px-6 py-2 border-2 border-black dark:border-white text-black dark:text-white font-bold hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  Làm lại
                </button>
              </div>
            </div>
          </div>

          <!-- Submit Button -->
          <div
            v-if="!isSubmitted"
            class="fixed bottom-0 right-0 left-0 lg:left-1/2 p-4 bg-white/95 dark:bg-[#121212]/95 border-t border-gray-200 dark:border-gray-800 backdrop-blur flex justify-end"
          >
            <button
              class="px-8 py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-sm tracking-widest uppercase hover:opacity-80 transition-opacity disabled:opacity-50"
              :disabled="submitting"
              @click="handleSubmit(false)"
            >
              {{ submitting ? 'ĐANG NỘP.. .' : 'NỘP BÀI' }}
            </button>
          </div>
        </div>
      </main>

      <!-- Sidebar (GIỐNG HỆT ReadingPlayerView) -->
      <transition name="slide-right">
        <aside
          v-if="isSidebarOpen"
          class="absolute right-0 top-0 bottom-0 w-72 bg-white dark:bg-[#1a1a1a] border-l border-gray-200 dark:border-gray-800 z-40 flex flex-col shadow-2xl"
        >
          <div class="h-12 px-4 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-gray-50 dark:bg-[#252525]">
            <span class="font-bold text-sm uppercase text-gray-700 dark:text-gray-200">Danh sách bài</span>
            <button @click="isSidebarOpen = false" class="text-gray-500 hover:text-gray-900 dark:hover:text-white">
              ✕
            </button>
          </div>

          <div class="flex-1 overflow-y-auto custom-scrollbar">
            <div
              v-for="(item, idx) in siblingLessons"
              :key="item.id"
              @click="switchLesson(item)"
              class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-[#252525] transition-colors"
              :class="{ 'bg-blue-50 dark:bg-blue-900/20 font-bold': item.id === lesson?. id }"
            >
              <div class="flex gap-2 items-start">
                <span class="text-gray-500 text-xs mt-0.5 shrink-0">{{ idx + 1 }}.</span>
                <span class="text-sm flex-1">{{ item.title }}</span>
                <span v-if="item.isCompleted" class="text-green-600 shrink-0">✓</span>
                <span v-else-if="!item.isUnlocked" class="text-gray-400 shrink-0">🔒</span>
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
import { ref, computed, onMounted, onUnmounted, defineAsyncComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { grammarUserAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Menu, Timer, Reading, Loading } from '@element-plus/icons-vue'
import 'quill/dist/quill.snow.css'

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
    lesson.value = res.data. data
    if (lesson.value?. topicId) {
      const tRes = await grammarUserAPI.getTopicDetails(lesson.value.topicId)
      siblingLessons.value = tRes.data.data.lessons || []
    }
    if (lesson.value. lessonType === 'THEORY' && ! lesson.value.isCompleted) {
      countdown.value = lesson.value.timeLimitSeconds || 30
      theoryTimer = setInterval(() => {
        readingTime.value++
        if (countdown.value > 0) countdown.value--
      }, 1000)
    } else if (lesson.value.lessonType === 'PRACTICE' && ! lesson.value.isCompleted) {
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

const finishTheory = async () => {
  if (lesson.value.isCompleted) {
    if (siblingLessons.value) {
      const currentIndex = siblingLessons.value.findIndex((l) => l.id === lesson. value.id)
      const nextLesson = siblingLessons. value[currentIndex + 1]
      if (nextLesson && nextLesson.isUnlocked) {
        goToNextLesson(nextLesson.id)
        return
      }
    }
    goBackToList()
    return
  }

  try {
    const res = await grammarUserAPI. submitLesson({
      lessonId: lesson. value.id,
      readingTimeSecond: readingTime.value,
    })

    ElMessage.success('Đã hoàn thành bài đọc!')

    if (res.data.data.hasUnlockedNext && res.data.data.nextLessonId) {
      goToNextLesson(res. data.data.nextLessonId)
    } else {
      goBackToList()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e. response?.data?.message || 'Lỗi lưu tiến độ')
  }
}

const handleSubmit = async (auto = false) => {
  if (submitting.value) return

  submitting.value = true
  clearTimers()

  if (auto) {
    ElMessage.warning('Hết giờ! Hệ thống đang tự động nộp bài.. .')
  }

  try {
    const payload = Object.entries(userAnswers.value).map(([qid, ans]) => ({
      questionId: parseInt(qid),
      answer: ans,
    }))

    const res = await grammarUserAPI. submitLesson({
      lessonId:  lesson.value.id,
      answers: payload,
    })

    resultData.value = res.data. data
    isSubmitted.value = true

    if (lesson.value.topicId) {
      const tRes = await grammarUserAPI. getTopicDetails(lesson.value.topicId)
      siblingLessons.value = tRes.data.data.lessons || []
    }

    document.getElementById('questions-container')?.scrollTo({ top: 0, behavior:  'smooth' })
  } catch (e) {
    console.error(e)
    ElMessage.error(e.response?.data?.message || 'Nộp bài thất bại.  Vui lòng thử lại.')
  } finally {
    submitting.value = false
  }
}

const goBackToList = () => router.push({ name: 'user-grammar' })
const goToNextLesson = (id) => router.push({ name: 'user-grammar-lesson', params: { lessonId:  id } })
const switchLesson = (item) => {
  if (! item. isUnlocked) return
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

const getQuestionComponent = (type) => {
  const t = type?.toUpperCase()
  if (['MULTIPLE_CHOICE', 'TRUE_FALSE', 'COMPLETE_CONVERSATION'].includes(t))
    return QuestionMultipleChoice
  if (['FILL_BLANK', 'VERB_FORM']. includes(t)) return QuestionFillBlank
  if (['TEXT_ANSWER']. includes(t)) return QuestionTextAnswer
  if (['MATCHING'].includes(t)) return QuestionMatching
  if (['SENTENCE_BUILDING'].includes(t)) return QuestionSentenceBuilding
  if (['SENTENCE_TRANSFORMATION'].includes(t)) return QuestionSentenceTransformation
  if (['PRONUNCIATION'].includes(t)) return QuestionPronunciation
  if (['ERROR_CORRECTION'].includes(t)) return QuestionErrorCorrection
  if (['OPEN_ENDED'].includes(t)) return QuestionOpenEnded
  return QuestionMultipleChoice
}

const shouldHideQuestionText = (question) => {
  const type = question. questionType?.toUpperCase()

  // Chỉ ẩn questionText nếu là FILL_BLANK và có nhiều blanks (có Word Bank)
  if (type === 'FILL_BLANK') {
    const blanks = question.metadata?.blanks || []
    return blanks.length > 1
  }

  return false
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
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

:deep(.ql-editor) {
  text-align: justify ! important;
  text-justify:  inter-word !important;
  padding: 0 !important;
}
:deep(.ql-editor p) {
  margin-bottom: 1em;
  line-height: 1.8;
}

.question-item {
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(229, 231, 235, 0.6);
}

.question-item:last-child {
  border-bottom:  none;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}
.slide-right-enter-from {
  transform: translateX(100%);
}
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
