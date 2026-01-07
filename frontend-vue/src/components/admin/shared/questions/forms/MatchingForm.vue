<template>
  <div>
    <el-alert
      title="Nhập các cặp từ tương ứng (Trái - Phải)."
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <div
      v-for="(pair, index) in localPairs"
      :key="index"
      class="flex items-center gap-4 mb-3"
    >
      <div class="w-8 text-center font-bold text-gray-400">{{ index + 1 }}</div>

      <el-input v-model="pair.left" placeholder="Vế Trái" @input="syncToParent" />
      <div class="text-gray-400">↔</div>
      <el-input v-model="pair.right" placeholder="Vế Phải" @input="syncToParent" />

      <el-button
        type="danger"
        :icon="Delete"
        circle
        plain
        @click="removePair(index)"
        :disabled="localPairs.length <= 2"
      />
    </div>

    <el-button type="primary" plain size="small" @click="addPair" class="mt-2">
      + Thêm cặp
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:metadata'])

const localPairs = ref([])

const initData = () => {
  if (props.metadata.pairs && props.metadata.pairs.length > 0) {
    localPairs.value = JSON.parse(JSON.stringify(props.metadata.pairs))
  } else {
    localPairs.value = [
      { left: '', right: '', order: 1 },
      { left: '', right: '', order: 2 },
    ]
    syncToParent()
  }
}

onMounted(() => {
  initData()
})

watch(
  () => props.metadata.pairs,
  (newPairs) => {
    if (newPairs && JSON.stringify(newPairs) !== JSON.stringify(localPairs.value)) {
      localPairs.value = JSON.parse(JSON.stringify(newPairs))
    }
  },
  { deep: true }
)

const syncToParent = () => {
  emit('update:metadata', {
    ...props.metadata,
    pairs: localPairs.value,
  })
}

const addPair = () => {
  localPairs.value.push({
    left: '',
    right: '',
    order: localPairs.value.length + 1,
  })
  syncToParent()
}

const removePair = (index) => {
  localPairs.value.splice(index, 1)
  localPairs.value.forEach((p, i) => (p.order = i + 1))
  syncToParent()
}

const validate = () => {
  if (localPairs.value.length < 2) {
    ElMessage.warning('Cần ít nhất 2 cặp')
    return false
  }
  if (localPairs.value.some((p) => !p.left.trim() || !p.right.trim())) {
    ElMessage.warning('Vui lòng điền đầy đủ 2 vế')
    return false
  }
  return true
}

defineExpose({ validate })
</script>
