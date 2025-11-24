<template>
  <div class="users-management-page">
    <div class="container">
      <!-- Page Header -->
      <div class="page-header">
        <h1 class="page-title">Quản lý người dùng</h1>
        <el-button type="primary" :icon="Plus" @click="showCreateAdminDialog">
          Tạo tài khoản Admin
        </el-button>
      </div>

      <!-- Statistics Cards -->
      <el-row :gutter="24" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total">
                <el-icon :size="32">
                  <User />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.total }}</div>
                <div class="stat-label">Tổng người dùng</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" class="stat-card" @click="$router.push('/admin/cleanup-user')">
            <div class="stat-content">
              <div class="stat-icon active">
                <el-icon :size="32">
                  <CircleCheck />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.active }}</div>
                <div class="stat-label">Đang hoạt động</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" class="stat-card" @click="$router.push('/admin/cleanup-user')">
            <div class="stat-content">
              <div class="stat-icon unverified">
                <el-icon :size="32">
                  <Warning />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.unverified }}</div>
                <div class="stat-label">Chưa xác thực</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon admin">
                <el-icon :size="32">
                  <UserFilled />
                </el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.admins }}</div>
                <div class="stat-label">Quản trị viên</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Main Table Card -->
      <el-card shadow="never" class="table-card">
        <!-- Filters & Search -->
        <div class="table-header">
          <div class="filters">
            <el-input v-model="searchQuery" placeholder="Tìm kiếm theo username, email, tên..." :prefix-icon="Search"
              clearable style="width: 300px" @input="handleSearch" />

            <el-select v-model="filters.role" placeholder="Vai trò" clearable style="width: 150px"
              @change="handleFilter">
              <el-option label="User" value="USER" />
              <el-option label="Admin" value="ADMIN" />
            </el-select>

            <el-select v-model="filters.isActive" placeholder="Trạng thái" clearable style="width: 150px"
              @change="handleFilter">
              <el-option label="Đang hoạt động" :value="true" />
              <el-option label="Đã khóa" :value="false" />
            </el-select>

            <el-select v-model="filters.isVerified" placeholder="Xác thực" clearable style="width: 150px"
              @change="handleFilter">
              <el-option label="Đã xác thực" :value="true" />
              <el-option label="Chưa xác thực" :value="false" />
            </el-select>

            <el-select v-model="filters.englishLevel" placeholder="Trình độ" clearable style="width: 150px"
              @change="handleFilter">
              <el-option label="Beginner" value="BEGINNER" />
              <el-option label="Intermediate" value="INTERMEDIATE" />
              <el-option label="Advanced" value="ADVANCED" />
            </el-select>

            <el-button :icon="RefreshRight" @click="resetFilters">
              Làm mới
            </el-button>
          </div>
        </div>

        <!-- Users Table -->
        <el-table v-loading="loading" :data="filteredUsers" stripe style="width: 100%"
          :default-sort="{ prop: 'createdDate', order: 'descending' }">
          <el-table-column prop="username" label="Username" width="150" sortable>
            <template #default="{ row }">
              <div class="username-cell">
                <el-icon v-if="row.role === 'ADMIN'" style="color: #f56c6c">
                  <UserFilled />
                </el-icon>
                <span>{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="email" label="Email" min-width="200" sortable />

          <el-table-column prop="fullName" label="Họ và tên" min-width="180" sortable />

          <el-table-column prop="role" label="Vai trò" width="100" sortable>
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
                {{ row.role }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="englishLevel" label="Trình độ" width="120" sortable>
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.englishLevel)" size="small" effect="plain">
                {{ row.englishLevel }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
                {{ row.isActive ? 'Hoạt động' : 'Khóa' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Xác thực" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isVerified ? 'success' : 'warning'" size="small">
                {{ row.isVerified ? 'Đã xác thực' : 'Chưa xác thực' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="totalPoints" label="Điểm" width="100" sortable align="center" />

          <el-table-column prop="streakDays" label="Streak" width="100" sortable align="center">
            <template #default="{ row }">
              <span>🔥 {{ row.streakDays }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Ngày tạo" width="180" sortable>
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column label="Thao tác" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="viewUserDetail(row)" />

              <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
                <el-button size="small" :icon="More" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="row.isActive" command="block" :icon="Lock">
                      Khóa tài khoản
                    </el-dropdown-item>
                    <el-dropdown-item v-else command="unblock" :icon="Unlock">
                      Mở khóa
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" :icon="Delete" divided style="color: #f56c6c">
                      Xóa tài khoản
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
            :total="filteredUsers.length" layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>
      </el-card>

      <!-- View User Detail Dialog -->
      <el-dialog v-model="detailDialogVisible" title="Chi tiết người dùng" width="600px">
        <div v-if="selectedUser" class="user-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="ID">
              {{ selectedUser.id }}
            </el-descriptions-item>
            <el-descriptions-item label="Username">
              {{ selectedUser.username }}
            </el-descriptions-item>
            <el-descriptions-item label="Email" :span="2">
              {{ selectedUser.email }}
            </el-descriptions-item>
            <el-descriptions-item label="Họ và tên" :span="2">
              {{ selectedUser.fullName }}
            </el-descriptions-item>
            <el-descriptions-item label="Vai trò">
              <el-tag :type="selectedUser.role === 'ADMIN' ? 'danger' : 'primary'">
                {{ selectedUser.role }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Trình độ">
              <el-tag :type="getLevelType(selectedUser.englishLevel)">
                {{ selectedUser.englishLevel }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Trạng thái">
              <el-tag :type="selectedUser.isActive ? 'success' : 'info'">
                {{ selectedUser.isActive ? 'Hoạt động' : 'Đã khóa' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Xác thực">
              <el-tag :type="selectedUser.isVerified ? 'success' : 'warning'">
                {{ selectedUser.isVerified ? 'Đã xác thực' : 'Chưa xác thực' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Tổng điểm">
              {{ selectedUser.totalPoints }}
            </el-descriptions-item>
            <el-descriptions-item label="Streak Days">
              🔥 {{ selectedUser.streakDays }}
            </el-descriptions-item>
            <el-descriptions-item label="Ngày tạo">
              {{ formatDate(selectedUser.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="Đăng nhập cuối">
              {{ formatDate(selectedUser.lastLoginDate) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-dialog>

      <!-- Create Admin Dialog -->
      <el-dialog v-model="createAdminDialogVisible" title="Tạo tài khoản Admin" width="500px">
        <el-form ref="adminFormRef" :model="adminForm" :rules="adminFormRules" label-width="120px">
          <el-form-item label="Họ và tên" prop="fullName">
            <el-input v-model="adminForm.fullName" placeholder="Nhập họ và tên" />
          </el-form-item>
          <el-form-item label="Username" prop="username">
            <el-input v-model="adminForm.username" placeholder="Nhập username" />
          </el-form-item>

          <el-form-item label="Email" prop="email">
            <el-input v-model="adminForm.email" placeholder="Nhập email" />
          </el-form-item>

          <el-form-item label="Mật khẩu" prop="password">
            <el-input v-model="adminForm.password" type="password" placeholder="Nhập mật khẩu" show-password />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="createAdminDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreateAdmin(adminFormRef)">
            Tạo Admin
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Plus,
  Search,
  RefreshRight,
  View,
  More,
  Lock,
  Unlock,
  Delete,
  User,
  UserFilled,
  CircleCheck,
  Warning,
} from '@element-plus/icons-vue'
import { useUsersManagement } from '@/composables/user/useUsersManagement'

// Use composable
const {
  loading,
  submitting,
  searchQuery,
  currentPage,
  pageSize,
  filters,
  stats,
  detailDialogVisible,
  createAdminDialogVisible,
  selectedUser,
  adminForm,
  adminFormRules,
  filteredUsers,
  fetchUsers,
  handleSearch,
  handleFilter,
  resetFilters,
  viewUserDetail,
  handleAction,
  showCreateAdminDialog,
  handleCreateAdmin,
  handleSizeChange,
  handleCurrentChange,
  formatDate,
  getLevelType,
} = useUsersManagement()

// Refs
const adminFormRef = ref(null)

// Lifecycle
onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.users-management-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0;
  color: #303133;
}

/* Statistics Cards */
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon.active {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-icon.unverified {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #f56c6c;
}

.stat-icon.admin {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #409eff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* Table Card */
.table-card {
  border-radius: 8px;
}

.table-header {
  margin-bottom: 20px;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.username-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* User Detail */
.user-detail {
  padding: 20px 0;
}

/* Responsive */
@media (max-width: 1200px) {
  .container {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .filters {
    flex-direction: column;
  }

  .filters>* {
    width: 100% !important;
  }
}
</style>
