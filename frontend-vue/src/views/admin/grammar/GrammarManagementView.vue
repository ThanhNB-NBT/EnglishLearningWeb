<template>
  <div class="w-full h-full bg-gray-50 dark:bg-[#0f0f0f]">
    <el-tabs v-model="activeTab" type="border-card" class="grammar-tabs shadow-sm !border-none overflow-hidden">
      <!-- Tab 1: Topics -->
      <el-tab-pane name="topics" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Collection /></el-icon>
            Chủ đề (Topics)
          </span>
        </template>
        <div class="p-4">
          <TopicList
            module-type="GRAMMAR"
            @view-lessons="handleSwitchToLessons"
            @add-lesson="handleAddLessonFromTopic"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: Lessons -->
      <el-tab-pane name="lessons" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Document /></el-icon>
            Bài học (Lessons)
          </span>
        </template>
        <div class="p-4">
          <LessonsList
            ref="lessonListRef"
            :init-topic-id="selectedTopicIdForLesson"
            @view-questions="handleSwitchToQuestions"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 3: Questions -->
      <el-tab-pane name="questions" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><QuestionFilled /></el-icon>
            Câu hỏi (Questions)
          </span>
        </template>
        <div class="p-4">
          <QuestionList ref="questionListRef" :init-lesson-id="selectedLessonIdForQuestion" :config="grammarConfig" />
        </div>
      </el-tab-pane>

      <!-- Tab 4: AI Import -->
      <el-tab-pane name="parsing" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><MagicStick /></el-icon>
            AI Import
          </span>
        </template>
        <div class="p-4">
          <AIParsingPanel />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, defineAsyncComponent, nextTick } from 'vue'
import { Collection, Document, QuestionFilled, MagicStick } from '@element-plus/icons-vue'
import TopicList from '@/components/admin/shared/topic/TopicList.vue'
import LessonsList from '@/components/admin/grammar/LessonList.vue'
import QuestionList from '@/components/admin/grammar/QuestionList.vue'
const AIParsingPanel = defineAsyncComponent(() => import('@/components/admin/grammar/AIParsingPanel.vue'))
import { useGrammarAdminStore } from '@/stores/admin/grammarAdmin'

const grammarStore = useGrammarAdminStore()

const grammarConfig = {
  moduleType: 'GRAMMAR',
  lessonLabel: 'Bài học Ngữ pháp',
  contentLabel: 'Nội dung',
  contentIcon: Document,
  showTopicSelector: true,

  // ✅ Thêm fetchLessonDetail
  fetchLessonDetail: async (lessonId) => {
    return await grammarStore.fetchLessonDetail(lessonId)
  },

  // ✅ Optional: Thêm các functions khác nếu cần
  fetchAllLessons: async () => {
    // Nếu cần load all lessons cho dropdown
    return grammarStore.lessons
  }
}

const activeTab = ref('topics')
const selectedTopicIdForLesson = ref(null)
const selectedLessonIdForQuestion = ref(null)
const lessonListRef = ref(null)

const handleSwitchToLessons = (topic) => {
  if (topic?.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'
  }
}

const handleAddLessonFromTopic = async (topic) => {
  if (topic?.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'
    await nextTick()
    if (lessonListRef.value?.openCreate) lessonListRef.value.openCreate()
  }
}

const handleSwitchToQuestions = (lesson) => {
  if (lesson?.id) {
    selectedLessonIdForQuestion.value = lesson.id
    activeTab.value = 'questions'
  }
}
</script>

<style scoped>
:deep(.el-tabs__content) {
  padding: 0;
  background-color: var(--el-bg-color);
}

:deep(.el-tabs--border-card) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: var(--el-bg-color);
  color: var(--el-color-primary);
  font-weight: 600;
}

html.dark:deep(.el-tabs--border-card) {
  border-color: #333;
}

html.dark:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: #1a1a1a;
  border-bottom-color: #333;
}

html.dark:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: #141414;
  color: #409eff;
}
</style>
