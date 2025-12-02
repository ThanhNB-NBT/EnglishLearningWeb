<!-- src/views/admin/GrammarManagementView.vue - FIXED -->
<template>
  <div class="grammar-management-view">
    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="border-card" class="compact-tabs">
      <!-- Topics Tab -->
      <el-tab-pane label="Topics" name="topics">
        <template #label>
          <span class="tab-label">
            <el-icon><Collection /></el-icon>
            <span v-if="!isMobile">Topics</span>
          </span>
        </template>
        <TopicsList
          @view-lessons="handleSwitchToLessons"
          @add-lesson="handleAddLessonFromTopic"
        />
      </el-tab-pane>

      <!-- Lessons Tab -->
      <el-tab-pane label="Lessons" name="lessons">
        <template #label>
          <span class="tab-label">
            <el-icon><Document /></el-icon>
            <span v-if="!isMobile">Lessons</span>
          </span>
        </template>
        <LessonsList
          ref="lessonListRef"
          :init-topic-id="selectedTopicIdForLesson"
          @view-questions="handleSwitchToQuestions"
        />
      </el-tab-pane>

      <!-- Questions Tab -->
      <el-tab-pane label="Questions" name="questions">
        <template #label>
          <span class="tab-label">
            <el-icon><QuestionFilled /></el-icon>
            <span v-if="!isMobile">Questions</span>
          </span>
        </template>
        <QuestionList
          ref="questionListRef"
          :init-lesson-id="selectedLessonIdForQuestion"
        />
      </el-tab-pane>

      <!-- AI Parsing Tab (Coming soon) -->
      <el-tab-pane label="AI Parsing" name="parsing" disabled>
        <template #label>
          <span class="tab-label">
            <el-icon><MagicStick /></el-icon>
            <span v-if="!isMobile">AI Parsing</span>
            <el-tag v-if="!isMobile" size="small" type="success">AI</el-tag>
          </span>
        </template>
        <el-empty description="Coming soon..." />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { nextTick, ref, computed } from 'vue'
import { Collection, Document, QuestionFilled, MagicStick } from '@element-plus/icons-vue'
import TopicsList from '@/components/admin/grammar/TopicList.vue'
import LessonsList from '@/components/admin/grammar/LessonList.vue'
import QuestionList from '@/components/admin/grammar/QuestionList.vue'

// Responsive
const isMobile = computed(() => window.innerWidth < 768)

// Active tab
const activeTab = ref('topics')
const selectedTopicIdForLesson = ref(null)
const selectedLessonIdForQuestion = ref(null)
const lessonListRef = ref(null)
const questionListRef = ref(null)

// ✅ Switch to Lessons tab from Topics
const handleSwitchToLessons = (topic) => {
  console.log('✅ Switching to lessons for topic:', topic.id, topic.name)
  if (topic && topic.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'
  }
}

// ✅ Add lesson from Topics tab
const handleAddLessonFromTopic = async (topic) => {
  console.log('✅ Adding lesson for topic:', topic.id)
  if (topic && topic.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'

    await nextTick()
    if (lessonListRef.value) {
      lessonListRef.value.openCreateDialog()
    }
  }
}

// ✅ NEW: Switch to Questions tab from Lessons (FIXED - Emit instead of router)
const handleSwitchToQuestions = (lesson) => {
  console.log('✅ Switching to questions for lesson:', lesson.id, lesson.title)
  if (lesson && lesson.id) {
    selectedLessonIdForQuestion.value = lesson.id
    activeTab.value = 'questions'
  }
}
</script>

<style scoped>
.grammar-management-view {
  padding: 0;
}

.compact-tabs {
  border: none;
  box-shadow: var(--el-box-shadow-light);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-tabs__header) {
  margin: 0 0 12px;
}

:deep(.el-tabs__item) {
  padding: 0 16px;
  height: 40px;
  line-height: 40px;
}

/* Responsive */
@media (max-width: 768px) {
  .tab-label {
    font-size: 12px;
    gap: 4px;
  }

  :deep(.el-tabs__item) {
    padding: 0 8px;
    font-size: 12px;
  }

  :deep(.el-tabs__nav-scroll) {
    overflow-x: auto;
  }
}

@media (max-width: 480px) {
  .tab-label {
    font-size: 11px;
  }

  :deep(.el-tabs__item) {
    padding: 0 6px;
  }
}
</style>
