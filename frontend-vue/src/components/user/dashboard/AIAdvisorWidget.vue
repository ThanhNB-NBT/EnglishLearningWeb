<template>
  <div class="ai-widget-card">
    <div class="widget-header">
      <div class="title-area">
        <span class="ai-emoji">🤖</span>
        <h3>AI Learning Mentor</h3>
        <span class="beta-tag">BETA</span>
      </div>
      <el-button
        link
        type="primary"
        :loading="aiStore.isLoading"
        @click="refresh"
      >
        Làm mới
      </el-button>
    </div>

    <div class="widget-body">
      <div v-if="aiStore.isLoading" class="state-box">
        <el-skeleton animated :rows="3" />
        <p class="thinking-text">Gemini đang phân tích hồ sơ của bạn...</p>
      </div>

      <div v-else-if="!aiStore.recommendations || aiStore.recommendations.length === 0" class="state-box">
        <p>👋 Chào bạn! Hãy làm bài tập để AI có dữ liệu phân tích nhé.</p>
        <el-button type="primary" size="small" @click="$router.push('/grammar')">Bắt đầu học</el-button>
      </div>

      <div v-else class="rec-list">
        <div
          v-for="rec in aiStore.recommendations"
          :key="rec.id"
          class="rec-item"
          :class="['priority-' + rec.priority]"
        >
          <div class="rec-content">
            <div class="rec-top">
              <span class="rec-title">{{ rec.title }}</span>
              <el-tag size="small" :type="getSkillColor(rec.targetSkill)">
                {{ rec.targetSkill }}
              </el-tag>
            </div>
            <p class="rec-desc">{{ rec.description }}</p>
            <div class="rec-reason" v-if="rec.reasoning">
              💡 {{ rec.reasoning }}
            </div>
          </div>
          <el-button type="primary" size="small" plain @click="handleAction(rec)">
            Go
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAIRecommendationStore } from '@/stores/aiRecommendation'
import { useRouter } from 'vue-router'

const aiStore = useAIRecommendationStore()
const router = useRouter()

onMounted(() => {
  if (aiStore.recommendations.length === 0) {
    aiStore.fetchRecommendations()
  }
})

const refresh = () => {
  aiStore.fetchRecommendations()
}

const getSkillColor = (skill) => {
  const map = { 'GRAMMAR': 'success', 'READING': 'warning', 'LISTENING': 'danger' }
  return map[skill] || 'info'
}

const handleAction = (rec) => {
  if (rec.targetSkill) {
      router.push(`/${rec.targetSkill.toLowerCase()}`)
  } else {
      router.push('/grammar')
  }
}
</script>

<style scoped>
.ai-widget-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}
.widget-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.title-area { display: flex; align-items: center; gap: 8px; }
.title-area h3 { margin: 0; font-size: 16px; font-weight: 700; color: #2c3e50; }
.beta-tag { font-size: 10px; background: #f0f0f0; padding: 2px 4px; border-radius: 4px; }

.rec-list { display: flex; flex-direction: column; gap: 10px; }
.rec-item {
  display: flex; gap: 10px; padding: 10px;
  background: #f9fafb; border-radius: 8px; border-left: 3px solid #ccc;
  align-items: center;
}
/* Màu sắc theo mức độ ưu tiên */
.rec-item.priority-5 { border-left-color: #ef4444; background: #fef2f2; }
.rec-item.priority-4 { border-left-color: #f59e0b; }
.rec-item.priority-3 { border-left-color: #3b82f6; }

.rec-content { flex: 1; }
.rec-title { font-weight: 600; font-size: 14px; margin-right: 8px; }
.rec-desc { margin: 4px 0 0; font-size: 13px; color: #555; }
.rec-reason { font-size: 11px; color: #888; font-style: italic; margin-top: 4px; }

.state-box { text-align: center; padding: 20px; color: #777; font-size: 13px; }
.thinking-text { font-style: italic; margin-top: 8px; animation: pulse 1.5s infinite; }
@keyframes pulse { 0% { opacity: 0.6; } 50% { opacity: 1; } 100% { opacity: 0.6; } }
</style>
