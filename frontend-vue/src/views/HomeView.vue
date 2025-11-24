<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">English Learning Platform</h1>
        <p class="hero-subtitle">
          Nâng cao kỹ năng tiếng Anh của bạn với các bài học tương tác
        </p>
        <div class="hero-actions">
          <template v-if="!isLoggedIn">
            <el-button
              type="primary"
              size="large"
              @click="$router.push('/auth/register')"
            >
              Bắt đầu ngay
            </el-button>
            <el-button
              size="large"
              @click="$router.push('/auth/login')"
            >
              Đăng nhập
            </el-button>
          </template>
          <el-button
            v-else
            type="primary"
            size="large"
            @click="$router.push('/dashboard')"
          >
            Vào Dashboard
          </el-button>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div class="container">
        <h2 class="section-title">Tính năng nổi bật</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <el-icon :size="48" color="#409eff">
                  <Reading />
                </el-icon>
              </div>
              <h3 class="feature-title">Ngữ pháp</h3>
              <p class="feature-description">
                Học ngữ pháp tiếng Anh qua các bài học được cấu trúc rõ ràng và bài tập thực hành
              </p>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <el-icon :size="48" color="#67c23a">
                  <Document />
                </el-icon>
              </div>
              <h3 class="feature-title">Đọc hiểu</h3>
              <p class="feature-description">
                Cải thiện kỹ năng đọc hiểu với các bài đọc đa dạng và câu hỏi kiểm tra
              </p>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <el-icon :size="48" color="#e6a23c">
                  <TrendCharts />
                </el-icon>
              </div>
              <h3 class="feature-title">Theo dõi tiến độ</h3>
              <p class="feature-description">
                Xem báo cáo chi tiết về quá trình học tập và thành tích của bạn
              </p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <div class="cta-content">
        <h2 class="cta-title">Sẵn sàng bắt đầu chưa?</h2>
        <p class="cta-subtitle">
          Tham gia cùng hàng nghìn học viên đang học tiếng Anh mỗi ngày
        </p>
        <el-button
          v-if="!isLoggedIn"
          type="primary"
          size="large"
          @click="$router.push('/auth/register')"
        >
          Đăng ký miễn phí
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Reading, Document, TrendCharts } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 124px);
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 120px 24px;
  text-align: center;
  color: white;
}

.hero-title {
  font-size: 48px;
  font-weight: bold;
  margin: 0 0 16px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.hero-subtitle {
  font-size: 20px;
  margin: 0 0 32px 0;
  opacity: 0.95;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* Features Section */
.features-section {
  padding: 80px 24px;
  background: white;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-size: 36px;
  font-weight: bold;
  text-align: center;
  margin: 0 0 48px 0;
  color: #303133;
}

.feature-card {
  text-align: center;
  height: 100%;
  transition: transform 0.3s;
  margin-bottom: 24px;
}

.feature-card:hover {
  transform: translateY(-8px);
}

.feature-icon {
  margin-bottom: 16px;
}

.feature-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 12px 0;
  color: #303133;
}

.feature-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

/* CTA Section */
.cta-section {
  background: #f5f7fa;
  padding: 80px 24px;
  text-align: center;
}

.cta-title {
  font-size: 36px;
  font-weight: bold;
  margin: 0 0 16px 0;
  color: #303133;
}

.cta-subtitle {
  font-size: 18px;
  color: #606266;
  margin: 0 0 32px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .section-title,
  .cta-title {
    font-size: 28px;
  }
}
</style>
