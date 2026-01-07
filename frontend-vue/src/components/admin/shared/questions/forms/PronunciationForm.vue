<template>
  <div>
    <el-alert
      title="Tạo các nhóm phát âm (Categories) và phân loại từ vào nhóm tương ứng."
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
    />

    <div class="mb-6 bg-gray-50 p-4 rounded border border-gray-200">
      <h4 class="font-bold text-sm text-gray-700 mb-2">Bước 1: Định nghĩa các nhóm (Categories)</h4>
      <div class="flex gap-2 mb-2">
        <el-input
          v-model="newCategory"
          placeholder="Nhập ký hiệu phiên âm (VD: /s/, /z/)..."
          @keyup.enter="addCategory"
        />
        <el-button type="primary" @click="addCategory">Thêm nhóm</el-button>
      </div>

      <div class="flex flex-wrap gap-2 mt-2">
        <el-tag
          v-for="(cat, idx) in categories"
          :key="idx"
          closable
          @close="removeCategory(idx)"
          size="large"
        >
          {{ cat }}
        </el-tag>
        <span v-if="!categories?.length" class="text-xs text-gray-400 italic">
          Chưa có nhóm nào.
        </span>
      </div>
    </div>

    <div>
      <h4 class="font-bold text-sm text-gray-700 mb-2">Bước 2: Thêm từ và phân loại</h4>

      <div
        v-for="(item, index) in classifications"
        :key="index"
        class="flex items-center gap-3 mb-2"
      >
        <div class="w-8 text-gray-400 font-bold text-center">{{ index + 1 }}</div>

        <el-input
          v-model="item.word"
          placeholder="Nhập từ vựng (VD: books)"
          class="flex-1"
          @input="syncToParent"
        />

        <el-select
          v-model="item.category"
          placeholder="Chọn nhóm"
          class="w-48"
          @change="syncToParent"
        >
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>

        <el-button type="danger" :icon="Delete" circle plain @click="removeClassification(index)" />
      </div>

      <el-button
        type="primary"
        plain
        size="small"
        @click="addClassification"
        class="mt-2"
        :disabled="!categories?.length"
      >
        + Thêm từ
      </el-button>
      <div v-if="!categories?.length" class="text-xs text-red-400 mt-1">
        Vui lòng tạo ít nhất 1 nhóm ở Bước 1 trước.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const categories = ref([])
const classifications = ref([])
const newCategory = ref('')

const initData = () => {
  if (props.metadata.categories) {
    categories.value = [...props.metadata.categories]
    classifications.value = props.metadata.classifications
      ? JSON.parse(JSON.stringify(props.metadata.classifications))
      : []
  } else {
    categories.value = []
    classifications.value = []
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => [props.metadata.categories, props.metadata.classifications],
  ([newCategories, newClassifications]) => {
    if (newCategories && JSON.stringify(newCategories) !== JSON.stringify(categories.value)) {
      categories.value = [...newCategories]
    }
    if (
      newClassifications &&
      JSON.stringify(newClassifications) !== JSON.stringify(classifications.value)
    ) {
      classifications.value = JSON.parse(JSON.stringify(newClassifications))
    }
  },
  { deep: true }
)

const syncToParent = () => {
  // Đồng bộ words từ classifications
  const words = classifications.value.map((c) => c.word).filter((w) => w)

  emit('update:metadata', {
    ...props.metadata,
    categories: categories.value,
    classifications: classifications.value,
    words: words,
  })
}

// Logic Categories
const addCategory = () => {
  const cat = newCategory.value.trim()
  if (!cat) return
  if (categories.value.includes(cat)) {
    ElMessage.warning('Nhóm này đã tồn tại')
    return
  }
  categories.value.push(cat)
  newCategory.value = ''
  syncToParent()
}

const removeCategory = (index) => {
  const catToRemove = categories.value[index]
  categories.value.splice(index, 1)

  // Xóa category của các classification liên quan
  classifications.value.forEach((c) => {
    if (c.category === catToRemove) c.category = ''
  })
  syncToParent()
}

// Logic Classifications
const addClassification = () => {
  classifications.value.push({ word: '', category: '' })
  syncToParent()
}

const removeClassification = (index) => {
  classifications.value.splice(index, 1)
  syncToParent()
}

const validate = () => {
  if (!categories.value || categories.value.length < 2) {
    ElMessage.warning('Cần ít nhất 2 nhóm phát âm (Categories)')
    return false
  }
  if (!classifications.value || classifications.value.length < 2) {
    ElMessage.warning('Cần ít nhất 2 từ vựng để phân loại')
    return false
  }
  // Check empty fields
  const hasEmpty = classifications.value.some((c) => !c.word.trim() || !c.category)
  if (hasEmpty) {
    ElMessage.warning('Vui lòng nhập từ vựng và chọn nhóm cho tất cả các dòng')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
