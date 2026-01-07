<template>
  <TopicTabsView
    title="Ngữ Pháp"
    icon="✏️"
    default-icon="📖"
    :topics="topics"
    :loading="loading"
    module-type="grammar"
    module-label="Grammar Module"
    route-name="user-grammar-lesson"
    :progress-summary="progressSummary"
    @fetch-topics="fetchData"
  />
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useGrammarUserStore } from '@/stores/user/grammarUser'
import TopicTabsView from '@/components/user/shared/TopicTabsView.vue'

const grammarStore = useGrammarUserStore()

const topics = computed(() => grammarStore.topics)
const loading = computed(() => grammarStore.topicsLoading)
const progressSummary = computed(() => grammarStore.progressSummary)

const fetchData = async () => {
  await Promise.all([
    grammarStore.fetchTopics(),
    grammarStore.fetchProgressSummary(),
  ])
}

onMounted(() => {
  fetchData()
})
</script>
