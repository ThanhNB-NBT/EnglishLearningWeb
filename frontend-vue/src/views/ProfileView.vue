<template>
  <div class="profile-page">
    <div class="container">
      <h1 class="page-title">Thông tin cá nhân</h1>

      <!-- Profile Card -->
      <el-card shadow="never" v-loading="loading">
        <div class="profile-header">
          <el-avatar :size="80" class="profile-avatar">
            {{ userInitial }}
          </el-avatar>
          <div class="profile-info">
            <h2 class="profile-name">{{ profileData?.username }}</h2>
            <el-tag :type="isAdmin ? 'danger' : 'info'" size="small">
              {{ isAdmin ? 'Quản trị viên' : 'Người dùng' }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <!-- Edit Form -->
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="160px"
          label-position="left"
          @submit.prevent="handleUpdate"
        >
          <!-- Username (readonly) -->
          <el-form-item label="Tên đăng nhập">
            <el-input v-model="profileData.username" disabled />
          </el-form-item>

          <!-- Email (readonly) -->
          <el-form-item label="Email">
            <el-input v-model="profileData.email" disabled />
          </el-form-item>

          <!-- Full Name (editable) -->
          <el-form-item label="Họ và tên" prop="fullName">
            <el-input
              v-model="formData.fullName"
              placeholder="Nhập họ và tên đầy đủ"
              clearable
            />
          </el-form-item>

          <!-- Action Buttons -->
          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              :disabled="loading"
            >
              Cập nhật thông tin
            </el-button>
            <el-button @click="goToChangePassword">
              Đổi mật khẩu
            </el-button>
            <el-button @click="goBack">
              Quay lại
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Stats Card (Only for USER) -->
      <el-card v-if="!isAdmin" shadow="never" style="margin-top: 24px">
        <template #header>
          <span style="font-weight: bold">Thống kê học tập</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="stat-item">
              <el-icon :size="24" color="#ff9800"><Trophy /></el-icon>
              <div class="stat-content">
                <div class="stat-value">{{ profileData?.totalPoints || 0 }}</div>
                <div class="stat-label">Tổng điểm</div>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="stat-item">
              <el-icon :size="24" color="#2196f3"><Calendar /></el-icon>
              <div class="stat-content">
                <div class="stat-value">{{ profileData?.streakDays || 0 }}</div>
                <div class="stat-label">Ngày liên tiếp</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProfile } from '@/composables/auth/useProfile'
import { Trophy, Calendar } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const { loading, formRef, fetchProfile, updateProfile } = useProfile()

const isAdmin = computed(() => !!authStore.currentAdmin)

const profileData = ref({
  username: '',
  email: '',
  fullName: '',
  role: '',
  totalPoints: 0,
  streakDays: 0,
})

const formData = ref({
  fullName: '',
})

const userInitial = computed(() =>
  profileData.value.username?.charAt(0).toUpperCase() || (isAdmin.value ? 'A' : 'U')
)

const rules = {
  fullName: [
    { required: true, message: 'Vui lòng nhập họ và tên', trigger: 'blur' },
    { min: 2, message: 'Họ và tên phải có ít nhất 2 ký tự', trigger: 'blur' },
  ],
}

const loadProfile = async () => {
  try {
    const data = await fetchProfile()
    profileData.value = data
    formData.value.fullName = data.fullName
  } catch (error) {
    console.error('Load profile failed:', error)
  }
}

const handleUpdate = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const updated = await updateProfile(formData.value.fullName)
      profileData.value.fullName = updated.fullName
    } catch (error) {
      console.error('Update profile failed:', error)
    }
  })
}

const goToChangePassword = () => {
  if (isAdmin.value) {
    router.push('/admin/change-password')
  } else {
    router.push('/user/change-password')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  padding: 24px;
}

.container {
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 24px 0;
  color: #303133;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px 0;
}

.profile-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 32px;
  font-weight: bold;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 8px 0;
  color: #303133;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
