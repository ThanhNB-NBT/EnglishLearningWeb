<!-- src/components/admin/shared/questions/TaskGroupSelector.vue -->
<template>
  <div class="task-group-selector">
    <div class="flex items-center justify-between mb-2">
      <label class="text-xs font-bold text-gray-500 uppercase flex items-center gap-1">
        <el-icon><Collection /></el-icon>
        Task Group (Optional)
      </label>
      <el-button
        v-if="modelValue"
        @click="clearSelection"
        type="danger"
        text
        size="small"
        :icon="Close"
      >
        Clear
      </el-button>
    </div>

    <div class="flex gap-2">
      <el-select
        :model-value="modelValue"
        @update:model-value="handleSelect"
        placeholder="-- Standalone (No Task) --"
        clearable
        filterable
        class="flex-1"
        :loading="loading"
      >
        <el-option :value="null" label="-- Standalone (No Task) --" />
        <el-option
          v-for="tg in taskGroups"
          :key="tg.id"
          :value="tg.id"
          :label="`${tg.taskName} (${tg.questionCount || 0} questions)`"
        >
          <div class="flex items-center justify-between">
            <span>{{ tg.taskName }}</span>
            <el-tag size="small" type="info">{{ tg.questionCount || 0 }}</el-tag>
          </div>
        </el-option>
      </el-select>

      <el-button
        type="success"
        :icon="Plus"
        @click="showCreateDialog = true"
        :loading="loading"
      >
        New Task
      </el-button>
    </div>

    <!-- Selected Task Info -->
    <div
      v-if="selectedTaskGroup"
      class="mt-3 p-3 bg-blue-50 dark:bg-blue-900/10 rounded-lg border border-blue-200 dark:border-blue-800"
    >
      <div class="flex items-start justify-between mb-2">
        <div class="flex items-center gap-2">
          <el-icon class="text-blue-600"><Collection /></el-icon>
          <span class="font-bold text-blue-700 dark:text-blue-300">{{ selectedTaskGroup.taskName }}</span>
        </div>
        <el-tag size="small" type="primary">{{ selectedTaskGroup.questionCount || 0 }} questions</el-tag>
      </div>

      <div v-if="selectedTaskGroup.instruction" class="text-sm text-gray-600 dark:text-gray-400 italic">
        "{{ selectedTaskGroup.instruction }}"
      </div>
    </div>

    <!-- Create TaskGroup Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="Create New Task Group"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="newTaskForm" :rules="rules" label-position="top">
        <el-form-item label="Task Name" prop="taskName">
          <el-input
            v-model="newTaskForm.taskName"
            placeholder="e.g., Task 1: Multiple Choice"
            clearable
          />
        </el-form-item>

        <el-form-item label="Instruction" prop="instruction">
          <el-input
            v-model="newTaskForm.instruction"
            type="textarea"
            :rows="2"
            placeholder="e.g., Choose the correct answer (A, B, C or D)"
          />
        </el-form-item>

        <el-form-item label="Order Index (Optional)">
          <el-input-number
            v-model="newTaskForm.orderIndex"
            :min="1"
            class="!w-full"
            placeholder="Auto if empty"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateTask">
          Create
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Collection, Plus, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Number,
    default: null,
  },
  taskGroups: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'create-task'])

const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref(null)
const newTaskForm = ref({
  taskName: '',
  instruction: '',
  orderIndex: null,
})

const rules = {
  taskName: [{ required: true, message: 'Please enter task name', trigger: 'blur' }],
}

const selectedTaskGroup = computed(() => {
  if (!props.modelValue) return null
  return props.taskGroups.find(tg => tg.id === props.modelValue)
})

const handleSelect = (value) => {
  emit('update:modelValue', value)
}

const clearSelection = () => {
  emit('update:modelValue', null)
}

const handleCreateTask = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        emit('create-task', newTaskForm.value)
        newTaskForm.value = {
          taskName: '',
          instruction: '',
          orderIndex: null,
        }
        showCreateDialog.value = false
      } catch (error) {
        console.error('Failed to create task group:', error)
      } finally {
        creating.value = false
      }
    } else {
      ElMessage.warning('Please fill required fields')
      return false
    }
  })
}

watch(showCreateDialog, (val) => {
  if (!val) {
    newTaskForm.value = {
      taskName: '',
      instruction: '',
      orderIndex: null,
    }
    formRef.value?.clearValidate()
  }
})
</script>

<style scoped>
.task-group-selector {
  width: 100%;
}
</style>
