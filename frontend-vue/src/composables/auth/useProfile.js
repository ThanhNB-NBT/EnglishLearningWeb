import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/textFormatter' // Đảm bảo bạn có util này hoặc dùng hàm format riêng

export function useProfile() {
  const authStore = useAuthStore()
  const loading = ref(false)

  // Model cho Form hiển thị
  const userProfile = reactive({
    fullName: '',
    email: '',
    username: '',
    role: '',

    // Stats Fields
    totalPoints: 0,
    streakDays: 0,
    level: 'A1',

    // Activity Fields
    lastLogin: '',
    lastIp: ''
  })

  // Đổ dữ liệu từ Store vào Reactive State
  const initProfileData = () => {
    const user = authStore.currentUser
    if (user) {
      userProfile.fullName = user.fullName || ''
      userProfile.email = user.email || ''
      userProfile.username = user.username || ''
      userProfile.role = user.role || ''

      // Lấy từ stats (đã gộp bởi fetchProfile)
      if (user.stats) {
          userProfile.totalPoints = user.stats.totalPoints || 0
          userProfile.streakDays = user.stats.currentStreak || 0 // Backend trả về currentStreak
          // Level mapping (ví dụ)
          userProfile.level = user.englishLevel || 'A1'
      }

      // Lấy từ activity (đã gộp bởi fetchProfile)
      if (user.activity) {
          userProfile.lastLogin = user.activity.lastLoginDate
            ? formatDate(user.activity.lastLoginDate)
            : 'Chưa có thông tin'
          userProfile.lastIp = user.activity.lastLoginIp || ''
      }
    }
  }

  // Khi component mount -> Fetch mới nhất -> Init data
  onMounted(async () => {
    loading.value = true
    await authStore.fetchProfile()
    initProfileData()
    loading.value = false
  })

  // Hàm update dùng cho nút "Lưu thay đổi"
  const updateProfile = async () => {
    loading.value = true
    try {
      // Backend DTO chỉ nhận fullName
      const payload = {
        fullName: userProfile.fullName
      }

      await authStore.updateProfile(payload)
      // Không cần init lại vì Store reactive đã update userProfile
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  return {
    userProfile,
    loading,
    updateProfile,
    initProfileData // Export để component có thể gọi reset form nếu cần
  }
}
