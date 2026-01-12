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

        <!-- ✅ FIX: Use native input with manual binding -->
        <input
          type="text"
          :value="classifications[index].word"
          @input="handleWordChange(index, $event.target.value)"
          placeholder="Nhập từ vựng (VD: books)"
          class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />

        <!-- ✅ FIX: Use native select instead of el-select -->
        <select
          :value="classifications[index].category"
          @change="handleCategoryChange(index, $event.target.value)"
          class="w-48 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
        >
          <option value="">Chọn nhóm</option>
          <option v-for="cat in categories" :key="cat" :value="cat">
            {{ cat }}
          </option>
        </select>

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
  const words = classifications.value.map((c) => c.word).filter((w) => w)

  emit('update:metadata', {
    ...props.metadata,
    categories: categories.value,
    classifications: classifications.value,
    words: words,
  })
}

// ✅ FIX: Direct mutation with force update
const handleWordChange = (index, value) => {
  // Create new array to trigger reactivity
  const newClassifications = [...classifications.value]
  newClassifications[index] = {
    ...newClassifications[index],
    word: value
  }
  classifications.value = newClassifications
  syncToParent()
}

const handleCategoryChange = (index, value) => {
  // Create new array to trigger reactivity
  const newClassifications = [...classifications.value]
  newClassifications[index] = {
    ...newClassifications[index],
    category: value
  }
  classifications.value = newClassifications
  syncToParent()
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
  const hasEmpty = classifications.value.some((c) => !c.word.trim() || !c.category)
  if (hasEmpty) {
    ElMessage.warning('Vui lòng nhập từ vựng và chọn nhóm cho tất cả các dòng')
    return false
  }
  return true
}

defineExpose({ validate })
</script>

<style scoped>
/* Match Element Plus styling for native inputs */
input[type="text"],
select {
  font-family: inherit;
  font-size: 14px;
  transition: all 0.2s;
}

input[type="text"]:hover,
select:hover {
  border-color: #c0c4cc;
}

input[type="text"]:focus,
select:focus {
  border-color: #409eff;
}

select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23606266' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 30px;
}
</style>
