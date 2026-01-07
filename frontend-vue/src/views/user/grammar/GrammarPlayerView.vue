{ type: uploaded file fileName: src/views/user/grammar/GrammarPlayerView.vue fullContent:
<template>
  <LearningSplitLayout :mode="layoutMode" v-if="!isLoading" :key="`lesson-${currentLesson?.id}`">
    <template #header-left>
      <el-button link :icon="ArrowLeft" @click="$router.push('/user/grammar')">Thoát</el-button>
      <div class="ml-2 hidden sm:block">
        <h1 class="text-sm font-bold truncate max-w-[200px]">{{ currentLesson?.title }}</h1>
      </div>
    </template>

    <template #header-center>
      <div
        v-if="shouldShowTimer"
        class="flex items-center gap-2 text-blue-600 bg-blue-50 px-3 py-1 rounded-md font-mono font-bold text-lg"
      >
        <el-icon><Timer /></el-icon>
        <span>{{ player.formatTime(player.remainingTime.value) }}</span>
      </div>
    </template>

    <template #sidebar>
      <LessonSidebar
        :lessons="topicLessons"
        :current-lesson-id="currentLesson?.id"
        @select-lesson="switchLesson"
      />
    </template>

    <template #content-left>
      <div v-if="currentPhase === 'theory'" class="prose dark:prose-invert prose-sm max-w-none">
        <h3 class="font-bold text-gray-800 dark:text-gray-100 mb-4">📖 Lý thuyết bài học</h3>
        <div v-html="currentLesson?.content"></div>
      </div>
    </template>

    <template #content-right>
      <div v-if="currentPhase === 'theory' && hasQuestions" class="pb-10">
        <div class="mb-4 pb-2 border-b border-gray-200 dark:border-gray-700">
          <span class="text-sm font-bold text-gray-500 dark:text-gray-400 uppercase"
            >Preview: Bài tập</span
          >
        </div>
        <TaskGroupRenderer
          v-for="(task, index) in groupedTasks"
          :key="'preview-' + task.taskGroupId + '-' + index"
          :task="task"
          :answers="{}"
          :label="'Task ' + (index + 1)"
          :start-index="getStartIndex(index)"
          :disabled="true"
          :show-feedback="false"
          @update-answer="() => {}"
        />
      </div>
    </template>

    <template #content-full>
      <div v-if="currentPhase === 'theory'" class="pb-32">
        <h2 class="text-3xl font-bold mb-6 text-gray-800 dark:text-white">
          {{ currentLesson?.title }}
        </h2>
        <div
          class="prose dark:prose-invert lg:prose-xl max-w-none"
          v-html="currentLesson?.content"
        ></div>

        <div
          id="end-of-theory-marker"
          class="h-10 mt-10 flex items-center justify-center text-gray-400 text-sm italic"
        >
          --- Cuộn xuống hết để hoàn thành ---
        </div>
      </div>

      <div v-else class="pb-10">
        <div class="mb-6 flex items-center justify-between border-b pb-4">
          <h2 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Bài tập thực hành</h2>
          <div
            class="text-sm bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 px-3 py-1 rounded-full font-bold"
          >
            {{ player.answeredCount.value }}/{{ totalQuestions }} câu
          </div>
        </div>

        <div v-if="hasQuestions">
          <TaskGroupRenderer
            v-for="(task, index) in groupedTasks"
            :key="'task-' + task.taskGroupId + '-' + index"
            :task="task"
            :answers="player.userAnswers.value"
            :label="'Task ' + (index + 1)"
            :start-index="getStartIndex(index)"
            :disabled="player.showResult.value"
            :show-feedback="player.showResult.value"
            @update-answer="player.handleAnswerUpdate"
          />

          <div v-if="standaloneQuestions.length > 0" class="mt-8">
            <div
              v-if="groupedTasks.length > 0"
              class="mb-4 pb-2 border-b border-gray-200 dark:border-gray-700"
            >
              <span class="text-xs font-bold text-gray-400 uppercase">Câu hỏi khác</span>
            </div>
            <div class="space-y-8">
              <div
                v-for="(q, idx) in standaloneQuestions"
                :key="'standalone-' + q.id"
                class="flex gap-4"
              >
                <div class="shrink-0 pt-0.5">
                  <span
                    class="text-lg font-bold font-mono"
                    :class="player.userAnswers.value[q.id] ? 'text-blue-600' : 'text-gray-400'"
                  >
                    {{ getStandaloneStartIndex() + idx }}.
                  </span>
                </div>
                <div class="flex-1">
                  <div
                    class="mb-3 text-gray-900 dark:text-gray-100 font-medium text-lg"
                    v-html="q.questionText"
                  ></div>
                  <QuestionRenderer
                    :question="q"
                    :model-value="player.userAnswers.value[q.id] || null"
                    @update:model-value="
                      (val) => player.handleAnswerUpdate({ questionId: q.id, value: val })
                    "
                    :disabled="player.showResult.value"
                    :show-feedback="player.showResult.value"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          v-else
          class="text-center py-20 bg-gray-50 dark:bg-gray-800 rounded-xl border border-dashed"
        >
          <p class="text-gray-500 dark:text-gray-400">Bài học này không có câu hỏi thực hành.</p>
        </div>
      </div>
    </template>

    <template #footer>
      <div v-if="currentPhase === 'theory'" class="w-full flex justify-between items-center">
        <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <template v-if="!canStartPractice">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>Vui lòng đọc bài & đợi {{ player.remainingTime.value }}s...</span>
          </template>
          <template v-else>
            <el-icon class="text-green-500 text-lg"><CircleCheckFilled /></el-icon>
            <span class="text-green-600 dark:text-green-400 font-bold"
              >Đã hoàn thành lý thuyết!</span
            >
          </template>
        </div>

        <el-button
          type="primary"
          size="large"
          :disabled="!canStartPractice"
          @click="handleTheoryAction"
          :loading="player.submitting.value"
        >
          {{ hasQuestions ? 'Làm bài tập' : 'Hoàn thành bài học' }}
          <el-icon class="ml-2"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-else class="w-full flex justify-between items-center">
        <div class="text-sm hidden sm:block"></div>
        <div class="flex gap-3 w-full sm:w-auto justify-end">
          <template v-if="player.showResult.value || isLessonCompleted">
            <div
              v-if="lastResult"
              class="flex items-center gap-2 bg-gray-100 dark:bg-gray-700 px-4 py-2 rounded-lg mr-2"
            >
              <span
                class="font-bold text-xl"
                :class="lastResult.isPassed ? 'text-green-600' : 'text-red-500'"
              >
                {{ lastResult.scorePercentage }}%
              </span>
            </div>
            <el-button v-if="nextLessonId" type="success" size="large" @click="goToNextLesson">
              Bài tiếp <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" @click="retryLesson">Làm lại</el-button>
          </template>
          <el-button
            v-else
            type="primary"
            size="large"
            :loading="player.submitting.value"
            @click="handleSubmit"
            :disabled="player.answeredCount.value === 0"
          >
            Nộp bài
          </el-button>
        </div>
      </div>
    </template>
  </LearningSplitLayout>

  <div v-else class="h-screen flex items-center justify-center bg-gray-50 dark:bg-[#141414]">
    <div class="text-center">
      <el-icon class="is-loading text-blue-500" :size="48"><Loading /></el-icon>
      <p class="mt-4 text-gray-500 dark:text-gray-400">Đang tải bài học...</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGrammarUserStore } from '@/stores/user/grammarUser'
import { useLearningPlayer } from '@/composables/common/useLearningPlayer'
import LearningSplitLayout from '@/layouts/LearningSplitLayout.vue'
import LessonSidebar from '@/components/user/shared/LessonSidebar.vue'
import QuestionRenderer from '@/components/user/questions/QuestionRenderer.vue'
import TaskGroupRenderer from '@/components/user/questions/TaskGroupRenderer.vue'
import { ArrowLeft, Timer, ArrowRight, CircleCheckFilled, Loading } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const grammarStore = useGrammarUserStore()
const player = useLearningPlayer(grammarStore)

const isLoading = ref(true)

const currentLesson = computed(() => grammarStore.currentLesson)
const topicLessons = computed(() => grammarStore.currentTopicLessons || [])
const isLessonCompleted = computed(() => currentLesson.value?.isCompleted)
const groupedTasks = computed(() => currentLesson.value?.groupedQuestions?.tasks || [])
const standaloneQuestions = computed(
  () => currentLesson.value?.groupedQuestions?.standaloneQuestions || [],
)
const lastResult = computed(() => grammarStore.lastSubmitResult)
const nextLessonId = computed(() => grammarStore.lastSubmitResult?.nextLessonId)

const totalQuestions = computed(() => {
  let count = standaloneQuestions.value.length
  groupedTasks.value.forEach((t) => (count += t.questions?.length || 0))
  return count
})

const hasQuestions = computed(() => totalQuestions.value > 0)
const currentPhase = ref('theory')

// Logic hiển thị Layout:
// Nếu là Theory + Có câu hỏi -> Split (chia đôi màn hình)
// Nếu là Theory + Không câu hỏi -> Full (chỉ hiện lý thuyết)
// Nếu là Practice -> Full (chỉ hiện bài tập)
const layoutMode = computed(() => {
  if (currentPhase.value === 'theory') {
    return hasQuestions.value ? 'split' : 'full'
  }
  return 'full'
})

const shouldShowTimer = computed(() => {
  if (player.showResult.value) return false
  if (isLessonCompleted.value && currentPhase.value === 'practice') return false
  if (player.remainingTime.value <= 0) return false
  return true
})

const canStartPractice = computed(() => {
  if (isLessonCompleted.value) return true
  // Nếu không set thời gian đọc lý thuyết thì cho qua luôn
  if (!currentLesson.value?.theoryDurationSeconds) return true

  // Logic: Hết thời gian VÀ đã cuộn xuống cuối (hoặc bài ngắn tự coi là đã cuộn)
  return player.remainingTime.value <= 0 && player.hasScrolledToBottom.value
})

onMounted(() => {
  if (route.params.lessonId) loadData(route.params.lessonId)
})

watch(
  () => route.params.lessonId,
  (newId) => {
    if (newId) loadData(newId)
  },
)

const loadData = async (lessonId) => {
  isLoading.value = true
  player.resetPlayerState()
  currentPhase.value = 'theory'
  grammarStore.clearCurrentLesson()

  try {
    await grammarStore.fetchLessonDetail(lessonId)

    if (currentLesson.value?.topicId) {
      await grammarStore.fetchLessonsByTopic(currentLesson.value.topicId)
    }

    const hasTheoryContent =
      !!currentLesson.value?.content && currentLesson.value.content.length > 0

    await nextTick()

    // Logic: Nếu không có lý thuyết -> Vào thẳng bài tập
    if (!hasTheoryContent) {
      currentPhase.value = 'practice'
      player.remainingTime.value = 0
      if (!isLessonCompleted.value && hasQuestions.value) {
        player.remainingTime.value = currentLesson.value?.timeLimitSeconds || 300
        player.startTimer(() => handleSubmit())
      }
      player.hasScrolledToBottom.value = true
    } else {
      // Có lý thuyết -> Ở lại 'theory'
      currentPhase.value = 'theory'

      // Nếu đã hoàn thành rồi thì không cần timer lý thuyết
      if (isLessonCompleted.value) {
        player.remainingTime.value = 0
        player.hasScrolledToBottom.value = true
      } else {
        player.remainingTime.value = currentLesson.value?.theoryDurationSeconds || 10
        player.startTimer(null)

        // Setup observer
        setTimeout(() => {
          player.setupScrollObserver('end-of-theory-marker')
        }, 100)
      }
    }

    isLoading.value = false
  } catch (error) {
    console.error('Load data error:', error)
    isLoading.value = false
  }
}

// Hàm điều hướng chính ở Footer
const handleTheoryAction = async () => {
  if (hasQuestions.value) {
    // Nếu có câu hỏi -> Chuyển sang làm bài tập
    startPractice()
  } else {
    // Nếu KHÔNG có câu hỏi (Bài Lý thuyết thuần túy) -> Nộp bài để hoàn thành
    await submitPureTheory()
  }
}

const startPractice = async () => {
  await nextTick()
  currentPhase.value = 'practice'

  if (isLessonCompleted.value) {
    player.remainingTime.value = 0
  } else {
    player.remainingTime.value = currentLesson.value?.timeLimitSeconds || 300
    player.startTimer(() => handleSubmit())
  }
}

// Xử lý nộp bài lý thuyết (không có câu hỏi)
const submitPureTheory = async () => {
  try {
    // 1. Gọi API nộp bài (dữ liệu rỗng vì là lý thuyết)
    await player.submitExam(currentLesson.value.id, [], [])

    // 2. Lấy kết quả từ store sau khi submit
    const result = grammarStore.lastSubmitResult

    // 3. Xử lý điều hướng
    if (result && result.isPassed) {
      if (result.nextLessonId) {
        // CASE A: Có bài tiếp theo -> Chuyển sang bài đó
        isLoading.value = true
        router.push({
          name: 'user-grammar-lesson',
          params: { lessonId: result.nextLessonId },
        })
      } else {
        // CASE B: Hết bài (hoặc bài cuối topic) -> Về danh sách topic
        // Dùng path trực tiếp để tránh lỗi sai tên route
        router.push('/user/grammar')
      }
    } else {
      // Fallback: Nếu API không trả về passed (hiếm gặp với theory) -> Về danh sách
      router.push('/user/grammar')
    }
  } catch (e) {
    console.error('Lỗi khi hoàn thành bài lý thuyết:', e)
    // Nếu lỗi, vẫn cho về danh sách để user không bị kẹt
    router.push('/user/grammar')
  }
}

const handleSubmit = async () => {
  await player.submitExam(currentLesson.value.id, groupedTasks.value, standaloneQuestions.value)
  if (currentLesson.value?.topicId) {
    grammarStore.fetchLessonsByTopic(currentLesson.value.topicId)
  }
}

const switchLesson = (id) => {
  isLoading.value = true
  router.push({ name: 'user-grammar-lesson', params: { lessonId: id } })
}

const goToNextLesson = () => {
  if (nextLessonId.value) switchLesson(nextLessonId.value)
}

const retryLesson = () => loadData(currentLesson.value.id)

const getStartIndex = (taskIndex) => {
  let count = 1
  for (let i = 0; i < taskIndex; i++) count += groupedTasks.value[i].questions?.length || 0
  return count
}

const getStandaloneStartIndex = () => {
  let count = 1
  groupedTasks.value.forEach((t) => (count += t.questions?.length || 0))
  return count
}
</script>
}
