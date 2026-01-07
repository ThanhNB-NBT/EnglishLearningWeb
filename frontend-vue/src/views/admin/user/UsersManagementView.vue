<!-- src/views/admin/users/UsersManagementView.vue -->
<template>
  <div class="w-full h-full bg-gray-50 dark:bg-[#0f0f0f]">
    <el-tabs
      v-model="activeTab"
      type="border-card"
      class="users-tabs shadow-sm !border-none overflow-hidden"
    >
      <!-- Tab 1: USER LIST -->
      <el-tab-pane name="users" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><User /></el-icon>
            Quản lý User ({{ userStats.total }})
          </span>
        </template>
        <div class="p-4">
          <UserList />
        </div>
      </el-tab-pane>

      <!-- Tab 2: TEACHER LIST -->
      <el-tab-pane name="teachers" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><UserFilled /></el-icon>
            Quản lý Teacher ({{ teacherStats.total }})
          </span>
        </template>
        <div class="p-4">
          <TeacherList />
        </div>
      </el-tab-pane>

      <!-- Tab 3: TEACHER ASSIGNMENT -->
      <el-tab-pane name="assignments" lazy>
        <template #label>
          <span class="flex items-center gap-2 px-2 py-1">
            <el-icon><Connection /></el-icon>
            Phân quyền Topic ({{ assignmentStats.total }})
          </span>
        </template>
        <div class="p-4">
          <TeacherAssignmentPanel />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, UserFilled, Connection } from '@element-plus/icons-vue'
import UserList from '@/components/admin/users/UserList.vue'
import TeacherList from '@/components/admin/users/TeacherList.vue'
import TeacherAssignmentPanel from '@/components/admin/users/TeacherAssignmentPanel.vue'
import { useUsersManagement } from '@/composables/user/useUsersManagement'
import { useTeacherManagement } from '@/composables/user/useTeacherManagement'
import { useTeacherAssignment } from '@/composables/user/useTeacherAssignment'

const activeTab = ref('users')

// Stats from composables
const { stats: userStats, fetchUsers } = useUsersManagement()
const { stats: teacherStats, fetchTeachers } = useTeacherManagement()
const { stats: assignmentStats, fetchAssignments } = useTeacherAssignment()

// ✅ FIX: Chỉ fetch stats cho tabs, không fetch full data
// Data sẽ được fetch bởi từng component con khi chúng mount
onMounted(async () => {
  console.log('✅ UsersManagementView mounted - fetching stats only...')

  try {
    // Fetch minimal data for tab counters
    await Promise.all([
      fetchUsers(),
      fetchTeachers(),
      fetchAssignments()
    ])
  } catch (error) {
    console.error('❌ Error fetching stats:', error)
  }
})
</script>

<style scoped>
/* Same styles as GrammarManagementView */
:deep(.el-tabs__content) {
  padding: 0;
  background-color: var(--el-bg-color);
}

:deep(.el-tabs--border-card) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: var(--el-bg-color);
  color: var(--el-color-primary);
  font-weight: 600;
}

html.dark:deep(.el-tabs--border-card) {
  border-color: #333;
}

html.dark:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: #1a1a1a;
  border-bottom-color: #333;
}

html.dark:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  background-color: #141414;
  color: #409eff;
}
</style>
