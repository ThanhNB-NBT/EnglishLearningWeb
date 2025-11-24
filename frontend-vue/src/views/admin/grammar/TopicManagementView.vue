<template>
  <div class="topic-management">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">Quản lý Grammar Topics</h2>
        <p class="page-subtitle">Quản lý các chủ đề ngữ pháp</p>
      </div>
      <div class="header-actions">
        <el-button
          type="info"
          :icon="RefreshRight"
          @click="grammarStore.validateAllTopicsOrder"
          :loading="grammarStore.loading"
        >
          Validate Order
        </el-button>
        <el-button
          type="primary"
          :icon="Plus"
          @click="topicForm.openCreateDialog"
        >
          Tạo Topic Mới
        </el-button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-cards">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <el-icon class="stat-icon" :size="32" color="#409eff">
            <Reading />
          </el-icon>
          <div class="stat-info">
            <div class="stat-value">{{ grammarStore.topicPagination.total }}</div>
            <div class="stat-label">Tổng Topics</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <el-icon class="stat-icon" :size="32" color="#67c23a">
            <CircleCheck />
          </el-icon>
          <div class="stat-info">
            <div class="stat-value">{{ activeTopicsCount }}</div>
            <div class="stat-label">Topics Đang Hoạt Động</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <el-icon class="stat-icon" :size="32" color="#f56c6c">
            <CircleClose />
          </el-icon>
          <div class="stat-info">
            <div class="stat-value">{{ inactiveTopicsCount }}</div>
            <div class="stat-label">Topics Đã Tắt</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Table Card -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="grammarStore.topics"
        v-loading="grammarStore.loading"
        stripe
        style="width: 100%"
      >
        <el-table-column
          prop="orderIndex"
          label="STT"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.orderIndex }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="name"
          label="Tên Topic"
          min-width="200"
        >
          <template #default="{ row }">
            <div class="topic-name">
              <strong>{{ row.name }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          prop="description"
          label="Mô Tả"
          min-width="300"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="text-secondary">{{ row.description || 'Chưa có mô tả' }}</span>
          </template>
        </el-table-column>

        <el-table-column
          prop="levelRequired"
          label="Level"
          width="150"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.levelRequired)" size="small">
              {{ getLevelLabel(row.levelRequired) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="isActive"
          label="Trạng Thái"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              :model-value="row.isActive"
              @change="grammarStore.toggleTopicActive(row.id, row.isActive)"
              :loading="grammarStore.loading"
            />
          </template>
        </el-table-column>

        <el-table-column
          label="Thao Tác"
          width="240"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button-group>
              <el-button
                size="small"
                :icon="Document"
                @click="handleViewLessons(row)"
              >
                Lessons
              </el-button>
              <el-button
                size="small"
                type="primary"
                :icon="Edit"
                @click="topicForm.openEditDialog(row)"
              >
                Sửa
              </el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="grammarStore.deleteTopic(row.id, row.name)"
              >
                Xóa
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="grammarStore.topicPagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="topicForm.dialogVisible"
      :title="topicForm.dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="topicForm.formData"
        :rules="topicForm.formRules"
        label-position="top"
        label-width="120px"
      >
        <el-form-item label="Tên Topic" prop="name">
          <el-input
            v-model="topicForm.formData.name"
            placeholder="Nhập tên topic..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="Mô Tả" prop="description">
          <el-input
            v-model="topicForm.formData.description"
            type="textarea"
            :rows="4"
            placeholder="Nhập mô tả topic..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Level Yêu Cầu" prop="levelRequired">
              <el-select
                v-model="topicForm.formData.levelRequired"
                placeholder="Chọn level"
                style="width: 100%"
              >
                <el-option
                  v-for="option in topicForm.levelOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                >
                  <span>{{ option.label }}</span>
                  <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
                    {{ option.description }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Thứ Tự" prop="orderIndex">
              <el-input-number
                v-model="topicForm.formData.orderIndex"
                :min="1"
                :max="999"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Trạng Thái">
          <el-switch
            v-model="topicForm.formData.isActive"
            active-text="Hoạt động"
            inactive-text="Tắt"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="topicForm.closeDialog">Hủy</el-button>
        <el-button
          type="primary"
          @click="handleFormSubmit"
          :loading="grammarStore.loading"
        >
          {{ topicForm.submitButtonText }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGrammarStore } from '@/stores/grammar'
import { useGrammarTopicForm } from '@/composables/grammar/useGrammarTopics'
import {
  Plus, Edit, Delete, Document, RefreshRight,
  Reading, CircleCheck, CircleClose
} from '@element-plus/icons-vue'

const router = useRouter()
const grammarStore = useGrammarStore()
const topicForm = useGrammarTopicForm()

const currentPage = ref(1)
const pageSize = ref(10)
const formRef = ref(null) // Local form ref

// Pass formRef to composable for validation
const handleFormSubmit = async () => {
  await topicForm.handleSubmit(formRef.value)
}

// Computed
const activeTopicsCount = computed(() =>
  grammarStore.topics.filter(t => t.isActive).length
)

const inactiveTopicsCount = computed(() =>
  grammarStore.topics.filter(t => !t.isActive).length
)

// Methods
const getLevelTagType = (level) => {
  const types = {
    BEGINNER: 'success',
    INTERMEDIATE: 'warning',
    ADVANCED: 'danger',
  }
  return types[level] || 'info'
}

const getLevelLabel = (level) => {
  const labels = {
    BEGINNER: 'Beginner',
    INTERMEDIATE: 'Intermediate',
    ADVANCED: 'Advanced',
  }
  return labels[level] || level
}

const handleViewLessons = (topic) => {
  router.push({
    name: 'admin-grammar-lessons',
    params: { topicId: topic.id },
  })
}

const handlePageChange = (page) => {
  currentPage.value = page
  grammarStore.fetchTopics(page - 1, pageSize.value)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  grammarStore.fetchTopics(0, size)
}

// Lifecycle
onMounted(() => {
  grammarStore.fetchTopics(0, pageSize.value)
})
</script>

<style scoped>
.topic-management {
  padding: 0;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--el-text-color-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* Stats Cards */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

/* Table */
.table-card {
  border-radius: 8px;
}

.topic-name strong {
  color: var(--el-text-color-primary);
}

.text-secondary {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
