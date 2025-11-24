<template>
  <div class="dashboard-page">
    <div class="container">
      <!-- Welcome Card - BLUE THEME -->
      <el-card class="welcome-card" shadow="never">
        <h1 class="welcome-title">
          Chào mừng trở lại, {{ user?.fullName }}!
        </h1>
        <p class="welcome-subtitle">
          Hãy tiếp tục hành trình học tập của bạn ngày hôm nay
        </p>
      </el-card>

      <!-- Stats Cards -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #fff3e0">
              <el-icon :size="32" color="#ff9800">
                <Calendar />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ user?.streakDays || 0 }}</div>
              <div class="stat-label">Ngày liên tiếp</div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #e3f2fd">
              <el-icon :size="32" color="#2196f3">
                <Medal />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ user?.totalPoints || 0 }}</div>
              <div class="stat-label">Tổng điểm</div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #f3e5f5">
              <el-icon :size="32" color="#9c27b0">
                <Reading />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">0</div>
              <div class="stat-label">Bài học hoàn thành</div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #e8f5e9">
              <el-icon :size="32" color="#4caf50">
                <Trophy />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">0</div>
              <div class="stat-label">Thành tích</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Learning Modules -->
      <h2 class="section-title">Các khóa học</h2>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="module-card">
            <div class="module-header">
              <div class="module-icon">
                <el-icon :size="40" color="#409eff">
                  <Reading />
                </el-icon>
              </div>
              <div class="module-info">
                <h3 class="module-title">Ngữ pháp</h3>
                <p class="module-description">Học ngữ pháp cơ bản đến nâng cao</p>
              </div>
            </div>
            <el-progress
              :percentage="0"
              :stroke-width="10"
              :show-text="false"
              class="module-progress"
            />
            <p class="progress-text">0% hoàn thành</p>
            <el-button type="primary" style="width: 100%" @click="$router.push('/grammar')">
              Bắt đầu học
            </el-button>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="module-card">
            <div class="module-header">
              <div class="module-icon">
                <el-icon :size="40" color="#67c23a">
                  <Document />
                </el-icon>
              </div>
              <div class="module-info">
                <h3 class="module-title">Đọc hiểu</h3>
                <p class="module-description">Rèn luyện kỹ năng đọc hiểu</p>
              </div>
            </div>
            <el-progress
              :percentage="0"
              :stroke-width="10"
              :show-text="false"
              color="#67c23a"
              class="module-progress"
            />
            <p class="progress-text">0% hoàn thành</p>
            <el-button type="success" style="width: 100%" @click="$router.push('/reading')">
              Bắt đầu học
            </el-button>
          </el-card>
        </el-col>
      </el-row>

      <!-- Recent Activity -->
      <h2 class="section-title">Hoạt động gần đây</h2>
      <el-card shadow="never">
        <el-empty description="Chưa có hoạt động nào. Bắt đầu học ngay!" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Calendar, Medal, Reading, Trophy, Document } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const user = computed(() => authStore.currentUser)
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  min-height: calc(100vh - 124px);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

/* Welcome Card - BLUE GRADIENT */
.welcome-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  color: white;
  border: none;
}

.welcome-card :deep(.el-card__body) {
  padding: 40px;
}

.welcome-title {
  font-size: 32px;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: white;
}

.welcome-subtitle {
  font-size: 16px;
  margin: 0;
  opacity: 0.95;
  color: rgba(255, 255, 255, 0.9);
}

/* Stats Cards */
.stats-row {
  margin-bottom: 32px;
}

.stat-card {
  margin-bottom: 24px;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* Section Title */
.section-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 16px 0;
  color: #303133;
}

/* Module Cards */
.module-card {
  margin-bottom: 24px;
  height: 100%;
  transition: transform 0.3s;
}

.module-card:hover {
  transform: translateY(-4px);
}

.module-card :deep(.el-card__body) {
  padding: 24px;
}

.module-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.module-icon {
  flex-shrink: 0;
}

.module-info {
  flex: 1;
}

.module-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 4px 0;
  color: #303133;
}

.module-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.module-progress {
  margin-bottom: 8px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin: 0 0 16px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .welcome-card :deep(.el-card__body) {
    padding: 24px;
  }

  .welcome-title {
    font-size: 24px;
  }

  .welcome-subtitle {
    font-size: 14px;
  }
}
</style>
