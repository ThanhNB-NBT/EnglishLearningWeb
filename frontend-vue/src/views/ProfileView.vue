<template>
  <div class="min-h-screen bg-gray-50 dark:bg-[#141414] p-4 md:p-8">
    <div class="max-w-7xl mx-auto">

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

        <div class="lg:col-span-1 flex flex-col gap-6">
          <div class="bg-white dark:bg-[#1d1d1d] rounded-2xl shadow-sm border border-gray-100 dark:border-[#303030] overflow-hidden">
            <div class="h-32 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 relative">
              <div class="absolute inset-0 bg-white/10 pattern-dots"></div>
            </div>

            <div class="px-6 pb-8 text-center relative">
              <div class="-mt-12 mb-4 inline-block relative">
                <el-avatar
                  :size="100"
                  class="!border-4 !border-white dark:!border-[#1d1d1d] !bg-gray-200 !text-4xl !font-bold !shadow-md"
                  :src="null"
                >
                  {{ userInitial }}
                </el-avatar>
                <span class="absolute bottom-1 right-1 w-5 h-5 bg-green-500 border-4 border-white dark:border-[#1d1d1d] rounded-full"></span>
              </div>

              <h2 class="text-xl font-bold text-gray-900 dark:text-white mb-1">
                {{ profileData.fullName || profileData.username }}
              </h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">{{ profileData.email }}</p>

              <div class="flex justify-center gap-2 mb-6">
                <el-tag :type="isAdmin ? 'danger' : 'success'" effect="dark" round class="px-4">
                  {{ isAdmin ? 'Administrator' : 'Thành viên' }}
                </el-tag>
                <el-tag type="info" effect="plain" round>
                  Level {{ calculatedLevel }}
                </el-tag>
              </div>

              <div class="grid grid-cols-2 gap-4 border-t border-gray-100 dark:border-[#303030] pt-6">
                <div class="text-center">
                  <div class="text-lg font-bold text-gray-800 dark:text-white">{{ profileData.streakDays || 0 }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wide">Streak</div>
                </div>
                <div class="text-center border-l border-gray-100 dark:border-[#303030]">
                  <div class="text-lg font-bold text-gray-800 dark:text-white">{{ profileData.totalPoints || 0 }}</div>
                  <div class="text-xs text-gray-500 uppercase tracking-wide">Điểm</div>
                </div>
              </div>
            </div>
          </div>

          <div class="lg:hidden bg-white dark:bg-[#1d1d1d] rounded-xl p-4 shadow-sm border border-gray-100 dark:border-[#303030]">
             <el-button @click="goToChangePassword" class="w-full" plain>
               <el-icon class="mr-2"><Lock /></el-icon> Đổi mật khẩu
             </el-button>
          </div>
        </div>

        <div class="lg:col-span-2 flex flex-col gap-6">

          <div v-if="!isAdmin" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-2xl shadow-sm border border-gray-100 dark:border-[#303030] flex items-center gap-4">
              <div class="w-12 h-12 rounded-full bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center text-orange-500">
                <el-icon :size="24"><Trophy /></el-icon>
              </div>
              <div>
                <p class="text-sm text-gray-500 dark:text-gray-400">Tổng điểm</p>
                <p class="text-xl font-bold text-gray-900 dark:text-white">{{ profileData.totalPoints || 0 }}</p>
              </div>
            </div>

            <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-2xl shadow-sm border border-gray-100 dark:border-[#303030] flex items-center gap-4">
              <div class="w-12 h-12 rounded-full bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-500">
                <el-icon :size="24"><Calendar /></el-icon>
              </div>
              <div>
                <p class="text-sm text-gray-500 dark:text-gray-400">Chuỗi ngày</p>
                <p class="text-xl font-bold text-gray-900 dark:text-white">{{ profileData.streakDays || 0 }} ngày</p>
              </div>
            </div>

            <div class="bg-white dark:bg-[#1d1d1d] p-5 rounded-2xl shadow-sm border border-gray-100 dark:border-[#303030] flex items-center gap-4">
              <div class="w-12 h-12 rounded-full bg-green-50 dark:bg-green-900/20 flex items-center justify-center text-green-500">
                <el-icon :size="24"><Medal /></el-icon>
              </div>
              <div>
                <p class="text-sm text-gray-500 dark:text-gray-400">Xếp hạng</p>
                <p class="text-xl font-bold text-gray-900 dark:text-white">Bạc</p>
              </div>
            </div>
          </div>

          <div v-loading="loading" class="bg-white dark:bg-[#1d1d1d] rounded-2xl shadow-sm border border-gray-100 dark:border-[#303030]">
            <div class="px-6 py-4 border-b border-gray-100 dark:border-[#303030] flex justify-between items-center">
              <h3 class="text-lg font-bold text-gray-800 dark:text-white">Thông tin cá nhân</h3>
              <!-- <el-button v-if="!isAdmin" size="small" type="primary" plain round>Xem lịch sử học</el-button> -->
            </div>

            <div class="p-6 md:p-8">
              <el-form
                ref="formRef"
                :model="formData"
                :rules="rules"
                label-position="top"
                size="large"
                @submit.prevent="handleUpdate"
              >
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <el-form-item label="Tên đăng nhập" class="md:col-span-1">
                    <el-input
                      v-model="profileData.username"
                      disabled
                      :prefix-icon="User"
                      class="!bg-gray-50 dark:!bg-[#262626]"
                    />
                    <span class="text-xs text-gray-400 mt-1 ml-1">Không thể thay đổi</span>
                  </el-form-item>

                  <el-form-item label="Địa chỉ Email" class="md:col-span-1">
                    <el-input
                      v-model="profileData.email"
                      disabled
                      :prefix-icon="Message"
                      class="!bg-gray-50 dark:!bg-[#262626]"
                    />
                    <span class="text-xs text-gray-400 mt-1 ml-1">Liên hệ admin để đổi email</span>
                  </el-form-item>

                  <el-form-item label="Họ và tên hiển thị" prop="fullName" class="md:col-span-2">
                    <el-input
                      v-model="formData.fullName"
                      placeholder="Nhập họ tên đầy đủ của bạn"
                      :prefix-icon="Edit"
                    />
                  </el-form-item>
                </div>

                <div class="flex flex-col-reverse sm:flex-row gap-4 justify-end pt-6 mt-2 border-t border-gray-50 dark:border-[#303030]">
                  <el-button @click="goToChangePassword" class="!h-10 !px-6">
                    <el-icon class="mr-2"><Key /></el-icon> Đổi mật khẩu
                  </el-button>
                  <el-button
                    type="primary"
                    native-type="submit"
                    :loading="loading"
                    class="!h-10 !px-8 !font-bold shadow-lg shadow-blue-500/20"
                  >
                    Lưu thay đổi
                  </el-button>
                </div>
              </el-form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProfile } from '@/composables/auth/useProfile'
import { Trophy, Calendar, Medal, User, Message, Edit, Key, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const { loading, formRef, fetchProfile, updateProfile } = useProfile()

const isAdmin = computed(() => !!authStore.currentAdmin)

const profileData = ref({ username: '', email: '', fullName: '', role: '', totalPoints: 0, streakDays: 0 })
const formData = ref({ fullName: '' })

const userInitial = computed(() => profileData.value.username?.charAt(0).toUpperCase() || (isAdmin.value ? 'A' : 'U'))

// Giả lập Level dựa trên điểm số (Ví dụ: 100 điểm = 1 level)
const calculatedLevel = computed(() => {
  const points = profileData.value.totalPoints || 0
  return Math.floor(points / 100) + 1
})

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
  } catch (error) { console.error(error) }
}

const handleUpdate = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const updated = await updateProfile(formData.value.fullName)
      profileData.value.fullName = updated.fullName
    } catch (error) { console.error(error) }
  })
}

const goToChangePassword = () => router.push(isAdmin.value ? '/admin/change-password' : '/user/change-password')

onMounted(loadProfile)
</script>

<style scoped>
/* Pattern Dots cho Cover background */
.pattern-dots {
  background-image: radial-gradient(rgba(255, 255, 255, 0.2) 1px, transparent 1px);
  background-size: 10px 10px;
}
</style>
