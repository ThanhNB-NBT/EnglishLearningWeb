<template>
  <TopicTabsView
    title="Luyện Nghe"
    icon="🎧"
    default-icon="💿"
    :topics="topics"
    :loading="loading"
    module-type="listening"
    module-label="Listening Module"
    route-name="user-listening-lesson"
    :progress-summary="progressSummary"
    @fetch-topics="fetchData"
  />
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useListeningUserStore } from '@/stores/user/listeningUser'
import TopicTabsView from '@/components/user/shared/TopicTabsView.vue'

const listeningStore = useListeningUserStore()

const topics = computed(() => listeningStore.topics)
const loading = computed(() => listeningStore.topicsLoading)
const progressSummary = computed(() => listeningStore.progressSummary)

const fetchData = async () => {
  await Promise.all([
    listeningStore.fetchTopics(),
    listeningStore.fetchProgressSummary(),
  ])
}

onMounted(() => {
  fetchData()
})
</script>
