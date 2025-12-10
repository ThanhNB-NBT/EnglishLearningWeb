<template>
  <div class="h-screen flex flex-col bg-gray-50 dark:bg-[#0f0f0f]">

    <header
      class="h-14 flex items-center justify-between px-4 border-b border-gray-300 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] shrink-0 z-30 shadow-sm">

      <div class="flex items-center gap-4 min-w-0 flex-1">
        <button @click="goBackToList"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#2c2c2c] hover:text-blue-600 transition-all border border-transparent hover:border-gray-200 dark:hover:border-gray-600">
          <el-icon :size="18">
            <ArrowLeft />
          </el-icon>
          <span class="text-sm font-medium hidden sm:block">Quay lại</span>
        </button>

        <div class="h-6 w-px bg-gray-300 dark:bg-gray-600 mx-2 hidden sm:block"></div>

        <div v-if="lesson" class="flex items-center gap-3 min-w-0">
          <h1 class="font-bold text-gray-800 dark:text-white text-base truncate">
            {{ lesson.title }}
          </h1>
          <el-tag size="small" :type="lesson.lessonType === 'THEORY' ? 'info' : 'warning'" effect="plain" round
            class="hidden md:inline-flex">
            {{ lesson.lessonType }}
          </el-tag>
        </div>
      </div>

      <div class="flex items-center gap-3 shrink-0">
        <div class="hidden md:flex items-center gap-3 px-3 py-1 bg-gray-100 dark:bg-[#262626] rounded-full">
          <span class="text-xs text-gray-500 dark:text-gray-400">Tiến độ:</span>
          <div class="flex items-center gap-1.5">
            <span class="text-xs font-bold text-blue-600 dark:text-blue-400">{{ completedCount }}/{{
              siblingLessons.length }}</span>
            <el-progress type="circle" :percentage="topicCompletionRate" :width="18" :stroke-width="3"
              :show-text="false" color="#2563eb" />
          </div>
        </div>

        <div class="h-6 w-px bg-gray-300 dark:bg-gray-600 mx-1 hidden md:block"></div>

        <el-tooltip :content="isSidebarOpen ? 'Thu gọn danh sách' : 'Mở danh sách'" placement="bottom">
          <button
            class="hidden md:flex p-2 rounded-md hover:bg-gray-200 dark:hover:bg-[#2c2c2c] text-gray-600 dark:text-gray-300 transition-colors"
            @click="isSidebarOpen = !isSidebarOpen">
            <el-icon :size="20">
              <component :is="isSidebarOpen ? 'Expand' : 'Fold'" />
            </el-icon>
          </button>
        </el-tooltip>

        <button class="md:hidden p-2 rounded-md text-gray-600 dark:text-gray-300 hover:bg-gray-100"
          @click="mobileDrawerOpen = true">
          <el-icon :size="22">
            <Menu />
          </el-icon>
        </button>
      </div>
    </header>

    <div class="flex flex-1 overflow-hidden relative">

      <main class="flex-1 overflow-y-auto bg-gray-100 dark:bg-[#0f0f0f] scroll-smooth relative flex flex-col"
        ref="contentArea" @scroll="handleScroll">

        <div v-if="loading" class="flex-1 flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <el-icon class="is-loading text-blue-500" :size="32">
              <Loading />
            </el-icon>
            <span class="text-gray-500 text-sm">Đang tải nội dung...</span>
          </div>
        </div>

        <div v-else-if="lesson" class="flex-1 flex flex-col w-full h-full">

          <div v-if="lesson.lessonType === 'THEORY'" class="flex flex-col min-h-full">
            <div class="w-full bg-white dark:bg-[#1d1d1d] flex-1 p-6 md:p-10 shadow-sm">
              <div class="ql-editor !p-0 text-gray-800 dark:text-gray-200 text-lg leading-relaxed w-full"
                v-html="lesson.content"></div>
            </div>

            <div
              class="sticky bottom-0 bg-white/95 dark:bg-[#1d1d1d]/95 backdrop-blur border-t border-gray-200 dark:border-gray-700 p-4 z-20 w-full shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
              <div class="flex flex-col items-center gap-3 max-w-4xl mx-auto w-full">
                <div v-if="!canFinishTheory"
                  class="text-orange-500 dark:text-orange-400 text-sm font-medium flex items-center gap-2 animate-pulse">
                  <el-icon class="is-loading">
                    <Loading />
                  </el-icon> Vui lòng đọc trong {{ countdown }}s...
                </div>
                <button
                  class="w-full md:w-1/2 lg:w-1/3 h-11 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg active:scale-95 flex items-center justify-center gap-2"
                  :class="{ 'grayscale': !canFinishTheory }" :disabled="!canFinishTheory" @click="finishTheory">
                  <span>{{ lesson.isCompleted ? 'Tiếp tục bài tiếp theo' : 'Hoàn thành bài học' }}</span>
                  <el-icon>
                    <Right />
                  </el-icon>
                </button>
              </div>
            </div>
          </div>

          <div v-else-if="lesson.lessonType === 'PRACTICE'"
            class="flex-1 w-full bg-gray-50 dark:bg-[#0f0f0f] p-4 md:p-6">
            <div v-if="!isSubmitted" class="space-y-6 w-full max-w-5xl mx-auto">
              <div v-for="(q, idx) in lesson.questions" :key="q.id"
                class="bg-white dark:bg-[#1d1d1d] rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm overflow-hidden"
                :id="'q-' + q.id">

                <div
                  class="flex justify-between items-center bg-gray-50 dark:bg-[#252525] px-5 py-3 border-b border-gray-200 dark:border-gray-700">
                  <div class="flex items-center gap-3">
                    <span class="bg-blue-600 text-white font-bold px-2.5 py-0.5 rounded text-xs">Câu {{ idx + 1
                      }}</span>
                    <span
                      class="text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-200 dark:bg-gray-700 px-2 py-0.5 rounded">{{
                      q.questionType }}</span>
                  </div>
                  <span class="text-sm font-bold text-gray-500">{{ q.points }} điểm</span>
                </div>

                <div class="p-5">
                  <div class="text-base font-medium text-gray-800 dark:text-gray-200 mb-6 ql-editor !p-0"
                    v-html="q.questionText"></div>

                  <div
                    class="bg-blue-50/50 dark:bg-[#262626] p-4 rounded-lg border border-blue-100 dark:border-gray-600">
                    <component :is="getQuestionComponent(q.questionType)" :question="q" v-model="userAnswers[q.id]" />
                  </div>
                </div>
              </div>

              <div class="sticky bottom-4 z-20 flex justify-center w-full">
                <div
                  class="bg-white/80 dark:bg-[#1d1d1d]/80 backdrop-blur-md p-2 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-700 flex gap-4">
                  <button
                    class="px-8 h-12 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-lg shadow-lg shadow-blue-500/30 transition-transform active:scale-95 flex items-center gap-2"
                    :disabled="submitting" @click="handleSubmit">
                    <el-icon v-if="submitting" class="is-loading">
                      <Loading />
                    </el-icon>
                    <span>NỘP BÀI</span>
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="flex items-center justify-center min-h-full">
              <div
                class="bg-white dark:bg-[#1d1d1d] p-8 md:p-12 rounded-2xl shadow-xl text-center w-full max-w-lg border border-gray-200 dark:border-gray-700">
                <div
                  class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6 shadow-inner ring-4 ring-offset-4 ring-offset-white dark:ring-offset-[#1d1d1d]"
                  :class="resultData.isPassed ? 'bg-green-100 text-green-500 ring-green-100' : 'bg-yellow-100 text-yellow-500 ring-yellow-100'">
                  <el-icon :size="48">
                    <component :is="resultData.isPassed ? Trophy : Warning" />
                  </el-icon>
                </div>

                <h2 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                  {{ resultData.isPassed ? 'Xuất sắc!' : 'Cần cố gắng hơn' }}
                </h2>
                <p class="text-gray-500 dark:text-gray-400 mb-8 text-lg">
                  Bạn đạt <b class="text-gray-900 dark:text-white text-2xl">{{ resultData.score }}</b> điểm
                  <br><span class="text-sm">({{ resultData.correctAnswers }}/{{ resultData.totalQuestions }} câu
                    đúng)</span>
                </p>

                <div class="grid gap-3 w-full">
                  <button v-if="resultData.hasUnlockedNext"
                    class="h-12 w-full bg-green-600 hover:bg-green-700 text-white rounded-xl font-bold transition-all shadow-md flex items-center justify-center gap-2"
                    @click="goToNextLesson(resultData.nextLessonId)">
                    Bài tiếp theo <el-icon>
                      <Right />
                    </el-icon>
                  </button>
                  <div class="flex gap-3">
                    <button
                      class="h-11 flex-1 bg-white dark:bg-[#2c2c2c] border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-[#333] text-gray-700 dark:text-white rounded-lg font-medium transition-colors"
                      @click="reviewAnswers">
                      Xem lại
                    </button>
                    <button
                      class="h-11 flex-1 text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900/20 dark:hover:bg-blue-900/40 rounded-lg font-medium transition-colors"
                      @click="retryLesson">
                      Làm lại
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <aside
        class="hidden md:flex flex-col bg-white dark:bg-[#1d1d1d] border-l border-gray-300 dark:border-gray-700 transition-all duration-300 ease-in-out shrink-0 z-20"
        :class="isSidebarOpen ? 'w-80 translate-x-0' : 'w-0 border-l-0 overflow-hidden'">
        <div
          class="h-12 flex items-center px-4 font-bold text-gray-700 dark:text-gray-200 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-[#252525]">
          Nội dung khóa học
        </div>
        <div class="flex-1 overflow-y-auto p-0">
          <div v-for="(item, idx) in siblingLessons" :key="item.id"
            class="flex items-center gap-3 p-4 border-b border-gray-100 dark:border-gray-800 cursor-pointer transition-all hover:bg-gray-50 dark:hover:bg-[#262626] group"
            :class="{
              'bg-blue-50 dark:bg-blue-900/10 border-l-4 border-l-blue-600': item.id === lesson?.id,
              'opacity-60': !item.isUnlocked && item.id !== lesson?.id
            }" @click="switchLesson(item)">
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center text-xs shrink-0 transition-colors font-bold shadow-sm border border-gray-200 dark:border-gray-700"
              :class="item.isCompleted ? 'bg-green-100 text-green-600 border-green-200' : (item.id === lesson?.id ? 'bg-blue-200 text-blue-700 border-blue-300' : 'bg-white dark:bg-[#333] text-gray-500')">
              <el-icon v-if="item.isCompleted">
                <Check />
              </el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <div
                class="text-sm font-medium line-clamp-1 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors"
                :class="item.id === lesson?.id ? 'text-blue-700 dark:text-blue-300' : 'text-gray-700 dark:text-gray-300'">
                {{ item.title }}
              </div>
              <div class="text-[10px] text-gray-400 uppercase font-bold mt-0.5 tracking-wider">{{ item.lessonType ===
                'THEORY' ? 'Lý thuyết' : 'Bài tập' }}</div>
            </div>
            <el-icon v-if="!item.isUnlocked" :size="16" class="text-gray-400">
              <Lock />
            </el-icon>
          </div>
        </div>
      </aside>

    </div>

    <el-drawer v-model="mobileDrawerOpen" title="Mục lục bài học" direction="rtl" size="85%" class="!rounded-l-2xl">
      <div class="flex flex-col">
        <div v-for="(item, idx) in siblingLessons" :key="item.id"
          class="flex items-center gap-3 p-4 border-b border-gray-100 dark:border-gray-800 cursor-pointer"
          :class="item.id === lesson?.id ? 'bg-blue-50 dark:bg-blue-900/20' : ''"
          @click="switchLesson(item); mobileDrawerOpen = false">
          <div class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center font-bold text-xs"
            :class="{ 'text-green-600 bg-green-100': item.isCompleted }">
            <el-icon v-if="item.isCompleted">
              <Check />
            </el-icon>
            <span v-else>{{ idx + 1 }}</span>
          </div>
          <div class="flex-1">
            <div class="font-medium text-sm">{{ item.title }}</div>
            <div class="text-xs text-gray-500">{{ item.lessonType }}</div>
          </div>
          <el-icon v-if="!item.isUnlocked">
            <Lock />
          </el-icon>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="isReviewMode" title="Kết quả chi tiết" size="100%" class="!p-0">
      <div class="h-full overflow-y-auto bg-gray-50 dark:bg-[#0f0f0f] p-4">
        <div class="max-w-3xl mx-auto space-y-6">
          <div v-for="(q, idx) in resultData?.questionResults" :key="idx"
            class="bg-white dark:bg-[#1d1d1d] p-6 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
            <div class="flex gap-3 mb-3 items-center">
              <span class="px-2 py-1 rounded text-xs font-bold text-white uppercase"
                :class="q.isCorrect ? 'bg-green-500' : 'bg-red-500'">
                {{ q.isCorrect ? 'Đúng' : 'Sai' }}
              </span>
              <span class="font-bold text-gray-700 dark:text-gray-200">Câu {{ idx + 1 }}</span>
            </div>
            <div class="ql-editor !p-0 mb-4 text-gray-800 dark:text-gray-200" v-html="q.questionText"></div>
            <div class="bg-gray-50 dark:bg-[#262626] p-4 rounded-lg text-sm border border-gray-100 dark:border-[#333]">
              <div class="mb-2">Bạn chọn: <span
                  :class="q.isCorrect ? 'text-green-600 font-bold' : 'text-red-600 font-bold'">{{
                    formatAnswer(q.userAnswer) }}</span></div>
              <div v-if="!q.isCorrect" class="text-green-600 font-bold">Đáp án đúng: {{ formatAnswer(q.correctAnswer) }}
              </div>
              <div v-if="q.explanation"
                class="mt-3 pt-3 border-t border-gray-200 dark:border-[#444] text-gray-500 italic">💡 {{ q.explanation
                }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
// Script giữ nguyên logic
import { ref, onMounted, onUnmounted, defineAsyncComponent, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { grammarUserAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading, Check, Lock, Trophy, Warning, Menu, Expand, Fold, Right } from '@element-plus/icons-vue'
import 'quill/dist/quill.snow.css'

// ... (Imports Component câu hỏi) ...
const QuestionMultipleChoice = defineAsyncComponent(() => import('@/components/user/questions/QuestionMultipleChoice.vue'))
const QuestionFillBlank = defineAsyncComponent(() => import('@/components/user/questions/QuestionFillBlank.vue'))
const QuestionMatching = defineAsyncComponent(() => import('@/components/user/questions/QuestionMatching.vue'))
const QuestionSentenceBuilding = defineAsyncComponent(() => import('@/components/user/questions/QuestionSentenceBuilding.vue'))
const QuestionPronunciation = defineAsyncComponent(() => import('@/components/user/questions/QuestionPronunciation.vue'))
const QuestionErrorCorrection = defineAsyncComponent(() => import('@/components/user/questions/QuestionErrorCorrection.vue'))
const QuestionOpenEnded = defineAsyncComponent(() => import('@/components/user/questions/QuestionOpenEnded.vue'))

const route = useRoute(); const router = useRouter()
const lesson = ref(null); const siblingLessons = ref([]); const loading = ref(true); const submitting = ref(false)
const readingTime = ref(0); const countdown = ref(0); let timerInterval = null
const userAnswers = ref({}); const isSubmitted = ref(false); const resultData = ref(null); const isReviewMode = ref(false)
const isSidebarOpen = ref(true); const mobileDrawerOpen = ref(false)

const canFinishTheory = computed(() => countdown.value === 0)
const completedCount = computed(() => siblingLessons.value.filter(l => l.isCompleted).length)
const topicCompletionRate = computed(() => siblingLessons.value.length ? Math.round((completedCount.value / siblingLessons.value.length) * 100) : 0)

const formatDuration = (s) => { const m = Math.floor(s / 60); return m > 0 ? `${m} phút` : `${s} giây` }
const formatAnswer = (ans) => typeof ans === 'object' ? JSON.stringify(ans) : (ans || '(Trống)')

const getQuestionComponent = (type) => {
  const t = type?.toUpperCase()
  if (['MULTIPLE_CHOICE', 'TRUE_FALSE', 'COMPLETE_CONVERSATION'].includes(t)) return QuestionMultipleChoice
  if (['FILL_BLANK', 'VERB_FORM', 'TEXT_ANSWER'].includes(t)) return QuestionFillBlank
  if (['MATCHING'].includes(t)) return QuestionMatching
  if (['SENTENCE_BUILDING', 'SENTENCE_TRANSFORMATION'].includes(t)) return QuestionSentenceBuilding
  if (['PRONUNCIATION'].includes(t)) return QuestionPronunciation
  if (['ERROR_CORRECTION'].includes(t)) return QuestionErrorCorrection
  if (['OPEN_ENDED'].includes(t)) return QuestionOpenEnded
  return QuestionMultipleChoice
}

const loadLesson = async (lessonId) => {
  loading.value = true; isSubmitted.value = false; userAnswers.value = {}; clearInterval(timerInterval)
  try {
    const res = await grammarUserAPI.getLessonContent(lessonId)
    lesson.value = res.data.data
    if (lesson.value?.topicId) {
      const topicRes = await grammarUserAPI.getTopicDetails(lesson.value.topicId)
      siblingLessons.value = topicRes.data.data.lessons || []
    }
    if (lesson.value?.lessonType === 'THEORY') {
      if (lesson.value.isCompleted) countdown.value = 0
      else startTimer(lesson.value.estimatedDuration || 30)
    }
  } catch (e) { console.error(e); ElMessage.error('Lỗi tải bài học') } finally { loading.value = false }
}

const startTimer = (d) => { countdown.value = d; timerInterval = setInterval(() => { readingTime.value++; if (countdown.value > 0) countdown.value-- }, 1000) }

const finishTheory = async () => {
  if (lesson.value.isCompleted) { autoNextLesson(); return }
  try {
    const res = await grammarUserAPI.submitLesson({ lessonId: lesson.value.id, readingTimeSecond: readingTime.value })
    ElMessage.success('Hoàn thành!')
    if (res.data.data.hasUnlockedNext && res.data.data.nextLessonId) goToNextLesson(res.data.data.nextLessonId)
    else goBackToList()
  } catch (e) { ElMessage.error(e.response?.data?.message || 'Lỗi') }
}

const autoNextLesson = () => {
  const currentIndex = siblingLessons.value.findIndex(l => l.id === lesson.value.id)
  if (currentIndex !== -1 && currentIndex < siblingLessons.value.length - 1) {
    const nextItem = siblingLessons.value[currentIndex + 1]
    if (nextItem.isUnlocked) switchLesson(nextItem)
    else ElMessage.warning('Bài tiếp theo chưa mở khóa')
  } else { ElMessage.info('Đã hoàn thành chủ đề!'); goBackToList() }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = Object.entries(userAnswers.value).map(([id, ans]) => ({ questionId: parseInt(id), answer: ans }))
    const res = await grammarUserAPI.submitLesson({ lessonId: lesson.value.id, answers: payload })
    resultData.value = res.data.data; isSubmitted.value = true
    if (lesson.value.topicId) {
      const topicRes = await grammarUserAPI.getTopicDetails(lesson.value.topicId); siblingLessons.value = topicRes.data.data.lessons
    }
    document.querySelector('.content-area').scrollTop = 0
  } catch (e) { ElMessage.error(e.response?.data?.message || 'Lỗi nộp bài') } finally { submitting.value = false }
}

const retryLesson = () => { isSubmitted.value = false; userAnswers.value = {}; resultData.value = null; document.querySelector('.content-area').scrollTop = 0 }
const reviewAnswers = () => isReviewMode.value = true
const goBackToList = () => router.push({ name: 'user-grammar' })
const goBackTopic = () => router.push({ name: 'user-grammar' })
const goToNextLesson = (id) => router.push({ name: 'user-grammar-lesson', params: { lessonId: id } })
const switchLesson = (item) => {
  if (item.id === lesson.value.id) return
  if (!item.isUnlocked) { ElMessage.warning('Chưa mở khóa'); return }
  router.push({ name: 'user-grammar-lesson', params: { lessonId: item.id } })
}

watch(() => route.params.lessonId, (id) => { if (id) loadLesson(id) }, { immediate: true })
onUnmounted(() => clearInterval(timerInterval))
</script>
