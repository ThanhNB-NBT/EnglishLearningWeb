<template>
  <div class="topics-list-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h1>Quản lý Grammar Topics</h1>
        <el-space wrap>
          <el-text type="info">
            Tổng: {{ grammarStore.topics.length }} topics
          </el-text>
          <el-text v-if="filters.status !== 'all' || filters.level" type="warning">
            • Đã lọc: {{ filteredTopics.length }} topics
          </el-text>
          <el-text v-if="totalPages > 1" type="primary">
            • Trang {{ currentPage }}/{{ totalPages }}
          </el-text>
        </el-space>
      </div>

      <div class="header-right">
        <el-button
          type="warning"
          :icon="Tools"
          @click="handleValidateOrder"
        >
          Validate Order
        </el-button>

        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
        >
          Tạo Topic mới
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="Trạng thái">
          <el-radio-group v-model="filters.status" @change="handleFilterChange">
            <el-radio-button label="all">Tất cả</el-radio-button>
            <el-radio-button label="active">Active</el-radio-button>
            <el-radio-button label="inactive">Inactive</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Level">
          <el-select
            v-model="filters.level"
            placeholder="Tất cả levels"
            clearable
            style="width: 200px"
            @change="handleFilterChange"
          >
            <el-option label="Tất cả" value="" />
            <el-option label="Beginner" value="BEGINNER" />
            <el-option label="Elementary" value="ELEMENTARY" />
            <el-option label="Intermediate" value="INTERMEDIATE" />
            <el-option label="Upper Intermediate" value="UPPER_INTERMEDIATE" />
            <el-option label="Advanced" value="ADVANCED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Sắp xếp">
          <el-select
            v-model="filters.sort"
            style="width: 200px"
            @change="handleFilterChange"
          >
            <el-option label="Order Index (Tăng)" value="orderIndex,asc" />
            <el-option label="Order Index (Giảm)" value="orderIndex,desc" />
            <el-option label="Tên (A-Z)" value="name,asc" />
            <el-option label="Tên (Z-A)" value="name,desc" />
            <el-option label="Ngày tạo (Mới nhất)" value="createdAt,desc" />
            <el-option label="Ngày tạo (Cũ nhất)" value="createdAt,asc" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            :icon="Refresh"
            @click="handleRefresh"
          >
            Làm mới
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Loading -->
    <div v-if="grammarStore.topicsLoading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Empty state -->
    <el-empty
      v-else-if="!grammarStore.topicsLoading && grammarStore.topics.length === 0"
      description="Chưa có topic nào"
    >
      <el-button type="primary" @click="handleCreate">
        Tạo topic đầu tiên
      </el-button>
    </el-empty>

    <!-- No results after filter -->
    <el-empty
      v-else-if="!grammarStore.topicsLoading && filteredTopics.length === 0"
      description="Không tìm thấy topic nào với bộ lọc này"
    >
      <el-button @click="handleRefresh">
        Xóa bộ lọc
      </el-button>
    </el-empty>

    <!-- Topics Grid -->
    <div v-else class="topics-grid">
      <TopicCard
        v-for="topic in paginatedTopics"
        :key="topic.id"
        :topic="topic"
        @edit="handleEdit"
        @delete="handleDelete"
        @toggle-active="handleToggleActive"
        @view-lessons="handleViewLessons"
        @add-lesson="handleAddLesson"
      />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 10, 20, 50]"
        :total="filteredTopics.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Topic Form Dialog -->
    <TopicForm
      v-model="showForm"
      :topic="selectedTopic"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Tools, Refresh } from '@element-plus/icons-vue'
import TopicCard from '@/components/admin/grammar/TopicCard.vue'
import TopicForm from '@/components/admin/grammar/TopicForm.vue'

// Store
const grammarStore = useGrammarStore()

// Refs
const showForm = ref(false)
const selectedTopic = ref(null)

// Pagination refs - Cho filtered topics
const currentPage = ref(1)
const pageSize = ref(10)

// Filters
const filters = ref({
  status: 'all', // all | active | inactive
  level: '',
  sort: 'orderIndex,asc',
})


// Computed - Filter topics ở client side
const filteredTopics = computed(() => {
  let topics = [...grammarStore.topics]

  // Filter by status
  if (filters.value.status === 'active') {
    topics = topics.filter(t => t.isActive)
  } else if (filters.value.status === 'inactive') {
    topics = topics.filter(t => !t.isActive)
  }

  // Filter by level
  if (filters.value.level) {
    topics = topics.filter(t => t.levelRequired === filters.value.level)
  }

  // Sort
  const [sortField, sortOrder] = filters.value.sort.split(',')
  topics.sort((a, b) => {
    let aVal = a[sortField]
    let bVal = b[sortField]

    // Handle string comparison
    if (typeof aVal === 'string') {
      aVal = aVal.toLowerCase()
      bVal = bVal.toLowerCase()
    }

    if (sortOrder === 'asc') {
      return aVal > bVal ? 1 : -1
    } else {
      return aVal < bVal ? 1 : -1
    }
  })

  return topics
})

// Paginated topics - Client-side pagination
const paginatedTopics = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTopics.value.slice(start, end)
})

// Total pages based on filtered topics
const totalPages = computed(() => {
  return Math.ceil(filteredTopics.value.length / pageSize.value)
})

// Watch filters - Reset to page 1 when filter changes
watch(() => filters.value, () => {
  currentPage.value = 1
}, { deep: true })

// Lifecycle
onMounted(() => {
  fetchAllTopics()
})

// Methods
const fetchAllTopics = async () => {
  try {
    // Load ALL topics without pagination from backend
    // Backend pagination: Lấy tất cả với size lớn
    await grammarStore.fetchTopics({
      page: 0,
      size: 1000, // Load all
      sort: 'orderIndex,asc',
    })

    console.log('Loaded all topics:', grammarStore.topics.length)
  } catch {
    // Error handled in store
  }
}

const handleCreate = () => {
  selectedTopic.value = null
  showForm.value = true
}

const handleEdit = (topic) => {
  selectedTopic.value = topic
  showForm.value = true
}

const handleDelete = async (topic) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa topic "${topic.name}"?`,
      'Xác nhận xóa',
      {
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        type: 'warning',
        beforeClose: async (action, instance, done) => {
          if (action === 'confirm') {
            instance.confirmButtonLoading = true
            try {
              await grammarStore.deleteTopic(topic.id)
              done()
            } catch {
              instance.confirmButtonLoading = false
            }
          } else {
            done()
          }
        },
      }
    )
  } catch {
    // User cancelled
  }
}

const handleToggleActive = async (topic) => {
  try {
    if (!topic.isActive) {
      // Deactivate
      await grammarStore.deactivateTopic(topic.id)
    } else {
      // Reactivate bằng cách update
      await grammarStore.updateTopic(topic.id, {
        name: topic.name,
        description: topic.description,
        levelRequired: topic.levelRequired,
        orderIndex: topic.orderIndex,
        isActive: true
      })
      ElMessage.success('Đã kích hoạt lại topic!')
    }

    // Refresh lại danh sách
    await fetchAllTopics()
  } catch {
    // Error handled in store
  }
}

const emit = defineEmits(['add-lessons', 'view-lessons'])

const handleViewLessons = (topic) => {
  emit('view-lessons', topic)
}

const handleAddLesson = (topic) => {
  emit('add-lessons', topic)
}

const handleValidateOrder = async () => {
  try {
    const result = await grammarStore.validateTopicsOrder()

    if (result.issuesFixed === 0) {
      ElMessage.success('Order Index đã chính xác!')
    }
  } catch {
    // Error handled in store
  }
}

const handleFilterChange = () => {
  // currentPage auto reset via watcher
  console.log('🔍 Filter changed:', filters.value)
}

const handleRefresh = async () => {
  filters.value = {
    status: 'all',
    level: '',
    sort: 'orderIndex,asc',
  }
  currentPage.value = 1
  await fetchAllTopics()
}

const handlePageChange = (page) => {
  currentPage.value = page
  console.log('Changed to page:', page)

  // Scroll to top
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  console.log('Changed page size to:', size)
}

const handleFormSuccess = async () => {
  // Refresh toàn bộ danh sách
  await fetchAllTopics()
  console.log('Refreshed topics list after form success')
}
</script>

<style scoped>
.topics-list-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
}

.header-left h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.filter-card {
  margin-bottom: 24px;
}

.filter-card :deep(.el-card__body) {
  padding: 16px;
}

.loading-container {
  padding: 24px;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(370px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 16px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .topics-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
