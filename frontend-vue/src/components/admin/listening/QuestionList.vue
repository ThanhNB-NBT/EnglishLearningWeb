<!-- src/components/admin/listening/QuestionList.vue - FIXED -->
<template>
  <div class="w-full h-full">
    <!-- ✅ Dùng Shared Component -->
    <SharedQuestionList
      :init-lesson-id="initLessonId"
      :config="listeningConfig"
      @open-form="handleOpenForm"
      @open-bulk="handleOpenBulk"
    />

    <!-- Listening Question Form Dialog -->
    <SharedQuestionFormDialog
      ref="formRef"
      :config="listeningConfig"
      :current-lesson="currentLesson"
      @submit="handleFormSubmit"
    />

    <!-- Listening Bulk Create Dialog -->
    <BulkCreateDialog
      ref="bulkRef"
      :config="listeningConfig"
      @submit="handleBulkSubmit"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useListeningAdminStore } from '@/stores/admin/listeningAdmin'
import { useTopicStore } from '@/composables/useTopicStore'
import { Headset } from '@element-plus/icons-vue'
import SharedQuestionList from '@/components/admin/shared/questions/QuestionList.vue'
import SharedQuestionFormDialog from '@/components/admin/shared/questions/QuestionFormDialog.vue'
import BulkCreateDialog from '@/components/admin/shared/questions/BulkCreateDialog.vue'

const props = defineProps({
  initLessonId: { type: Number, default: null }
})

const store = useListeningAdminStore()
const topicOps = useTopicStore('LISTENING')
const formRef = ref(null)
const bulkRef = ref(null)
const currentLessonId = ref(props.initLessonId)
const currentLesson = ref(null)

// ==================== CONFIG ====================
const listeningConfig = computed(() => ({
  // Module info
  moduleType: 'LISTENING',
  topicLabel: 'Chủ đề',
  lessonLabel: 'Bài nghe',
  contentLabel: 'Transcript',
  contentIcon: Headset,

  // Features
  showTopicSelector: true,

  // ✅ FIX: Add all required API methods
  fetchTopics: async () => {
    await topicOps.fetchTopics({ size: 100 })
    return topicOps.topics.value || []
  },

  fetchLessons: async (topicId) => {
    await store.fetchLessons(topicId, { size: 1000 })
    return store.lessons || []
  },

  fetchAllLessons: async () => {
    return store.lessons || []
  },

  fetchLessonById: async (lessonId) => {
    const lesson = await store.fetchLessonById(lessonId)
    currentLesson.value = lesson
    return lesson
  },

  fetchLessonDetail: async (lessonId) => {
    return await store.fetchLessonDetail(lessonId)
  },

  fetchQuestions: async (lessonId, params) => {
    await store.fetchQuestions(lessonId, params)
    return store.questions || []
  },

  validateOrder: async (lessonId) => {
    console.log('Validate order for lesson:', lessonId)
  },
}))

// ==================== HANDLERS ====================
const handleOpenForm = async (mode, question, lessonId) => {
  const targetLessonId = lessonId || currentLessonId.value

  if (!targetLessonId) {
    console.error('❌ No lesson ID available')
    return
  }

  if (!currentLesson.value || currentLesson.value.id !== targetLessonId) {
    try {
      currentLesson.value = await store.fetchLessonById(targetLessonId)
    } catch (error) {
      console.error('❌ Failed to load lesson:', error)
      return
    }
  }

  if (mode === 'create') {
    formRef.value?.openCreate(targetLessonId)
  } else if (mode === 'edit' && question) {
    formRef.value?.openEdit(question)
  }
}

const handleOpenBulk = async (lesson) => {
  if (lesson) {
    currentLessonId.value = lesson.id
    currentLesson.value = lesson
  } else if (currentLessonId.value) {
    try {
      currentLesson.value = await store.fetchLessonById(currentLessonId.value)
    } catch (error) {
      console.error('❌ Failed to load lesson:', error)
      return
    }
  }

  if (!currentLesson.value) {
    console.error('❌ No lesson data available')
    return
  }

  bulkRef.value?.open(currentLesson.value)
}

const handleFormSubmit = async (formData, mode) => {
  try {
    if (!formData.parentId) {
      formData.parentId = currentLessonId.value
    }

    if (!formData.parentType) {
      formData.parentType = 'LISTENING'
    }

    if (mode === 'create') {
      await store.createQuestion(formData.parentId, formData)
    } else {
      await store.updateQuestion(formData.id, formData)
    }

    await store.fetchQuestions(currentLessonId.value)
    return true
  } catch (error) {
    console.error('Form submit error:', error)
    throw error
  }
}

const handleBulkSubmit = async (questions, lesson) => {
  try {
    await store.createQuestionsInBulk(lesson.id, questions)
    await store.fetchQuestions(lesson.id)
    return true
  } catch (error) {
    console.error('Bulk submit error:', error)
    throw error
  }
}

watch(
  () => props.initLessonId,
  (newVal) => {
    if (newVal && newVal !== currentLessonId.value) {
      currentLessonId.value = newVal
    }
  },
  { immediate: true }
)

defineExpose({
  refresh: async () => {
    if (currentLessonId.value) {
      await store.fetchQuestions(currentLessonId.value)
    }
  }
})
</script>
