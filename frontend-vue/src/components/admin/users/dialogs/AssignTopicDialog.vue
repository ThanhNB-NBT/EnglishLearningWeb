<!-- src/components/admin/users/dialogs/AssignTopicDialog.vue - FIXED -->
<template>
  <el-dialog
    v-model="visible"
    title="Phân quyền Topic cho Teacher"
    width="600px"
    align-center
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <!-- Teacher Selection -->
      <el-form-item label="Chọn Teacher" prop="teacherId">
        <el-select
          v-model="form.teacherId"
          placeholder="Chọn giáo viên"
          filterable
          class="w-full"
          :loading="teachersLoading"
        >
          <el-option
            v-for="teacher in teachers"
            :key="teacher.id"
            :label="`${teacher.fullName || teacher.username} (@${teacher.username})`"
            :value="teacher.id"
          >
            <div class="flex items-center justify-between">
              <span>{{ teacher.fullName || teacher.username }}</span>
              <el-tag type="success" size="small">{{ teacher.assignedTopicsCount || 0 }} topics</el-tag>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- Module Type Selection -->
      <el-form-item label="Module" prop="moduleType">
        <el-select
          v-model="form.moduleType"
          placeholder="Chọn module"
          @change="handleModuleChange"
          class="w-full"
        >
          <el-option label="Grammar" value="GRAMMAR">
            <div class="flex items-center gap-2">
              <el-icon><Reading /></el-icon>
              <span>Grammar</span>
            </div>
          </el-option>
          <el-option label="Reading" value="READING">
            <div class="flex items-center gap-2">
              <el-icon><Document /></el-icon>
              <span>Reading</span>
            </div>
          </el-option>
          <el-option label="Listening" value="LISTENING">
            <div class="flex items-center gap-2">
              <el-icon><Microphone /></el-icon>
              <span>Listening</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- Topic Selection -->
      <el-form-item label="Chọn Topic" prop="topicId">
        <el-select
          v-model="form.topicId"
          placeholder="Chọn topic"
          filterable
          :loading="topicsLoading"
          :disabled="!form.moduleType"
          class="w-full"
        >
          <el-option
            v-for="topic in availableTopics"
            :key="topic.id"
            :label="topic.name"
            :value="topic.id"
          >
            <div class="flex items-center justify-between">
              <span>{{ topic.name }}</span>
              <el-tag :type="topic.isActive ? 'success' : 'info'" size="small">
                {{ topic.isActive ? 'Active' : 'Inactive' }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <span class="text-sm">Teacher sẽ có quyền quản lý Lessons và Questions trong Topic này</span>
        </template>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">Hủy</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        Phân quyền
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, Document, Microphone } from '@element-plus/icons-vue'
import { teacherAPI } from '@/api/modules/teacher.api'
import { userAPI } from '@/api'
import { topicAPI } from '@/api/modules/topic.api'

const props = defineProps({
  modelValue: Boolean,
  teacher: {
    type: Object,
    default: null,
  }
})

const emit = defineEmits(['update:modelValue', 'assigned'])

const visible = ref(props.modelValue)
const formRef = ref(null)
const submitting = ref(false)
const topicsLoading = ref(false)
const teachersLoading = ref(false)

const teachers = ref([])
const allTopics = ref([])

const form = reactive({
  teacherId: null,
  moduleType: null,
  topicId: null,
})

const rules = {
  teacherId: [{ required: true, message: 'Vui lòng chọn giáo viên', trigger: 'change' }],
  moduleType: [{ required: true, message: 'Vui lòng chọn module', trigger: 'change' }],
  topicId: [{ required: true, message: 'Vui lòng chọn topic', trigger: 'change' }],
}

// Computed: Filter topics by selected module
const availableTopics = computed(() => {
  if (!form.moduleType) return []
  return allTopics.value.filter((t) => t.moduleType === form.moduleType)
})

watch(
  () => props.modelValue,
  async (val) => {
    visible.value = val
    if (val) {
      await Promise.all([fetchTeachers(), fetchAllTopics()])

      // ✅ Pre-fill teacher if passed from TeacherList
      if (props.teacher) {
        form.teacherId = props.teacher.id
      }
    }
  },
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

/**
 * ✅ Fetch teachers với assignments count
 */
const fetchTeachers = async () => {
  teachersLoading.value = true
  try {
    console.log('🔄 Fetching teachers...')
    const response = await userAPI.getAllUsers()
    const allUsers = response.data.data || []

    // Filter TEACHER role
    const teacherUsers = allUsers.filter((u) => u.role === 'TEACHER')

    // ✅ Fetch assignments count for each teacher
    const teachersWithCount = await Promise.all(
      teacherUsers.map(async (teacher) => {
        try {
          const assignResponse = await teacherAPI.getTeacherAssignments(teacher.id)
          const assignments = assignResponse.data.data || []

          return {
            ...teacher,
            assignedTopicsCount: assignments.length,
          }
        } catch (error) {
          console.error(`❌ Error fetching assignments for teacher ${teacher.id}:`, error)
          console.warn(`⚠️ Could not fetch assignments for teacher ${teacher.id}`)
          return {
            ...teacher,
            assignedTopicsCount: 0,
          }
        }
      })
    )

    teachers.value = teachersWithCount
    console.log('✅ Loaded teachers:', teachers.value.length)
  } catch (error) {
    console.error('❌ Error fetching teachers:', error)
    ElMessage.error('Không thể tải danh sách giáo viên')
    teachers.value = []
  } finally {
    teachersLoading.value = false
  }
}

/**
 * ✅ Fetch all topics from all modules
 */
const fetchAllTopics = async () => {
  topicsLoading.value = true
  try {
    console.log('🔄 Fetching topics from all modules...')

    // ✅ FIXED: Use topicAPI with correct method
    const [grammarRes, readingRes, listeningRes] = await Promise.all([
      topicAPI.getTopicsByModule('GRAMMAR', { page: 1, size: 1000 }),
      topicAPI.getTopicsByModule('READING', { page: 1, size: 1000 }),
      topicAPI.getTopicsByModule('LISTENING', { page: 1, size: 1000 }),
    ])

    const grammarTopics = grammarRes.data.data?.data || []
    const readingTopics = readingRes.data.data?.data || []
    const listeningTopics = listeningRes.data.data?.data || []

    allTopics.value = [...grammarTopics, ...readingTopics, ...listeningTopics]
    console.log('✅ Loaded topics:', allTopics.value.length)
  } catch (error) {
    console.error('❌ Error fetching topics:', error)
    ElMessage.error('Không thể tải danh sách topics')
    allTopics.value = []
  } finally {
    topicsLoading.value = false
  }
}

/**
 * Reset topic when module changes
 */
const handleModuleChange = () => {
  form.topicId = null
}

/**
 * ✅ Submit assignment
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      console.log('📤 Assigning teacher to topic:', form)

      await teacherAPI.assignTeacher({
        teacherId: form.teacherId,
        topicId: form.topicId,
      })

      ElMessage.success('✅ Phân quyền thành công!')
      emit('assigned')
      handleClose()
    } catch (error) {
      console.error('❌ Error assigning teacher:', error)
      const message = error.response?.data?.message || 'Không thể phân quyền'
      ElMessage.error(message)
    } finally {
      submitting.value = false
    }
  })
}

/**
 * Close dialog
 */
const handleClose = () => {
  formRef.value?.resetFields()
  form.teacherId = null
  form.moduleType = null
  form.topicId = null
  visible.value = false
}
</script>
