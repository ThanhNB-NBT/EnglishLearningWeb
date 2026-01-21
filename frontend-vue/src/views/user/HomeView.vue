<template>
  <div class="w-full max-w-7xl mx-auto p-8 space-y-6">
    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center min-h-[400px]">
      <div class="text-center">
        <el-icon class="is-loading" :size="48" color="#409eff">
          <Loading />
        </el-icon>
        <p class="mt-4 text-gray-600">Đang tải dữ liệu...</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="flex items-center justify-center min-h-[400px]">
      <el-empty description="Có lỗi xảy ra khi tải dữ liệu">
        <el-button type="primary" @click="fetchDashboard">Thử lại</el-button>
      </el-empty>
    </div>

    <!-- Dashboard Content -->
    <template v-else-if="dashboard">
      <!-- Welcome Header -->
      <div class="gradient-bg rounded-2xl p-8 text-white shadow-lg">
        <h1 class="text-3xl font-bold mb-2">
          Chào mừng trở lại, {{ userName }}! 👋
        </h1>
        <p class="text-blue-100 text-lg opacity-90">
          Hãy tiếp tục hành trình chinh phục tiếng Anh của bạn.
        </p>

        <!-- Weekly Goal Progress -->
        <div v-if="quickStats?.weeklyGoalProgress !== undefined" class="mt-4 bg-white/10 backdrop-blur-sm rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm">Weekly Goal (7-day streak)</span>
            <span class="text-sm font-semibold">
              {{ Math.round(quickStats.weeklyGoalProgress || 0) }}%
            </span>
          </div>
          <div class="w-full bg-white/20 rounded-full h-2">
            <div
              class="bg-white h-2 rounded-full transition-all"
              :style="{ width: `${quickStats.weeklyGoalProgress || 0}%` }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Quick Stats Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          v-for="(stat, idx) in statsCards"
          :key="idx"
          :icon="stat.icon"
          :value="stat.value"
          :label="stat.label"
          :subtext="stat.subtext"
          :iconColor="stat.iconColor"
          :bgClass="stat.bgClass"
        />
      </div>

      <!-- AI Advisor Widget -->
      <div>
        <h2 class="text-xl font-bold text-gray-800 dark:text-white mb-4">
          Gợi ý cho bạn
        </h2>
        <AIAdvisorWidget />
      </div>

      <!-- Skill Progress - Only if user has completed lessons -->
      <div v-if="hasAnySkillProgress">
        <h2 class="text-xl font-bold text-gray-800 dark:text-white mb-4">
          Tiến độ học tập
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <SkillProgressCard
            v-if="dashboard.skillProgress?.grammar?.completed > 0"
            title="Grammar"
            icon="📚"
            :data="dashboard.skillProgress.grammar"
            color="blue"
          />
          <SkillProgressCard
            v-if="dashboard.skillProgress?.reading?.completed > 0"
            title="Reading"
            icon="📖"
            :data="dashboard.skillProgress.reading"
            color="yellow"
          />
          <SkillProgressCard
            v-if="dashboard.skillProgress?.listening?.completed > 0"
            title="Listening"
            icon="🎧"
            :data="dashboard.skillProgress.listening"
            color="red"
          />
        </div>
      </div>

      <!-- Empty State when no progress -->
      <div v-else class="empty-state-section">
        <div class="empty-state-card">
          <el-empty description="Chưa có tiến độ học tập" :image-size="120">
            <template #image>
              <span style="font-size: 64px">🚀</span>
            </template>
            <template #description>
              <p class="empty-title">Bắt đầu hành trình học tập!</p>
              <p class="empty-desc">
                Hoàn thành bài học đầu tiên để xem tiến độ của bạn.
              </p>
            </template>
            <div class="mt-4 flex gap-3 justify-center">
              <el-button type="primary" @click="$router.push('/user/grammar')">
                Học Grammar
              </el-button>
              <el-button type="warning" @click="$router.push('/user/reading')">
                Đọc hiểu
              </el-button>
              <el-button type="danger" @click="$router.push('/user/listening')">
                Luyện nghe
              </el-button>
            </div>
          </el-empty>
        </div>
      </div>

      <!-- Streak Info Card -->
      <div v-if="streak" class="streak-card">
        <div class="streak-header">
          <span class="streak-icon">🔥</span>
          <h3>Streak Information</h3>
        </div>
        <div class="streak-stats">
          <div class="streak-stat">
            <div class="stat-value">{{ streak.currentStreak }}</div>
            <div class="stat-label">Current Streak</div>
            <div v-if="streak.hasStreakToday" class="stat-badge success">
              ✓ Đã học hôm nay
            </div>
            <div v-else class="stat-badge warning">
              Chưa học hôm nay
            </div>
          </div>
          <div class="streak-stat">
            <div class="stat-value">{{ streak.longestStreak }}</div>
            <div class="stat-label">Longest Streak</div>
            <div v-if="streak.lastStreakDate" class="stat-info">
              Lần cuối: {{ streak.lastStreakDate }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useDashboard } from '@/composables/useDashboard'
import { Loading } from '@element-plus/icons-vue'
import AIAdvisorWidget from '@/components/user/dashboard/AIAdvisorWidget.vue'
import StatCard from '@/components/user/dashboard/StatCard.vue'
import SkillProgressCard from '@/components/user/dashboard/SkillProgressCard.vue'

const authStore = useAuthStore()
const {
  dashboardData: dashboard,
  isLoading,
  error,
  fetchDashboard,
  quickStats,
  streak
} = useDashboard()

const user = computed(() => authStore.currentUser)
const userName = computed(() => user.value?.fullName || user.value?.username || 'Bạn')

// Check if user has any skill progress (completed > 0)
const hasAnySkillProgress = computed(() => {
  const skills = dashboard.value?.skillProgress
  if (!skills) return false

  return (
    (skills.grammar?.completed || 0) > 0 ||
    (skills.reading?.completed || 0) > 0 ||
    (skills.listening?.completed || 0) > 0
  )
})

// Stats cards configuration
const statsCards = computed(() => {
  const stats = quickStats.value || {}
  const streakInfo = streak.value || {}

  return [
    {
      label: 'Ngày liên tiếp (Current Streak)',
      value: stats.currentStreak || 0,
      subtext: streakInfo.hasStreakToday ? '✓ Hôm nay đã học' : 'Chưa học hôm nay',
      icon: 'calendar',
      iconColor: '#ff9800',
      bgClass: 'bg-orange-50 dark:bg-orange-900/20',
    },
    {
      label: 'Tổng điểm (Total Points)',
      value: stats.totalPoints || 0,
      subtext: '',
      icon: 'medal',
      iconColor: '#2196f3',
      bgClass: 'bg-blue-50 dark:bg-blue-900/20',
    },
    {
      label: 'Bài học xong (Completed)',
      value: stats.totalLessonsCompleted || 0,
      subtext: '',
      icon: 'reading',
      iconColor: '#9c27b0',
      bgClass: 'bg-purple-50 dark:bg-purple-900/20',
    },
    {
      label: 'Chuỗi dài nhất (Longest)',
      value: streakInfo.longestStreak || 0,
      subtext: 'ngày',
      icon: 'trophy',
      iconColor: '#4caf50',
      bgClass: 'bg-green-50 dark:bg-green-900/20',
    },
  ]
})

onMounted(async () => {
  await fetchDashboard()
})
</script>

<style scoped>
.gradient-bg {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.empty-state-section {
  margin-top: 32px;
}

.empty-state-card {
  background: white;
  border-radius: 16px;
  border: 2px dashed #e5e7eb;
  padding: 60px 40px;
  text-align: center;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #6b7280;
}

.streak-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.streak-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.streak-icon {
  font-size: 32px;
}

.streak-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.streak-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}

.streak-stat {
  text-align: center;
  padding: 20px;
  background: #f9fafb;
  border-radius: 12px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
}

.stat-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  margin-top: 8px;
}

.stat-badge.success {
  background: #d1fae5;
  color: #065f46;
}

.stat-badge.warning {
  background: #fef3c7;
  color: #92400e;
}

.stat-info {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .empty-state-card {
    padding: 40px 20px;
  }

  .streak-stats {
    grid-template-columns: 1fr;
  }
}
</style>
