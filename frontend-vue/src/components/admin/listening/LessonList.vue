<template>
  <div class="w-full flex flex-col h-full">
    <!-- Header Controls -->
    <div class="mb-4 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
      <div class="flex gap-3 items-center flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Tìm kiếm bài nghe..."
          prefix-icon="Search"
          clearable
          class="w-full sm:w-64"
        />

        <el-select v-model="filterDifficulty" placeholder="Độ khó" clearable class="! w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="Dễ" value="BEGINNER" />
          <el-option label="Trung bình" value="INTERMEDIATE" />
          <el-option label="Khó" value="ADVANCED" />
        </el-select>

        <el-select v-model="filterStatus" placeholder="Trạng thái" clearable class="!w-40">
          <el-option label="Tất cả" value="" />
          <el-option label="Kích hoạt" :value="true" />
          <el-option label="Ẩn" :value="false" />
        </el-select>
      </div>

      <div class="flex gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate" class="! rounded-lg font-bold">
          Thêm bài nghe
        </el-button>
        <el-button :icon="Refresh" @click="loadLessons" circle />
      </div>
    </div>

    <!-- Empty State -->
    <el-empty
      v-if="! lessons || lessons.length === 0"
      description="Chưa có bài nghe nào"
      :image-size="120"
    >
      <el-button type="primary" @click="handleCreate">Tạo bài nghe đầu tiên</el-button>
    </el-empty>

    <!-- Lessons Table -->
    <el-card
      v-else
      shadow="never"
      class="! border-gray-300 dark:!border-gray-700 ! rounded-xl ! overflow-hidden flex flex-col"
      :body-style="{ padding: '0px', flex: '1', display: 'flex', flexDirection:  'column' }"
    >
      <el-table
        :data="paginatedLessons"
        v-loading="loading"
        style="width: 100%"
        border
        stripe
        row-key="id"
        :header-cell-style="{ background:  '#f9fafb', color: '#6b7280', fontWeight: '600' }"
      >
        <el-table-column label="STT" width="70" align="center" fixed="left">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono text-sm font-bold">{{ row.orderIndex }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Bài nghe" min-width="350">
          <template #default="{ row }">
            <div class="py-2 flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <el-tag
                  :type="getDifficultyType(row.difficulty)"
                  effect="dark"
                  size="small"
                  class="! rounded uppercase ! text-[10px] !h-5 !px-2 tracking-wider"
                >
                  {{ getDifficultyLabel(row.difficulty) }}
                </el-tag>

                <span class="font-bold text-gray-800 dark:text-gray-200">{{ row.title }}</span>

                <el-tag v-if="! row.isActive" type="info" size="small" effect="plain">Ẩn</el-tag>
              </div>

              <div class="flex items-center gap-3 text-xs text-gray-500">
                <span class="flex items-center gap-1">
                  <el-icon><Headset /></el-icon>
                  {{ row.audioUrl ? 'Có audio' : 'Chưa có audio' }}
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><QuestionFilled /></el-icon>
                  {{ row.questionCount || 0 }} câu hỏi
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><Medal /></el-icon>
                  {{ row.pointsReward }} điểm
                </span>
                <span class="flex items-center gap-1">
                  <el-icon><Timer /></el-icon>
                  {{ Math.floor((row.timeLimitSeconds || 0) / 60) }} phút
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Cài đặt phát lại" width="180" align="center">
          <template #default="{ row }">
            <div class="flex flex-col gap-1 text-xs">
              <el-tag :type="row.allowUnlimitedReplay ? 'success' : 'warning'" size="small">
                {{ row.allowUnlimitedReplay ? 'Không giới hạn' : `Tối đa ${row.maxReplayCount} lần` }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isActive"
              @change="handleToggleStatus(row)"
              active-text="Hiện"
              inactive-text="Ẩn"
            />
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-1 justify-center">
              <el-tooltip content="Chỉnh sửa" placement="top">
                <el-button type="primary" :icon="Edit" size="small" circle @click="handleEdit(row)" />
              </el-tooltip>

              <el-tooltip content="Quản lý câu hỏi" placement="top">
                <el-button
                  type="success"
                  :icon="QuestionFilled"
                  size="small"
                  circle
                  @click="$emit('view-questions', row)"
                />
              </el-tooltip>

              <el-tooltip content="Xóa" placement="top">
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  circle
                  @click="handleDelete(row)"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="px-4 py-3 border-t border-gray-200 dark:border-gray-700">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Lesson Form Dialog -->
    <LessonFormDialog
      ref="lessonFormRef"
      @success="loadLessons"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Refresh, Edit, Delete, QuestionFilled, Headset, Medal, Timer } from '@element-plus/icons-vue'
import { useListeningLessonList } from '@/composables/listening/useListeningLessons'
import LessonFormDialog from './LessonFormDialog.vue'

// Composables
const {
  loading,
  lessons,
  searchQuery,
  filterDifficulty,
  filterStatus,
  filteredLessons,
  loadLessons,
  deleteLesson,
  toggleStatus,
  getDifficultyType,
  getDifficultyLabel,
} = useListeningLessonList()

// Refs
const lessonFormRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

// Computed
const paginatedLessons = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredLessons.value.slice(start, end)
})

const totalElements = computed(() => filteredLessons.value.length)

// Emits
const emit = defineEmits(['view-questions'])

// Methods
const handleCreate = () => {
  lessonFormRef.value?.openCreate()
}

const handleEdit = (lesson) => {
  lessonFormRef.value?.openEdit(lesson)
}

const handleDelete = async (lesson) => {
  await deleteLesson(lesson)
}

const handleToggleStatus = async (lesson) => {
  await toggleStatus(lesson)
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// Lifecycle
onMounted(async () => {
  await loadLessons({ page: 0, size: 1000, sort: 'orderIndex,asc' })
})
</script>
