<!-- src/components/admin/grammar/QuestionList.vue - FULLY FIXED -->
<template>
  <div class="w-full h-full">
    <SharedQuestionList
      :init-lesson-id="initLessonId"
      :config="grammarConfig"
      @open-form="handleOpenForm"
      @open-bulk="handleOpenBulk"
      @update:lessonId="handleLessonIdUpdate"
    />

    <SharedQuestionFormDialog
      ref="formRef"
      :config="grammarConfig"
      :current-lesson="currentLesson"
      @submit="handleFormSubmit"
    />

    <BulkCreateDialog ref="bulkRef" :config="grammarConfig" @submit="handleBulkSubmit" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useGrammarAdminStore } from '@/stores/admin/grammarAdmin'
import { useTopicStore } from '@/composables/useTopicStore'
import { Document } from '@element-plus/icons-vue'
import SharedQuestionList from '@/components/admin/shared/questions/QuestionList.vue'
import SharedQuestionFormDialog from '@/components/admin/shared/questions/QuestionFormDialog.vue'
import BulkCreateDialog from '@/components/admin/shared/questions/BulkCreateDialog.vue'

const props = defineProps({
  initLessonId: { type: Number, default: null },
})

const grammarStore = useGrammarAdminStore()
const topicOps = useTopicStore('GRAMMAR')

const formRef = ref(null)
const bulkRef = ref(null)
const currentLessonId = ref(props.initLessonId)
const currentLesson = ref(null)

// ✅ FULLY FIXED: Config with all required API methods
const grammarConfig = computed(() => ({
  moduleType: 'GRAMMAR',
  topicLabel: 'Chủ đề',
  lessonLabel: 'Bài học',
  contentLabel: 'Nội dung bài học',
  contentIcon: Document,

  // ✅ Enable topic selector
  showTopicSelector: true,

  // ✅ ALL API Methods properly implemented
  fetchTopics: async () => {
    await topicOps.fetchTopics({ size: 100 })
    return topicOps.topics.value || []
  },

  fetchLessons: async (topicId) => {
    await grammarStore.fetchLessons(topicId, { size: 1000 })
    return grammarStore.lessons || []
  },

  fetchAllLessons: async () => {
    return grammarStore.lessons || []
  },

  fetchLessonById: async (lessonId) => {
    const lesson = await grammarStore.fetchLessonDetail(lessonId)
    return lesson
  },

  fetchLessonDetail: async (lessonId) => {
    return await grammarStore.fetchLessonDetail(lessonId)
  },

  fetchQuestions: async (lessonId) => {
    await grammarStore.fetchQuestions(lessonId)
    return grammarStore.groupedQuestions?.standaloneQuestions || []
  },

  validateOrder: async (lessonId) => {
    await grammarStore.fixQuestionOrder(lessonId)
  },
}))

// ✅ Watch for prop changes
watch(
  () => props.initLessonId,
  (newVal) => {
    if (newVal) {
      currentLessonId.value = newVal
    }
  },
  { immediate: true },
)

const handleLessonIdUpdate = (lessonId) => {
  currentLessonId.value = lessonId
  console.log('✅ Lesson ID updated:', lessonId)
}

const handleOpenForm = async (mode, question, lessonId) => {
  const targetLessonId = lessonId || currentLessonId.value

  if (!targetLessonId) {
    console.error('❌ No lesson ID available')
    return
  }

  // Ensure we have the lesson data
  if (!currentLesson.value || currentLesson.value.id !== targetLessonId) {
    try {
      currentLesson.value = await grammarStore.fetchLessonDetail(targetLessonId)
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
      currentLesson.value = await grammarStore.fetchLessonDetail(currentLessonId.value)
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
      formData.parentType = 'GRAMMAR'
    }

    if (mode === 'create') {
      await grammarStore.createQuestion(formData.parentId, formData)
    } else {
      await grammarStore.updateQuestion(formData.id, formData)
    }

    await grammarStore.fetchQuestions(currentLessonId.value)
    return true
  } catch (error) {
    console.error('❌ Form submit error:', error)
    throw error
  }
}

const handleBulkSubmit = async (questions, lesson) => {
  try {
    console.log('📤 Bulk submitting:', { count: questions.length, lessonId: lesson.id })

    await grammarStore.createQuestionsInBulk(lesson.id, questions)
    await grammarStore.fetchQuestions(lesson.id)

    console.log('✅ Bulk submit successful')
    return true
  } catch (error) {
    console.error('❌ Bulk submit error:', error)
    throw error
  }
}

defineExpose({
  refresh: async () => {
    if (currentLessonId.value) {
      await grammarStore.fetchQuestions(currentLessonId.value)
    }
  },
})
</script>
