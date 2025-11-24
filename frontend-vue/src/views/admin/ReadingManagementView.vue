<template>
  <div class="reading-management-page">
    <div class="container">
      <h1 class="page-title">Quản lý bài đọc</h1>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 24px"
      >
        <template #title>
          Chức năng đang được phát triển
        </template>
        Trang quản lý bài đọc sẽ sớm được cập nhật.
      </el-alert>

      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>Danh sách bài đọc</span>
            <el-button type="primary" :icon="Plus" disabled>
              Thêm bài đọc
            </el-button>
          </div>
        </template>

        <el-empty
          description="Chức năng đang được phát triển"
          :image-size="150"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
</script>

<style scoped>
.reading-management-page {
  padding: 24px;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 24px 0;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
