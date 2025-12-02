<template>
  <div class="matching-form">
    <el-alert title="Lưu ý: Hệ thống sẽ tự động xáo trộn vị trí khi hiển thị cho người dùng." type="warning"
      :closable="false" class="mb-4" show-icon />

    <el-form-item label="Các cặp từ nối" required>
      <div class="pairs-container">
        <transition-group name="list">
          <div v-for="(pair, index) in localMetadata.pairs" :key="index" class="pair-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }">
              <div class="pair-row">
                <div class="pair-index">
                  <el-tag type="info" size="small">{{ index + 1 }}</el-tag>
                </div>

                <div class="pair-col">
                  <el-input v-model="pair.item1" placeholder="Vế A (VD: Hello)" @input="emitUpdate">
                    <template #prepend>A</template>
                  </el-input>
                </div>

                <div class="pair-icon">
                  <el-icon>
                    <Switch />
                  </el-icon>
                </div>

                <div class="pair-col">
                  <el-input v-model="pair.item2" placeholder="Vế B (VD: Xin chào)" @input="emitUpdate">
                    <template #prepend>B</template>
                  </el-input>
                </div>

                <el-button type="danger" icon="Delete" circle plain size="small" @click="removePair(index)"
                  :disabled="localMetadata.pairs.length <= 2" />
              </div>
            </el-card>
          </div>
        </transition-group>

        <el-button type="primary" plain :icon="Plus" class="w-full mt-2 dashed-btn" @click="addPair">
          Thêm cặp mới
        </el-button>
      </div>
    </el-form-item>

    <el-alert v-if="validationError" :title="validationError" type="error" show-icon :closable="false" class="mt-4" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Switch } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

// Init data: Mặc định 2 cặp
const localMetadata = ref({
  pairs: props.metadata?.pairs || [
    { item1: '', item2: '' },
    { item1: '', item2: '' }
  ]
})

watch(() => props.metadata, (newVal) => {
  if (newVal && newVal.pairs) {
    localMetadata.value.pairs = newVal.pairs
  }
}, { deep: true })

const validationError = computed(() => {
  if (localMetadata.value.pairs.length < 2) return 'Cần ít nhất 2 cặp để nối.'
  const empty = localMetadata.value.pairs.some(p => !p.item1.trim() || !p.item2.trim())
  if (empty) return 'Vui lòng điền đầy đủ thông tin cho cả 2 vế.'
  return null
})

// Actions
const addPair = () => {
  localMetadata.value.pairs.push({ item1: '', item2: '' })
  emitUpdate()
}

const removePair = (index) => {
  if (localMetadata.value.pairs.length <= 2) return
  localMetadata.value.pairs.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.matching-form {
  padding: 10px 0;
}

.pair-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pair-col {
  flex: 1;
}

.pair-icon {
  color: #909399;
  display: flex;
  align-items: center;
}

.w-full {
  width: 100%;
}

.dashed-btn {
  border-style: dashed;
}

.mb-3 {
  margin-bottom: 12px;
}

.mt-4 {
  margin-top: 16px;
}

/* Transition */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

@media (max-width: 600px) {
  .pair-row {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
    position: relative;
  }

  .pair-icon {
    transform: rotate(90deg);
    justify-content: center;
    margin: 4px 0;
  }

  .pair-index {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 1;
  }

  .el-button {
    align-self: flex-end;
  }
}
</style>
