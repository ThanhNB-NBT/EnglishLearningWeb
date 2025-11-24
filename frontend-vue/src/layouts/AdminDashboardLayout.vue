<template>
  <el-container class="dashboard-layout">
    <!-- Overlay for mobile -->
    <div v-if="isMobileMenuOpen" class="mobile-overlay" @click="closeMobileMenu"></div>

    <!-- Sidebar - FIXED HEIGHT 100vh -->
    <el-aside :width="sidebarWidth" class="sidebar" :class="{
      'is-collapsed': isCollapsed,
      'is-mobile-open': isMobileMenuOpen
    }">
      <div class="sidebar-header">
        <transition name="fade" mode="out-in">
          <img v-if="isCollapsed" src="/favicon.ico" alt="Logo" class="logo-icon" key="icon" />
          <span v-else class="logo-text" key="text">Admin Panel</span>
        </transition>
      </div>

      <el-scrollbar class="sidebar-scrollbar">
        <el-menu :default-active="$route.path" :collapse="isCollapsed" :collapse-transition="false" router
          class="sidebar-menu" @select="handleMenuSelect">
          <el-menu-item index="/admin/dashboard">
            <el-icon>
              <Odometer />
            </el-icon>
            <template #title>Dashboard</template>
          </el-menu-item>

          <el-menu-item index="/admin/users">
            <el-icon>
              <User />
            </el-icon>
            <template #title>Quản lý người dùng</template>
          </el-menu-item>

          <el-menu-item index="/admin/grammar">
            <el-icon>
              <Reading />
            </el-icon>
            <template #title>Quản lý ngữ pháp</template>
          </el-menu-item>

          <el-menu-item index="/admin/reading">
            <el-icon>
              <Document />
            </el-icon>
            <template #title>Quản lý bài đọc</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <!-- Collapse Button - FIXED AT BOTTOM -->
      <div class="sidebar-footer">
        <el-button :icon="isCollapsed ? Expand : Fold" circle @click="toggleSidebar" class="collapse-btn" />
      </div>
    </el-aside>

    <!-- Main Content -->
    <el-container class="main-container">
      <!-- Header -->
      <el-header class="header">
        <!-- Mobile Menu Button (only visible on mobile) -->
        <el-button class="mobile-menu-btn" :icon="Menu" circle @click="toggleMobileMenu" />

        <!-- Breadcrumb -->
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">Dashboard</el-breadcrumb-item>
          <el-breadcrumb-item v-if="breadcrumbName">{{ breadcrumbName }}</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- Right Actions -->
        <div class="header-actions">
          <!-- Dark Mode Toggle -->
          <el-tooltip :content="isDark ? 'Chế độ sáng' : 'Chế độ tối'" placement="bottom">
            <el-button :icon="isDark ? Sunny : Moon" circle @click="toggleTheme" class="theme-toggle" />
          </el-tooltip>

          <!-- Admin Dropdown -->
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="36" class="user-avatar admin-avatar">
                {{ adminInitial }}
              </el-avatar>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="user-info">
                    <div class="user-name">{{ currentAdmin?.fullName }}</div>
                    <div class="user-email">{{ currentAdmin?.email }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="profile">
                  <el-icon>
                    <User />
                  </el-icon>
                  Thông tin cá nhân
                </el-dropdown-item>
                <el-dropdown-item command="change-password">
                  <el-icon>
                    <Lock />
                  </el-icon>
                  Thay đổi mật khẩu
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon>
                    <SwitchButton />
                  </el-icon>
                  Đăng xuất
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDarkMode } from '@/composables/useDarkMode'
import {
  Odometer, User, Reading, Document, Expand, Fold,
  Moon, Sunny, ArrowDown, Lock, SwitchButton, Menu
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useDarkMode()

const isCollapsed = ref(false)
const isMobileMenuOpen = ref(false)

const sidebarWidth = computed(() => isCollapsed.value ? '64px' : '225px')

const currentAdmin = computed(() => authStore.currentAdmin)
const adminInitial = computed(() => currentAdmin.value?.username?.charAt(0).toUpperCase() || 'A')

const breadcrumbName = computed(() => {
  const path = route.path
  const map = {
    '/admin/profile': 'Thông tin cá nhân',
    '/admin/change-password': 'Đổi mật khẩu',
    '/admin/users': 'Quản lý người dùng',
    '/admin/grammar/topics': 'Quản lý ngữ pháp - Topics',
    '/admin/grammar': 'Quản lý ngữ pháp',
    '/admin/reading': 'Quản lý bài đọc',
  }

  // Check dynamic routes
  if (path.startsWith('/admin/grammar/topics/') && path.includes('/lessons')) {
    return 'Quản lý ngữ pháp - Lessons'
  }
  if (path.startsWith('/admin/grammar/lessons/') && path.includes('/questions')) {
    return 'Quản lý ngữ pháp - Questions'
  }

  return map[path] || ''
})

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

const handleMenuSelect = () => {
  // Close mobile menu when selecting a menu item
  if (window.innerWidth <= 768) {
    closeMobileMenu()
  }
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/admin/profile')
      break
    case 'change-password':
      router.push('/admin/change-password')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = async () => {
  await authStore.logoutAdmin()
  router.push('/admin/login')
}
</script>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
}

/* ===== SIDEBAR ===== */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s, transform 0.3s;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

/* Sidebar header */
.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e4e7ed;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
}

.logo-icon {
  width: 32px;
  height: 32px;
}

/* Scrollable menu area */
.sidebar-scrollbar {
  flex: 1;
  overflow-y: auto;
}

.sidebar-scrollbar :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.sidebar-menu {
  border: none;
}

/* ===== SIDEBAR FOOTER ===== */
.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  text-align: center;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  z-index: 10;
}

.collapse-btn {
  transition: transform 0.3s;
}

.collapse-btn:hover {
  transform: scale(1.1);
}

/* ===== MAIN CONTAINER ===== */
.main-container {
  margin-left: 225px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s;
  background: #f5f7fa;
}

/* Khi sidebar collapsed, main container dịch lại */
.sidebar.is-collapsed+.main-container {
  margin-left: 64px;
}

.header {
  height: 60px !important;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.breadcrumb {
  flex: 1;
}

/* Mobile menu button - hidden on desktop */
.mobile-menu-btn {
  display: none;
  margin-right: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  transition: transform 0.3s, background-color 0.3s;
}

.theme-toggle:hover {
  transform: rotate(20deg);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-dropdown:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: bold;
}

/* Admin avatar - màu đỏ để phân biệt */
.admin-avatar {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%) !important;
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

/* ===== MAIN CONTENT - SCROLLABLE ===== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* Mobile overlay */
.mobile-overlay {
  display: none;
}

/* Fade animation */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== RESPONSIVE - MOBILE ===== */
@media (max-width: 768px) {

  /* Show mobile menu button */
  .mobile-menu-btn {
    display: block;
  }

  /* Hide breadcrumb text on very small screens */
  .breadcrumb {
    display: none;
  }

  /* Main container takes full width */
  .main-container {
    margin-left: 0 !important;
  }

  /* Sidebar hidden by default on mobile */
  .sidebar {
    transform: translateX(-100%);
    width: 225px !important;
    /* Always full width on mobile */
  }

  /* Show sidebar when mobile menu is open */
  .sidebar.is-mobile-open {
    transform: translateX(0);
  }

  /* Show overlay when mobile menu is open */
  .mobile-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 99;
  }

  /* Adjust main content padding */
  .main-content {
    padding: 16px;
  }

  /* Header adjustments */
  .header {
    padding: 0 12px;
  }

  .header-actions {
    gap: 8px;
  }

  /* Hide collapse button on mobile */
  .sidebar-footer {
    display: none;
  }

  /* User avatar smaller on mobile */
  .user-avatar {
    width: 32px !important;
    height: 32px !important;
  }
}

/* ===== DARK MODE SUPPORT ===== */
.dark .sidebar {
  background: #1a1a1a;
  border-right-color: #2c2c2c;
}

.dark .sidebar-header {
  border-bottom-color: #2c2c2c;
}

.dark .sidebar-footer {
  border-top-color: #2c2c2c;
  background: #1a1a1a;
}

.dark .header {
  background: #1a1a1a;
  border-bottom-color: #2c2c2c;
}

.dark .user-dropdown:hover {
  background: #2c2c2c;
}
</style>
