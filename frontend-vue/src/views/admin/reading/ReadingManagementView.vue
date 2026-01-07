<template>
  <div class="w-full h-full bg-gray-50 dark:bg-[#0f0f0f]">
    <el-tabs v-model="activeTab" type="border-card" class="shadow-sm !border-none overflow-hidden">
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
            module-type="READING"
            @view-lessons="handleSwitchToLessons"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: Lessons -->
      <el-tab-pane name="lessons" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Document /></el-icon>
            Bài đọc (Lessons)
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
          <!-- ✅ FIX: Add config prop like ListeningManagementView -->
          <QuestionList
            ref="questionListRef"
            :init-lesson-id="selectedLessonIdForQuestion"
            :config="readingConfig"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Collection, Document, QuestionFilled } from '@element-plus/icons-vue'
import TopicList from '@/components/admin/shared/topic/TopicList.vue'
import LessonsList from '@/components/admin/reading/LessonList.vue'
import QuestionList from '@/components/admin/reading/QuestionList.vue'
import { useReadingAdminStore } from '@/stores/admin/readingAdmin'

const readingAdminStore = useReadingAdminStore()

// ✅ FIX: Add config like ListeningManagementView
const readingConfig = {
  moduleType: 'READING',
  lessonLabel: 'Bài học Đọc',
  contentLabel: 'Nội dung',
  contentIcon: Document,
  showTopicSelector: true,

  // ✅ Add fetchLessonDetail
  fetchLessonDetail: async (lessonId) => {
    return await readingAdminStore.fetchLessonDetail(lessonId)
  },

  // ✅ Add fetchAllLessons
  fetchAllLessons: async () => {
    return readingAdminStore.lessons
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
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-tabs__item) {
  font-weight: 500;
  transition: all 0.2s;
}

:deep(.el-tabs__item:hover) {
  color: var(--el-color-primary);
}

:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
