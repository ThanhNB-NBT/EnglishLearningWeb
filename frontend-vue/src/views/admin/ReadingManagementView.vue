<template>
  <div class="w-full">
    <el-tabs
      v-model="activeTab"
      type="border-card"
      class="shadow-sm !border-none overflow-hidden"
      @tab-change="handleTabChange"
    >
      <!-- Tab 1: Lessons -->
      <el-tab-pane name="lessons" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Document /></el-icon> Bài đọc (Lessons)
          </span>
        </template>
        <div class="p-4">
          <LessonsList ref="lessonListRef" @view-questions="handleSwitchToQuestions" />
        </div>
      </el-tab-pane>

      <!-- Tab 2: Questions -->
      <el-tab-pane name="questions" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><QuestionFilled /></el-icon> Câu hỏi (Questions)
          </span>
        </template>
        <div class="p-4">
          <QuestionList ref="questionListRef" :init-lesson-id="selectedLessonIdForQuestion" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document, QuestionFilled } from '@element-plus/icons-vue'
import LessonsList from '@/components/admin/reading/LessonList.vue'
import QuestionList from '@/components/admin/reading/QuestionList.vue'

const activeTab = ref('lessons')
const selectedLessonIdForQuestion = ref(null)
const lessonListRef = ref(null)
const questionListRef = ref(null)

const handleSwitchToQuestions = (lesson) => {
  if (lesson?. id) {
    selectedLessonIdForQuestion.value = lesson.id
    activeTab.value = 'questions'
  }
}

const handleTabChange = (tabName) => {
  console.log('Tab changed to:', tabName)
}
</script>

<style scoped>
/* Override Element Plus Tabs */
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

/* Quill Editor Styles */
:deep(.ql-editor) {
  font-size: 14px;
  line-height: 1.6;
}

:deep(.ql-editor p) {
  margin-bottom: 0. 5em;
}

:deep(.ql-editor h1),
:deep(.ql-editor h2),
:deep(.ql-editor h3) {
  font-weight: 600;
  margin-top: 1em;
  margin-bottom: 0.5em;
}

:deep(.ql-editor ul),
:deep(.ql-editor ol) {
  padding-left: 1.5em;
}

:deep(.ql-editor blockquote) {
  border-left: 4px solid #ccc;
  padding-left:  1em;
  margin-left: 0;
  font-style: italic;
}
</style>
