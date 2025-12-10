<template>
  <div class="w-full h-full bg-gray-50 dark:bg-[#0f0f0f]">
    <el-tabs v-model="activeTab" type="border-card"
      class="grammar-tabs shadow-sm !border-none overflow-hidden">
      <el-tab-pane name="topics" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon>
              <Collection />
            </el-icon> Chủ đề (Topics)
          </span>
        </template>
        <div class="p-4">
          <TopicsList @view-lessons="handleSwitchToLessons" @add-lesson="handleAddLessonFromTopic" />
        </div>
      </el-tab-pane>

      <el-tab-pane name="lessons" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon>
              <Document />
            </el-icon> Bài học (Lessons)
          </span>
        </template>
        <div class="p-4">
          <LessonsList ref="lessonListRef" :init-topic-id="selectedTopicIdForLesson"
            @view-questions="handleSwitchToQuestions" />
        </div>
      </el-tab-pane>

      <el-tab-pane name="questions" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon>
              <QuestionFilled />
            </el-icon> Câu hỏi (Questions)
          </span>
        </template>
        <div class="p-4">
          <QuestionList ref="questionListRef" :init-lesson-id="selectedLessonIdForQuestion" />
        </div>
      </el-tab-pane>

      <el-tab-pane name="parsing" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon>
              <MagicStick />
            </el-icon> AI Import
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
import { Collection, Document, QuestionFilled, MagicStick, Reading } from '@element-plus/icons-vue'
import TopicsList from '@/components/admin/grammar/TopicList.vue'
import LessonsList from '@/components/admin/grammar/LessonList.vue'
import QuestionList from '@/components/admin/grammar/QuestionList.vue'
const AIParsingPanel = defineAsyncComponent(() => import('@/components/admin/grammar/AIParsingPanel.vue'))

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
/* Override Element Plus Tabs để đẹp hơn với Tailwind */
:deep(.el-tabs__content) {
  padding: 0;
  background-color: var(--el-bg-color);
}

:deep(.el-tabs--border-card) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card>.el-tabs__header) {
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card>.el-tabs__header .el-tabs__item.is-active) {
  background-color: var(--el-bg-color);
  border-right-color: var(--el-border-color);
  border-left-color: var(--el-border-color);
  color: var(--el-color-primary);
  font-weight: 600;
}

/* Dark mode specifics */
html.dark :deep(.el-tabs--border-card) {
  border-color: #333;
}

html.dark :deep(.el-tabs--border-card>.el-tabs__header) {
  background-color: #1a1a1a;
  border-bottom-color: #333;
}

html.dark :deep(.el-tabs--border-card>.el-tabs__header .el-tabs__item.is-active) {
  background-color: #141414;
  border-right-color: #333;
  border-left-color: #333;
  color: #409eff;
}
</style>
