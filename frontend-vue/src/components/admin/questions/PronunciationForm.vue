<!-- src/components/admin/questions/PronunciationForm.vue -->
<template>
  <div class="pronunciation-form">
    <!-- Hint (Optional) -->
    <el-form-item label="Hint (Optional)">
      <el-input
        v-model="localMetadata.hint"
        type="textarea"
        :rows="2"
        placeholder="Enter a hint to help users..."
        maxlength="200"
        show-word-limit
        @input="emitUpdate"
      />
    </el-form-item>

    <!-- Words -->
    <el-form-item label="Words to Classify" required>
      <el-input
        v-model="wordsInput"
        placeholder="Enter words separated by commas (e.g., cat, hat, cake, make)"
        @input="handleWordsInput"
      />
    </el-form-item>

    <!-- Words Preview -->
    <el-form-item label="Words Preview" v-if="localMetadata.words.length > 0">
      <el-space wrap>
        <el-tag
          v-for="(word, index) in localMetadata.words"
          :key="index"
          closable
          @close="removeWord(index)"
        >
          {{ word }}
        </el-tag>
      </el-space>
    </el-form-item>

    <!-- Categories -->
    <el-form-item label="Pronunciation Categories" required>
      <el-input
        v-model="categoriesInput"
        placeholder="Enter categories separated by commas (e.g., /æ/, /eɪ/)"
        @input="handleCategoriesInput"
      />
      <template #extra>
        <el-text size="small" type="info">
          Example: /æ/ (as in "cat"), /eɪ/ (as in "cake")
        </el-text>
      </template>
    </el-form-item>

    <!-- Categories Preview -->
    <el-form-item label="Categories Preview" v-if="localMetadata.categories.length > 0">
      <el-space wrap>
        <el-tag
          v-for="(category, index) in localMetadata.categories"
          :key="index"
          type="success"
          closable
          @close="removeCategory(index)"
        >
          {{ category }}
        </el-tag>
      </el-space>
    </el-form-item>

    <!-- Classifications -->
    <el-form-item label="Correct Classifications" required>
      <el-space direction="vertical" fill style="width: 100%">
        <div
          v-for="(classification, index) in localMetadata.classifications"
          :key="index"
          class="classification-item"
        >
          <el-card shadow="hover" :body-style="{ padding: '12px' }">
            <div class="classification-header">
              <el-text tag="b">{{ classification.word }}</el-text>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                @click="removeClassification(index)"
              />
            </div>

            <el-select
              v-model="classification.category"
              placeholder="Select category"
              style="width: 100%; margin-top: 8px"
              @change="emitUpdate"
            >
              <el-option
                v-for="cat in localMetadata.categories"
                :key="cat"
                :label="cat"
                :value="cat"
              />
            </el-select>
          </el-card>
        </div>

        <!-- Add Classification Button -->
        <el-button
          type="primary"
          :icon="Plus"
          @click="addClassification"
          :disabled="localMetadata.words.length === 0 || localMetadata.categories.length === 0"
          style="width: 100%"
        >
          Add Classification
        </el-button>
      </el-space>
    </el-form-item>

    <!-- Validation Info -->
    <el-alert
      v-if="validationError"
      :title="validationError"
      type="error"
      :closable="false"
      style="margin-top: 12px"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  words: props.metadata.words || [],
  categories: props.metadata.categories || [],
  classifications: props.metadata.classifications || [],
})

const wordsInput = ref(
  props.metadata.words ? props.metadata.words.join(', ') : ''
)

const categoriesInput = ref(
  props.metadata.categories ? props.metadata.categories.join(', ') : ''
)

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        words: newVal.words || [],
        categories: newVal.categories || [],
        classifications: newVal.classifications || [],
      }
      wordsInput.value = newVal.words ? newVal.words.join(', ') : ''
      categoriesInput.value = newVal.categories ? newVal.categories.join(', ') : ''
    }
  },
  { deep: true }
)

const validationError = computed(() => {
  if (localMetadata.value.words.length < 2) {
    return 'Need at least 2 words'
  }

  if (localMetadata.value.categories.length < 2) {
    return 'Need at least 2 categories'
  }

  if (localMetadata.value.classifications.length === 0) {
    return 'Need at least 1 classification'
  }

  const hasEmptyCategory = localMetadata.value.classifications.some(
    (c) => !c.category || c.category.trim() === ''
  )
  if (hasEmptyCategory) {
    return 'All classifications must have a category'
  }

  return null
})

const handleWordsInput = () => {
  const words = wordsInput.value
    .split(',')
    .map((w) => w.trim())
    .filter((w) => w.length > 0)

  localMetadata.value.words = words
  emitUpdate()
}

const removeWord = (index) => {
  localMetadata.value.words.splice(index, 1)
  wordsInput.value = localMetadata.value.words.join(', ')
  emitUpdate()
}

const handleCategoriesInput = () => {
  const categories = categoriesInput.value
    .split(',')
    .map((c) => c.trim())
    .filter((c) => c.length > 0)

  localMetadata.value.categories = categories
  emitUpdate()
}

const removeCategory = (index) => {
  localMetadata.value.categories.splice(index, 1)
  categoriesInput.value = localMetadata.value.categories.join(', ')
  emitUpdate()
}

const addClassification = () => {
  if (localMetadata.value.words.length > 0) {
    localMetadata.value.classifications.push({
      word: localMetadata.value.words[0],
      category: '',
    })
    emitUpdate()
  }
}

const removeClassification = (index) => {
  localMetadata.value.classifications.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.pronunciation-form {
  padding: 8px 0;
}

.classification-item {
  width: 100%;
}

.classification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
</style>
