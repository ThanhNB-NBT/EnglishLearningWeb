<template>
  <div class="grammar-management-view">
    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="border-card" class="compact-tabs">
      <!-- Topics Tab -->
      <el-tab-pane label="Topics" name="topics">
        <template #label>
          <span class="tab-label">
            <el-icon><Collection /></el-icon>
            Topics
          </span>
        </template>
        <TopicsList @view-lessons="handleSwitchToLessons" @add-lesson="handleAddLessonFromTopic" />
      </el-tab-pane>

      <!-- Lessons Tab -->
      <el-tab-pane label="Lessons" name="lessons">
        <template #label>
          <span class="tab-label">
            <el-icon><Document /></el-icon>
            Lessons
          </span>
        </template>
        <LessonsList ref="lessonListRef" :init-topic-id="selectedTopicIdForLesson" />
      </el-tab-pane>

      <!-- Questions Tab (Coming soon) -->
      <el-tab-pane label="Questions" name="questions" disabled>
        <template #label>
          <span class="tab-label">
            <el-icon><QuestionFilled /></el-icon>
            Questions
            <el-tag size="small" type="info">Soon</el-tag>
          </span>
        </template>
        <el-empty description="Coming soon..." />
      </el-tab-pane>

      <!-- AI Parsing Tab (Coming soon) -->
      <el-tab-pane label="AI Parsing" name="parsing" disabled>
        <template #label>
          <span class="tab-label">
            <el-icon><MagicStick /></el-icon>
            AI Parsing
            <el-tag size="small" type="success">AI</el-tag>
          </span>
        </template>
        <el-empty description="Coming soon..." />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { Collection, Document, QuestionFilled, MagicStick } from '@element-plus/icons-vue'
import TopicsList from '@/components/admin/grammar/TopicList.vue'
import LessonsList from '@/components/admin/grammar/LessonList.vue'

// Active tab
const activeTab = ref('topics')
const selectedTopicIdForLesson = ref(null)
const lessonListRef = ref(null)

const handleSwitchToLessons = (topic) => {
  console.log('Switching to topic', topic)
  if (topic && topic.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'
  }
}

const handleAddLessonFromTopic = async (topic) => {
  console.log('Adding lesson for topic:', topic)
  if (topic && topic.id) {
    selectedTopicIdForLesson.value = topic.id
    activeTab.value = 'lessons'

    await nextTick()
    if (lessonListRef.value) {
      lessonListRef.value.openCreateDialog()
    }
  }
}
</script>

<style scoped>
.grammar-management-view {
  padding: 0; /* Remove padding, đã có padding ở layout */
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

/* Compact tabs content */
:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-tabs__header) {
  margin: 0 0 12px; /* Giảm margin bottom */
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
    padding: 0 12px;
  }
}
</style>
