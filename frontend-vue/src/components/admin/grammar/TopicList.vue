<template>
  <div class="topic-list-container">
    <div class="header-actions">
      <div class="left-actions">
        <el-input v-model="searchQuery" placeholder="Tìm kiếm chủ đề..." :prefix-icon="Search" clearable
          class="search-input" />

        <el-select v-model="filterLevel" placeholder="Lọc theo Level" clearable class="filter-select">
          <el-option label="Tất cả" value="" />
          <el-option label="Beginner" value="BEGINNER" />
          <el-option label="Intermediate" value="INTERMEDIATE" />
          <el-option label="Advanced" value="ADVANCED" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          Tạo Topic
        </el-button>
        <el-button :icon="Refresh" @click="loadTopics">
          Làm mới
        </el-button>
        <el-tooltip content="Kiểm tra và sửa lỗi thứ tự sắp xếp" placement="top">
          <el-button :icon="Tools" @click="handleValidateOrder">
            Validate
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <el-skeleton :rows="3" v-if="loading" animated />

    <el-empty v-else-if="filteredTopics.length === 0" description="Không tìm thấy chủ đề nào" />

    <div v-else class="topic-grid">
      <el-row :gutter="24">
        <el-col v-for="topic in paginatedTopics" :key="topic.id" :xs="24" :sm="12" :md="8" class="mb-6">
          <TopicCard
            :topic="topic"
            @edit="handleEdit"
            @delete="handleDelete"
            @view-lessons="handleViewLessons"
            @toggle-active="handleToggleActive" />
        </el-col>
      </el-row>
    </div>

    <div class="pagination-wrapper"
      v-if="filteredTopics.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 9, 12, 24]"
        :total="filteredTopics.length"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange" />
    </div>

    <TopicForm
      ref="topicFormRef"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Refresh, Search, Tools } from '@element-plus/icons-vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessageBox, ElMessage } from 'element-plus'
import TopicCard from './TopicCard.vue'
import TopicForm from './TopicForm.vue'

const emit = defineEmits(['view-lessons'])
const store = useGrammarStore()
const topicFormRef = ref(null)

const loading = ref(false)
const searchQuery = ref('')
const filterLevel = ref('')
const currentPage = ref(1)
const pageSize = ref(9)

// --- Actions (Xử lý sự kiện) ---

// Mở form tạo mới
const handleCreate = () => {
  if (topicFormRef.value) {
    topicFormRef.value.openCreate()
  }
}

// Mở form chỉnh sửa
const handleEdit = (topic) => {
  if (topicFormRef.value) {
    topicFormRef.value.openEdit(topic)
  }
}

// Khi form submit thành công
const handleFormSuccess = async () => {
  await loadTopics()
}

// Các hàm khác giữ nguyên
const loadTopics = async () => {
  loading.value = true
  try {
    await store.fetchTopics({ size: 100 })
  } finally {
    loading.value = false
  }
}

const handleValidateOrder = async () => {
  try {
    await store.validateTopicsOrder()
  } catch (e) {
    console.error(e)
  }
}

const filteredTopics = computed(() => {
  let result = store.topics
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(t => t.name.toLowerCase().includes(query))
  }
  if (filterLevel.value) {
    result = result.filter(t => t.levelRequired === filterLevel.value)
  }
  return result
})

const paginatedTopics = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTopics.value.slice(start, start + pageSize.value)
})

const handleToggleActive = async (topic) => {
  try {
    if (topic.isActive) {
      await store.updateTopic(topic.id, { ...topic, isActive: true })
    } else {
      await store.deactivateTopic(topic.id)
    }
  } catch (e) {
    ElMessage.error('Có lỗi xảy ra khi cập nhật trạng thái topic.')
    console.error(e)
  }
}

const handleDelete = async (topic) => {
  try {
    await ElMessageBox.confirm(`Bạn có chắc muốn xóa topic "${topic.name}"?`, 'Cảnh báo', {
      type: 'warning', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy'
    })
    await store.deleteTopic(topic.id)

    await loadTopics()
  } catch (e) {
    ElMessage.error('Xóa topic thất bại.')
    console.error(e)
  }
}

const handleViewLessons = (topic) => emit('view-lessons', topic)
const handleSizeChange = () => currentPage.value = 1
const handlePageChange = (val) => currentPage.value = val

onMounted(loadTopics)
</script>

<style scoped>
.topic-list-container {
  padding: 16px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 12px;
  flex-wrap: wrap;
}

.left-actions {
  display: flex;
  gap: 16px;
  flex: 1;
  min-width: 300px;
}

.right-actions {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 2;
}

.mb-6 {
  margin-bottom: 24px;
}

.filter-select {
  flex: 1;
  min-width: 140px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .topic-list-container {
    padding: 16px;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .left-actions {
    flex-direction: column;
    min-width: 100%;
  }

  .right-actions {
    justify-content: flex-end;
    margin-top:12px;
  }
}
</style>
