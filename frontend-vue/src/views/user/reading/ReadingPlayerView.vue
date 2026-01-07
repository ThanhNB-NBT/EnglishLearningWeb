<template>
  <LearningSplitLayout mode="split">
    <template #header-left>
      <el-button link :icon="ArrowLeft" @click="$router.back()">Thoát</el-button>
      <h1 class="text-sm md:text-base font-bold truncate max-w-[150px] md:max-w-md ml-2">
        {{ currentLesson?.title }}
      </h1>
    </template>

    <template #header-center>
      <div
        v-if="remainingTime > 0"
        class="flex items-center gap-2 text-orange-600 font-mono font-bold text-lg bg-orange-50 px-3 py-1 rounded-md"
      >
        <el-icon><Timer /></el-icon>
        {{ formatTime(remainingTime) }}
      </div>
    </template>

    <template #sidebar>
      <div class="p-4">
        <h3 class="font-bold text-gray-500 uppercase text-xs mb-3 px-2">Danh sách bài đọc</h3>
        <ul class="space-y-1">
          <li v-for="lesson in topicLessons" :key="lesson.id">
            <button
              class="w-full text-left px-3 py-2 rounded-lg text-sm transition-colors flex items-center justify-between group"
              :class="
                lesson.id === currentLesson?.id
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'hover:bg-gray-100 text-gray-700 dark:text-gray-300 dark:hover:bg-gray-800'
              "
              @click="switchLesson(lesson.id)"
            >
              <span class="truncate">{{ lesson.title }}</span>
              <el-icon v-if="lesson.isCompleted" class="text-green-500"><CircleCheck /></el-icon>
            </button>
          </li>
        </ul>
      </div>
    </template>

    <template #content-left>
      <div class="prose dark:prose-invert max-w-none">
        <img
          v-if="currentLesson?.imageUrl"
          :src="currentLesson.imageUrl"
          class="rounded-lg mb-4 w-full object-cover max-h-60"
          alt="Lesson Image"
        />

        <div
          v-html="currentLesson?.content"
          class="reading-content font-serif text-lg leading-relaxed"
        ></div>
      </div>
    </template>

    <template #content-right>
      <div class="space-y-8">
        <div
          v-for="(question, index) in questions"
          :key="question.id"
          class="border-b border-gray-100 dark:border-gray-800 pb-6 last:border-0"
        >
          <div class="flex gap-3 mb-3">
            <span
              class="bg-blue-100 text-blue-800 text-xs font-bold px-2 py-1 rounded h-fit shrink-0"
              >Câu {{ index + 1 }}</span
            >
            <div
              class="font-medium text-gray-800 dark:text-gray-200"
              v-html="question.questionText"
            ></div>
          </div>

          <QuestionRenderer
            :question="question"
            v-model="userAnswers[question.id]"
            :disabled="showResult"
          />
        </div>
      </div>
    </template>

    <template #footer>
      <div class="w-full flex justify-between items-center">
        <div class="text-sm">
          Đã làm: <span class="font-bold">{{ answeredCount }}/{{ questions.length }}</span>
        </div>

        <div class="flex gap-3">
          <template v-if="showResult">
            <div class="flex items-center gap-2 mr-2">
              <span
                class="font-bold text-lg"
                :class="resultData.isPassed ? 'text-green-600' : 'text-red-600'"
              >
                {{ resultData.scorePercentage }}%
              </span>
              <span class="text-sm text-gray-500"
                >({{ resultData.isPassed ? 'ĐẠT' : 'CHƯA ĐẠT' }})</span
              >
            </div>
            <el-button v-if="resultData.hasUnlockedNext" type="success" @click="nextLesson">
              Bài tiếp theo <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
            <el-button @click="retryLesson">Làm lại</el-button>
          </template>

          <el-button
            v-else
            type="primary"
            size="large"
            :loading="submitting"
            @click="submitExam"
            :disabled="answeredCount === 0"
          >
            Nộp bài
          </el-button>
        </div>
      </div>
    </template>
  </LearningSplitLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReadingUserStore } from '@/stores/user/readingUser'
import LearningSplitLayout from '@/layouts/LearningSplitLayout.vue'
import QuestionRenderer from '@/components/user/questions/QuestionRenderer.vue'
import { ArrowLeft, Timer, ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage} from 'element-plus'

const route = useRoute()
const router = useRouter()
const readingStore = useReadingUserStore()

// State
const remainingTime = ref(0)
const timerInterval = ref(null)
const userAnswers = ref({})
const submitting = ref(false)
const showResult = ref(false)
const resultData = ref({})
const topicLessons = ref([])

// Computed
const currentLesson = computed(() => readingStore.currentLesson)
const questions = computed(() => readingStore.questions || [])
const answeredCount = computed(() => Object.keys(userAnswers.value).length)

// === INIT ===
onMounted(() => {
  loadData(route.params.lessonId)
})

watch(
  () => route.params.lessonId,
  (newId) => {
    if (newId) loadData(newId)
  },
)

const loadData = async (lessonId) => {
  showResult.value = false
  userAnswers.value = {}
  clearInterval(timerInterval.value)

  await readingStore.fetchLessonDetail(lessonId)

  // Load sidebar list (nếu cần)
  if (currentLesson.value?.topicId) {
    // Gọi API lấy list bài học của Topic này để hiển thị Sidebar
    // topicLessons.value = await readingStore.getLessonsByTopic(currentLesson.value.topicId)
  }

  if (currentLesson.value?.timeLimitSeconds) {
    remainingTime.value = currentLesson.value.timeLimitSeconds
    startTimer()
  }
}

// === TIMER & ACTIONS (Giống Grammar) ===
const startTimer = () => {
  clearInterval(timerInterval.value)
  timerInterval.value = setInterval(() => {
    if (remainingTime.value > 0) remainingTime.value--
    else {
      clearInterval(timerInterval.value)
      if (!showResult.value) {
        ElMessage.warning('Hết giờ! Tự động nộp bài.')
        submitExam()
      }
    }
  }, 1000)
}

const submitExam = async () => {
  if (answeredCount.value === 0 && !showResult.value) return
  submitting.value = true
  try {
    const answers = Object.entries(userAnswers.value).map(([qId, ans]) => ({
      questionId: parseInt(qId),
      textAnswer: typeof ans === 'string' ? ans : JSON.stringify(ans),
    }))

    const res = await readingStore.submitLesson(currentLesson.value.id, { answers })
    resultData.value = res
    showResult.value = true
    clearInterval(timerInterval.value)
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const switchLesson = (id) => router.push({ name: 'user-reading-detail', params: { lessonId: id } })
const nextLesson = () =>
  resultData.value.nextLessonId && switchLesson(resultData.value.nextLessonId)
const retryLesson = () => loadData(currentLesson.value.id)

const formatTime = (s) => {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2, '0')}`
}

onUnmounted(() => clearInterval(timerInterval.value))
</script>
