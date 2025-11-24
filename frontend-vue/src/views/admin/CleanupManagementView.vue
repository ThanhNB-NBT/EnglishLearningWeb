<template>
  <div class="cleanup-management-page">
    <div class="container">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">Quản lý tài khoản chưa xác thực</h1>
          <p class="page-subtitle">Tự động xóa tài khoản sau {{ stats.cutoffHours }} giờ</p>
        </div>
        <div class="header-actions">
          <el-button
            :icon="RefreshRight"
            @click="refreshAll"
            :loading="loading"
          >
            Làm mới
          </el-button>
          <el-button
            type="danger"
            :icon="Delete"
            :loading="cleanupLoading"
            :disabled="preview.willBeDeleted === 0"
            @click="handleRunCleanup"
          >
            Chạy Cleanup ({{ preview.willBeDeleted }})
          </el-button>
        </div>
      </div>

      <!-- Info Alert -->
      <el-alert
        :type="cleanupStatus"
        :closable="false"
        show-icon
        style="margin-bottom: 24px"
      >
        <template #title>
          <strong>Trạng thái Cleanup</strong>
        </template>
        <div class="alert-content">
          <p>
            Hệ thống tự động xóa tài khoản chưa xác thực sau <strong>{{ stats.cutoffHours }} giờ</strong>.
          </p>
          <p>
            Cleanup tự động chạy mỗi ngày lúc <strong>2:00 AM</strong>.
          </p>
          <p v-if="lastCleanupTime" class="last-cleanup">
            Lần cleanup gần nhất: <strong>{{ formatRelativeTime(lastCleanupTime) }}</strong>
            ({{ formatDate(lastCleanupTime) }})
          </p>
        </div>
      </el-alert>

      <!-- Statistics Cards -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total">
                <el-icon :size="32"><Warning /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalUnverified }}</div>
                <div class="stat-label">Tổng tài khoản chưa xác thực</div>
                <div class="stat-meta">
                  <el-tag size="small" type="info">Tất cả</el-tag>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="stat-card danger-stat">
            <div class="stat-content">
              <div class="stat-icon old">
                <el-icon :size="32"><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.oldUnverified }}</div>
                <div class="stat-label">Quá {{ stats.cutoffHours }}h (sẽ bị xóa)</div>
                <div class="stat-meta">
                  <el-tag size="small" type="danger">{{ deletionPercentage }}%</el-tag>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon cleanup">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ lastCleanupCount }}</div>
                <div class="stat-label">Đã xóa lần cleanup trước</div>
                <div class="stat-meta" v-if="lastCleanupTime">
                  <el-tag size="small" type="success">
                    {{ formatRelativeTime(lastCleanupTime) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Preview Card -->
      <el-card shadow="never" class="preview-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon><View /></el-icon>
              Preview Cleanup
            </span>
            <el-button
              type="primary"
              :icon="RefreshRight"
              @click="fetchPreview"
              size="small"
            >
              Làm mới Preview
            </el-button>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="Số tài khoản sẽ bị xóa">
            <el-tag
              :type="preview.willBeDeleted > 0 ? 'danger' : 'success'"
              size="large"
              effect="dark"
            >
              {{ preview.willBeDeleted }} tài khoản
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Thời gian cutoff">
            <el-text type="info">
              {{ formatDate(preview.cutoffDate) }}
            </el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Giới hạn giờ">
            <el-tag type="warning">{{ preview.cutoffHours }} giờ</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Cập nhật lúc">
            <el-text type="info">
              {{ formatDate(preview.timestamp) }}
            </el-text>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="preview-actions">
          <el-space direction="vertical" alignment="flex-start" :size="12">
            <el-button
              type="danger"
              :icon="Delete"
              :loading="cleanupLoading"
              :disabled="preview.willBeDeleted === 0"
              @click="handleRunCleanup"
              size="large"
            >
              Xóa {{ preview.willBeDeleted }} tài khoản ngay
            </el-button>
            <el-text type="info" size="small">
              * Chỉ xóa tài khoản chưa xác thực quá {{ stats.cutoffHours }} giờ
            </el-text>
            <el-text v-if="preview.willBeDeleted === 0" type="success" size="small">
              ✅ Không có tài khoản nào cần xóa
            </el-text>
          </el-space>
        </div>
      </el-card>

      <!-- Unverified Users Table -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon><List /></el-icon>
              Danh sách tài khoản chưa xác thực
              <el-tag type="warning" size="small" style="margin-left: 8px">
                {{ unverifiedUsers.length }}
              </el-tag>
            </span>
            <el-button
              :icon="RefreshRight"
              @click="fetchUnverifiedUsers"
              :loading="loading"
              size="small"
            >
              Làm mới
            </el-button>
          </div>
        </template>

        <!-- Empty State -->
        <el-empty
          v-if="!loading && unverifiedUsers.length === 0"
          description="Không có tài khoản chưa xác thực"
          :image-size="120"
        >
          <el-button type="primary" @click="fetchUnverifiedUsers">
            Tải lại
          </el-button>
        </el-empty>

        <!-- Table -->
        <el-table
          v-else
          v-loading="loading"
          :data="paginatedUsers"
          stripe
          style="width: 100%"
          :default-sort="{ prop: 'hoursOld', order: 'descending' }"
        >
          <el-table-column type="index" label="#" width="60" />

          <el-table-column prop="username" label="Username" width="200" />

          <el-table-column prop="email" label="Email" min-width="250" />

          <el-table-column label="Ngày tạo" width="180" sortable>
            <template #default="{ row }">
              {{ formatDate(row.createdDate) }}
            </template>
          </el-table-column>

          <el-table-column
            label="Tuổi tài khoản"
            width="180"
            align="center"
            prop="hoursOld"
            sortable
          >
            <template #default="{ row }">
              <el-tag
                :type="getAgeType(row.hoursOld)"
                size="small"
                effect="dark"
              >
                {{ getAgeText(row.hoursOld) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái" width="150" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.hoursOld >= stats.cutoffHours ? 'danger' : 'success'"
                size="small"
                effect="plain"
              >
                {{ row.hoursOld >= stats.cutoffHours ? '⚠️ Sẽ bị xóa' : '✅ Còn thời gian' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination" v-if="unverifiedUsers.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="unverifiedUsers.length"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import {
  Delete,
  RefreshRight,
  Warning,
  Clock,
  CircleCheck,
  View,
  List,
} from '@element-plus/icons-vue'
import { useCleanupManagement } from '@/composables/user/useCleanupManagement'

// Use composable
const {
  loading,
  cleanupLoading,
  unverifiedUsers,
  currentPage,
  pageSize,
  lastCleanupCount,
  lastCleanupTime,
  stats,
  preview,
  paginatedUsers,
  deletionPercentage,
  cleanupStatus,
  fetchPreview,
  fetchUnverifiedUsers,
  handleRunCleanup,
  formatDate,
  formatRelativeTime,
  getAgeType,
  getAgeText,
  initialize,
  refreshAll,
} = useCleanupManagement()

// Lifecycle
onMounted(async () => {
  await initialize()
})
</script>

<style scoped>
.cleanup-management-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 4px 0;
  color: #303133;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* Alert Content */
.alert-content {
  line-height: 1.8;
}

.alert-content p {
  margin: 4px 0;
}

.last-cleanup {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.3);
}

/* Statistics Cards */
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  transition: all 0.3s;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card.danger-stat {
  border: 2px solid #f56c6c;
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

.stat-icon.total {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #e6a23c;
}

.stat-icon.old {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #f56c6c;
}

.stat-icon.cleanup {
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
  color: #409eff;
}

.stat-info {
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
  margin-bottom: 8px;
}

.stat-meta {
  margin-top: 8px;
}

/* Cards */
.preview-card,
.table-card {
  margin-bottom: 24px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

/* Preview Actions */
.preview-actions {
  margin-top: 16px;
}

/* Pagination */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions button {
    width: 100%;
  }

  .preview-actions {
    width: 100%;
  }

  .preview-actions button {
    width: 100%;
  }

  .stats-row .el-col {
    margin-bottom: 16px;
  }
}
</style>
