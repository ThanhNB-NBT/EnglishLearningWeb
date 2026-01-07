<!-- src/components/admin/users/UserList.vue - FIXED -->
<template>
  <div>
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">Danh sách người dùng</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Quản lý tài khoản người dùng
        </p>
      </div>
      <el-button
        type="primary"
        :icon="Plus"
        @click="showCreateAdminDialog"
        class="!rounded-lg font-bold"
      >
        Tạo Admin
      </el-button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-blue-600 dark:text-blue-400"><User /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.total }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Tổng user</div>
        </div>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4 cursor-pointer hover:border-blue-400 transition"
        @click="$router.push('/admin/cleanup-user')"
      >
        <div class="w-12 h-12 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-green-600 dark:text-green-400"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.active }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Đang hoạt động</div>
        </div>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4 cursor-pointer hover:border-red-400 transition"
        @click="$router.push('/admin/cleanup-user')"
      >
        <div class="w-12 h-12 rounded-lg bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-orange-600 dark:text-orange-400"><Warning /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.unverified }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Chưa xác thực</div>
        </div>
      </div>

      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-purple-50 dark:bg-purple-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-purple-600 dark:text-purple-400"><UserFilled /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.admins }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Admin</div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="!border-gray-300 dark:!border-gray-700 !rounded-xl">
      <div class="flex flex-wrap gap-3 mb-5">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm..."
          :prefix-icon="Search"
          clearable
          class="!w-64"
          @input="handleSearch"
        />

        <el-select v-model="filters.isActive" placeholder="Trạng thái" clearable class="!w-40" @change="handleFilter">
          <el-option label="Hoạt động" :value="true" />
          <el-option label="Đã khóa" :value="false" />
        </el-select>

        <el-select v-model="filters.englishLevel" placeholder="Trình độ" clearable class="!w-40" @change="handleFilter">
          <el-option label="A1" value="A1" />
          <el-option label="A2" value="A2" />
          <el-option label="B1" value="B1" />
          <el-option label="B2" value="B2" />
          <el-option label="C1" value="C1" />
          <el-option label="C2" value="C2" />
        </el-select>

        <div class="flex-1"></div>

        <el-button :icon="RefreshRight" circle @click="resetFilters" />
      </div>

      <!-- Table -->
      <el-table
        v-loading="loading"
        :data="paginatedUsers"
        border
        stripe
        :empty-text="loading ? 'Đang tải...' : 'Không có dữ liệu'"
      >

        <el-table-column prop="username" label="Tài khoản" min-width="180" sortable fixed="left">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <div>
                <div class="font-bold text-gray-900 dark:text-white">{{ row.username }}</div>
                <div class="text-xs text-gray-500 truncate max-w-[150px]">{{ row.email }}</div>
              </div>
              <el-tag v-if="row.role === 'ADMIN'" type="danger" size="small" class="ml-auto">A</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fullName" label="Họ tên" min-width="150" />

        <el-table-column label="Level" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.englishLevel)" size="small">{{ row.englishLevel || 'N/A' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small" class="!rounded-full px-3">
              {{ row.isActive ? 'Active' : 'Locked' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Streak" width="90" align="center">
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
                    <el-dropdown-item v-if="row.isActive" command="block" :icon="Lock">Khóa</el-dropdown-item>
                    <el-dropdown-item v-else command="unblock" :icon="Unlock">Mở khóa</el-dropdown-item>
                    <el-dropdown-item command="delete" :icon="Delete" divided class="text-danger">Xóa</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredUsers.length"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- Dialogs -->
    <UserDetailDialog v-model="detailDialogVisible" :user="selectedUser" />
    <CreateAdminDialog v-model="createAdminDialogVisible" @created="fetchUsers" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Plus, Search, RefreshRight, View, More, Lock, Unlock, Delete, User, UserFilled, CircleCheck, Warning } from '@element-plus/icons-vue'
import { useUsersManagement } from '@/composables/user/useUsersManagement'
import UserDetailDialog from './dialogs/UserDetailDialog.vue'
import CreateAdminDialog from './dialogs/CreateAdminDialog.vue'

const {
  loading,
  users, // ✅ ADD: Expose users for debugging
  searchQuery,
  currentPage,
  pageSize,
  filters,
  stats,
  detailDialogVisible,
  createAdminDialogVisible,
  selectedUser,
  filteredUsers,
  paginatedUsers,
  fetchUsers,
  handleSearch,
  handleFilter,
  resetFilters,
  viewUserDetail,
  handleAction,
  showCreateAdminDialog,
  formatDate,
  getLevelType
} = useUsersManagement()

// ✅ ADD: Fetch on mount with debug
onMounted(async () => {
  console.log('🚀 UserList mounted, fetching users...')
  await fetchUsers()
  console.log('📊 Users loaded:', users.value.length)
  console.log('📊 Filtered:', filteredUsers.value.length)
  console.log('📊 Paginated:', paginatedUsers.value.length)
})
</script>

<style scoped>
.text-danger { color: #f56c6c; }
</style>
