<template>
  <div class="quill-editor-container">
    <div ref="editorRef" :style="{ height: height }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import Quill from 'quill'
// Import CSS gốc của Quill 1.3.7
import 'quill/dist/quill.snow.css'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Nhập nội dung...' },
  height: { type: String, default: '200px' },
  toolbar: { type: [String, Array], default: 'full' } // 'full', 'basic', 'question'
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus'])
const editorRef = ref(null)
let quill = null

// Cấu hình Toolbar cho các trường hợp khác nhau
const getToolbarOptions = () => {
  if (props.toolbar === 'basic') {
    return [
      ['bold', 'italic', 'underline'],
      [{ 'list': 'ordered'}, { 'list': 'bullet' }],
      ['clean']
    ]
  }

  if (props.toolbar === 'question') {
    return [
      ['bold', 'italic', 'underline', 'strike'],
      [{ 'script': 'sub'}, { 'script': 'super' }],
      [{ 'color': [] }, { 'background': [] }],
      ['clean']
    ]
  }

  // Full toolbar (mặc định)
  return [
    [{ 'header': [1, 2, 3, false] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'script': 'sub'}, { 'script': 'super' }],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    [{ 'indent': '-1'}, { 'indent': '+1' }],
    [{ 'align': [] }],
    ['link', 'image'], // Bỏ video nếu không cần
    ['clean']
  ]
}

onMounted(() => {
  if (!editorRef.value) return

  // Khởi tạo Quill
  quill = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: props.placeholder,
    modules: {
      // TUYỆT ĐỐI KHÔNG KHAI BÁO 'table': true Ở ĐÂY VỚI QUILL 1.3.7
      toolbar: getToolbarOptions(),
      keyboard: {
        bindings: {
          // Custom bindings nếu cần
        }
      }
    }
  })

  // Set nội dung ban đầu
  if (props.modelValue) {
    quill.root.innerHTML = props.modelValue
  }

  // Lắng nghe sự thay đổi
  quill.on('text-change', () => {
    let html = quill.root.innerHTML
    if (html === '<p><br></p>') html = ''
    emit('update:modelValue', html)
  })

  quill.on('selection-change', (range) => {
    if (range) emit('focus', range)
    else emit('blur')
  })
})

// Watch modelValue để cập nhật lại editor nếu thay đổi từ bên ngoài
watch(() => props.modelValue, (newVal) => {
  if (quill && newVal !== quill.root.innerHTML) {
    // Check sơ bộ để tránh loop, gán lại nếu khác biệt
    // (Có thể cần xử lý cursor position nếu update real-time phức tạp)
    if (!newVal) {
        quill.root.innerHTML = ''
    } else {
        const currentContent = quill.root.innerHTML
        if (newVal !== currentContent) {
            quill.root.innerHTML = newVal
        }
    }
  }
})

onBeforeUnmount(() => {
  quill = null
})
</script>

<style scoped>
/* Tùy chỉnh giao diện Quill cho đẹp hơn */
.quill-editor-container {
  display: flex;
  flex-direction: column;
  background-color: white;
}

:deep(.ql-toolbar) {
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  border-color: #dcdfe6;
  background-color: #f5f7fa;
}

:deep(.ql-container) {
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
  border-color: #dcdfe6;
  font-size: 14px;
}

/* Dark mode support */
html.dark .quill-editor-container {
  background-color: #1d1d1d;
}
html.dark :deep(.ql-toolbar) {
  background-color: #252525;
  border-color: #4c4d4f;
}
html.dark :deep(.ql-container) {
  border-color: #4c4d4f;
  color: #e5eaf3;
}
html.dark :deep(.ql-stroke) {
  stroke: #a3a6ad;
}
html.dark :deep(.ql-fill) {
  fill: #a3a6ad;
}
html.dark :deep(.ql-picker) {
  color: #a3a6ad;
}
</style>
