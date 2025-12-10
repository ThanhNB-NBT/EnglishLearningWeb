<template>
  <div class="w-full p-4 md:p-6">
    <div class="w-full">
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Dọn dẹp tài khoản rác</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1" v-if="stats">
            Tự động xóa tài khoản chưa xác thực sau {{ stats.cutoffHours }} giờ
          </p>
        </div>
        <div class="flex gap-2">
          <el-button :icon="RefreshRight" @click="refreshAll" :loading="loading" class="!rounded-lg">
            Làm mới
          </el-button>
          <el-button
            type="danger"
            :icon="Delete"
            :loading="cleanupLoading"
            :disabled="!preview || preview.willBeDeleted === 0"
            @click="handleRunCleanup"
            class="!rounded-lg font-bold"
          >
            Chạy Cleanup ({{ preview?.willBeDeleted || 0 }})
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="stats"
        :type="cleanupStatus"
        :closable="false"
        show-icon
        class="!mb-6 !bg-blue-50 dark:!bg-blue-900/20 !border-blue-200 dark:!border-blue-800"
      >
        <template #title>
          <span class="font-bold text-blue-800 dark:text-blue-300">Trạng thái Cleanup</span>
        </template>
        <div class="text-sm text-blue-700 dark:text-blue-300 mt-1">
          <p>Hệ thống tự động xóa tài khoản chưa xác thực quá <b>{{ stats.cutoffHours }} giờ</b>.</p>
          <p>Lịch chạy tự động: <b>2:00 AM</b> hàng ngày.</p>
          <p v-if="lastCleanupTime" class="mt-1 pt-1 border-t border-blue-200 dark:border-blue-800 opacity-90">
            Lần chạy cuối: {{ formatRelativeTime(lastCleanupTime) }} ({{ formatDate(lastCleanupTime) }})
          </p>
        </div>
      </el-alert>

      <div v-if="stats" class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4">
          <div class="w-12 h-12 rounded-lg bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center text-orange-600 dark:text-orange-400">
            <el-icon :size="24"><Warning /></el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stats.totalUnverified }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Chưa xác thực</div>
          </div>
        </div>

        <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-red-300 dark:border-red-800 shadow-sm flex items-center gap-4 relative overflow-hidden">
          <div class="absolute right-0 top-0 p-2 opacity-10">
             <el-icon :size="100" class="text-red-500"><Delete /></el-icon>
          </div>
          <div class="w-12 h-12 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center text-red-600 dark:text-red-400 z-10">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="z-10">
            <div class="text-2xl font-bold text-red-600 dark:text-red-400">{{ stats.oldUnverified }}</div>
            <div class="text-xs text-red-800 dark:text-red-300 uppercase font-bold">Sẽ bị xóa (>{{ stats.cutoffHours }}h)</div>
            <div class="text-[10px] text-gray-500 mt-1">Chiếm {{ deletionPercentage }}% tổng số</div>
          </div>
        </div>

        <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm flex items-center gap-4">
          <div class="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400">
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ lastCleanupCount }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 uppercase font-medium">Đã xóa lần trước</div>
          </div>
        </div>
      </div>

      <div v-if="preview" class="mb-6 bg-white dark:bg-[#1d1d1d] rounded-xl border border-gray-300 dark:border-gray-700 shadow-sm p-6">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-bold text-gray-800 dark:text-white flex items-center gap-2">
            <el-icon class="text-blue-500"><View /></el-icon> Xem trước (Preview)
          </h3>
          <el-button size="small" :icon="RefreshRight" @click="fetchPreview">Cập nhật Preview</el-button>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
           <div class="p-3 bg-gray-50 dark:bg-[#252525] rounded-lg">
              <div class="text-gray-500 mb-1">Số lượng sẽ xóa</div>
              <div class="font-bold text-lg" :class="preview.willBeDeleted > 0 ? 'text-red-600' : 'text-green-600'">
                 {{ preview.willBeDeleted }} tài khoản
              </div>
           </div>
           <div class="p-3 bg-gray-50 dark:bg-[#252525] rounded-lg">
              <div class="text-gray-500 mb-1">Mốc thời gian (Cutoff)</div>
              <div class="font-bold text-gray-800 dark:text-gray-200">{{ formatDate(preview.cutoffDate) }}</div>
           </div>
           <div class="p-3 bg-gray-50 dark:bg-[#252525] rounded-lg">
              <div class="text-gray-500 mb-1">Giới hạn</div>
              <div class="font-bold text-gray-800 dark:text-gray-200">{{ preview.cutoffHours }} giờ</div>
           </div>
           <div class="p-3 bg-gray-50 dark:bg-[#252525] rounded-lg">
              <div class="text-gray-500 mb-1">Cập nhật lúc</div>
              <div class="font-bold text-gray-800 dark:text-gray-200">{{ formatDate(preview.timestamp) }}</div>
           </div>
        </div>
      </div>

      <el-card shadow="never" class="!border-gray-300 dark:!border-gray-700 !rounded-xl !overflow-visible" :body-style="{ padding: '0px' }">
        <div class="p-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-[#252525] flex justify-between items-center">
           <h3 class="font-bold text-gray-700 dark:text-gray-200 flex items-center gap-2">
             <el-icon><List /></el-icon> Danh sách tài khoản rác
             <el-tag type="warning" effect="dark" round size="small">{{ unverifiedUsers.length }}</el-tag>
           </h3>
           <el-button size="small" :icon="RefreshRight" @click="fetchUnverifiedUsers" :loading="loading" circle />
        </div>

        <el-table
          v-loading="loading"
          :data="paginatedUsers"
          style="width: 100%"
          stripe
          :header-cell-style="{ background: '#f9fafb', color: '#6b7280', fontWeight: '600' }"
        >
          <el-table-column type="index" label="#" width="60" align="center" />

          <el-table-column prop="username" label="Tài khoản" min-width="200">
             <template #default="{ row }">
                <div>
                   <div class="font-bold text-gray-800 dark:text-white">{{ row.username }}</div>
                   <div class="text-xs text-gray-500">{{ row.email }}</div>
                </div>
             </template>
          </el-table-column>

          <el-table-column label="Ngày tạo" width="180" sortable prop="createdDate">
            <template #default="{ row }">
              {{ formatDate(row.createdDate) }}
            </template>
          </el-table-column>

          <el-table-column label="Tuổi (Giờ)" width="150" align="center" prop="hoursOld" sortable>
            <template #default="{ row }">
              <el-tag :type="getAgeType(row.hoursOld)" effect="light">
                {{ getAgeText(row.hoursOld) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái" width="180" align="center">
            <template #default="{ row }">
              <span
                class="px-2 py-1 rounded text-xs font-bold border"
                :class="row.hoursOld >= (stats?.cutoffHours || 48)
                  ? 'bg-red-50 text-red-600 border-red-200 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800'
                  : 'bg-green-50 text-green-600 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800'"
              >
                {{ row.hoursOld >= (stats?.cutoffHours || 48) ? '⚠️ Sẽ bị xóa' : '✅ An toàn' }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div class="p-4 border-t border-gray-200 dark:border-gray-700 flex justify-end" v-if="unverifiedUsers.length > 0">
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

onMounted(async () => {
  await initialize()
})
</script>
