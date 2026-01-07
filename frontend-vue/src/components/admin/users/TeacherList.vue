<!-- src/components/admin/users/TeacherList.vue -->
<template>
  <div>
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">Danh sách Teacher</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Quản lý tài khoản giáo viên (Teacher role)
        </p>
      </div>
      <el-button
        type="success"
        :icon="Plus"
        @click="showCreateTeacherDialog"
        class="!rounded-lg font-bold"
      >
        Tạo Teacher
      </el-button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-green-600 dark:text-green-400"><UserFilled /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.total }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Tổng Teacher</div>
        </div>
      </div>

      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-blue-600 dark:text-blue-400"><Connection /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.assigned }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Đã phân quyền</div>
        </div>
      </div>

      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-orange-600 dark:text-orange-400"><Warning /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.unassigned }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Chưa phân quyền</div>
        </div>
      </div>

      <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-gray-50 dark:bg-gray-900/20 flex items-center justify-center">
          <el-icon :size="24" class="text-gray-600 dark:text-gray-400"><Lock /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.inactive }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Đã khóa</div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="!border-gray-300 dark:!border-gray-700 !rounded-xl">
      <div class="flex flex-wrap gap-3 mb-5">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm teacher..."
          :prefix-icon="Search"
          clearable
          class="!w-64"
          @input="handleSearch"
        />

        <el-select v-model="filters.isActive" placeholder="Trạng thái" clearable class="!w-40" @change="handleFilter">
          <el-option label="Hoạt động" :value="true" />
          <el-option label="Đã khóa" :value="false" />
        </el-select>

        <el-select v-model="filters.hasAssignment" placeholder="Phân quyền" clearable class="!w-40" @change="handleFilter">
          <el-option label="Đã phân quyền" :value="true" />
          <el-option label="Chưa phân quyền" :value="false" />
        </el-select>

        <div class="flex-1"></div>

        <el-button :icon="RefreshRight" circle @click="resetFilters" />
      </div>

      <!-- Table -->
      <el-table v-loading="loading" :data="paginatedTeachers" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="username" label="Tài khoản" min-width="180" sortable>
          <template #default="{ row }">
            <div>
              <div class="font-bold text-gray-900 dark:text-white flex items-center gap-2">
                {{ row.username }}
                <el-tag type="success" size="small">T</el-tag>
              </div>
              <div class="text-xs text-gray-500">{{ row.email }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fullName" label="Họ tên" min-width="150" />

        <el-table-column label="Topics được gán" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.assignedTopicsCount > 0 ? 'primary' : 'info'" size="small">
              {{ row.assignedTopicsCount }} topics
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small" class="!rounded-full px-3">
              {{ row.isActive ? 'Active' : 'Locked' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Ngày tạo" width="120" sortable prop="createdAt">
          <template #default="{ row }">
            <span class="text-xs">{{ formatDate(row.createdAt).split(' ')[0] }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-tooltip content="Chi tiết" placement="top" :hide-after="0">
                <el-button link type="primary" :icon="View" @click="viewTeacherDetail(row)" />
              </el-tooltip>

              <el-tooltip content="Phân quyền" placement="top" :hide-after="0">
                <el-button link type="success" :icon="Connection" @click="assignTopics(row)" />
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
          :total="filteredTeachers.length"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- Teacher Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="Chi tiết Teacher"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedTeacher" class="space-y-6">
        <!-- Basic Info -->
        <div class="bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 p-5 rounded-xl">
          <div class="flex items-center gap-4">
            <el-avatar :size="80" :style="{ background: '#16a34a' }" class="shrink-0">
              <span class="text-2xl font-bold">{{ selectedTeacher.username?.charAt(0).toUpperCase() }}</span>
            </el-avatar>
            <div class="flex-1">
              <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-1">
                {{ selectedTeacher.fullName || selectedTeacher.username }}
              </h3>
              <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">@{{ selectedTeacher.username }}</p>
              <div class="flex gap-2">
                <el-tag type="success" size="small">TEACHER</el-tag>
                <el-tag :type="selectedTeacher.isActive ? 'success' : 'info'" size="small">
                  {{ selectedTeacher.isActive ? 'Active' : 'Locked' }}
                </el-tag>
                <el-tag v-if="selectedTeacher.emailVerified" type="primary" size="small">Verified</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- Contact Info -->
        <div class="space-y-3">
          <h4 class="text-sm font-bold text-gray-700 dark:text-gray-300 uppercase">Thông tin liên hệ</h4>
          <div class="grid grid-cols-1 gap-3">
            <div class="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
              <el-icon :size="20" class="text-gray-500"><Message /></el-icon>
              <div>
                <div class="text-xs text-gray-500 dark:text-gray-400">Email</div>
                <div class="font-medium text-gray-900 dark:text-white">{{ selectedTeacher.email }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Assignment Info -->
        <div class="space-y-3">
          <h4 class="text-sm font-bold text-gray-700 dark:text-gray-300 uppercase">Phân quyền</h4>
          <div class="grid grid-cols-2 gap-3">
            <div class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg text-center">
              <div class="text-3xl font-bold text-blue-600 dark:text-blue-400">
                {{ selectedTeacher.assignedTopicsCount }}
              </div>
              <div class="text-xs text-gray-600 dark:text-gray-400 mt-1">Topics được gán</div>
            </div>
            <div class="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg text-center">
              <div class="text-3xl font-bold text-green-600 dark:text-green-400">
                {{ selectedTeacher.assignments?.filter(a => a.isActive).length || 0 }}
              </div>
              <div class="text-xs text-gray-600 dark:text-gray-400 mt-1">Active assignments</div>
            </div>
          </div>
        </div>

        <!-- Assigned Topics List -->
        <div v-if="selectedTeacher.assignments && selectedTeacher.assignments.length > 0" class="space-y-3">
          <h4 class="text-sm font-bold text-gray-700 dark:text-gray-300 uppercase">Danh sách Topics</h4>
          <div class="max-h-48 overflow-y-auto space-y-2">
            <div
              v-for="assignment in selectedTeacher.assignments"
              :key="assignment.id"
              class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg"
            >
              <div class="flex-1">
                <div class="font-medium text-gray-900 dark:text-white">{{ assignment.topicTitle }}</div>
                <div class="text-xs text-gray-500 dark:text-gray-400">
                  {{ assignment.moduleType }} • Assigned {{ formatDate(assignment.assignedAt).split(' ')[0] }}
                </div>
              </div>
              <el-tag :type="assignment.isActive ? 'success' : 'info'" size="small">
                {{ assignment.isActive ? 'Active' : 'Inactive' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-else class="text-center py-8">
          <el-icon :size="48" class="text-gray-400 mb-2"><DocumentRemove /></el-icon>
          <p class="text-gray-500 dark:text-gray-400">Chưa có topic nào được phân quyền</p>
        </div>

        <!-- Account Info -->
        <div class="space-y-3">
          <h4 class="text-sm font-bold text-gray-700 dark:text-gray-300 uppercase">Thông tin tài khoản</h4>
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div>
              <span class="text-gray-500 dark:text-gray-400">Ngày tạo:</span>
              <span class="ml-2 font-medium text-gray-900 dark:text-white">
                {{ formatDate(selectedTeacher.createdAt) }}
              </span>
            </div>
            <div>
              <span class="text-gray-500 dark:text-gray-400">Cập nhật:</span>
              <span class="ml-2 font-medium text-gray-900 dark:text-white">
                {{ formatDate(selectedTeacher.updatedAt) }}
              </span>
            </div>
            <div>
              <span class="text-gray-500 dark:text-gray-400">Đăng nhập gần nhất:</span>
              <span class="ml-2 font-medium text-gray-900 dark:text-white">
                {{ selectedTeacher.lastLogin ? formatDate(selectedTeacher.lastLogin) : 'Chưa đăng nhập' }}
              </span>
            </div>
            <div>
              <span class="text-gray-500 dark:text-gray-400">User ID:</span>
              <span class="ml-2 font-mono text-xs font-medium text-gray-900 dark:text-white">
                #{{ selectedTeacher.id }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-between">
          <div>
            <el-button type="success" :icon="Connection" @click="assignTopicsFromDetail">
              Phân quyền Topics
            </el-button>
          </div>
          <div class="flex gap-2">
            <el-button @click="detailDialogVisible = false">Đóng</el-button>
            <el-button
              v-if="selectedTeacher?.isActive"
              type="warning"
              :icon="Lock"
              @click="blockTeacherFromDetail"
            >
              Khóa tài khoản
            </el-button>
            <el-button
              v-else
              type="success"
              :icon="Unlock"
              @click="unblockTeacherFromDetail"
            >
              Mở khóa
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- Other Dialogs -->
    <CreateTeacherDialog v-if="createTeacherDialogVisible" v-model="createTeacherDialogVisible" @created="fetchTeachers" />
    <AssignTopicDialog v-if="assignDialogVisible" v-model="assignDialogVisible" :teacher="selectedTeacher" @assigned="fetchTeachers" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, RefreshRight, View, Connection, More, Lock, Unlock, Delete, UserFilled, Warning, Message, DocumentRemove } from '@element-plus/icons-vue'
import { useTeacherManagement } from '@/composables/user/useTeacherManagement'
import CreateTeacherDialog from './dialogs/CreateTeacherDialog.vue'
import AssignTopicDialog from './dialogs/AssignTopicDialog.vue'

const {
  loading,
  searchQuery,
  currentPage,
  pageSize,
  filters,
  stats,
  createTeacherDialogVisible,
  selectedTeacher,
  filteredTeachers,
  paginatedTeachers,
  fetchTeachers,
  handleSearch,
  handleFilter,
  resetFilters,
  assignTopics,
  handleAction,
  showCreateTeacherDialog,
  formatDate,
  assignDialogVisible
} = useTeacherManagement()

// Teacher detail dialog
const detailDialogVisible = ref(false)

const viewTeacherDetail = (teacher) => {
  selectedTeacher.value = teacher
  detailDialogVisible.value = true
}

const assignTopicsFromDetail = () => {
  detailDialogVisible.value = false
  assignDialogVisible.value = true
}

const blockTeacherFromDetail = async () => {
  detailDialogVisible.value = false
  await handleAction('block', selectedTeacher.value)
}

const unblockTeacherFromDetail = async () => {
  detailDialogVisible.value = false
  await handleAction('unblock', selectedTeacher.value)
}

// ✅ Fetch teachers khi component mount
onMounted(async () => {
  console.log('✅ TeacherList mounted - fetching teachers...')
  await fetchTeachers()
})
</script>

<style scoped>
.text-danger { color: #f56c6c; }
</style>
