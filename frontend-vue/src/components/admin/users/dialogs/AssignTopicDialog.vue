<template>
  <el-dialog
    :model-value="modelValue"
    title="Phân quyền Teacher - Topic"
    width="500px"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="handleClose"
    append-to-body
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
      <el-form-item label="Teacher" prop="teacherId">
        <el-select
          v-model="form.teacherId"
          placeholder="Chọn giáo viên"
          class="w-full"
          filterable
          :loading="loadingTeachers"
          no-data-text="Không tìm thấy giáo viên nào"
        >
          <el-option
            v-for="teacher in teachers"
            :key="teacher.id"
            :label="`${teacher.fullName || teacher.username} (@${teacher.username})`"
            :value="teacher.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="Module" prop="moduleType">
        <el-select
          v-model="form.moduleType"
          placeholder="Chọn kỹ năng"
          class="w-full"
          @change="handleModuleChange"
        >
          <el-option label="Grammar" value="GRAMMAR" />
          <el-option label="Reading" value="READING" />
          <el-option label="Listening" value="LISTENING" />
        </el-select>
      </el-form-item>

      <el-form-item label="Topic" prop="topicId">
        <el-select
          v-model="form.topicId"
          placeholder="Chọn chủ đề"
          class="w-full"
          filterable
          :disabled="!form.moduleType"
          :loading="topicsLoading"
          no-data-text="Không có topic nào"
        >
          <el-option
            v-for="item in topics"
            :key="item.id"
            :label="item.displayLabel"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">Hủy</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          Phân quyền
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userAPI, topicAPI } from '@/api'
import { useTeacherAssignmentStore } from '@/stores/admin/teacherAssignment'

// Props & Emits
defineProps({
  modelValue: Boolean,
})
const emit = defineEmits(['update:modelValue', 'assigned'])

// Store
const store = useTeacherAssignmentStore()

// State
const formRef = ref(null)
const submitting = ref(false)
const topicsLoading = ref(false)
const loadingTeachers = ref(false)
const teachers = ref([])
const topics = ref([])

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

// === METHODS ===

// 1. Fetch Teachers (FIXED)
const fetchTeachers = async () => {
  loadingTeachers.value = true
  try {
    // Nếu userAPI.getTeachers() không có, hãy thử dùng getAllUsers() và filter
    // Ở đây mình cố gắng lấy dữ liệu an toàn nhất
    let data = []

    // Thử gọi API getTeachers nếu có
    if (typeof userAPI.getTeachers === 'function') {
      const res = await userAPI.getTeachers()
      // ⚠️ FIX: Xử lý cả 2 trường hợp response structure
      data = Array.isArray(res.data) ? res.data : res.data?.data || []
    }
    // Fallback: Gọi getAllUsers nếu getTeachers lỗi hoặc không có
    else {
      const res = await userAPI.getAllUsers()
      const allUsers = Array.isArray(res.data) ? res.data : res.data?.data || []
      // Filter role TEACHER thủ công
      data = allUsers.filter((u) => u.role === 'TEACHER')
    }

    teachers.value = data
    console.log('✅ Teachers loaded:', teachers.value.length)
  } catch (error) {
    console.error('❌ Error fetching teachers:', error)
    ElMessage.error('Không thể tải danh sách giáo viên')
  } finally {
    loadingTeachers.value = false
  }
}

// 2. Handle Module Change (FIXED)
const handleModuleChange = async (val) => {
  // 1. Reset dữ liệu cũ
  form.topicId = null
  topics.value = []

  if (!val) return

  topicsLoading.value = true
  try {
    console.log('🔄 Fetching topics for module:', val)

    // Gọi API
    const res = await topicAPI.getTopicsByModule(val)
    console.log('📦 API Response:', res.data) // Log để kiểm tra

    // 2. ⚠️ TRÍCH XUẤT DỮ LIỆU MẢNG AN TOÀN (FIX LỖI map is not a function)
    let itemsArray = []

    // Kiểm tra từng lớp dữ liệu để tìm ra mảng 'content' hoặc mảng dữ liệu gốc
    if (res.data?.data?.content && Array.isArray(res.data.data.content)) {
      // Trường hợp 1: Phân trang (Data nằm trong data.content) -> Đây là case của bạn
      itemsArray = res.data.data.content
    } else if (Array.isArray(res.data?.data)) {
      // Trường hợp 2: Backend trả về mảng bọc trong data (không phân trang)
      itemsArray = res.data.data
    } else if (Array.isArray(res.data)) {
      // Trường hợp 3: Backend trả về mảng trực tiếp
      itemsArray = res.data
    } else if (res.data?.content && Array.isArray(res.data.content)) {
      // Trường hợp 4: Một số cấu hình trả thẳng content ở root
      itemsArray = res.data.content
    }

    // 3. Map dữ liệu (Lúc này itemsArray chắc chắn là Array nên hàm .map sẽ không lỗi)
    topics.value = itemsArray.map((t) => ({
      id: t.id,
      // Ưu tiên hiển thị: name -> topicName -> title
      displayLabel: t.name || t.topicName || t.title || `Topic #${t.id}`,
    }))

    console.log(`✅ Loaded ${topics.value.length} topics`)
  } catch (error) {
    console.error('❌ Error fetching topics:', error)
    // Không show error message để tránh làm phiền user nếu lỗi do race condition
  } finally {
    topicsLoading.value = false
  }
}

// 3. Submit Form
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await store.assignTeacher(form.teacherId, form.topicId)
        handleClose()
        emit('assigned')
      } catch (error) {
        // Error handled in store
        console.error('❌ Assignment failed:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleClose = () => {
  // Không reset form hoàn toàn để UX tốt hơn (giữ lại module/teacher nếu muốn add tiếp)
  // Chỉ reset nếu đóng hẳn, ở đây ta reset các trường cần thiết
  form.topicId = null
  emit('update:modelValue', false)
}

// Lifecycle
onMounted(() => {
  fetchTeachers()
})
</script>
