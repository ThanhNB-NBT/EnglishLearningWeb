<template>
  <div class="reading-page">
    <div class="container">
      <h1 class="page-title">Đọc hiểu</h1>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 24px"
      >
        <template #title>
          Chức năng đang được phát triển
        </template>
        Trang đọc hiểu sẽ sớm được cập nhật với các bài đọc và câu hỏi thực hành.
      </el-alert>

      <el-empty
        description="Nội dung đọc hiểu sẽ được cập nhật sớm"
        :image-size="200"
      >
        <el-button type="primary" @click="$router.push('/dashboard')">
          Quay về Dashboard
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
// Placeholder view - sẽ implement sau
</script>

<style scoped>
.reading-page {
  padding: 24px;
  min-height: calc(100vh - 124px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 24px 0;
  color: #303133;
}
</style>
