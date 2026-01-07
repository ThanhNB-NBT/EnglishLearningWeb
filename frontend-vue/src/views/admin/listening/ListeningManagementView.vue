<template>
  <div class="w-full h-full bg-gray-50 dark:bg-[#0f0f0f]">
    <el-tabs v-model="activeTab" type="border-card" class="listening-tabs shadow-sm !border-none overflow-hidden">
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
            module-type="LISTENING"
            @view-lessons="handleSwitchToLessons"
            @add-lesson="handleAddLessonFromTopic"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: Lessons -->
      <el-tab-pane name="lessons" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Headset /></el-icon>
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
          <QuestionList
            ref="questionListRef"
            :init-lesson-id="selectedLessonIdForQuestion"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Collection, Headset, QuestionFilled } from '@element-plus/icons-vue'
import TopicList from '@/components/admin/shared/topic/TopicList.vue'
import LessonsList from '@/components/admin/listening/LessonList.vue'
import QuestionList from '@/components/admin/listening/QuestionList.vue'

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
