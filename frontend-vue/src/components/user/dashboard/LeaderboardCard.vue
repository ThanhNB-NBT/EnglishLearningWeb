<template>
  <div class="leaderboard-card">
    <div class="card-header">
      <h3 class="card-title">🏅 Bảng xếp hạng</h3>
      <el-segmented v-model="activeTab" :options="tabs" size="small" />
    </div>

    <!-- Your Rank Summary -->
    <div class="rank-summary">
      <div class="rank-item">
        <div class="rank-label">Your Rank (Points)</div>
        <div class="rank-value">
          #{{ data.pointsRank }}
          <span class="rank-total">/ {{ data.pointsTotal }}</span>
        </div>
      </div>
      <div class="rank-divider"></div>
      <div class="rank-item">
        <div class="rank-label">Your Rank (Streak)</div>
        <div class="rank-value">
          #{{ data.streakRank }}
          <span class="rank-total">/ {{ data.streakTotal }}</span>
        </div>
      </div>
    </div>

    <!-- Top Users List -->
    <div v-if="data.topUsers && data.topUsers.length > 0" class="top-users-list">
      <h4 class="list-title">Top 3 Users</h4>
      <div
        v-for="(user) in data.topUsers"
        :key="user.userId"
        class="user-item"
        :class="{ highlight: isCurrentUser(user.userId) }"
      >
        <div class="user-rank">
          <span class="medal">{{ getMedal(user.rank) }}</span>
        </div>
        <div class="user-info">
          <div class="user-name">
            {{ user.displayName || user.username }}
            <el-tag v-if="isCurrentUser(user.userId)" type="success" size="small" effect="light">
              You
            </el-tag>
          </div>
          <div class="user-username">@{{ user.username }}</div>
        </div>
        <div class="user-points">
          <span class="points-value">{{ formatPoints(user.points) }}</span>
          <span class="points-label">pts</span>
        </div>
      </div>
    </div>

    <el-button link type="primary" class="view-full-btn" @click="handleViewFull">
      View full leaderboard
      <el-icon><ArrowRight /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ArrowRight } from '@element-plus/icons-vue'

defineProps({
  data: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['view-full'])

const authStore = useAuthStore()
const activeTab = ref('Points')
const tabs = ['Points', 'Streak']

const currentUserId = computed(() => authStore.currentUser?.id)

const isCurrentUser = (userId) => {
  return userId === currentUserId.value
}

const getMedal = (rank) => {
  const medals = {
    1: '🥇',
    2: '🥈',
    3: '🥉',
  }
  return medals[rank] || `#${rank}`
}

const formatPoints = (points) => {
  if (points >= 1000000) {
    return `${(points / 1000000).toFixed(1)}M`
  }
  if (points >= 1000) {
    return `${(points / 1000).toFixed(1)}K`
  }
  return points.toString()
}

const handleViewFull = () => {
  emit('view-full')
  // Could navigate to full leaderboard page
}
</script>

<style scoped>
.leaderboard-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.rank-summary {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  margin-bottom: 20px;
  color: white;
}

.rank-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
}

.rank-label {
  font-size: 12px;
  opacity: 0.9;
  font-weight: 500;
}

.rank-value {
  font-size: 24px;
  font-weight: 700;
}

.rank-total {
  font-size: 14px;
  opacity: 0.8;
}

.rank-divider {
  width: 1px;
  background: rgba(255, 255, 255, 0.3);
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin: 0 0 12px 0;
}

.top-users-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.user-item:hover {
  background: #f3f4f6;
}

.user-item.highlight {
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  border-color: #3b82f6;
}

.user-rank {
  flex-shrink: 0;
  width: 40px;
  display: flex;
  justify-content: center;
}

.medal {
  font-size: 28px;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-username {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.user-points {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.points-value {
  font-size: 18px;
  font-weight: 700;
  color: #3b82f6;
}

.points-label {
  font-size: 11px;
  color: #6b7280;
}

.view-full-btn {
  width: 100%;
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .rank-summary {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .rank-divider {
    display: none;
  }

  .user-item {
    padding: 10px;
  }

  .medal {
    font-size: 24px;
  }
}
</style>
