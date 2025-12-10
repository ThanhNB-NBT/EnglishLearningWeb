<template>
  <div class="w-full">
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
          <el-option label="Beginner" value="BEGINNER" />
          <el-option label="Intermediate" value="INTERMEDIATE" />
          <el-option label="Advanced" value="ADVANCED" />
        </el-select>
      </div>

      <div class="flex gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate" class="!rounded-lg font-bold">
          Thêm chủ đề
        </el-button>

        <el-tooltip content="Làm mới" placement="top">
          <el-button :icon="Refresh" circle @click="loadTopics" />
        </el-tooltip>

        <el-tooltip content="Sửa lỗi thứ tự" placement="top">
          <el-button :icon="Tools" circle @click="handleValidateOrder" />
        </el-tooltip>
      </div>
    </div>

    <el-skeleton :rows="5" v-if="loading" animated />

    <el-empty v-else-if="filteredTopics.length === 0" description="Không tìm thấy chủ đề nào" :image-size="120" />

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-6">
      <TopicCard
         v-for="topic in paginatedTopics"
         :key="topic.id"
         :topic="topic"
         @edit="handleEdit"
         @delete="handleDelete"
         @view-lessons="handleViewLessons"
         @toggle-active="handleToggleActive"
      />
    </div>

    <div class="flex justify-center p-4">
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

    <TopicForm ref="topicFormRef" @success="handleFormSuccess" />
  </div>
</template>

<script setup>
// Script giữ nguyên logic cũ
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
const pageSize = ref(12) // Default size cho Grid

const handleCreate = () => topicFormRef.value?.openCreate()
const handleEdit = (topic) => topicFormRef.value?.openEdit(topic)
const handleFormSuccess = async () => await loadTopics()
const loadTopics = async () => {
  loading.value = true
  try { await store.fetchTopics({ size: 100 }) } finally { loading.value = false }
}
const handleValidateOrder = async () => { try { await store.validateTopicsOrder() } catch(e){} }

const filteredTopics = computed(() => {
  let result = store.topics
  if (searchQuery.value) result = result.filter(t => t.name.toLowerCase().includes(searchQuery.value.toLowerCase()))
  if (filterLevel.value) result = result.filter(t => t.levelRequired === filterLevel.value)
  return result
})
const paginatedTopics = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTopics.value.slice(start, start + pageSize.value)
})

const handleToggleActive = async (topic) => {
  const newStatus = topic.isActive; // Giá trị này do v-model ở Card gửi lên
  const actionName = newStatus ? 'kích hoạt' : 'tắt';

  try {
    if (newStatus) {
      await store.activateTopic(topic.id)
    } else {
      await store.deactivateTopic(topic.id)
    }
    ElMessage.success(`Đã ${actionName} chủ đề "${topic.name}"`)
  } catch (e) {
    // Revert UI nếu lỗi (do v-model đã update trước đó)
    topic.isActive = !newStatus
    ElMessage.error(`Lỗi khi ${actionName} chủ đề.`)
    console.error(e)
  }
}
const handleDelete = async (topic) => {
  try {
    await ElMessageBox.confirm(`Xóa chủ đề "${topic.name}"?`, 'Cảnh báo', { type: 'warning', confirmButtonText: 'Xóa' })
    await store.deleteTopic(topic.id)
    await loadTopics()
  } catch(e) {}
}
const handleViewLessons = (t) => emit('view-lessons', t)
const handleSizeChange = () => currentPage.value = 1
const handlePageChange = (val) => currentPage.value = val

onMounted(loadTopics)
</script>
