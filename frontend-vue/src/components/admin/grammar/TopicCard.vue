<template>
  <el-card
    class="topic-card"
    :body-style="{ padding: '0px', height: '100%', display: 'flex', flexDirection: 'column' }"
    shadow="hover"
  >
    <div class="topic-header" :class="getLevelClass(topic.levelRequired)">
      <div class="header-content">
        <span class="topic-level-badge">{{ topic.levelRequired }}</span>
      </div>

      <div class="header-switch" @click.stop>
        <el-tooltip :content="topic.isActive ? 'Đang hoạt động' : 'Đã tắt'" placement="top">
          <el-switch
            v-model="localActive"
            size="small"
            style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
            @change="handleToggleActive"
          />
        </el-tooltip>
      </div>
    </div>

    <div class="topic-body">
      <h3 class="topic-title" :title="topic.name">{{ topic.name }}</h3>

      <p class="topic-desc">
        {{ topic.description || 'Chưa có mô tả.' }}
      </p>

      <div class="topic-meta">
        <span class="meta-item">
          <el-icon><Document /></el-icon> {{ topic.totalLessons || 0 }} bài
        </span>
        <span class="meta-item">
          <el-icon><Timer /></el-icon> STT: {{ topic.orderIndex }}
        </span>
      </div>
    </div>

    <div class="topic-footer">
      <el-button
        type="primary"
        plain
        size="small"
        class="manage-btn"
        @click="$emit('view-lessons', topic)"
      >
        Quản lý bài học
      </el-button>

      <div class="footer-actions">
        <el-tooltip content="Chỉnh sửa" placement="top">
          <el-button link type="primary" class="icon-btn" @click="$emit('edit', topic)">
            <el-icon :size="18"><Edit /></el-icon>
          </el-button>
        </el-tooltip>

        <el-tooltip content="Xóa" placement="top">
          <el-button link type="danger" class="icon-btn" @click="$emit('delete', topic)">
            <el-icon :size="18"><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Document, Timer, Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  topic: { type: Object, required: true }
})

const emit = defineEmits(['edit', 'delete', 'view-lessons', 'toggle-active'])

const localActive = ref(false)

watch(() => props.topic?.isActive, (newVal) => {
  localActive.value = newVal
}, { immediate: true })

const handleToggleActive = (val) => {
  emit('toggle-active', { ...props.topic, isActive: val })
}

const getLevelClass = (level) => {
  switch(level) {
    case 'BEGINNER': return 'bg-beginner';
    case 'INTERMEDIATE': return 'bg-intermediate';
    case 'ADVANCED': return 'bg-advanced';
    default: return 'bg-beginner';
  }
}
</script>

<style scoped>
.topic-card {
  height: 100%;
  transition: transform 0.2s ease-in-out;
  border-radius: 12px; /* Bo góc mềm mại hơn */
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
}
.topic-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-light);
}

/* Header: Tăng Padding */
.topic-header {
  height: 50px;
  padding: 0 20px; /* Padding rộng hơn */
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.bg-beginner { background-color: #f0f9eb; border-left: 4px solid #67c23a; }
.bg-intermediate { background-color: #fdf6ec; border-left: 4px solid #e6a23c; }
.bg-advanced { background-color: #ecf5ff; border-left: 4px solid #409eff; }

html.dark .bg-beginner { background-color: #1e2b20; border-left-color: #467a4b; }
html.dark .bg-intermediate { background-color: #2b2318; border-left-color: #916d31; }
html.dark .bg-advanced { background-color: #18222c; border-left-color: #335d88; }

.topic-level-badge {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--el-text-color-regular);
}

/* Body: Tăng Padding */
.topic-body {
  padding: 20px; /* Padding rộng hơn cho nội dung */
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topic-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 10px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topic-desc {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 20px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 42px;
}

.topic-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: auto;
}
.meta-item { display: flex; align-items: center; gap: 6px; }

/* Footer: Tăng Padding & Spacing */
.topic-footer {
  padding: 12px 20px; /* Padding rộng hơn */
  background-color: #f9faFc;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Footer Dark Mode */
html.dark .topic-footer {
  background-color: #1d1e1f;
  border-top-color: #363637;
}

.footer-actions {
  display: flex;
  gap: 12px; /* Khoảng cách giữa các nút icon */
}

.icon-btn {
  padding: 6px; /* Tăng vùng click */
  margin-left: 0 !important;
}
</style>
