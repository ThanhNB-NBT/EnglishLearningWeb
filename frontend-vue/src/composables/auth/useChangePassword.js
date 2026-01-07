import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useChangePassword() {
  const authStore = useAuthStore()
  const loading = ref(false)

  // Form Model
  const form = reactive({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  // Error States
  const errors = reactive({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  // Validate Form
  const validate = () => {
    let isValid = true
    // Reset lỗi
    Object.keys(errors).forEach(key => errors[key] = '')

    if (!form.currentPassword) {
      errors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại'
      isValid = false
    }

    if (!form.newPassword) {
      errors.newPassword = 'Vui lòng nhập mật khẩu mới'
      isValid = false
    } else if (form.newPassword.length < 8) { // Backend yêu cầu min 8
      errors.newPassword = 'Mật khẩu phải có ít nhất 8 ký tự'
      isValid = false
    }

    if (form.newPassword !== form.confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
      isValid = false
    }

    return isValid
  }

  // Handle Submit
  const changePassword = async () => {
    if (!validate()) return

    loading.value = true
    try {
      // ✅ QUAN TRỌNG: Map đúng field name của Backend DTO (ChangePasswordRequest)
      const payload = {
        oldPassword: form.currentPassword, // Frontend gọi là current, Backend cần old
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword
      }

      await authStore.changePassword(payload)

      // Reset form nếu thành công
      form.currentPassword = ''
      form.newPassword = ''
      form.confirmPassword = ''

      // Toast đã được gọi bên trong Store rồi, nhưng nếu thích có thể gọi thêm ở đây
    } catch (error) {
      console.error(error)
      // Lỗi validation từ backend (nếu có) sẽ được toast ở store
    } finally {
      loading.value = false
    }
  }

  return {
    form,
    errors,
    loading,
    changePassword
  }
}
