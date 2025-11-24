<template>
  <el-container class="layout-container">
    <!-- Header -->
    <el-header class="header">
      <div class="header-content">
        <!-- Logo -->
        <router-link to="/" class="logo">
          <span class="logo-text">English Learning</span>
        </router-link>

        <!-- Desktop Navigation -->
        <el-menu
          mode="horizontal"
          :ellipsis="false"
          class="desktop-menu"
          router
        >
        </el-menu>

        <!-- Auth Buttons / User Menu -->
        <div class="header-right">
          <template v-if="!isLoggedIn">
            <el-button @click="$router.push('/auth/login')">Đăng nhập</el-button>
            <el-button type="primary" @click="$router.push('/auth/register')">
              Đăng ký
            </el-button>
          </template>

          <template v-else>
            <el-dropdown @command="handleCommand">
              <span class="user-dropdown">
                <el-avatar :size="36" class="user-avatar">
                  {{ userInitial }}
                </el-avatar>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <div class="user-info">
                      <div class="user-name">{{ currentUser?.username }}</div>
                      <div class="user-email">{{ currentUser?.email }}</div>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="dashboard">
                    <el-icon><Odometer /></el-icon>
                    Dashboard
                  </el-dropdown-item>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    Hồ sơ
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    Đăng xuất
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>

        <!-- Mobile Menu Button -->
        <el-button
          class="mobile-menu-btn"
          :icon="Menu"
          @click="drawerVisible = true"
        />
      </div>
    </el-header>

    <!-- Mobile Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="Menu"
      direction="ltr"
      size="70%"
    >
      <el-menu router @select="drawerVisible = false">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>Trang chủ</span>
        </el-menu-item>
        <el-menu-item index="/grammar">
          <el-icon><Reading /></el-icon>
          <span>Ngữ pháp</span>
        </el-menu-item>
        <el-menu-item index="/reading">
          <el-icon><Document /></el-icon>
          <span>Đọc hiểu</span>
        </el-menu-item>

        <el-divider v-if="!isLoggedIn" />

        <template v-if="!isLoggedIn">
          <el-menu-item index="/auth/login">
            <el-icon><User /></el-icon>
            <span>Đăng nhập</span>
          </el-menu-item>
          <el-menu-item index="/auth/register">
            <el-icon><UserFilled /></el-icon>
            <span>Đăng ký</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-drawer>

    <!-- Main Content -->
    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- Footer -->
    <el-footer class="footer">
      <div class="footer-content">
        <span>© 2025 English Learning Platform. All rights reserved.</span>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  Menu, ArrowDown, Odometer, User, SwitchButton,
  HomeFilled, Reading, Document, UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const drawerVisible = ref(false)

const isLoggedIn = computed(() => authStore.isLoggedIn)
const currentUser = computed(() => authStore.currentUser)
const userInitial = computed(() => currentUser.value?.username?.charAt(0).toUpperCase() || 'U')

const handleCommand = (command) => {
  switch (command) {
    case 'dashboard':
      router.push('/dashboard')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = async () => {
  await authStore.logout()
  router.push({ name: 'home' })
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #409eff;
  font-size: 20px;
  font-weight: bold;
  white-space: nowrap;
}

.logo-icon {
  font-size: 28px;
}

.desktop-menu {
  flex: 1;
  border: none;
  background: transparent;
}

.desktop-menu :deep(.el-menu-item) {
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-dropdown:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: #409eff;
  color: white;
  font-weight: bold;
}

.user-info {
  padding: 8px 0;
}

.user-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.user-email {
  font-size: 12px;
  color: #909399;
}

.mobile-menu-btn {
  display: none;
}

.main-content {
  flex: 1;
  padding: 0;
  background: #f5f7fa;
}

.footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  height: 60px;
  padding: 0;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .desktop-menu {
    display: none;
  }

  .header-right > .el-button {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .logo-text {
    display: none;
  }
}
</style>
