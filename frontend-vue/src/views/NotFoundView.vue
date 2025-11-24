<template>
  <v-container fluid fill-height>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="6" class="text-center">
        <v-icon size="120" color="grey-lighten-1" class="mb-4">mdi-emoticon-sad-outline</v-icon>
        <h1 class="text-h1 font-weight-bold mb-4">404</h1>
        <h2 class="text-h4 mb-4">Trang không tìm thấy</h2>
        <p class="text-h6 text-grey-darken-1 mb-8">
          Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã được di chuyển.
        </p>
        <v-btn size="large" color="primary" to="/" prepend-icon="mdi-home">
          Về trang chủ
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
// No additional logic needed
</script>
