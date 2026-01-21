<template>
  <div class="stat-card">
    <div class="stat-icon" :class="bgClass">
      <el-icon :size="24" :color="iconColor">
        <Calendar v-if="icon === 'calendar'" />
        <Medal v-if="icon === 'medal'" />
        <Reading v-if="icon === 'reading'" />
        <Trophy v-if="icon === 'trophy'" />
      </el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
      <div v-if="subtext" class="stat-subtext">{{ subtext }}</div>
    </div>
  </div>
</template>

<script setup>
import { Calendar, Medal, Reading, Trophy } from '@element-plus/icons-vue'

defineProps({
  icon: {
    type: String,
    required: true,
  },
  value: {
    type: [String, Number],
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  subtext: {
    type: String,
    default: '',
  },
  iconColor: {
    type: String,
    default: '#3b82f6',
  },
  bgClass: {
    type: String,
    default: 'bg-blue-50',
  },
})
</script>

<style scoped>
.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.stat-subtext {
  font-size: 12px;
  color: #10b981;
  margin-top: 4px;
  font-weight: 500;
}

@media (max-width: 640px) {
  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 24px;
  }
}
</style>
