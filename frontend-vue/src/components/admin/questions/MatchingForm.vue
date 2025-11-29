<!-- src/components/admin/questions/MatchingForm.vue -->
<template>
  <div class="matching-form">
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

    <!-- Pairs -->
    <el-form-item label="Matching Pairs" required>
      <el-space direction="vertical" fill style="width: 100%">
        <div
          v-for="(pair, index) in localMetadata.pairs"
          :key="index"
          class="pair-item"
        >
          <el-card shadow="hover" :body-style="{ padding: '12px' }">
            <div class="pair-header">
              <el-text tag="b">Pair {{ index + 1 }}</el-text>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                @click="removePair(index)"
                :disabled="localMetadata.pairs.length <= 2"
              />
            </div>

            <el-row :gutter="12" style="margin-top: 8px">
              <!-- Left Side -->
              <el-col :span="11">
                <el-input
                  v-model="pair.left"
                  placeholder="Left side (e.g., word)"
                  @input="emitUpdate"
                />
              </el-col>

              <!-- Arrow Icon -->
              <el-col :span="2" style="text-align: center; line-height: 32px">
                <el-icon size="20"><Right /></el-icon>
              </el-col>

              <!-- Right Side -->
              <el-col :span="11">
                <el-input
                  v-model="pair.right"
                  placeholder="Right side (e.g., definition)"
                  @input="emitUpdate"
                />
              </el-col>
            </el-row>
          </el-card>
        </div>

        <!-- Add Pair Button -->
        <el-button type="primary" :icon="Plus" @click="addPair" style="width: 100%">
          Add Pair
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
import { Plus, Delete, Right } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  pairs: props.metadata.pairs || [
    { left: '', right: '', order: 1 },
    { left: '', right: '', order: 2 },
  ],
})

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        pairs: newVal.pairs || localMetadata.value.pairs,
      }
    }
  },
  { deep: true }
)

const validationError = computed(() => {
  if (!localMetadata.value.pairs || localMetadata.value.pairs.length < 2) {
    return 'Need at least 2 pairs'
  }

  const hasEmptyLeft = localMetadata.value.pairs.some((p) => !p.left || p.left.trim() === '')
  const hasEmptyRight = localMetadata.value.pairs.some((p) => !p.right || p.right.trim() === '')

  if (hasEmptyLeft || hasEmptyRight) {
    return 'All pairs must have both left and right values'
  }

  return null
})

const addPair = () => {
  const nextOrder = localMetadata.value.pairs.length + 1
  localMetadata.value.pairs.push({
    left: '',
    right: '',
    order: nextOrder,
  })
  emitUpdate()
}

const removePair = (index) => {
  if (localMetadata.value.pairs.length <= 2) return

  localMetadata.value.pairs.splice(index, 1)

  // Reorder
  localMetadata.value.pairs.forEach((pair, idx) => {
    pair.order = idx + 1
  })

  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.matching-form {
  padding: 8px 0;
}

.pair-item {
  width: 100%;
}

.pair-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
</style>
