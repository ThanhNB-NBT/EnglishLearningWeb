<template>
  <TopicTabsView
    title="Luyện Đọc"
    icon="📖"
    default-icon="📚"
    :topics="topics"
    :loading="loading"
    module-type="reading"
    module-label="Reading Module"
    route-name="user-reading-lesson"
    :progress-summary="progressSummary"
    @fetch-topics="fetchData"
  />
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useReadingUserStore } from '@/stores/user/readingUser'
import TopicTabsView from '@/components/user/shared/TopicTabsView.vue'

const readingStore = useReadingUserStore()

const topics = computed(() => readingStore.topics)
const loading = computed(() => readingStore.topicsLoading)
const progressSummary = computed(() => readingStore.progressSummary)

const fetchData = async () => {
  await Promise.all([
    readingStore.fetchTopics(),
    readingStore.fetchProgressSummary(),
  ])
}

onMounted(() => {
  fetchData()
})
</script>
