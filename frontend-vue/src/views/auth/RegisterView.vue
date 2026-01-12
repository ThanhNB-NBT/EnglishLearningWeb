<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <div
        class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-50 dark:bg-green-900/30 text-green-500 mb-4"
      >
        <el-icon :size="32"><UserFilled /></el-icon>
      </div>
      <h2 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">Đăng ký tài khoản</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">Tạo tài khoản để bắt đầu học tập</p>
    </div>

    <!-- ✅ Hiển thị lỗi -->
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="true"
      @close="errorMessage = ''"
      class="mb-4"
      show-icon
    />

    <!-- ✅ Yêu cầu mật khẩu (thu gọn) -->
    <el-collapse v-model="showPasswordHints" class="mb-4">
      <el-collapse-item name="hints" title="Yêu cầu mật khẩu">
        <ul class="text-xs text-gray-600 dark:text-gray-400 space-y-1">
          <li :class="passwordChecks.length ? 'text-green-600' : ''">
            <el-icon :size="14" class="mr-1">
              <component :is="passwordChecks.length ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            Ít nhất 8 ký tự
          </li>
          <li :class="passwordChecks.uppercase ? 'text-green-600' : ''">
            <el-icon :size="14" class="mr-1">
              <component :is="passwordChecks.uppercase ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            Có ít nhất 1 chữ in hoa
          </li>
          <li :class="passwordChecks.lowercase ? 'text-green-600' : ''">
            <el-icon :size="14" class="mr-1">
              <component :is="passwordChecks.lowercase ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            Có ít nhất 1 chữ thường
          </li>
          <li :class="passwordChecks.number ? 'text-green-600' : ''">
            <el-icon :size="14" class="mr-1">
              <component :is="passwordChecks.number ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            Có ít nhất 1 chữ số
          </li>
        </ul>
      </el-collapse-item>
    </el-collapse>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleRegister"
      class="space-y-3"
    >
      <el-form-item label="Tên đăng nhập" prop="username" class="!mb-3">
        <el-input
          v-model="formData.username"
          placeholder="Nhập tên đăng nhập (3-20 ký tự)"
          :prefix-icon="User"
          clearable
          :disabled="loading"
          @input="errorMessage = ''"
        />
      </el-form-item>

      <el-form-item label="Email" prop="email" class="!mb-3">
        <el-input
          v-model="formData.email"
          type="email"
          placeholder="example@email.com"
          :prefix-icon="Message"
          clearable
          :disabled="loading"
          @input="errorMessage = ''"
        />
      </el-form-item>

      <el-form-item label="Họ và tên" prop="fullName" class="!mb-3">
        <el-input
          v-model="formData.fullName"
          placeholder="Nhập họ và tên đầy đủ"
          :prefix-icon="UserFilled"
          clearable
          :disabled="loading"
          @input="errorMessage = ''"
        />
      </el-form-item>

      <el-form-item label="Mật khẩu" prop="password" class="!mb-3">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Nhập mật khẩu"
          :prefix-icon="Lock"
          show-password
          clearable
          :disabled="loading"
          @input="
            errorMessage = '',checkPassword()
          "
          @focus="showPasswordHints = ['hints']"
        />
      </el-form-item>

      <el-form-item label="Xác nhận mật khẩu" prop="confirmPassword" class="!mb-3">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu"
          :prefix-icon="Lock"
          show-password
          clearable
          :disabled="loading"
          @input="errorMessage = ''"
        />
      </el-form-item>

      <el-form-item class="!mb-0">
        <el-checkbox v-model="agreeTerms" :disabled="loading">
          Tôi đồng ý với
          <a href="#" class="text-blue-600 dark:text-blue-400 hover:underline"
            >Điều khoản sử dụng</a
          >
        </el-checkbox>
      </el-form-item>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading || !agreeTerms || !isPasswordValid"
        class="!w-full !h-11 !text-base !font-bold !rounded-lg shadow-md shadow-green-500/20 !mt-4"
      >
        Đăng ký
      </el-button>

      <div class="text-center mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          Đã có tài khoản?
          <router-link
            to="/auth/login"
            class="text-blue-600 dark:text-blue-400 hover:underline font-medium no-underline ml-1"
          >
            Đăng nhập
          </router-link>
        </p>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import { User, UserFilled, Lock, Message } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const formRef = ref(null)
const loading = ref(false)
const agreeTerms = ref(false)
const errorMessage = ref('')
const showPasswordHints = ref([])

const formData = reactive({
  username: '',
  email: '',
  fullName: '',
  password: '',
  confirmPassword: '',
  englishLevel: '',
})

// ✅ Kiểm tra yêu cầu mật khẩu real-time
const passwordChecks = reactive({
  length: false,
  uppercase: false,
  lowercase: false,
  number: false,
})

const checkPassword = () => {
  const pwd = formData.password
  passwordChecks.length = pwd.length >= 8
  passwordChecks.uppercase = /[A-Z]/.test(pwd)
  passwordChecks.lowercase = /[a-z]/.test(pwd)
  passwordChecks.number = /\d/.test(pwd)
}

const isPasswordValid = computed(() => {
  return (
    passwordChecks.length &&
    passwordChecks.uppercase &&
    passwordChecks.lowercase &&
    passwordChecks.number &&
    formData.password === formData.confirmPassword &&
    formData.confirmPassword.length > 0
  )
})

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập tên đăng nhập'))
  } else if (value.length < 3 || value.length > 20) {
    callback(new Error('Tên đăng nhập phải có từ 3-20 ký tự'))
  } else if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    callback(new Error('Tên đăng nhập chỉ được chứa chữ, số và dấu gạch dưới'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập email'))
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('Email không hợp lệ'))
  } else {
    callback()
  }
}

const validateFullName = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập họ và tên'))
  } else if (value.length < 2) {
    callback(new Error('Họ tên phải có ít nhất 2 ký tự'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập mật khẩu'))
  } else if (value.length < 8) {
    callback(new Error('Mật khẩu phải có ít nhất 8 ký tự'))
  } else if (!/[A-Z]/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ in hoa'))
  } else if (!/[a-z]/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ thường'))
  } else if (!/\d/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ số'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng xác nhận mật khẩu'))
  } else if (value !== formData.password) {
    callback(new Error('Mật khẩu xác nhận không khớp'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  fullName: [{ validator: validateFullName, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

const handleRegister = async () => {
  if (!formRef.value) return

  if (!agreeTerms.value) {
    toast.warning('Vui lòng đồng ý với điều khoản sử dụng')
    return
  }

  // Reset error
  errorMessage.value = ''

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const registerData = {
          username: formData.username,
          email: formData.email,
          fullName: formData.fullName,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
        }

        await authStore.register(registerData)

        // ✅ Hiển thị thông báo thành công
        toast.success('Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.')

        // Chờ 1 giây rồi chuyển sang trang verify
        setTimeout(() => {
          router.push({
            path: '/auth/verify-email',
            query: { email: formData.email },
          })
        }, 1000)
      } catch (error) {
        console.error('Register error:', error)

        // ✅ Xử lý các loại lỗi từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Đăng ký thất bại'

        errorMessage.value = errorMsg
        toast.error(errorMsg)

        // ✅ Xử lý lỗi username/email đã tồn tại
        if (errorMsg.includes('tên tài khoản') || errorMsg.includes('username')) {
          // Focus vào trường username
          formRef.value.validateField('username')
        } else if (errorMsg.includes('email')) {
          // Focus vào trường email
          formRef.value.validateField('email')
        }
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.el-icon {
  vertical-align: middle;
}

:deep(.el-collapse-item__header) {
  font-size: 0.875rem;
  font-weight: 500;
}
</style>
