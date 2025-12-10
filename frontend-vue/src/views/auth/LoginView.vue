<template>
  <div class="w-full">
    <h2 class="text-2xl font-bold text-center text-gray-800 mb-2">Đăng nhập</h2>
    <p class="text-center text-gray-500 text-sm mb-8">Chào mừng bạn quay trở lại!</p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="Tên đăng nhập" prop="usernameOrEmail" class="!mb-5">
        <el-input
          v-model="formData.usernameOrEmail"
          placeholder="Nhập tên đăng nhập"
          :prefix-icon="User"
          clearable
        />
      </el-form-item>

      <el-form-item label="Mật khẩu" prop="password" class="!mb-2">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Nhập mật khẩu"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <div class="flex justify-end mb-6">
        <router-link to="/auth/forgot-password" class="text-sm text-blue-500 hover:text-blue-600 font-medium no-underline transition-colors">
          Quên mật khẩu?
        </router-link>
      </div>

      <el-form-item class="!mb-0">
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
          class="!w-full !h-11 !text-base !font-bold !rounded-lg"
        >
          Đăng nhập
        </el-button>
      </el-form-item>

      <div class="text-center text-sm text-gray-600 mt-6">
        <span>Chưa có tài khoản? </span>
        <router-link to="/auth/register" class="text-blue-500 hover:text-blue-600 font-bold ml-1 no-underline transition-colors">
          Đăng ký ngay
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  usernameOrEmail: '',
  password: '',
})

const rules = {
  usernameOrEmail: [{ required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' }],
  password: [{ required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.login(formData)
        const redirectPath = route.query.redirect || '/user/home'
        // Delay nhỏ để UX mượt hơn
        setTimeout(() => router.push(redirectPath), 500)
      } catch (error) {
        console.error('Login failed:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
