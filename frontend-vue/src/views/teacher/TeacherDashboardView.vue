<!-- src/views/teacher/TeacherDashboardView.vue - COMPLETE VERSION -->
<template>
  <div class="teacher-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div>
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
          Teacher Dashboard
        </h1>
        <p class="text-gray-500 dark:text-gray-400 mt-1">
          Xin chào, {{ teacher?.fullName || teacher?.username }}! 👋
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
    <div class="stats-grid" v-loading="statsLoading">
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
        :default-sort="{ prop: 'assignedAt', order: 'descending' }"
      >
        <el-table-column type="index" label="#" width="60" />

        <el-table-column prop="topicName" label="Tên Topic" min-width="200">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-tag
                :type="getModuleTagType(row.moduleType)"
                size="small"
                effect="plain"
              >
                {{ getModuleLabel(row.moduleType) }}
              </el-tag>
              <span class="font-medium">{{ row.topicName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="moduleType" label="Module" width="120">
          <template #default="{ row }">
            <el-tag :type="getModuleTagType(row.moduleType)">
              {{ row.moduleType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="isActive" label="Trạng thái" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
              {{ row.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="assignedAt" label="Ngày phân công" width="180">
          <template #default="{ row }">
            {{ formatDate(row.assignedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :icon="Edit"
              @click="goToManageTopic(row)"
            >
              Quản lý
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="Chưa có phân công nào">
            <el-button type="primary" @click="refreshData">
              Làm mới
            </el-button>
          </el-empty>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { teacherAPI } from '@/api'
import { ElMessage } from 'element-plus'
import {
  Document,
  Reading,
  Memo,
  Headset,
  Refresh,
  Search,
  Edit
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// ==================== STATE ====================
const teacher = ref(authStore.teacher)
const assignments = ref([])
const loading = ref(false)
const statsLoading = ref(false)
const searchQuery = ref('')

const stats = ref({
  totalAssignments: 0,
  grammarTopics: 0,
  readingTopics: 0,
  listeningTopics: 0
})

// ==================== COMPUTED ====================
const filteredAssignments = computed(() => {
  if (!searchQuery.value) return assignments.value

  const query = searchQuery.value.toLowerCase()
  return assignments.value.filter(item =>
    item.topicName?.toLowerCase().includes(query) ||
    item.moduleType?.toLowerCase().includes(query)
  )
})

// ==================== METHODS ====================

/**
 * ✅ Load teacher's assignments
 */
const loadAssignments = async () => {
  try {
    loading.value = true

    console.log('🔄 Loading teacher assignments...')
    const response = await teacherAPI.getMyAssignments()

    assignments.value = response.data.data || []
    console.log('✅ Assignments loaded:', assignments.value.length)
  } catch (error) {
    console.error('❌ Failed to load assignments:', error)
    ElMessage.error({
      message: 'Không thể tải danh sách phân công',
      duration: 3000
    })
  } finally {
    loading.value = false
  }
}

/**
 * ✅ Load teacher statistics
 */
const loadStats = async () => {
  try {
    statsLoading.value = true

    console.log('🔄 Loading teacher stats...')
    const response = await teacherAPI.getTeacherStats()

    stats.value = response.data.data || stats.value
    console.log('✅ Stats loaded:', stats.value)
  } catch (error) {
    console.error('❌ Failed to load stats:', error)
    // Don't show error message for stats, just log it
  } finally {
    statsLoading.value = false
  }
}

/**
 * Refresh all data
 */
const refreshData = async () => {
  await Promise.all([
    loadAssignments(),
    loadStats()
  ])
  ElMessage.success('Đã làm mới dữ liệu')
}

/**
 * Navigate to topic management page
 */
const goToManageTopic = (assignment) => {
  const moduleType = assignment.moduleType.toLowerCase()

  // Route mapping based on module type
  const routeMap = {
    'grammar': 'teacher-grammar',
    'reading': 'teacher-reading',
    'listening': 'teacher-listening'
  }

  const routeName = routeMap[moduleType]

  if (routeName) {
    router.push({
      name: routeName,
      query: { topicId: assignment.topicId }
    })
  } else {
    ElMessage.warning('Module type không hợp lệ')
  }
}

/**
 * Get module tag type for styling
 */
const getModuleTagType = (moduleType) => {
  const typeMap = {
    'GRAMMAR': 'success',
    'READING': 'warning',
    'LISTENING': 'danger'
  }
  return typeMap[moduleType] || 'info'
}

/**
 * Get module label
 */
const getModuleLabel = (moduleType) => {
  const labelMap = {
    'GRAMMAR': 'Ngữ pháp',
    'READING': 'Đọc hiểu',
    'LISTENING': 'Nghe hiểu'
  }
  return labelMap[moduleType] || moduleType
}

/**
 * Format date string
 */
const formatDate = (dateString) => {
  if (!dateString) return '-'

  const date = new Date(dateString)
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// ==================== LIFECYCLE ====================
onMounted(() => {
  console.log('🎯 Teacher Dashboard mounted')
  console.log('👤 Current teacher:', teacher.value)

  // Load data
  loadAssignments()
  loadStats()
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

/* Statistics Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
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
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #111827;
}

.dark .stat-value {
  color: #f9fafb;
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Responsive */
@media (max-width: 768px) {
  .teacher-dashboard {
    padding: 16px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>
