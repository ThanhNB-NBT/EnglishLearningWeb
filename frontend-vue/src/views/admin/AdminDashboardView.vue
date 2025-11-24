<template>
  <div class="admin-dashboard-page">
    <div class="container">
      <!-- Welcome Card - BLUE THEME -->
      <el-card class="welcome-card" shadow="never">
        <h1 class="welcome-title">
          Chào mừng, {{ admin?.username }}! 🛡️
        </h1>
        <p class="welcome-subtitle">
          Bảng điều khiển quản trị hệ thống
        </p>
      </el-card>

      <!-- Stats Cards -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #e3f2fd">
              <el-icon :size="32" color="#2196f3">
                <User />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">0</div>
              <div class="stat-label">Tổng người dùng</div>
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
              <div class="stat-label">Bài ngữ pháp</div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #e8f5e9">
              <el-icon :size="32" color="#4caf50">
                <Document />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">0</div>
              <div class="stat-label">Bài đọc</div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" style="background: #fff3e0">
              <el-icon :size="32" color="#ff9800">
                <TrendCharts />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">0</div>
              <div class="stat-label">Hoạt động</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Quick Actions -->
      <h2 class="section-title">Thao tác nhanh</h2>
      <el-row :gutter="24">
        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="action-card" @click="$router.push('/admin/users')">
            <el-icon :size="40" color="#409eff"><User /></el-icon>
            <h3>Quản lý người dùng</h3>
            <p>Xem, chỉnh sửa và quản lý người dùng</p>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="action-card" @click="$router.push('/admin/grammar')">
            <el-icon :size="40" color="#67c23a"><Reading /></el-icon>
            <h3>Quản lý ngữ pháp</h3>
            <p>Thêm, sửa, xóa bài học ngữ pháp</p>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="8">
          <el-card shadow="hover" class="action-card" @click="$router.push('/admin/reading')">
            <el-icon :size="40" color="#e6a23c"><Document /></el-icon>
            <h3>Quản lý bài đọc</h3>
            <p>Quản lý nội dung bài đọc hiểu</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { User, Reading, Document, TrendCharts } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const admin = computed(() => authStore.currentAdmin)
</script>

<style scoped>
.admin-dashboard-page {
  padding: 24px;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

/* Welcome Card - BLUE THEME */
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

/* Action Cards */
.action-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s;
  margin-bottom: 24px;
}

.action-card:hover {
  transform: translateY(-8px);
}

.action-card :deep(.el-card__body) {
  padding: 32px;
}

.action-card h3 {
  font-size: 18px;
  font-weight: bold;
  margin: 16px 0 8px 0;
  color: #303133;
}

.action-card p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
</style>
