<template>
  <SharedQuestionList :init-lesson-id="initLessonId" :config="grammarConfig" />
</template>

<script setup>
import { useGrammarStore } from '@/stores/grammar'
import SharedQuestionList from '@/components/admin/shared/questions/QuestionList.vue'

defineProps({
  initLessonId: { type: Number, default: null }
})

const grammarConfig = {
  store: useGrammarStore(),
  topicLabel: 'Chủ đề',
  lessonLabel: 'Bài học',
  parentType: 'GRAMMAR'
}
</script>
