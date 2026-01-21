<!-- ========================================
     ListeningPlayerView.vue - SIMPLE FIX
     ======================================== -->
<template>
  <LearningSplitLayout
    mode="split"
    v-if="!isLoading"
    :key="`lesson-${currentLesson?.id}-${retryCount}`"
  >
    <template #header-left>
      <el-button link :icon="ArrowLeft" @click="$router.push('/user/listening')">Thoát</el-button>
      <div class="ml-2 hidden sm:block">
        <h1 class="text-sm font-bold truncate max-w-[200px]">{{ currentLesson?.title }}</h1>
      </div>
    </template>

    <template #header-center>
      <div
        v-if="shouldShowTimer"
        class="flex items-center gap-2 text-orange-600 bg-orange-50 px-3 py-1 rounded-md font-mono font-bold text-lg"
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
      <AudioPlayer
        v-if="currentLesson?.audioUrl"
        ref="audioPlayerRef"
        :key="`audio-${retryCount}`"
        :audio-url="currentLesson.audioUrl"
        :play-count="currentLesson.playCount || 0"
        @play="handleAudioPlay"
        @error="handleAudioError"
      />

      <div class="flex-1 mt-4">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
            <el-icon><Document /></el-icon>
            Transcript
          </h3>

          <!-- ✅ Hiển thị điều kiện unlock rõ ràng -->
          <el-button
            v-if="!currentLesson?.transcriptUnlocked"
            size="small"
            type="primary"
            plain
            :loading="unlockingTranscript"
            :disabled="!canUnlockTranscript"
            @click="unlockTranscript"
          >
            <el-icon class="mr-1"><Lock /></el-icon>
            {{ canUnlockTranscript ? 'Mở khóa' : 'Chưa đủ điều kiện' }}
          </el-button>
          <el-tag v-else type="success" size="small">
            <el-icon><CircleCheck /></el-icon> Đã mở
          </el-tag>
        </div>

        <!-- ✅ UNLOCKED: Hiển thị cả transcript và translation -->
        <div
          v-if="currentLesson?.transcriptUnlocked && currentLesson?.transcript"
          class="space-y-4"
        >
          <!-- Original Transcript -->
          <div class="transcript-card">
            <div class="transcript-label">
              <el-icon><Document /></el-icon>
              <span>Original Transcript</span>
            </div>
            <div class="transcript-content">
              {{ currentLesson.transcript }}
            </div>
          </div>

          <!-- Vietnamese Translation -->
          <div v-if="currentLesson.transcriptTranslation" class="transcript-card translation">
            <div class="transcript-label">
              <el-icon><Document /></el-icon>
              <span>Bản dịch tiếng Việt</span>
            </div>
            <div class="transcript-content">
              {{ currentLesson.transcriptTranslation }}
            </div>
          </div>
        </div>

        <!-- ✅ LOCKED: Hiển thị thông tin điều kiện unlock -->
        <div v-else-if="!currentLesson?.transcriptUnlocked" class="transcript-locked">
          <el-icon size="32" class="mb-3 text-gray-400"><Lock /></el-icon>
          <div class="text-center space-y-2">
            <p class="text-sm font-semibold text-gray-600 dark:text-gray-300">
              Transcript đang bị khóa
            </p>
            <div class="text-xs text-gray-500 dark:text-gray-400 space-y-1">
              <p>📌 Để mở khóa, bạn cần:</p>
              <p class="font-semibold text-orange-600 dark:text-orange-400">
                ✅ Hoàn thành bài học (đạt ≥80%)
              </p>
            </div>
            <!-- ✅ Progress indicator -->
            <div class="mt-3 text-xs">
              <div class="flex items-center justify-center gap-2">
                <span>Trạng thái:</span>
                <el-tag size="small" :type="currentLesson?.isCompleted ? 'success' : 'info'">
                  {{ currentLesson?.isCompleted ? 'Đã hoàn thành' : 'Chưa hoàn thành' }}
                </el-tag>
              </div>
              <div v-if="currentLesson?.scorePercentage" class="mt-2 text-gray-500">
                Điểm hiện tại: {{ currentLesson.scorePercentage.toFixed(0) }}%
              </div>
            </div>
          </div>
        </div>

        <!-- Empty state -->
        <div v-else class="transcript-empty">
          <el-icon size="24" class="mb-2 text-gray-400"><WarningFilled /></el-icon>
          <span class="text-sm text-gray-500">Bài học chưa có transcript</span>
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
            class="text-sm bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-400 px-3 py-1 rounded-full font-bold"
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
                    :class="player.userAnswers.value[q.id] ? 'text-orange-600' : 'text-gray-400'"
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
          <p class="text-gray-500 dark:text-gray-400">Bài nghe này không có câu hỏi.</p>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="w-full flex justify-between items-center">
        <div class="text-sm hidden sm:block"></div>
        <div class="flex gap-3 w-full sm:w-auto justify-end">
          <template v-if="player.showResult.value">
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
      <el-icon class="is-loading text-orange-500" :size="48"><Loading /></el-icon>
      <p class="mt-4 text-gray-500 dark:text-gray-400">Đang tải bài nghe...</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useListeningUserStore } from '@/stores/user/listeningUser'
import { useLearningPlayer } from '@/composables/common/useLearningPlayer'
import LearningSplitLayout from '@/layouts/LearningSplitLayout.vue'
import LessonSidebar from '@/components/user/shared/LessonSidebar.vue'
import QuestionRenderer from '@/components/user/questions/QuestionRenderer.vue'
import TaskGroupRenderer from '@/components/user/questions/TaskGroupRenderer.vue'
import AudioPlayer from '@/components/user/listening/AudioPlayer.vue'
import LevelUpgradeAlert from '@/components/user/shared/LevelUpgradeAlert.vue'
import {
  ArrowLeft,
  Timer,
  ArrowRight,
  Lock,
  Loading,
  Document,
  CircleCheck,
  WarningFilled,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const listeningStore = useListeningUserStore()
const player = useLearningPlayer(listeningStore)

const isLoading = ref(true)
const audioPlayerRef = ref(null)
const unlockingTranscript = ref(false)
const retryCount = ref(0)

const currentLesson = computed(() => listeningStore.currentLesson)
const topicLessons = computed(() => listeningStore.currentTopicLessons || [])
const groupedTasks = computed(() => currentLesson.value?.groupedQuestions?.tasks || [])
const standaloneQuestions = computed(
  () => currentLesson.value?.groupedQuestions?.standaloneQuestions || [],
)
const lastResult = computed(() => listeningStore.lastSubmitResult)
const nextLessonId = computed(() => listeningStore.lastSubmitResult?.nextLessonId)

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
  listeningStore.clearCurrentLesson()

  try {
    await listeningStore.fetchLessonDetail(lessonId)

    if (currentLesson.value?.topicId) {
      await listeningStore.fetchLessonsByTopic(currentLesson.value.topicId)
    }

    await nextTick()

    // Luôn set timer từ timeLimitSeconds
    player.remainingTime.value = currentLesson.value?.timeLimitSeconds || 300

    if (!player.showResult.value) {
      player.startTimer(() => handleSubmit())
    }

    isLoading.value = false
  } catch (error) {
    console.error('Load data error:', error)
    isLoading.value = false
  }
}

const handleAudioPlay = async () => {
  if (!currentLesson.value?.id) return
  try {
    await listeningStore.trackPlay(currentLesson.value.id)
  } catch (error) {
    console.error('Failed to track play:', error)
  }
}

const handleAudioError = (error) => {
  console.error('Audio error:', error)
  ElMessage.error('Không thể phát audio. Vui lòng thử lại.')
}

const canUnlockTranscript = computed(() => {
  if (!currentLesson.value) return false
  if (currentLesson.value.transcriptUnlocked) return true

  // Chỉ khi đã hoàn thành bài học
  return currentLesson.value.isCompleted || false
})

const unlockTranscript = async () => {
  if (!currentLesson.value?.id) return

  if (!canUnlockTranscript.value) {
    ElMessage.warning({
      message: 'Bạn cần hoàn thành bài học (đạt ≥80%) để mở transcript.',
      duration: 4000
    })
    return
  }

  unlockingTranscript.value = true
  try {
    await listeningStore.viewTranscript(currentLesson.value.id)
    await listeningStore.fetchLessonDetail(currentLesson.value.id)
    ElMessage.success('Đã mở khóa transcript!')
  } catch (error) {
    console.error('Failed to unlock transcript:', error)
    ElMessage.error(error.response?.data?.message || 'Không thể mở khóa transcript')
  } finally {
    unlockingTranscript.value = false
  }
}

const handleSubmit = async () => {
  await player.submitExam(currentLesson.value.id, groupedTasks.value, standaloneQuestions.value)
  if (currentLesson.value?.topicId) {
    listeningStore.fetchLessonsByTopic(currentLesson.value.topicId)
  }
}

const switchLesson = (id) => {
  isLoading.value = true
  retryCount.value = 0
  router.push({ name: 'user-listening-lesson', params: { lessonId: id } })
}

const goToNextLesson = () => {
  if (nextLessonId.value) switchLesson(nextLessonId.value)
}

const retryLesson = () => {
  console.log('🔄 Retrying lesson...')
  retryCount.value++
  player.showResult.value = false
  player.submitting.value = false
  player.userAnswers.value = {}
  player.clearQuestionsState(groupedTasks.value, standaloneQuestions.value)
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

<style scoped>
.transcript-card {
  border-radius: 0.75rem;
  border: 1px solid rgb(229 231 235);
  background: white;
  overflow: hidden;
}

html.dark .transcript-card {
  background: rgb(31 41 55);
  border-color: rgb(55 65 81);
}

.transcript-card.translation {
  background: rgb(254 249 195);
  border-color: rgb(250 204 21);
}

html.dark .transcript-card.translation {
  background: rgb(113 63 18);
  border-color: rgb(161 98 7);
}

.transcript-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgb(249 250 251);
  border-bottom: 1px solid rgb(229 231 235);
  font-size: 0.875rem;
  font-weight: 600;
  color: rgb(55 65 81);
}

html.dark .transcript-label {
  background: rgb(17 24 39);
  border-color: rgb(55 65 81);
  color: rgb(209 213 219);
}

.transcript-card.translation .transcript-label {
  background: rgb(253 224 71);
  border-color: rgb(250 204 21);
  color: rgb(113 63 18);
}

html.dark .transcript-card.translation .transcript-label {
  background: rgb(161 98 7);
  border-color: rgb(202 138 4);
  color: rgb(254 249 195);
}

.transcript-content {
  padding: 1.25rem;
  color: rgb(55 65 81);
  line-height: 1.75;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.9375rem;
}

html.dark .transcript-content {
  color: rgb(209 213 219);
}

.transcript-card.translation .transcript-content {
  color: rgb(113 63 18);
  font-style: italic;
}

html.dark .transcript-card.translation .transcript-content {
  color: rgb(254 249 195);
}

.transcript-locked {
  min-height: 16rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: rgb(249 250 251);
  border: 2px dashed rgb(209 213 219);
  border-radius: 0.75rem;
}

html.dark .transcript-locked {
  background: rgb(31 41 55);
  border-color: rgb(75 85 99);
}

.transcript-empty {
  height: 8rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgb(156 163 175);
  background: rgb(243 244 246);
  border: 1px dashed rgb(209 213 219);
  border-radius: 0.5rem;
}

html.dark .transcript-empty {
  background: rgb(31 41 55);
  border-color: rgb(75 85 99);
}
</style>
