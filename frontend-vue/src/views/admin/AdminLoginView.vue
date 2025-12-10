<template>
  <div class="min-h-screen w-full flex items-center justify-center p-4 bg-gray-100 dark:bg-[#0f0f0f] transition-colors duration-300">

    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-xl bg-gray-900 dark:bg-white text-white dark:text-gray-900 mb-4 shadow-lg">
          <el-icon :size="32"><Monitor /></el-icon>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">Admin Portal</h1>
        <p class="text-gray-500 dark:text-gray-400 text-sm mt-1">Đăng nhập để truy cập hệ thống quản trị</p>
      </div>

      <div class="bg-white dark:bg-[#1d1d1d] rounded-2xl shadow-xl border border-gray-200 dark:border-[#333] overflow-hidden">
        <div class="p-8">
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
                placeholder="admin"
                :prefix-icon="User"
                class="!h-11"
              />
            </el-form-item>

            <el-form-item label="Mật khẩu" prop="password" class="!mb-2">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="••••••••"
                :prefix-icon="Lock"
                show-password
                class="!h-11"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              class="!w-full !h-11 !text-base !font-bold !rounded-lg !bg-gray-900 hover:!bg-gray-800 dark:!bg-blue-600 dark:hover:!bg-blue-700 !border-none transition-colors"
            >
              Truy cập Dashboard
            </el-button>
          </el-form>
        </div>

        <div class="px-8 py-4 bg-gray-50 dark:bg-[#262626] border-t border-gray-100 dark:border-[#333] text-center">
          <router-link to="/" class="text-sm text-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors no-underline flex items-center justify-center gap-2">
            <el-icon><ArrowLeft /></el-icon> Quay về trang chủ
          </router-link>
        </div>
      </div>

      <p class="text-center text-xs text-gray-400 mt-6">
        &copy; 2025 English Learning Platform. Admin Access Only.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Monitor, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

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
    if (valid) {
      loading.value = true
      try {
        await authStore.loginAdmin(formData)
        router.push('/admin/dashboard')
      } catch (error) {
        // Lỗi đã được xử lý ở store hoặc interceptor, nhưng có thể hiện thêm thông báo ở đây nếu cần
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
