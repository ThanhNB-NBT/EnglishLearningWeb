<!-- src/components/admin/shared/questions/SingleQuestionEditor.vue - UPDATED -->
<template>
  <div
    class="w-full bg-white dark:bg-[#1d1d1d] border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden transition-all duration-300 hover:shadow-md"
    :class="{ 'border-blue-400 ring-1 ring-blue-400': !localCollapsed }"
  >
    <!-- Header -->
    <div
      class="flex items-center justify-between px-4 py-3 bg-gray-50 dark:bg-[#252525] cursor-pointer border-b border-transparent hover:bg-gray-100 dark:hover:bg-[#2c2c2c] transition-colors"
      :class="{ '!border-gray-200 dark:!border-gray-700': !localCollapsed }"
      @click="toggleCollapse"
    >
      <div class="flex items-center gap-3 overflow-hidden flex-1">
        <el-icon class="text-gray-400 transition-transform" :class="{ 'rotate-90': !localCollapsed }">
          <CaretRight />
        </el-icon>

        <span class="bg-blue-600 text-white text-xs font-bold px-2 py-0.5 rounded shadow-sm shrink-0">
          #{{ index + 1 }}
        </span>

        <el-select
          v-model="localData.questionType"
          placeholder="Loại"
          class="!w-44 shrink-0"
          size="small"
          @change="handleTypeChange"
          @click.stop
        >
          <el-option-group label="Cơ bản">
            <el-option label="Trắc nghiệm" value="MULTIPLE_CHOICE" />
            <el-option label="Đúng/Sai" value="TRUE_FALSE" />
            <el-option label="Điền từ" value="FILL_BLANK" />
            <el-option label="Chia động từ" value="VERB_FORM" />
            <el-option label="Trả lời ngắn" value="TEXT_ANSWER" />
          </el-option-group>
          <el-option-group label="Nâng cao">
            <el-option label="Nối từ" value="MATCHING" />
            <el-option label="Sắp xếp câu" value="SENTENCE_BUILDING" />
            <el-option label="Viết lại câu" value="SENTENCE_TRANSFORMATION" />
            <el-option label="Tìm lỗi sai" value="ERROR_CORRECTION" />
            <el-option label="Phát âm" value="PRONUNCIATION" />
            <el-option label="Câu hỏi mở" value="OPEN_ENDED" />
          </el-option-group>
        </el-select>

        <!-- ✅ NEW: Show selected TaskGroup in collapsed view -->
        <el-tag
          v-if="localCollapsed && selectedTaskGroupName"
          size="small"
          type="primary"
          effect="dark"
          class="!rounded"
        >
          <el-icon class="mr-1"><Collection /></el-icon>
          {{ selectedTaskGroupName }}
        </el-tag>

        <span
          v-if="localCollapsed"
          class="text-sm text-gray-500 dark:text-gray-400 truncate max-w-[300px] italic ml-2 border-l border-gray-300 pl-2"
          v-html="getPreviewText(localData.questionText)"
        ></span>
      </div>

      <div class="flex items-center gap-2" @click.stop>
        <div class="flex items-center px-2 py-0.5 font-bold" title="Điểm số">
          <span class="text-sm text-gray-500 mr-1">Điểm:</span>
          <el-input-number
            v-model="localData.points"
            :min="1"
            size="small"
            controls-position="right"
            class="!w-16"
            @change="emitUpdate"
          />
        </div>
        <el-tooltip content="Nhân bản">
          <el-button circle size="small" :icon="CopyDocument" @click="$emit('clone')" />
        </el-tooltip>
        <el-tooltip content="Xóa">
          <el-button
            type="danger"
            circle
            size="small"
            plain
            :icon="Delete"
            @click="$emit('remove')"
          />
        </el-tooltip>
      </div>
    </div>

    <!-- Expanded Content -->
    <div v-show="!localCollapsed" class="p-5 bg-white dark:bg-[#1d1d1d]">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Left Column: Question Text -->
        <div class="lg:col-span-7 flex flex-col">
          <div class="flex justify-between mb-2">
            <label class="text-xs font-bold text-gray-500 uppercase flex items-center gap-1">
              <el-icon><EditPen /></el-icon> Nội dung câu hỏi
            </label>
            <el-tag size="small" type="info" effect="plain" class="!text-[10px]">
              Optional
            </el-tag>
          </div>
          <div class="border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden bg-white dark:bg-[#252525] shadow-inner flex-1 min-h-[150px]">
            <QuillRichEditor
              v-model="localData.questionText"
              height="370px"
              toolbar="basic"
              placeholder="Nhập câu hỏi..."
              @update:modelValue="emitUpdate"
            />
          </div>
        </div>

        <!-- Right Column -->
        <div class="lg:col-span-5 flex flex-col gap-4">
          <!-- Explanation -->
          <div>
            <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Giải thích</label>
            <el-input
              v-model="localData.explanation"
              placeholder="Giải thích đáp án..."
              @input="emitUpdate"
            />
          </div>

          <!-- ✅ NEW: TaskGroup Selector (Simplified for Bulk) -->
          <div class="bg-blue-50 dark:bg-blue-900/10 p-3 rounded-lg border border-blue-200 dark:border-blue-800">
            <div class="flex items-center justify-between mb-2">
              <label class="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase flex items-center gap-1">
                <el-icon><Collection /></el-icon> Task Grouping
              </label>
              <el-button
                v-if="localData.taskGroupId"
                @click="clearTaskGroup"
                type="danger"
                text
                size="small"
                :icon="Close"
              >
                Clear
              </el-button>
            </div>

            <el-select
              v-model="localData.taskGroupId"
              placeholder="-- Standalone (No Task) --"
              clearable
              filterable
              class="w-full"
              size="small"
              @change="emitUpdate"
            >
              <el-option :value="null" label="-- Standalone --" />
              <el-option
                v-for="tg in taskGroups"
                :key="tg.id"
                :value="tg.id"
                :label="tg.taskName"
              >
                <div class="flex items-center justify-between">
                  <span>{{ tg.taskName }}</span>
                  <el-tag size="small" type="info">{{ tg.questionCount || 0 }}</el-tag>
                </div>
              </el-option>
            </el-select>

            <!-- Selected Task Info -->
            <div
              v-if="selectedTaskGroupName"
              class="mt-2 text-xs text-gray-600 dark:text-gray-400 italic"
            >
              <el-icon class="mr-1"><InfoFilled /></el-icon>
              {{ selectedTaskGroupInstruction || 'No instruction' }}
            </div>
          </div>

          <!-- Answer Configuration -->
          <div class="flex-1 bg-gray-50 dark:bg-[#252525] p-4 rounded-xl border border-dashed border-gray-300 dark:border-gray-600 flex flex-col">
            <label class="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase mb-3 flex items-center gap-1">
              <el-icon><Tools /></el-icon> Cấu hình đáp án
            </label>

            <component
              v-if="currentFormComponent"
              :is="currentFormComponent"
              :key="localData.questionType"
              v-model:metadata="localData.metadata"
              :question-type="localData.questionType"
              @update:metadata="handleMetadataUpdate"
            />
            <div v-else class="text-center text-gray-400 text-sm py-4">
              Chọn loại câu hỏi để cấu hình
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, defineAsyncComponent } from 'vue'
import {
  Delete,
  CaretRight,
  EditPen,
  Tools,
  CopyDocument,
  Collection,
  Close,
  InfoFilled,
} from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'

const MultipleChoiceForm = defineAsyncComponent(() => import('./forms/MultipleChoiceForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('./forms/FillBlankForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('./forms/MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('./forms/SentenceBuildingForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('./forms/PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('./forms/ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('./forms/OpenEndedForm.vue'))
const SentenceTransformationForm = defineAsyncComponent(() => import('./forms/SentenceTransformationForm.vue'))

const props = defineProps({
  modelValue: { type: Object, required: true },
  index: { type: Number, default: 0 },
  taskGroups: { type: Array, default: () => [] }, // ✅ NEW: Pass from parent
})

const emit = defineEmits(['update:modelValue', 'remove', 'clone'])

const localData = ref({
  questionType: 'MULTIPLE_CHOICE',
  questionText: '',
  points: 10,
  orderIndex: 1,
  explanation: '',
  metadata: {},
  taskGroupId: null, // ✅ NEW: thay thế 3 fields cũ
  ...props.modelValue
})

const localCollapsed = ref(props.modelValue.isCollapsed !== false)

// ✅ Computed: Get selected TaskGroup info
const selectedTaskGroup = computed(() => {
  if (!localData.value.taskGroupId) return null
  return props.taskGroups.find(tg => tg.id === localData.value.taskGroupId)
})

const selectedTaskGroupName = computed(() => selectedTaskGroup.value?.taskName || null)
const selectedTaskGroupInstruction = computed(() => selectedTaskGroup.value?.instruction || null)

const currentFormComponent = computed(() => {
  const map = {
    MULTIPLE_CHOICE: MultipleChoiceForm,
    TRUE_FALSE: MultipleChoiceForm,
    FILL_BLANK: FillBlankForm,
    TEXT_ANSWER: FillBlankForm,
    MATCHING: MatchingForm,
    SENTENCE_BUILDING: SentenceBuildingForm,
    SENTENCE_TRANSFORMATION: SentenceTransformationForm,
    ERROR_CORRECTION: ErrorCorrectionForm,
    PRONUNCIATION: PronunciationForm,
    OPEN_ENDED: OpenEndedForm,
  }
  return map[localData.value.questionType] || null
})

watch(() => props.modelValue, (newVal) => {
  if (JSON.stringify(newVal) !== JSON.stringify(localData.value)) {
    localData.value = { ...localData.value, ...newVal }
  }
}, { deep: true })

const emitUpdate = () => {
  const emittedData = {
    ...localData.value,
    isCollapsed: localCollapsed.value
  }
  console.log('📤 Emitting to parent:', emittedData)
  emit('update:modelValue', emittedData)
}

const handleMetadataUpdate = (newMetadata) => {
  console.log('📝 Metadata updated:', newMetadata)
  localData.value.metadata = newMetadata
  emitUpdate()
}

const toggleCollapse = () => {
  localCollapsed.value = !localCollapsed.value
  emitUpdate()
}

const handleTypeChange = () => {
  localData.value.metadata = {}
  localCollapsed.value = false
  emitUpdate()
}

const clearTaskGroup = () => {
  localData.value.taskGroupId = null
  emitUpdate()
}

const getPreviewText = (html) => {
  if (!html || html === '<p><br></p>') return '<span class="text-gray-400">(Trống)</span>'
  const div = document.createElement('div')
  div.innerHTML = html
  const text = div.textContent || div.innerText || ''
  return text.length > 50 ? text.substring(0, 50) + '...' : text
}
</script>

<style scoped>
.rotate-90 {
  transform: rotate(90deg);
}
</style>
