<!-- src/views/teacher/TeacherLoginView.vue - FINAL FIX -->
<template>
  <div class="min-h-screen w-full flex items-center justify-center p-4 bg-gray-100 dark:bg-[#0f0f0f] transition-colors duration-300">
    <div class="w-full max-w-md">
      <!-- Header -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-xl bg-green-600 text-white mb-4 shadow-lg">
          <el-icon :size="32"><Reading /></el-icon>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">Teacher Portal</h1>
        <p class="text-gray-500 dark:text-gray-400 text-sm mt-1">Đăng nhập để quản lý bài học được phân công</p>
      </div>

      <!-- Login Form Card -->
      <div class="bg-white dark:bg-[#1d1d1d] rounded-2xl shadow-xl border border-gray-200 dark:border-[#333] overflow-hidden">
        <div class="p-8">
          <!-- Retry indicator -->
          <el-alert
            v-if="retrying"
            type="info"
            :closable="false"
            class="mb-4"
          >
            <template #title>
              <div class="flex items-center gap-2">
                <el-icon class="animate-spin"><Loading /></el-icon>
                <span>Đang xử lý... Vui lòng đợi</span>
              </div>
            </template>
          </el-alert>

          <el-form
            ref="formRef"
            :model="formData"
            :rules="rules"
            label-position="top"
            size="large"
            @submit.prevent="handleLogin"
            class="space-y-5"
          >
            <el-form-item label="Tên đăng nhập / Email" prop="usernameOrEmail" class="!mb-0">
              <el-input
                v-model="formData.usernameOrEmail"
                placeholder="teacher123"
                :prefix-icon="User"
                :disabled="loading || retrying"
                class="!h-11"
              />
            </el-form-item>

            <el-form-item label="Mật khẩu" prop="password" class="!mb-2">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="••••••••"
                :prefix-icon="Lock"
                :disabled="loading || retrying"
                show-password
                class="!h-11"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-button
              type="success"
              native-type="submit"
              :loading="loading"
              :disabled="loading || retrying"
              class="!w-full !h-11 !text-base !font-bold !rounded-lg !bg-green-600 hover:!bg-green-700 dark:!bg-green-600 dark:hover:!bg-green-700 !border-none transition-colors"
            >
              {{ loading ? 'Đang xử lý...' : 'Đăng nhập Teacher' }}
            </el-button>
          </el-form>
        </div>

        <!-- Footer -->
        <div class="px-8 py-4 bg-gray-50 dark:bg-[#262626] border-t border-gray-100 dark:border-[#333] text-center">
          <router-link
            to="/"
            class="text-sm text-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors no-underline flex items-center justify-center gap-2"
          >
            <el-icon><ArrowLeft /></el-icon> Quay về trang chủ
          </router-link>
        </div>
      </div>

      <p class="text-center text-xs text-gray-400 mt-6">
        &copy; 2025 English Learning Platform. Teacher Access Only.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Reading, ArrowLeft, Loading } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const retrying = ref(false)

const formData = reactive({
  usernameOrEmail: '',
  password: '',
})

const rules = {
  usernameOrEmail: [{ required: true, message: 'Vui lòng nhập tài khoản', trigger: 'blur' }],
  password: [{ required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    retrying.value = false

    try {
      console.log('🔐 Teacher login attempt...')

      // ✅ Call loginTeacher from auth store
      await authStore.loginTeacher(formData)

      console.log('✅ Teacher login successful, redirecting...')
      router.push('/teacher/dashboard')
    } catch (error) {
      console.error('❌ Teacher login failed:', error)

      // Check optimistic lock
      const isOptimisticLock =
        error.response?.status === 409 ||
        error.response?.data?.message?.includes('optimistic')

      if (isOptimisticLock) {
        retrying.value = true
        setTimeout(() => {
          retrying.value = false
        }, 2000)
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
