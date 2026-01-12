<template>
  <div class="placement-test-container">
    <!-- ==================== LOADING STATE ==================== -->
    <div v-if="store.loading" class="loading-state">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p class="mt-4 text-gray-600 dark:text-gray-400">Đang tải đề thi...</p>
    </div>

    <!-- ==================== COOLDOWN STATE ==================== -->
    <div v-else-if="store.isInCooldown && !testStarted" class="cooldown-state">
      <el-result icon="warning" title="Bạn chưa thể làm lại bài thi">
        <template #sub-title>
          <div class="text-center">
            <p class="mb-4">
              {{ store.cooldownMessage || 'Vui lòng đợi 24 giờ sau lần làm bài trước.' }}
            </p>

            <div v-if="store.canRetakeAfter" class="cooldown-timer">
              <el-icon :size="24"><Clock /></el-icon>
              <div class="timer-text">
                <p class="font-bold text-xl">{{ remainingTime }}</p>
                <p class="text-sm text-gray-500">Thời gian còn lại</p>
              </div>
            </div>
          </div>
        </template>
        <template #extra>
          <el-button type="primary" @click="$router.push('/user/home')">
            <el-icon><HomeFilled /></el-icon>
            Quay về trang chủ
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- ==================== WELCOME SCREEN ==================== -->
    <div v-else-if="!testStarted && !showResult" class="welcome-screen">
      <el-card shadow="hover" class="welcome-card">
        <div class="welcome-content">
          <div class="icon-wrapper">
            <el-icon :size="64" color="#409EFF"><Document /></el-icon>
          </div>

          <h1 class="title">Bài Kiểm Tra Đầu Vào</h1>
          <p class="subtitle">Placement Test</p>

          <div class="test-info">
            <div class="info-item">
              <el-icon :size="20"><EditPen /></el-icon>
              <span
                ><strong>{{ store.totalQuestions }}</strong> câu hỏi</span
              >
            </div>
            <div class="info-item">
              <el-icon :size="20"><Timer /></el-icon>
              <span
                ><strong>{{ Math.floor(store.timeLimitSeconds / 60) }}</strong> phút</span
              >
            </div>
            <div class="info-item">
              <el-icon :size="20"><Reading /></el-icon>
              <span><strong>3</strong> phần thi (Grammar, Reading, Listening)</span>
            </div>
          </div>

          <el-alert type="info" :closable="false" class="instructions">
            <template #title>
              <div class="font-bold mb-2">📋 Hướng dẫn:</div>
            </template>
            <ul class="instruction-list">
              <li>Bài thi gồm 3 phần: Ngữ pháp, Đọc hiểu, và Nghe hiểu</li>
              <li>
                Thời gian làm bài:
                <strong>{{ Math.floor(store.timeLimitSeconds / 60) }} phút</strong>
              </li>
              <li>Kết quả sẽ xác định trình độ của bạn (A1 - C1)</li>
              <li>Bạn chỉ có thể làm lại sau <strong>24 giờ</strong></li>
            </ul>
          </el-alert>

          <el-button type="primary" size="large" @click="startTest" class="start-button">
            <el-icon><CaretRight /></el-icon>
            Bắt đầu làm bài
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- ==================== TEST IN PROGRESS ==================== -->
    <LearningSplitLayout v-else-if="testStarted && !showResult" mode="full">
      <template #header-left>
        <el-button link :icon="ArrowLeft" @click="handleExit"> Thoát </el-button>
        <div class="ml-2">
          <h1 class="text-sm font-bold">Placement Test</h1>
        </div>
      </template>

      <template #header-center>
        <div class="timer-badge">
          <el-icon><Timer /></el-icon>
          <span>{{ formatTime(remainingSeconds) }}</span>
        </div>
      </template>

      <template #header-right>
        <div class="progress-badge">{{ answeredCount }}/{{ store.totalQuestions }} câu</div>
      </template>

      <template #content-full>
        <div class="test-content">
          <!-- Grammar Section -->
          <div v-if="store.grammarSection" class="section-block">
            <div class="section-header">
              <h2>
                <el-icon><Reading /></el-icon>
                Part 1: Grammar
              </h2>
              <el-tag type="info">{{ store.grammarSection.questions?.length || 0 }} câu</el-tag>
            </div>

            <div class="questions-list">
              <div
                v-for="(q, idx) in store.grammarSection.questions"
                :key="'grammar-' + q.id"
                class="question-item"
              >
                <div class="question-number">{{ idx + 1 }}.</div>
                <div class="question-content">
                  <div class="question-text" v-html="q.questionText"></div>
                  <QuestionRenderer
                    :question="q"
                    :model-value="userAnswers[q.id] || null"
                    @update:model-value="(val) => updateAnswer(q.id, val)"
                    :disabled="false"
                    :show-feedback="false"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Reading Sections -->
          <div
            v-for="(section, sIdx) in store.readingSections"
            :key="'reading-' + sIdx"
            class="section-block"
          >
            <div class="section-header">
              <h2>
                <el-icon><Document /></el-icon>
                Part {{ 1 + (store.grammarSection ? 1 : 0) + sIdx }}: Reading - {{ section.title }}
              </h2>
              <el-tag type="warning">{{ section.questions?.length || 0 }} câu</el-tag>
            </div>

            <el-card shadow="never" class="passage-card">
              <div class="passage-content" v-html="section.content"></div>
              <el-divider />
              <div class="passage-translation text-gray-600 italic text-sm">
                {{ section.contentTranslation }}
              </div>
            </el-card>

            <div class="questions-list">
              <div
                v-for="(q, idx) in section.questions"
                :key="'reading-' + q.id"
                class="question-item"
              >
                <div class="question-number">{{ getReadingQuestionNumber(sIdx, idx) }}.</div>
                <div class="question-content">
                  <div class="question-text" v-html="q.questionText"></div>
                  <QuestionRenderer
                    :question="q"
                    :model-value="userAnswers[q.id] || null"
                    @update:model-value="(val) => updateAnswer(q.id, val)"
                    :disabled="false"
                    :show-feedback="false"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Listening Sections -->
          <div
            v-for="(section, sIdx) in store.listeningSections"
            :key="'listening-' + sIdx"
            class="section-block"
          >
            <div class="section-header">
              <h2>
                <el-icon><Microphone /></el-icon>
                Part {{ 1 + (store.grammarSection ? 1 : 0) + store.readingSections.length + sIdx }}:
                Listening - {{ section.title }}
              </h2>
              <el-tag type="success">{{ section.questions?.length || 0 }} câu</el-tag>
            </div>

            <el-card shadow="never" class="audio-card">
              <div class="audio-player">
                <audio
                  :id="'audio-' + sIdx"
                  :src="section.audioUrl"
                  controls
                  class="w-full"
                ></audio>
              </div>

              <el-collapse v-if="section.transcript">
                <el-collapse-item title="📄 Xem transcript (sau khi nghe)" name="transcript">
                  <div class="transcript-content">
                    <div class="transcript-text">{{ section.transcript }}</div>
                    <el-divider />
                    <div class="transcript-translation text-gray-600 italic text-sm">
                      {{ section.transcriptTranslation }}
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-card>

            <div class="questions-list">
              <div
                v-for="(q, idx) in section.questions"
                :key="'listening-' + q.id"
                class="question-item"
              >
                <div class="question-number">{{ getListeningQuestionNumber(sIdx, idx) }}.</div>
                <div class="question-content">
                  <div class="question-text" v-html="q.questionText"></div>
                  <QuestionRenderer
                    :question="q"
                    :model-value="userAnswers[q.id] || null"
                    @update:model-value="(val) => updateAnswer(q.id, val)"
                    :disabled="false"
                    :show-feedback="false"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="footer-actions">
          <div class="footer-left">
            <el-button @click="handleExit" :disabled="store.submitting">
              <el-icon><Close /></el-icon>
              Hủy bỏ
            </el-button>
          </div>
          <div class="footer-right">
            <el-button
              type="primary"
              size="large"
              :loading="store.submitting"
              :disabled="answeredCount === 0"
              @click="handleSubmit"
            >
              <el-icon><Check /></el-icon>
              Nộp bài ({{ answeredCount }}/{{ store.totalQuestions }})
            </el-button>
          </div>
        </div>
      </template>
    </LearningSplitLayout>

    <!-- ==================== RESULT SCREEN (REDESIGNED) ==================== -->
    <div v-else-if="showResult && store.lastResult" class="result-screen">
      <div class="result-container">
        <el-card class="result-card">
          <!-- Header -->
          <div class="result-header">
            <h2 class="result-title">Kết quả Placement Test</h2>
            <el-tag :type="getLevelTagType()" size="large" effect="dark" class="level-tag">
              {{ store.lastResult.assignedLevel }}
            </el-tag>
          </div>

          <!-- Score Circle -->
          <div class="score-section">
            <div class="score-circle" :class="getScoreClass()">
              <div class="score-inner">
                <div class="score-value">{{ Math.round(store.lastResult.score) }}</div>
                <div class="score-unit">điểm</div>
              </div>
            </div>
            <p class="level-description">{{ store.lastResult.levelDescription }}</p>
          </div>

          <!-- Stats Grid -->
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon correct">
                <el-icon :size="24"><CircleCheck /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">Số câu đúng</div>
                <div class="stat-value">
                  {{ store.lastResult.correctAnswers }}/{{ store.lastResult.totalQuestions }}
                </div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon time">
                <el-icon :size="24"><Timer /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">Thời gian hoàn thành</div>
                <div class="stat-value">
                  {{ Math.floor((store.timeLimitSeconds - remainingSeconds) / 60) }} phút
                </div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon accuracy">
                <el-icon :size="24"><DataAnalysis /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">Độ chính xác</div>
                <div class="stat-value">{{ Math.round(store.lastResult.score) }}%</div>
              </div>
            </div>
          </div>

          <!-- Cooldown Info -->
          <el-alert type="info" :closable="false" show-icon class="cooldown-info">
            <template #title>
              <strong>Lưu ý:</strong> Bạn có thể làm lại bài thi sau 24 giờ
            </template>
            <div class="mt-1">
              Thời gian có thể làm lại:
              <strong>{{ formatDateTime(store.lastResult.canRetakeAfter) }}</strong>
            </div>
          </el-alert>

          <!-- Actions -->
          <div class="result-actions">
            <el-button size="large" @click="$router.push('/user/home')"> Về trang chủ </el-button>
            <el-button type="primary" size="large" @click="$router.push('/user/learning')">
              <el-icon><Reading /></el-icon>
              Bắt đầu học
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { usePlacementTestStore } from '@/stores/user/placementTest'
import LearningSplitLayout from '@/layouts/LearningSplitLayout.vue'
import QuestionRenderer from '@/components/user/questions/QuestionRenderer.vue'
import {
  Loading,
  Document,
  Timer,
  CaretRight,
  ArrowLeft,
  Reading,
  Microphone,
  Check,
  Close,
  EditPen,
  Clock,
  HomeFilled,
  CircleCheck,
  DataAnalysis,
} from '@element-plus/icons-vue'

const router = useRouter()
const store = usePlacementTestStore()

const testStarted = ref(false)
const showResult = ref(false)
const userAnswers = ref({})
const remainingSeconds = ref(0)
let timerInterval = null

const answeredCount = computed(() => Object.keys(userAnswers.value).length)
const remainingTime = computed(() => {
  if (!store.canRetakeAfter) return ''
  const now = new Date()
  const retakeTime = new Date(store.canRetakeAfter)
  const diff = retakeTime - now

  if (diff <= 0) return '0 giờ'

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (hours > 0) return `${hours} giờ ${minutes} phút`
  return `${minutes} phút`
})

const startTest = async () => {
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn bắt đầu bài thi? Thời gian sẽ được tính ngay khi bạn bấm "Xác nhận".',
      'Xác nhận bắt đầu',
      {
        confirmButtonText: 'Bắt đầu',
        cancelButtonText: 'Hủy',
        type: 'warning',
      },
    )

    testStarted.value = true
    remainingSeconds.value = store.timeLimitSeconds
    startTimer()
  } catch (e) {
    console.log('User cancelled start', e)
  }
}

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval)

  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      stopTimer()
      ElMessage.warning('Hết thời gian! Bài thi sẽ được nộp tự động.')
      handleSubmit()
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const updateAnswer = (questionId, value) => {
  userAnswers.value[questionId] = value
}

const getReadingQuestionNumber = (sectionIndex, questionIndex) => {
  let count = store.grammarSection?.questions?.length || 0

  for (let i = 0; i < sectionIndex; i++) {
    count += store.readingSections[i]?.questions?.length || 0
  }

  return count + questionIndex + 1
}

const getListeningQuestionNumber = (sectionIndex, questionIndex) => {
  let count = store.grammarSection?.questions?.length || 0

  store.readingSections.forEach((section) => {
    count += section.questions?.length || 0
  })

  for (let i = 0; i < sectionIndex; i++) {
    count += store.listeningSections[i]?.questions?.length || 0
  }

  return count + questionIndex + 1
}

const handleSubmit = async () => {
  if (answeredCount.value < store.totalQuestions) {
    try {
      await ElMessageBox.confirm(
        `Bạn mới trả lời ${answeredCount.value}/${store.totalQuestions} câu. Bạn có chắc muốn nộp bài?`,
        'Xác nhận nộp bài',
        {
          confirmButtonText: 'Nộp bài',
          cancelButtonText: 'Tiếp tục làm',
          type: 'warning',
        },
      )
    } catch (e) {
      console.log('User cancelled submit', e)
      return
    }
  }

  stopTimer()

  const answers = []
  store.allQuestions.forEach((q) => {
    const answer = userAnswers.value[q.id]

    if (answer !== undefined && answer !== null) {
      answers.push({
        questionId: q.id,
        selectedOptions: typeof answer === 'number' ? [answer] : [],
        textAnswer:
          typeof answer === 'string' || typeof answer === 'object'
            ? typeof answer === 'object'
              ? JSON.stringify(answer)
              : answer
            : null,
      })
    }
  })

  try {
    await store.submitPlacementTest(answers)
    showResult.value = true
    testStarted.value = false
  } catch (error) {
    console.error('Submit error:', error)
  }
}

const handleExit = async () => {
  try {
    await ElMessageBox.confirm(
      'Bài thi của bạn sẽ không được lưu. Bạn có chắc muốn thoát?',
      'Xác nhận thoát',
      {
        confirmButtonText: 'Thoát',
        cancelButtonText: 'Ở lại',
        type: 'warning',
      },
    )

    stopTimer()
    router.push('/user/home')
  } catch (e) {
    console.log('User cancelled exit', e)
  }
}

const getScoreClass = () => {
  const score = store.lastResult?.score || 0
  if (score >= 85) return 'excellent'
  if (score >= 70) return 'good'
  if (score >= 55) return 'average'
  return 'below-average'
}

const getLevelTagType = () => {
  const level = store.lastResult?.assignedLevel
  if (['C1', 'C2'].includes(level)) return 'success'
  if (['B1', 'B2'].includes(level)) return 'primary'
  return 'warning'
}

onMounted(async () => {
  await store.fetchPlacementTest()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.placement-test-container {
  min-height: 100vh;
  background: white;
  padding: 2rem;
}

/* Loading & Cooldown States */
.loading-state,
.cooldown-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: white;
}

.cooldown-timer {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: white;
  padding: 1.5rem 2rem;
  border-radius: 12px;
  margin-top: 1.5rem;
}

.timer-text {
  text-align: left;
}

/* Welcome Screen */
.welcome-screen {
  max-width: 800px;
  margin: 0 auto;
}

.welcome-card {
  border-color: #303133;
  border-radius: 16px;
  overflow: hidden;
}

.welcome-content {
  text-align: center;
  padding: 2rem;
}

.icon-wrapper {
  margin-bottom: 1.5rem;
}

.title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.subtitle {
  font-size: 1.25rem;
  color: #909399;
  margin-bottom: 2rem;
}

.test-info {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
}

.instructions {
  text-align: left;
  margin-bottom: 2rem;
}

.instruction-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.instruction-list li {
  margin-bottom: 0.5rem;
  padding-left: 1.5rem;
  position: relative;
}

.instruction-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #409eff;
  font-weight: bold;
}

.start-button {
  min-width: 200px;
}

/* Test Header */
.timer-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #409eff;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1.125rem;
}

.progress-badge {
  background: #f0f2f5;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: 600;
}

/* Test Content */
.test-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
}

.section-block {
  margin-bottom: 3rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e4e7ed;
}

.section-header h2 {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}

.passage-card,
.audio-card {
  margin-bottom: 1.5rem;
}

.passage-content {
  line-height: 1.8;
  font-size: 1rem;
}

.audio-player {
  margin-bottom: 1rem;
}

.transcript-content {
  padding: 1rem;
  background: #f9fafb;
  border-radius: 8px;
}

.transcript-text {
  line-height: 1.8;
  margin-bottom: 1rem;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.question-item {
  display: flex;
  gap: 1rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.question-number {
  font-size: 1.25rem;
  font-weight: 700;
  color: #409eff;
  min-width: 2rem;
}

.question-content {
  flex: 1;
}

.question-text {
  margin-bottom: 1rem;
  font-size: 1rem;
  line-height: 1.6;
}

/* Footer */
.footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* Result Screen */
.result-screen {
  max-width: 700px;
  margin: 0 auto;
}

.result-card {
  border-radius: 16px;
  overflow: hidden;
}

.result-content {
  text-align: center;
  padding: 3rem 2rem;
}

.result-icon {
  margin-bottom: 2rem;
}

.result-icon.result-excellent {
  color: #67c23a;
}

.result-icon.result-good {
  color: #409eff;
}

.result-icon.result-average {
  color: #e6a23c;
}

.result-score {
  margin-bottom: 2rem;
}

.score-value {
  font-size: 4rem;
  font-weight: 700;
  margin: 0;
  color: #303133;
}

.score-label {
  font-size: 1.25rem;
  color: #909399;
  margin: 0.5rem 0 0 0;
}

.result-level {
  margin-bottom: 2rem;
}

.level-description {
  margin-top: 1rem;
  font-size: 1rem;
  color: #606266;
}

.result-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-label {
  font-size: 0.875rem;
  color: #909399;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #303133;
}

.cooldown-alert {
  margin-bottom: 2rem;
}

.result-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

/* Responsive */
@media (max-width: 768px) {
  .placement-test-container {
    padding: 1rem;
  }

  .test-info {
    flex-direction: column;
    gap: 1rem;
  }

  .question-item {
    flex-direction: column;
  }

  .question-number {
    min-width: auto;
  }

  .result-stats {
    grid-template-columns: 1fr;
  }
}
</style>
