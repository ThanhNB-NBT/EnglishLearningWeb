<template>
  <div class="multiple-choice-form">
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

    <!-- Options -->
    <el-form-item label="Options" required>
      <el-space direction="vertical" fill style="width: 100%">
        <div
          v-for="(option, index) in localMetadata.options"
          :key="index"
          class="option-item"
        >
          <el-card shadow="hover" :body-style="{ padding: '12px' }">
            <div class="option-header">
              <el-text tag="b">Option {{ index + 1 }}</el-text>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                @click="removeOption(index)"
                :disabled="localMetadata.options.length <= 2"
              />
            </div>

            <el-row :gutter="12" style="margin-top: 8px">
              <!-- Option Text -->
              <el-col :span="16">
                <el-input
                  v-model="option.text"
                  placeholder="Enter option text..."
                  @input="emitUpdate"
                />
              </el-col>

              <!-- Is Correct -->
              <el-col :span="4">
                <el-checkbox
                  v-model="option.isCorrect"
                  label="Correct"
                  @change="handleCorrectChange(index)"
                />
              </el-col>

              <!-- Order -->
              <el-col :span="4">
                <el-input-number
                  v-model="option.order"
                  :min="1"
                  :max="localMetadata.options.length"
                  controls-position="right"
                  style="width: 100%"
                  @change="emitUpdate"
                />
              </el-col>
            </el-row>
          </el-card>
        </div>

        <!-- Add Option Button -->
        <el-button type="primary" :icon="Plus" @click="addOption" style="width: 100%">
          Add Option
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
  options: props.metadata.options || [
    { text: '', isCorrect: false, order: 1 },
    { text: '', isCorrect: false, order: 2 },
  ],
})

// Watch props.metadata changes
watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        options: newVal.options || localMetadata.value.options,
      }
    }
  },
  { deep: true }
)

const validationError = computed(() => {
  if (!localMetadata.value.options || localMetadata.value.options.length < 2) {
    return 'Need at least 2 options'
  }

  const hasCorrect = localMetadata.value.options.some((opt) => opt.isCorrect)
  if (!hasCorrect) {
    return 'At least one option must be marked as correct'
  }

  const hasEmptyText = localMetadata.value.options.some((opt) => !opt.text || opt.text.trim() === '')
  if (hasEmptyText) {
    return 'All options must have text'
  }

  return null
})

const addOption = () => {
  const nextOrder = localMetadata.value.options.length + 1
  localMetadata.value.options.push({
    text: '',
    isCorrect: false,
    order: nextOrder,
  })
  emitUpdate()
}

const removeOption = (index) => {
  if (localMetadata.value.options.length <= 2) return

  localMetadata.value.options.splice(index, 1)

  // Reorder
  localMetadata.value.options.forEach((opt, idx) => {
    opt.order = idx + 1
  })

  emitUpdate()
}

const handleCorrectChange = (index) => {
  // For multiple choice, only one can be correct
  localMetadata.value.options.forEach((opt, idx) => {
    if (idx !== index) {
      opt.isCorrect = false
    }
  })
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.multiple-choice-form {
  padding: 8px 0;
}

.option-item {
  width: 100%;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
</style>
