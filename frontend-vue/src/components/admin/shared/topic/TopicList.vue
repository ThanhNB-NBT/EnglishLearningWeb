<!-- src/components/admin/shared/topic/TopicList.vue - FIXED VERSION -->
<template>
  <div class="w-full">
    <!-- Toolbar -->
    <div class="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm">
      <div class="flex flex-wrap gap-3 w-full md:w-auto">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm chủ đề..."
          :prefix-icon="Search"
          clearable
          class="!w-full md:!w-64"
        />

        <el-select v-model="filterLevel" placeholder="Level" clearable class="!w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="A1" value="A1" />
          <el-option label="A2" value="A2" />
          <el-option label="B1" value="B1" />
          <el-option label="B2" value="B2" />
          <el-option label="C1" value="C1" />
          <el-option label="C2" value="C2" />
        </el-select>

        <el-select v-model="filterStatus" placeholder="Trạng thái" clearable class="!w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="Active" :value="true" />
          <el-option label="Inactive" :value="false" />
        </el-select>
      </div>

      <div class="flex gap-2">
        <!-- Show "Add Topic" button only for admin -->
        <el-button
          v-if="topicOps.isAdmin.value"
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          class="!rounded-lg font-bold"
        >
          Thêm chủ đề
        </el-button>

        <el-tooltip content="Làm mới" placement="top">
          <el-button
            :icon="Refresh"
            circle
            @click="handleRefresh"
            :loading="topicOps.isLoading.value"
          />
        </el-tooltip>

        <!-- Show "Fix Order" button only for admin -->
        <el-tooltip v-if="topicOps.isAdmin.value" content="Sửa lỗi thứ tự" placement="top">
          <el-button
            :icon="Tools"
            circle
            @click="topicOps.fixOrderIndexes"
          />
        </el-tooltip>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border shadow-sm">
        <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ filteredTopics?.length || 0 }}</div>
        <div class="text-xs text-gray-500 uppercase">Tổng topics</div>
      </div>
      <div class="bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border shadow-sm">
        <div class="text-2xl font-bold text-green-600">{{ activeCount || 0 }}</div>
        <div class="text-xs text-gray-500 uppercase">Đang hoạt động</div>
      </div>
      <div class="bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border shadow-sm">
        <div class="text-2xl font-bold text-red-600">{{ inactiveCount || 0 }}</div>
        <div class="text-xs text-gray-500 uppercase">Đã ẩn</div>
      </div>
      <div class="bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border shadow-sm">
        <div class="text-2xl font-bold text-blue-600">{{ totalLessons || 0 }}</div>
        <div class="text-xs text-gray-500 uppercase">Tổng bài học</div>
      </div>
    </div>

    <!-- Loading -->
    <el-skeleton :rows="5" v-if="topicOps.isLoading.value" animated />

    <!-- Empty State -->
    <el-empty
      v-else-if="!filteredTopics || filteredTopics.length === 0"
      description="Không tìm thấy chủ đề nào"
      :image-size="120"
    />

    <!-- Grid -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-6">
      <TopicCard
        v-for="topic in paginatedTopics"
        :key="topic.id"
        :topic="topic"
        :is-admin="topicOps.isAdmin.value"
        @edit="handleEdit"
        @delete="handleDelete"
        @view-lessons="handleViewLessons"
        @toggle-active="handleToggleActive"
      />
    </div>

    <!-- Pagination -->
    <div v-if="filteredTopics.length > 0" class="flex justify-center p-4">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[8, 12, 24, 48]"
        :total="filteredTopics.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Form Dialog -->
    <TopicForm
      v-if="topicOps.isAdmin.value"
      ref="topicFormRef"
      :module-type="moduleType"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Refresh, Search, Tools } from '@element-plus/icons-vue'
import { useTopicStore } from '@/composables/useTopicStore'
import TopicCard from './TopicCard.vue'
import TopicForm from './TopicForm.vue'

const props = defineProps({
  moduleType: {
    type: String,
    required: true,
    validator: (value) => ['GRAMMAR', 'LISTENING', 'READING'].includes(value),
  },
})

const emit = defineEmits(['view-lessons', 'add-lesson'])

// ✅ FIX: Pass moduleType to composable
const topicOps = useTopicStore(props.moduleType)

// Debug: Log topicOps structure
console.log('🔍 TopicList: topicOps initialized', {
  hasTopics: !!topicOps.topics,
  hasIsLoading: !!topicOps.isLoading,
  hasIsAdmin: !!topicOps.isAdmin,
  isAdmin: topicOps.isAdmin?.value,
  isTeacher: topicOps.isTeacher?.value,
})

// Local UI state
const topicFormRef = ref(null)
const searchQuery = ref('')
const filterLevel = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(12)

// ✅ FIX: Access the computed ref correctly using .value
const filteredTopics = computed(() => {
  // topicOps.topics is a computed ref, so we need to use .value
  const topicsArray = topicOps.topics.value

  console.log('🔍 filteredTopics computed - topicsArray:', topicsArray)

  // Safety check
  if (!Array.isArray(topicsArray)) {
    console.warn('⚠️ topicsArray is not an array:', typeof topicsArray)
    return []
  }

  if (topicsArray.length === 0) {
    console.log('📭 No topics found')
    return []
  }

  let result = [...topicsArray]

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase().trim()
    result = result.filter((t) =>
      t.name?.toLowerCase().includes(query) ||
      t.description?.toLowerCase().includes(query)
    )
  }

  // Level filter
  if (filterLevel.value) {
    result = result.filter((t) => t.levelRequired === filterLevel.value)
  }

  // Status filter
  if (filterStatus.value !== '') {
    result = result.filter((t) => t.isActive === filterStatus.value)
  }

  console.log('📊 Filtered topics count:', result.length)
  return result
})

const paginatedTopics = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTopics.value.slice(start, start + pageSize.value)
})

const activeCount = computed(() => {
  if (!filteredTopics.value) return 0
  return filteredTopics.value.filter((t) => t.isActive).length
})

const inactiveCount = computed(() => {
  if (!filteredTopics.value) return 0
  return filteredTopics.value.filter((t) => !t.isActive).length
})

const totalLessons = computed(() => {
  if (!filteredTopics.value) return 0
  return filteredTopics.value.reduce((sum, t) => sum + (t.lessonCount || 0), 0)
})

// Methods - Simple delegates to composable
const handleRefresh = async () => {
  await topicOps.fetchTopics()
}

const handleCreate = () => {
  if (!topicOps.isAdmin.value) {
    console.warn('Only admin can create topics')
    return
  }
  topicFormRef.value?.openCreate()
}

const handleEdit = (topic) => {
  topicFormRef.value?.openEdit(topic)
}

const handleDelete = async (topic) => {
  if (!topicOps.isAdmin.value) {
    console.warn('Only admin can delete topics')
    return
  }

  try {
    await topicOps.deleteTopic(topic)

    // Reset to page 1 if current page is now empty
    const maxPage = Math.ceil(filteredTopics.value.length / pageSize.value)
    if (currentPage.value > maxPage) {
      currentPage.value = Math.max(1, maxPage)
    }
  } catch (error) {
    console.error('Error deleting topic:', error)
    // Error handled by composable
  }
}

const handleToggleActive = async (topic) => {
  try {
    await topicOps.toggleStatus(topic)
  } catch (error) {
    console.error('Error toggling topic status:', error)
    // Error handled by composable
  }
}

const handleViewLessons = (topic) => {
  emit('view-lessons', topic)
}

const handleFormSuccess = async () => {
  await topicOps.fetchTopics()
  currentPage.value = 1
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

// Lifecycle
onMounted(() => {
  console.log('📌 TopicList mounted for:', props.moduleType)
  console.log('👤 Current role:', topicOps.currentRole.value)
  topicOps.fetchTopics()
})

// Expose
defineExpose({
  refresh: handleRefresh,
})
</script>

<style scoped>
.grid {
  scroll-behavior: smooth;
}

.el-button {
  transition: all 0.2s ease;
}
</style>
