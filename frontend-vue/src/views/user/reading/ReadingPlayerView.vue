<template>
  <LearningSplitLayout mode="split" v-if="!isLoading" :key="`lesson-${currentLesson?.id}-${retryCount}`">
    <template #header-left>
      <el-button link :icon="ArrowLeft" @click="$router.push('/user/reading')">Thoát</el-button>
      <div class="ml-2 hidden sm:block">
        <h1 class="text-sm font-bold truncate max-w-[200px]">{{ currentLesson?.title }}</h1>
      </div>
    </template>

    <template #header-center>
      <div
        v-if="shouldShowTimer"
        class="flex items-center gap-2 text-green-600 bg-green-50 px-3 py-1 rounded-md font-mono font-bold text-lg"
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
      <!-- Reading Passage -->
      <div class="prose dark:prose-invert prose-sm max-w-none">
        <h3 class="font-bold text-gray-800 dark:text-gray-100 mb-4">📖 Bài đọc</h3>
        <div
          v-if="currentLesson?.content"
          class="p-4 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 leading-relaxed"
          v-html="currentLesson.content"
        ></div>
        <div
          v-else
          class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border border-dashed text-gray-400 text-center"
        >
          Không có nội dung bài đọc
        </div>
      </div>
    </template>

    <template #content-right>
      <div class="pb-10">
        <LevelUpgradeAlert
          v-if="player.showResult.value && lastResult?.levelUpgradeResult"
          :level-result="lastResult.levelUpgradeResult"
        />

        <div class="mb-6 flex items-center justify-between border-b pb-4">
          <h2 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Câu hỏi</h2>
          <div
            class="text-sm bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-400 px-3 py-1 rounded-full font-bold"
          >
            {{ player.answeredCount.value }}/{{ totalQuestions }} câu
          </div>
        </div>

        <div v-if="hasQuestions">
          <TaskGroupRenderer
            v-for="(task, index) in groupedTasks"
            :key="'task-' + task.taskGroupId + '-' + index + '-' + retryCount"
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
                :key="'standalone-' + q.id + '-' + retryCount"
                class="flex gap-4"
              >
                <div class="shrink-0 pt-0.5">
                  <span
                    class="text-lg font-bold font-mono"
                    :class="player.userAnswers.value[q.id] ? 'text-green-600' : 'text-gray-400'"
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
                    :key="`q-${q.id}-${retryCount}`"
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
          <p class="text-gray-500 dark:text-gray-400">Bài đọc này không có câu hỏi.</p>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="w-full flex justify-between items-center">
        <div class="text-sm hidden sm:block"></div>
        <div class="flex gap-3 w-full sm:w-auto justify-end">
          <template v-if="player.showResult.value">
            <!-- Đã nộp bài - Hiển thị kết quả -->
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

          <template v-else>
            <!-- Đang làm bài - Hiển thị nút Nộp bài -->
            <el-button
              type="primary"
              size="large"
              :loading="player.submitting.value"
              @click="handleSubmit"
              :disabled="player.answeredCount.value === 0"
            >
              Nộp bài
            </el-button>
          </template>
        </div>
      </div>
    </template>
  </LearningSplitLayout>

  <div v-else class="h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
    <div class="text-center">
      <el-icon class="is-loading text-green-500" :size="48"><Loading /></el-icon>
      <p class="mt-4 text-gray-500 dark:text-gray-400">Đang tải bài đọc...</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReadingUserStore } from '@/stores/user/readingUser'
import { useLearningPlayer } from '@/composables/common/useLearningPlayer'
import LearningSplitLayout from '@/layouts/LearningSplitLayout.vue'
import LessonSidebar from '@/components/user/shared/LessonSidebar.vue'
import QuestionRenderer from '@/components/user/questions/QuestionRenderer.vue'
import TaskGroupRenderer from '@/components/user/questions/TaskGroupRenderer.vue'
import LevelUpgradeAlert from '@/components/user/shared/LevelUpgradeAlert.vue'
import { ArrowLeft, Timer, ArrowRight, Loading } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const readingStore = useReadingUserStore()
const player = useLearningPlayer(readingStore)

const isLoading = ref(true)
const retryCount = ref(0) // ✅ Force remount counter

const currentLesson = computed(() => readingStore.currentLesson)
const topicLessons = computed(() => readingStore.currentTopicLessons || [])
const groupedTasks = computed(() => currentLesson.value?.groupedQuestions?.tasks || [])
const standaloneQuestions = computed(
  () => currentLesson.value?.groupedQuestions?.standaloneQuestions || [],
)
const lastResult = computed(() => readingStore.lastSubmitResult)
const nextLessonId = computed(() => readingStore.lastSubmitResult?.nextLessonId)

const totalQuestions = computed(() => {
  let count = standaloneQuestions.value.length
  groupedTasks.value.forEach((t) => (count += t.questions?.length || 0))
  return count
})

const hasQuestions = computed(() => totalQuestions.value > 0)

const shouldShowTimer = computed(() => {
  if (player.showResult.value) return false
  if (player.remainingTime.value <= 0) return false
  return true
})

onMounted(() => {
  if (route.params.lessonId) loadData(route.params.lessonId)
})

watch(
  () => route.params.lessonId,
  (newId) => {
    if (newId) {
      retryCount.value = 0
      loadData(newId)
    }
  },
)

const loadData = async (lessonId) => {
  isLoading.value = true
  player.resetPlayerState()
  readingStore.clearCurrentLesson()

  try {
    await readingStore.fetchLessonDetail(lessonId)

    if (currentLesson.value?.topicId) {
      await readingStore.fetchLessonsByTopic(currentLesson.value.topicId)
    }

    await nextTick()

    // ✅ Luôn set timer từ timeLimitSeconds
    player.remainingTime.value = currentLesson.value?.timeLimitSeconds || 300

    // Chỉ start timer nếu chưa xem kết quả
    if (!player.showResult.value) {
      player.startTimer(() => handleSubmit())
    }

    isLoading.value = false
  } catch (error) {
    console.error('Load data error:', error)
    isLoading.value = false
  }
}

const handleSubmit = async () => {
  await player.submitExam(currentLesson.value.id, groupedTasks.value, standaloneQuestions.value)
  if (currentLesson.value?.topicId) {
    readingStore.fetchLessonsByTopic(currentLesson.value.topicId)
  }
}

const switchLesson = (id) => {
  isLoading.value = true
  retryCount.value = 0
  router.push({ name: 'user-reading-lesson', params: { lessonId: id } })
}

const goToNextLesson = () => {
  if (nextLessonId.value) switchLesson(nextLessonId.value)
}

// ✅ FIXED: Retry function
const retryLesson = () => {
  console.log('🔄 Retrying lesson...')

  // 1. Increment retry counter để force remount
  retryCount.value++

  // 2. Reset player state
  player.showResult.value = false
  player.submitting.value = false
  player.userAnswers.value = {}

  // 3. Clear questions state
  player.clearQuestionsState(groupedTasks.value, standaloneQuestions.value)

  // 4. Reload data
  loadData(currentLesson.value.id)
}

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
