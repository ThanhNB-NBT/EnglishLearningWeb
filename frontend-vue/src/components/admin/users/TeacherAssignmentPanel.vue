<!-- src/components/admin/users/TeacherAssignmentPanel.vue - FIXED -->
<template>
  <div>
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">
          Phân quyền Topics cho Teacher
        </h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Quản lý quyền truy cập vào các topics theo module
        </p>
      </div>
      <el-button
        type="primary"
        :icon="Plus"
        @click="showAssignDialog"
        class="!rounded-lg font-bold"
      >
        Phân quyền mới
      </el-button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4"
      >
        <div
          class="w-12 h-12 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center"
        >
          <el-icon :size="24" class="text-blue-600 dark:text-blue-400"><Connection /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.total }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Tổng phân quyền</div>
        </div>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4"
      >
        <div
          class="w-12 h-12 rounded-lg bg-purple-50 dark:bg-purple-900/20 flex items-center justify-center"
        >
          <el-icon :size="24" class="text-purple-600 dark:text-purple-400"><Reading /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.grammar }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Grammar</div>
        </div>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4"
      >
        <div
          class="w-12 h-12 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-center"
        >
          <el-icon :size="24" class="text-green-600 dark:text-green-400"><Document /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.reading }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Reading</div>
        </div>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border shadow-sm flex items-center gap-4"
      >
        <div
          class="w-12 h-12 rounded-lg bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center"
        >
          <el-icon :size="24" class="text-orange-600 dark:text-orange-400"><Microphone /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.listening }}</div>
          <div class="text-xs text-gray-500 uppercase font-medium">Listening</div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="!border-gray-300 dark:!border-gray-700 !rounded-xl">
      <div class="flex flex-wrap gap-3 mb-5">
        <el-select
          v-model="filters.teacherId"
          placeholder="Chọn Teacher"
          clearable
          filterable
          class="!w-64"
          @change="handleFilter"
        >
          <el-option
            v-for="teacher in teachers"
            :key="teacher.id"
            :label="`${teacher.fullName || teacher.username} (@${teacher.username})`"
            :value="teacher.id"
          />
        </el-select>

        <el-select
          v-model="filters.moduleType"
          placeholder="Module"
          clearable
          class="!w-40"
          @change="handleFilter"
        >
          <el-option label="Grammar" value="GRAMMAR" />
          <el-option label="Reading" value="READING" />
          <el-option label="Listening" value="LISTENING" />
        </el-select>

        <el-select
          v-model="filters.isActive"
          placeholder="Trạng thái"
          clearable
          class="!w-40"
          @change="handleFilter"
        >
          <el-option label="Đang hoạt động" :value="true" />
          <el-option label="Đã vô hiệu" :value="false" />
        </el-select>

        <div class="flex-1"></div>

        <el-button :icon="RefreshRight" circle @click="resetFilters" />
      </div>

      <!-- Table -->
      <el-table v-loading="loading" :data="paginatedAssignments" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column label="Teacher" min-width="200">
          <template #default="{ row }">
            <div>
              <div class="font-bold text-gray-900 dark:text-white">{{ row.teacherFullName }}</div>
              <div class="text-xs text-gray-500">@{{ row.teacherUsername }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Topic" min-width="250">
          <template #default="{ row }">
            <div>
              <div class="font-semibold text-gray-800 dark:text-gray-200">{{ row.topicName }}</div>
              <el-tag :type="getModuleTypeColor(row.moduleType)" size="small" class="mt-1">
                {{ row.moduleType }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.isActive ? 'success' : 'info'"
              size="small"
              class="!rounded-full px-3"
            >
              {{ row.isActive ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Người phân quyền" width="150">
          <template #default="{ row }">
            <span class="text-sm text-gray-600 dark:text-gray-400"
              >@{{ row.assignedByUsername }}</span
            >
          </template>
        </el-table-column>

        <el-table-column label="Ngày phân quyền" width="120" sortable prop="assignedAt">
          <template #default="{ row }">
            <span class="text-xs">{{ formatDate(row.assignedAt).split(' ')[0] }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="Xác nhận thu hồi quyền?"
              confirm-button-text="Thu hồi"
              cancel-button-text="Hủy"
              @confirm="revokeAssignment(row)"
            >
              <template #reference>
                <el-button link type="danger" :icon="Delete">Thu hồi</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredAssignments.length"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- Assign Dialog -->
    <AssignTopicDialog
      v-model="assignDialogVisible"
      @assigned="handleAssigned"
    />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import {
  Plus,
  RefreshRight,
  Delete,
  Connection,
  Reading,
  Document,
  Microphone,
} from '@element-plus/icons-vue'
import { useTeacherAssignment } from '@/composables/user/useTeacherAssignment'
import AssignTopicDialog from './dialogs/AssignTopicDialog.vue'

const {
  loading,
  currentPage,
  pageSize,
  filters,
  stats,
  teachers,
  assignDialogVisible,
  filteredAssignments,
  paginatedAssignments,
  fetchAssignments,
  handleFilter,
  resetFilters,
  revokeAssignment,
  showAssignDialog,
  formatDate,
  getModuleTypeColor,
  initialize,
} = useTeacherAssignment()

/**
 * ✅ Handle assignment success
 */
const handleAssigned = async () => {
  console.log('✅ Assignment successful, refreshing data...')
  await fetchAssignments()
}

/**
 * ✅ Initialize on mount
 */
onMounted(async () => {
  console.log('✅ TeacherAssignmentPanel mounted - initializing...')
  await initialize()
})
</script>
