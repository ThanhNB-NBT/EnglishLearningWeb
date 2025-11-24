<template>
  <div class="auth-layout">
    <div class="auth-container">
      <!-- Logo/Brand -->
      <div class="auth-brand">
        <router-link to="/" class="brand-link">
          <h1 class="brand-title">English Learning</h1>
        </router-link>
      </div>

      <!-- Auth Card -->
      <el-card class="auth-card" shadow="always">
        <router-view />
      </el-card>

      <!-- Back to Home -->
      <div class="auth-footer">
        <el-button link @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          Quay về trang chủ
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.auth-container {
  width: 100%;
  max-width: 450px;
}

.auth-brand {
  text-align: center;
  margin-bottom: 16px;
}

.brand-link {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: white;
  transition: transform 0.3s;
}

.brand-link:hover {
  transform: scale(1.05);
}

.brand-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.brand-title {
  font-size: 32px;
  font-weight: bold;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.auth-card {
  border-radius: 12px;
  overflow: hidden;
}

.auth-card :deep(.el-card__body) {
  padding: 40px;
}

.auth-footer {
  margin-top: 10px;
  text-align: center;
}

.auth-footer .el-button {
  color: white;
  font-size: 14px;
}

.auth-footer .el-button:hover {
  color: rgba(255, 255, 255, 0.8);
}

/* Responsive */
@media (max-width: 576px) {
  .auth-layout {
    padding: 16px;
  }

  .auth-card :deep(.el-card__body) {
    padding: 24px;
  }

  .brand-title {
    font-size: 24px;
  }
}
</style>
