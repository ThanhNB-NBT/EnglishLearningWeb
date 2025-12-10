<template>
  <div class="w-full p-4 md:p-6">
    <div class="w-full">
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Quản lý người dùng</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Xem và quản lý tài khoản thành viên</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="showCreateAdminDialog" class="!rounded-lg font-bold">
          Tạo Admin
        </el-button>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div
          class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4">
          <div
            class="w-12 h-12 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400">
            <el-icon :size="24">
              <User />
            </el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.total }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Tổng user</div>
          </div>
        </div>

        <div
          class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4 cursor-pointer hover:border-blue-400 transition-colors"
          @click="$router.push('/admin/cleanup-user')">
          <div
            class="w-12 h-12 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-center text-green-600 dark:text-green-400">
            <el-icon :size="24">
              <CircleCheck />
            </el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.active }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Đang hoạt động</div>
          </div>
        </div>

        <div
          class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4 cursor-pointer hover:border-red-400 transition-colors"
          @click="$router.push('/admin/cleanup-user')">
          <div
            class="w-12 h-12 rounded-lg bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center text-orange-600 dark:text-orange-400">
            <el-icon :size="24">
              <Warning />
            </el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.unverified }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Chưa xác thực</div>
          </div>
        </div>

        <div
          class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4">
          <div
            class="w-12 h-12 rounded-lg bg-purple-50 dark:bg-purple-900/20 flex items-center justify-center text-purple-600 dark:text-purple-400">
            <el-icon :size="24">
              <UserFilled />
            </el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.admins }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Quản trị viên</div>
          </div>
        </div>
      </div>

      <el-card shadow="never" class="!border-gray-300 dark:!border-gray-700 !rounded-xl !overflow-visible">

        <div class="flex flex-wrap gap-3 mb-5">
          <el-input v-model="searchQuery" placeholder="Tìm kiếm..." :prefix-icon="Search" clearable class="!w-64"
            @input="handleSearch" />

          <el-select v-model="filters.role" placeholder="Vai trò" clearable class="!w-32" @change="handleFilter">
            <el-option label="User" value="USER" />
            <el-option label="Admin" value="ADMIN" />
          </el-select>

          <el-select v-model="filters.isActive" placeholder="Trạng thái" clearable class="!w-40" @change="handleFilter">
            <el-option label="Hoạt động" :value="true" />
            <el-option label="Đã khóa" :value="false" />
          </el-select>

          <el-select v-model="filters.englishLevel" placeholder="Trình độ" clearable class="!w-40"
            @change="handleFilter">
            <el-option label="Beginner" value="BEGINNER" />
            <el-option label="Intermediate" value="INTERMEDIATE" />
            <el-option label="Advanced" value="ADVANCED" />
          </el-select>

          <div class="flex-1"></div>

          <el-button :icon="RefreshRight" circle @click="resetFilters" />
        </div>

        <el-table v-loading="loading" :data="filteredUsers" style="width: 100%" border stripe>
          <el-table-column prop="username" label="Tài khoản" min-width="100" sortable fixed="left">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <div>
                  <div class="font-bold text-gray-900 dark:text-white leading-tight">{{ row.username }}</div>
                  <div class="text-xs text-gray-500 truncate max-w-[150px]">{{ row.email }}</div>
                </div>
                <el-tag v-if="row.role === 'ADMIN'" type="danger" size="small" effect="plain"
                  class="ml-auto">A</el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="fullName" label="Họ tên" min-width="150" />

          <el-table-column label="Level" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.englishLevel)" size="small" effect="light">{{ row.englishLevel }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'" size="small" effect="dark" class="!rounded-full px-3">
                {{ row.isActive ? 'Active' : 'Locked' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Streak" width="90" align="center" prop="streakDays">
            <template #default="{ row }">
              <div class="font-bold text-orange-500 flex items-center justify-center gap-1">
                🔥 {{ row.streakDays }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Ngày tạo" width="120" sortable prop="createdAt">
            <template #default="{ row }">
              <span class="text-xs">{{ formatDate(row.createdAt).split(' ')[0] }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Thao tác" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center justify-center gap-2">
                <el-tooltip content="Chi tiết" placement="top" :hide-after="0">
                  <el-button link type="primary" :icon="View" @click="viewUserDetail(row)" />
                </el-tooltip>

                <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
                  <el-button link :icon="More" class="!text-gray-600 dark:!text-gray-300" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="row.isActive" command="block" :icon="Lock" class="text-warning">Khóa tài
                        khoản</el-dropdown-item>
                      <el-dropdown-item v-else command="unblock" :icon="Unlock" class="text-success">Mở
                        khóa</el-dropdown-item>
                      <el-dropdown-item command="delete" :icon="Delete" divided class="text-danger">Xóa vĩnh
                        viễn</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
            :total="filteredUsers.length" layout="total, sizes, prev, pager, next" background
            @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>
      </el-card>

      <el-dialog v-model="detailDialogVisible" title="Thông tin chi tiết" width="500px" align-center
        class="!rounded-xl">
        <div v-if="selectedUser" class="p-2">
          <div class="flex items-center gap-4 mb-6">
            <el-avatar :size="64" class="!bg-blue-600 text-white !text-2xl font-bold">{{
              selectedUser.username.charAt(0).toUpperCase() }}</el-avatar>
            <div>
              <h3 class="text-lg font-bold">{{ selectedUser.fullName }}</h3>
              <p class="text-gray-500">@{{ selectedUser.username }}</p>
              <div class="flex gap-2 mt-1">
                <el-tag size="small">{{ selectedUser.role }}</el-tag>
                <el-tag size="small" :type="selectedUser.isVerified ? 'success' : 'warning'">{{ selectedUser.isVerified
                  ?
                  'Verified' : 'Unverified' }}</el-tag>
              </div>
            </div>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="Email">{{ selectedUser.email }}</el-descriptions-item>
            <el-descriptions-item label="Ngày tham gia">{{ formatDate(selectedUser.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="Đăng nhập cuối">{{ formatDate(selectedUser.lastLoginDate)
              }}</el-descriptions-item>
            <el-descriptions-item label="Tổng điểm"><span class="font-bold text-blue-600">{{ selectedUser.totalPoints
                }}</span></el-descriptions-item>
          </el-descriptions>
        </div>
      </el-dialog>

      <el-dialog v-model="createAdminDialogVisible" title="Thêm quản trị viên" width="450px" align-center
        class="!rounded-xl">
        <el-form ref="adminFormRef" :model="adminForm" :rules="adminFormRules" label-position="top">
          <el-form-item label="Họ tên" prop="fullName">
            <el-input v-model="adminForm.fullName" placeholder="Nguyen Van A" />
          </el-form-item>
          <el-form-item label="Username" prop="username">
            <el-input v-model="adminForm.username" placeholder="admin_new" />
          </el-form-item>
          <el-form-item label="Email" prop="email">
            <el-input v-model="adminForm.email" placeholder="admin@example.com" />
          </el-form-item>
          <el-form-item label="Mật khẩu" prop="password">
            <el-input v-model="adminForm.password" type="password" show-password placeholder="••••••••" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createAdminDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreateAdmin(adminFormRef)">Xác nhận</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, RefreshRight, View, More, Lock, Unlock, Delete, User, UserFilled, CircleCheck, Warning } from '@element-plus/icons-vue'
import { useUsersManagement } from '@/composables/user/useUsersManagement'

const {
  loading, submitting, searchQuery, currentPage, pageSize, filters, stats,
  detailDialogVisible, createAdminDialogVisible, selectedUser, adminForm, adminFormRules,
  filteredUsers, fetchUsers, handleSearch, handleFilter, resetFilters, viewUserDetail,
  handleAction, showCreateAdminDialog, handleCreateAdmin, handleSizeChange, handleCurrentChange,
  formatDate, getLevelType
} = useUsersManagement()

const adminFormRef = ref(null)

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.text-warning {
  color: #e6a23c;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

/* FIX: Bỏ overflow-x: auto ở parent của bảng
  để el-table (đã có cơ chế scroll riêng) tính toán được fixed column
*/
:deep(.el-card__body) {
  padding: 20px;
}
</style>
