<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <div
        class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-500 mb-4"
      >
        <el-icon :size="32"><User /></el-icon>
      </div>
      <h2 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">Đăng nhập</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">Đăng nhập để tiếp tục học tập</p>
    </div>

    <!-- ✅ Hiển thị lỗi -->
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      :type="alertType"
      :closable="true"
      @close="errorMessage = ''"
      class="mb-4"
      show-icon
    />

    <!-- ✅ Hiển thị số lần thử còn lại -->
    <el-alert
      v-if="attemptsRemaining !== null && attemptsRemaining < 5 && attemptsRemaining > 0"
      :title="`Cảnh báo: Còn ${attemptsRemaining} lần thử trước khi tài khoản bị khóa`"
      type="warning"
      :closable="false"
      class="mb-4"
      show-icon
    />

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleLogin"
      class="space-y-4"
    >
      <el-form-item label="Tên đăng nhập hoặc Email" prop="usernameOrEmail" class="!mb-4">
        <el-input
          v-model="formData.usernameOrEmail"
          placeholder="Nhập tên đăng nhập hoặc email"
          :prefix-icon="User"
          clearable
          :disabled="loading || isBlocked"
          @input="errorMessage = ''"
        />
      </el-form-item>

      <el-form-item label="Mật khẩu" prop="password" class="!mb-2">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Nhập mật khẩu"
          :prefix-icon="Lock"
          show-password
          clearable
          :disabled="loading || isBlocked"
          @input="errorMessage = ''"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <div class="flex items-center justify-between mb-4">
        <el-checkbox v-model="rememberMe" :disabled="loading || isBlocked"
          >Ghi nhớ đăng nhập</el-checkbox
        >
        <router-link
          to="/auth/forgot-password"
          class="text-sm text-blue-600 dark:text-blue-400 hover:underline no-underline"
        >
          Quên mật khẩu?
        </router-link>
      </div>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading || isBlocked"
        class="!w-full !h-11 !text-base !font-bold !rounded-lg shadow-md shadow-blue-500/20"
      >
        {{ isBlocked ? 'Tài khoản đã bị khóa' : 'Đăng nhập' }}
      </el-button>

      <div class="text-center mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          Chưa có tài khoản?
          <router-link
            to="/auth/register"
            class="text-blue-600 dark:text-blue-400 hover:underline font-medium no-underline ml-1"
          >
            Đăng ký ngay
          </router-link>
        </p>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const errorMessage = ref('')
const alertType = ref('error')
const attemptsRemaining = ref(null)
const isBlocked = ref(false)
const blockTimeRemaining = ref(null)

const formData = reactive({
  usernameOrEmail: '',
  password: '',
})

const validateUsernameOrEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập tên đăng nhập hoặc email'))
  } else if (value.length < 3) {
    callback(new Error('Tên đăng nhập phải có ít nhất 3 ký tự'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập mật khẩu'))
  } else if (value.length < 6) {
    callback(new Error('Mật khẩu phải có ít nhất 6 ký tự'))
  } else {
    callback()
  }
}

const rules = {
  usernameOrEmail: [{ validator: validateUsernameOrEmail, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!formRef.value) return

  // Reset error
  errorMessage.value = ''

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.loginUser({
          usernameOrEmail: formData.usernameOrEmail,
          password: formData.password,
        })

        // ✅ Hiển thị thông báo thành công
        toast.success('Đăng nhập thành công!')

        // Chuyển hướng về trang chủ
        router.push('/')
      } catch (error) {
        console.error('Login error:', error)

        // ✅ Xử lý các loại lỗi cụ thể từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Đăng nhập thất bại'

        errorMessage.value = errorMsg

        // ✅ Xử lý tài khoản bị khóa
        if (errorMsg.includes('bị khóa') || errorMsg.includes('blocked')) {
          alertType.value = 'error'
          isBlocked.value = true

          // Parse thời gian còn lại
          const timeMatch = errorMsg.match(/(\d+)\s*phút/)
          if (timeMatch) {
            blockTimeRemaining.value = parseInt(timeMatch[1])
            errorMessage.value = `Tài khoản tạm thời bị khóa. Vui lòng thử lại sau ${blockTimeRemaining.value} phút.`
          }

          toast.error(errorMessage.value)

          // Tự động mở khóa sau thời gian
          if (blockTimeRemaining.value) {
            setTimeout(
              () => {
                isBlocked.value = false
                errorMessage.value = ''
                toast.info('Tài khoản đã được mở khóa. Bạn có thể thử đăng nhập lại.')
              },
              blockTimeRemaining.value * 60 * 1000,
            )
          }

          // ✅ Xử lý sai mật khẩu
        } else if (
          errorMsg.includes('sai') ||
          errorMsg.includes('không chính xác') ||
          errorMsg.includes('invalid')
        ) {
          alertType.value = 'warning'

          // Parse số lần thử còn lại từ message
          const remainingMatch = errorMsg.match(/Còn lại (\d+) lần thử/)
          if (remainingMatch) {
            attemptsRemaining.value = parseInt(remainingMatch[1])
            errorMessage.value = `Sai tên đăng nhập hoặc mật khẩu. Còn ${attemptsRemaining.value} lần thử.`
          } else {
            errorMessage.value = 'Sai tên đăng nhập hoặc mật khẩu'
          }

          toast.error(errorMessage.value)

          // ✅ Xử lý tài khoản chưa xác thực
        } else if (errorMsg.includes('chưa được kích hoạt') || errorMsg.includes('chưa xác thực')) {
          alertType.value = 'info'
          errorMessage.value = 'Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác thực.'
          toast.warning(errorMessage.value)

          // Tự động chuyển sang trang verify sau 3 giây
          setTimeout(() => {
            router.push({
              path: '/auth/verify-email',
              query: { email: formData.usernameOrEmail },
            })
          }, 3000)

          // ✅ Xử lý tài khoản bị vô hiệu hóa
        } else if (errorMsg.includes('vô hiệu hóa') || errorMsg.includes('disabled')) {
          alertType.value = 'error'
          errorMessage.value = 'Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.'
          toast.error(errorMessage.value)

          // ✅ Xử lý tài khoản không tồn tại
        } else if (errorMsg.includes('không tồn tại') || errorMsg.includes('not found')) {
          alertType.value = 'error'
          errorMessage.value = 'Tài khoản không tồn tại'
          toast.error(errorMessage.value)

          // ✅ Lỗi khác
        } else {
          alertType.value = 'error'
          toast.error(errorMsg)
        }
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
