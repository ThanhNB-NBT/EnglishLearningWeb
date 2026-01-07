<!-- src/components/admin/shared/questions/TaskGroupDisplay.vue - FIXED -->
<template>
  <div class="task-group-display">
    <!-- Stats Header -->
    <div v-if="showStats && taskStats" class="mb-4 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <el-icon class="text-blue-600"><Grid /></el-icon>
          <span class="font-bold text-blue-700 dark:text-blue-300">Task Structure Summary</span>
        </div>
        <el-tag type="primary" size="small">{{ taskStats.taskCount }} tasks</el-tag>
      </div>
    </div>

    <!-- Task Groups -->
    <div v-if="taskGroups.length > 0" class="space-y-4 mb-6">
      <div
        v-for="task in sortedTaskGroups"
        :key="task.taskName"
        class="task-group border border-gray-300 dark:border-gray-700 rounded-xl overflow-hidden bg-white dark:bg-[#1d1d1d] shadow-sm"
      >
        <!-- Task Header -->
        <div
          class="task-header px-4 py-3 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 border-b border-gray-200 dark:border-gray-700 cursor-pointer select-none"
          @click="toggleTask(task.taskName)"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <!-- Expand/Collapse Icon -->
              <el-icon class="text-blue-600 transition-transform" :class="{ 'rotate-90': expandedTasks[task.taskName] }">
                <ArrowRight />
              </el-icon>

              <!-- Task Name -->
              <div class="flex items-center gap-2">
                <el-icon class="text-blue-600"><Collection /></el-icon>
                <span class="font-bold text-gray-800 dark:text-white text-base">
                  {{ task.taskName }}
                </span>
              </div>

              <!-- Task Instruction -->
              <span v-if="task.taskInstruction" class="text-sm text-gray-600 dark:text-gray-400 italic ml-2">
                {{ task.taskInstruction }}
              </span>
            </div>

            <!-- Task Stats -->
            <div class="flex items-center gap-3">
              <el-tag size="small" type="info">
                {{ task.questions.length }} questions
              </el-tag>
              <el-tag size="small" type="warning">
                {{ calculateTaskPoints(task) }} points
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Task Questions (Collapsible) -->
        <el-collapse-transition>
          <div v-show="expandedTasks[task.taskName]">
            <el-table
              :data="task.questions"
              style="width: 100%"
              :show-header="showTableHeader"
              @selection-change="(val) => handleSelectionChange(val, task.taskName)"
            >
              <el-table-column v-if="selectable" type="selection" width="40" align="center" />

              <el-table-column label="STT" width="60" align="center">
                <template #default="{ row }">
                  <span class="text-gray-500 font-mono text-xs">{{ row.orderIndex }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Nội dung câu hỏi" min-width="350">
                <template #default="{ row }">
                  <div class="py-2 flex flex-col gap-1.5 cursor-pointer" @click="$emit('view-detail', row)">
                    <div class="flex items-center gap-2">
                      <span
                        class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border"
                        :class="getQuestionTypeClass(row.questionType)"
                      >
                        {{ row.questionType.replace('_', ' ') }}
                      </span>

                      <span class="text-[11px] font-bold text-orange-600 bg-orange-50 dark:bg-orange-900/20 dark:text-orange-400 px-1.5 rounded border border-orange-200 dark:border-orange-800">
                        +{{ row.points }}đ
                      </span>
                    </div>

                    <div class="text-sm font-medium text-gray-800 dark:text-gray-200 line-clamp-2 hover:text-blue-600 transition-colors">
                      {{ truncateHtml(row.questionText, 150) }}
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="Đáp án" min-width="200">
                <template #default="{ row }">
                  <div class="text-xs text-gray-500 truncate max-w-[300px]">
                    <span v-html="getAnswerPreview(row)"></span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="" width="60" align="center" fixed="right">
                <template #default="{ row }">
                  <el-dropdown trigger="click" @command="(cmd) => $emit('action', cmd, row)">
                    <span class="cursor-pointer flex justify-center items-center p-2">
                      <el-icon :size="18">
                        <MoreFilled />
                      </el-icon>
                    </span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit" :icon="Edit">Chỉnh sửa</el-dropdown-item>
                        <el-dropdown-item command="delete" :icon="Delete" divided class="!text-red-500">
                          Xóa
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- Standalone Questions -->
    <div v-if="standaloneQuestions && standaloneQuestions.length > 0" class="standalone-section">
      <div class="mb-3 px-4 py-2 bg-gray-100 dark:bg-gray-800 rounded-lg border border-gray-300 dark:border-gray-700">
        <div class="flex items-center gap-2">
          <el-icon class="text-gray-600"><Document /></el-icon>
          <span class="font-bold text-gray-700 dark:text-gray-300">Standalone Questions</span>
          <el-tag size="small" type="info">{{ standaloneQuestions.length }}</el-tag>
        </div>
      </div>

      <el-table
        :data="standaloneQuestions"
        style="width: 100%"
        border
        stripe
        @selection-change="(val) => handleSelectionChange(val, 'standalone')"
      >
        <el-table-column v-if="selectable" type="selection" width="40" align="center" />

        <el-table-column label="STT" width="60" align="center">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-xs">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung câu hỏi" min-width="350">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-1.5 cursor-pointer" @click="$emit('view-detail', row)">
              <div class="flex items-center gap-2">
                <span
                  class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border"
                  :class="getQuestionTypeClass(row.questionType)"
                >
                  {{ row.questionType.replace('_', ' ') }}
                </span>

                <span class="text-[11px] font-bold text-orange-600 bg-orange-50 dark:bg-orange-900/20 dark:text-orange-400 px-1.5 rounded border border-orange-200 dark:border-orange-800">
                  +{{ row.points }}đ
                </span>
              </div>

              <div class="text-sm font-medium text-gray-800 dark:text-gray-200 line-clamp-2 hover:text-blue-600 transition-colors">
                {{ truncateHtml(row.questionText, 150) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Đáp án" min-width="200">
          <template #default="{ row }">
            <div class="text-xs text-gray-500 truncate max-w-[300px]">
              <span v-html="getAnswerPreview(row)"></span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="" width="60" align="center" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(cmd) => $emit('action', cmd, row)">
              <span class="cursor-pointer flex justify-center items-center p-2">
                <el-icon :size="18">
                  <MoreFilled />
                </el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="Edit">Chỉnh sửa</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided class="!text-red-500">
                    Xóa
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  ArrowRight,
  Collection,
  Document,
  Grid,
  Edit,
  Delete,
  MoreFilled,
} from '@element-plus/icons-vue'
import { useQuestionUtils } from '@/composables/questions/useQuestionUtils' // ✅ FIX: Import composable

const props = defineProps({
  taskGroups: {
    type: Array,
    default: () => [],
  },
  standaloneQuestions: {
    type: Array,
    default: () => [],
  },
  taskStats: {
    type: Object,
    default: null,
  },
  showStats: {
    type: Boolean,
    default: true,
  },
  showTableHeader: {
    type: Boolean,
    default: false,
  },
  selectable: {
    type: Boolean,
    default: true,
  },
  defaultExpanded: {
    type: Boolean,
    default: true,
  },
})

defineEmits(['view-detail', 'action', 'selection-change'])

// ✅ FIX: Use composable
const { getQuestionTypeClass, getAnswerPreview, truncateHtml } = useQuestionUtils()

// Expanded state for each task
const expandedTasks = ref({})

// Sort task groups by taskOrder
const sortedTaskGroups = computed(() => {
  return [...props.taskGroups].sort((a, b) => (a.taskOrder || 0) - (b.taskOrder || 0))
})

// Initialize expanded state
const initializeExpandedState = () => {
  sortedTaskGroups.value.forEach(task => {
    expandedTasks.value[task.taskName] = props.defaultExpanded
  })
}

// Toggle task expansion
const toggleTask = (taskName) => {
  expandedTasks.value[taskName] = !expandedTasks.value[taskName]
}

// Calculate total points for a task
const calculateTaskPoints = (task) => {
  return task.questions.reduce((sum, q) => sum + (q.points || 0), 0)
}

// Handle selection change
const handleSelectionChange = (selection, groupName) => {
  console.log('Selection changed:', { groupName, count: selection.length })
}

// Initialize on mount
initializeExpandedState()
</script>

<style scoped>
.rotate-90 {
  transform: rotate(90deg);
}

.task-group-display :deep(.el-table) {
  border-radius: 0;
}

.task-group-display :deep(.el-table__header th) {
  background-color: #f9fafb;
  color: #6b7280;
  font-weight: 600;
}

html.dark .task-group-display :deep(.el-table__header th) {
  background-color: #252525;
  color: #a3a3a3;
  border-bottom-color: #333;
}

html.dark .task-group-display :deep(.el-table__row td) {
  background-color: #1d1d1d;
  border-bottom-color: #333;
}
</style>
