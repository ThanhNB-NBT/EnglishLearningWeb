<template>
  <div
    class="w-full bg-white dark:bg-[#1d1d1d] border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden transition-all duration-300 hover:shadow-md"
    :class="{ 'border-blue-400 ring-1 ring-blue-400': !isCollapsed }"
  >
    <div
      class="flex items-center justify-between px-4 py-3 bg-gray-50 dark:bg-[#252525] cursor-pointer border-b border-transparent hover:bg-gray-100 dark:hover:bg-[#2c2c2c] transition-colors"
      :class="{ '!border-gray-200 dark:!border-gray-700': !isCollapsed }"
      @click="toggleCollapse"
    >
      <div class="flex items-center gap-3 overflow-hidden flex-1">
        <el-icon class="text-gray-400 transition-transform" :class="{ 'rotate-90': !isCollapsed }"
          ><CaretRight
        /></el-icon>

        <span
          class="bg-blue-600 text-white text-xs font-bold px-2 py-0.5 rounded shadow-sm shrink-0"
          >#{{ index + 1 }}</span
        >

        <el-select
          v-model="localQuestion.questionType"
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

        <span
          v-if="isCollapsed"
          class="text-sm text-gray-500 dark:text-gray-400 truncate max-w-[300px] italic ml-2 border-l border-gray-300 pl-2"
          v-html="getPreviewText(localQuestion.questionText)"
        ></span>
      </div>

      <div class="flex items-center gap-2" @click.stop>
        <div
          class="flex items-center px-2 py-0.5 font-bold"
          title="Điểm số"
        >
          <span class="text-sm text-gray-500 mr-1">Điểm:</span>
          <el-input-number
            v-model="localQuestion.points"
            :min="1"
            size="small"
            controls-position="right"
            class="!w-13"
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

    <div v-show="!isCollapsed" class="p-5 bg-white dark:bg-[#1d1d1d]">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div class="lg:col-span-7 flex flex-col">
          <div class="flex justify-between mb-2">
            <label class="text-xs font-bold text-gray-500 uppercase flex items-center gap-1"
              ><el-icon><EditPen /></el-icon> Nội dung câu hỏi</label
            >
            <el-tag size="small" type="info" effect="plain" class="!text-[10px]"
              >Optional (Có thể để trống)</el-tag
            >
          </div>
          <div
            class="border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden bg-white dark:bg-[#252525] shadow-inner flex-1 min-h-[150px]"
          >
            <QuillRichEditor
              v-model="localQuestion.questionText"
              height="370px"
              toolbar="basic"
              placeholder="Nhập câu hỏi (nếu cần)..."
            />
          </div>
        </div>

        <div class="lg:col-span-5 flex flex-col gap-4">
          <div class="flex gap-3">
            <div class="flex-1">
              <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Giải thích</label>
              <el-input v-model="localQuestion.explanation" placeholder="Giải thích đáp án..." />
            </div>
          </div>

          <div
            class="flex-1 bg-gray-50 dark:bg-[#252525] p-4 rounded-xl border border-dashed border-gray-300 dark:border-gray-600 flex flex-col"
          >
            <label
              class="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase mb-3 block flex items-center gap-1"
              ><el-icon><Tools /></el-icon> Cấu hình đáp án</label
            >
            <div class="flex-1">
              <component
                :is="getFormComponent(localQuestion.questionType)"
                v-model:metadata="localQuestion.metadata"
                :question-type="localQuestion.questionType"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineAsyncComponent } from 'vue'
import { Delete, CaretRight, EditPen, Tools, CopyDocument } from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'

// Import Lazy các form con (Giữ nguyên)
const MultipleChoiceForm = defineAsyncComponent(() => import('./MultipleChoiceForm.vue'))
const FillBlankForm = defineAsyncComponent(() => import('./FillBlankForm.vue'))
const MatchingForm = defineAsyncComponent(() => import('./MatchingForm.vue'))
const SentenceBuildingForm = defineAsyncComponent(() => import('./SentenceBuildingForm.vue'))
const PronunciationForm = defineAsyncComponent(() => import('./PronunciationForm.vue'))
const ErrorCorrectionForm = defineAsyncComponent(() => import('./ErrorCorrectionForm.vue'))
const OpenEndedForm = defineAsyncComponent(() => import('./OpenEndedForm.vue'))
const VerbFormForm = defineAsyncComponent(() => import('./VerbFormForm.vue'))
const SentenceTransformationForm = defineAsyncComponent(
  () => import('./SentenceTransformationForm.vue'),
)
const props = defineProps({
  modelValue: { type: Object, required: true },
  index: { type: Number, default: 0 },
})

const emit = defineEmits(['update:modelValue', 'remove', 'clone'])

const localQuestion = ref(JSON.parse(JSON.stringify(props.modelValue)))
const isCollapsed = ref(props.modelValue.isCollapsed ?? true)

// --- FIX LỖI RECURSIVE UPDATE ---
// 1. Watch prop change (từ parent) -> update local
watch(() => props.modelValue, (newVal) => {
  // Chỉ update nếu dữ liệu thực sự thay đổi để tránh vòng lặp
  if (JSON.stringify(newVal) !== JSON.stringify(localQuestion.value)) {
    localQuestion.value = JSON.parse(JSON.stringify(newVal))
  }
  // Sync collapse state
  if (newVal.isCollapsed !== undefined && newVal.isCollapsed !== isCollapsed.value) {
    isCollapsed.value = newVal.isCollapsed
  }
}, { deep: true })

// 2. Watch local change -> emit up (to parent)
watch(localQuestion, (newVal) => {
  // Emit bản copy kèm trạng thái collapsed
  const payload = { ...newVal, isCollapsed: isCollapsed.value }
  // So sánh ngược lại để tránh emit dư thừa (nguyên nhân chính gây loop)
  if (JSON.stringify(payload) !== JSON.stringify(props.modelValue)) {
    emit('update:modelValue', payload)
  }
}, { deep: true })

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  // Emit để parent biết trạng thái UI đã đổi (nếu parent quản lý)
  emit('update:modelValue', { ...localQuestion.value, isCollapsed: isCollapsed.value })
}

const handleTypeChange = () => {
  localQuestion.value.metadata = {}
  isCollapsed.value = false // Mở ra khi đổi loại để nhập liệu
  emit('update:modelValue', { ...localQuestion.value, isCollapsed: false })
}

const getPreviewText = (html) => {
  if (!html || html === '<p><br></p>') return '<span class="text-gray-400">(Trống)</span>'
  const div = document.createElement('div')
  div.innerHTML = html
  const text = div.textContent || div.innerText || ''
  return text.length > 50 ? text.substring(0, 50) + '...' : text
}

const getFormComponent = (type) => {
  const map = {
    MULTIPLE_CHOICE: MultipleChoiceForm,
    TRUE_FALSE: MultipleChoiceForm,
    FILL_BLANK: FillBlankForm,
    TEXT_ANSWER: FillBlankForm,
    VERB_FORM: VerbFormForm,
    MATCHING: MatchingForm,
    SENTENCE_BUILDING: SentenceBuildingForm,
    SENTENCE_TRANSFORMATION: SentenceTransformationForm,
    ERROR_CORRECTION: ErrorCorrectionForm,
    PRONUNCIATION: PronunciationForm,
    OPEN_ENDED: OpenEndedForm,
  }
  return map[type]
}
</script>
