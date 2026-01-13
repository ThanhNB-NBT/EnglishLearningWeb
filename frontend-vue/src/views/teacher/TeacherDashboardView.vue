<!-- src/views/teacher/TeacherDashboardView.vue - FIXED -->
<template>
  <div class="teacher-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div>
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
          Teacher Dashboard
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-1">
          Xin chào, {{ teacher?. fullName || teacher?.username }}!  👋
        </p>
      </div>

      <el-button
        type="primary"
        :icon="Refresh"
        @click="refreshData"
        :loading="loading"
      >
        Làm mới
      </el-button>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid" v-loading="loading">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-blue-100 text-blue-600">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">Tổng số phân công</p>
            <p class="stat-value">{{ stats.totalAssignments }}</p>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-green-100 text-green-600">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">Grammar Topics</p>
            <p class="stat-value">{{ stats.grammarTopics }}</p>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-purple-100 text-purple-600">
            <el-icon :size="24"><Memo /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">Reading Topics</p>
            <p class="stat-value">{{ stats.readingTopics }}</p>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-orange-100 text-orange-600">
            <el-icon :size="24"><Headset /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">Listening Topics</p>
            <p class="stat-value">{{ stats.listeningTopics }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Assignments Table -->
    <el-card shadow="hover" class="mt-6">
      <template #header>
        <div class="card-header">
          <span class="text-lg font-semibold">Danh sách phân công của tôi</span>
          <el-input
            v-model="searchQuery"
            placeholder="Tìm kiếm topic..."
            :prefix-icon="Search"
            style="width: 300px"
            clearable
          />
        </div>
      </template>

      <el-table
        :data="filteredAssignments"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
        class="cursor-pointer"
      >
        <el-table-column label="Module" width="120">
          <template #default="{ row }">
            <el-tag :type="getModuleTypeTag(row.moduleType)">
              {{ row.moduleType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Topic" prop="name" min-width="200" />

        <el-table-column label="Mô tả" prop="description" min-width="250" show-overflow-tooltip />

        <el-table-column label="Trạng thái" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
              {{ row.isActive ? 'Active' : 'Inactive' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Ngày tạo" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="150" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click.stop="goToModule(row)"
            >
              Quản lý
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTopicTeacherStore } from '@/stores/teacher/topicTeacher'
import { ElMessage } from 'element-plus'
import { Document, Reading, Memo, Headset, Refresh, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const teacherStore = useTopicTeacherStore()

const loading = ref(false)
const searchQuery = ref('')

const stats = ref({
  totalAssignments:  0,
  grammarTopics: 0,
  readingTopics: 0,
  listeningTopics: 0,
})

const teacher = computed(() => authStore.teacher)

// ✅ Combine all topics from 3 modules
const allAssignments = computed(() => {
  const grammar = teacherStore.grammarTopics. map(t => ({ ...t, moduleType: 'GRAMMAR' }))
  const reading = teacherStore.readingTopics. map(t => ({ ...t, moduleType: 'READING' }))
  const listening = teacherStore.listeningTopics.map(t => ({ ...t, moduleType: 'LISTENING' }))

  return [...grammar, ...reading, ...listening]
})

const filteredAssignments = computed(() => {
  if (!searchQuery.value) return allAssignments.value

  const query = searchQuery.value.toLowerCase()
  return allAssignments. value.filter((a) =>
    a.name?.toLowerCase().includes(query) ||
    a.description?.toLowerCase().includes(query)
  )
})

/**
 * ✅ Fetch topics from 3 modules
 */
const fetchAssignments = async () => {
  loading.value = true
  try {
    console.log('📄 Fetching teacher assigned topics from all modules...')

    // ✅ Call all 3 modules in parallel
    await Promise.all([
      teacherStore.fetchMyTopics('GRAMMAR', { size: 100 }),
      teacherStore.fetchMyTopics('READING', { size: 100 }),
      teacherStore.fetchMyTopics('LISTENING', { size: 100 }),
    ])

    // Calculate stats
    stats.value.grammarTopics = teacherStore.grammarTopics.length
    stats.value.readingTopics = teacherStore.readingTopics.length
    stats.value.listeningTopics = teacherStore.listeningTopics.length
    stats.value.totalAssignments =
      stats.value.grammarTopics +
      stats.value.readingTopics +
      stats. value.listeningTopics

    console.log('✅ Loaded assignments:', {
      grammar: stats.value. grammarTopics,
      reading: stats.value.readingTopics,
      listening: stats. value.listeningTopics,
      total: stats.value. totalAssignments,
    })

  } catch (error) {
    console.error('❌ Error fetching assignments:', error)
    ElMessage. error('Không thể tải danh sách phân công')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await fetchAssignments()
  ElMessage.success('Đã làm mới dữ liệu')
}

const goToModule = (assignment) => {
  const moduleType = assignment.moduleType. toLowerCase()

  console.log('🔍 Navigating to module:', moduleType, 'topicId:', assignment.id)

  // Navigate to correct module management page
  router.push({
    name: `teacher-${moduleType}`,
  })
}

const handleRowClick = (row) => {
  goToModule(row)
}

const getModuleTypeTag = (type) => {
  const map = {
    GRAMMAR: 'success',
    READING: 'warning',
    LISTENING: 'danger',
  }
  return map[type] || 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('DD/MM/YYYY HH:mm')
}

onMounted(() => {
  fetchAssignments()
})
</script>

<style scoped>
.teacher-dashboard {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight:  bold;
  color: #111827;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cursor-pointer :deep(.el-table__row) {
  cursor: pointer;
}

.cursor-pointer :deep(.el-table__row:hover) {
  background-color: #f3f4f6;
}

.dark .stat-value {
  color: #f9fafb;
}

.dark .cursor-pointer :deep(.el-table__row:hover) {
  background-color: #374151;
}
</style>
