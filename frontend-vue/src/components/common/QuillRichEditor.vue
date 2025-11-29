<!-- src/components/common/QuillRichEditor.vue -->
<template>
  <div class="quill-editor-wrapper">
    <QuillEditor
      ref="quillEditorRef"
      v-model:content="localContent"
      content-type="html"
      :theme="theme"
      :toolbar="customToolbar"
      :style="{ height: height }"
      @ready="onEditorReady"
      @update:content="handleContentUpdate"
    />
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

// Props
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  theme: {
    type: String,
    default: 'snow' // 'snow' | 'bubble'
  },
  height: {
    type: String,
    default: '400px'
  },
  placeholder: {
    type: String,
    default: 'Enter content here...'
  },
  toolbar: {
    type: [Array, String],
    default: 'full' // 'full' | 'essential' | 'minimal' | custom array
  },
  readOnly: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:modelValue'])

// Refs
const quillEditorRef = ref(null)
const localContent = ref(props.modelValue || '')

// Custom toolbar configurations
const toolbarConfigs = {
  full: [
    [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
    [{ 'font': [] }],
    [{ 'size': ['small', false, 'large', 'huge'] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'script': 'sub'}, { 'script': 'super' }],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    [{ 'indent': '-1'}, { 'indent': '+1' }],
    [{ 'align': [] }],
    ['blockquote', 'code-block'],
    ['link', 'image', 'video'],
    ['clean']
  ],
  essential: [
    [{ 'header': [1, 2, 3, false] }],
    ['bold', 'italic', 'underline'],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    [{ 'color': [] }, { 'background': [] }],
    ['link', 'image'],
    ['clean']
  ],
  minimal: [
    ['bold', 'italic', 'underline'],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    ['link'],
    ['clean']
  ]
}

// Computed toolbar based on prop
const customToolbar = computed(() => {
  if (Array.isArray(props.toolbar)) {
    return props.toolbar
  }
  return toolbarConfigs[props.toolbar] || toolbarConfigs.full
})

// Watch props.modelValue to sync with parent
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal !== localContent.value) {
      localContent.value = newVal || ''
    }
  }
)

// Handle content update
const handleContentUpdate = (content) => {
  emit('update:modelValue', content)
}

// Editor ready callback
const onEditorReady = (editor) => {
  console.log('✅ Quill editor ready:', editor)

  // Set placeholder
  if (props.placeholder) {
    editor.root.dataset.placeholder = props.placeholder
  }

  // Set readOnly
  if (props.readOnly) {
    editor.enable(false)
  }
}

// Public methods (expose to parent if needed)
defineExpose({
  getEditor: () => quillEditorRef.value?.getQuill(),
  setContent: (content) => {
    localContent.value = content
  },
  clear: () => {
    localContent.value = ''
  }
})
</script>

<style scoped>
.quill-editor-wrapper {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.3s;
}

.quill-editor-wrapper:hover {
  border-color: var(--el-color-primary);
}

.quill-editor-wrapper:focus-within {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
}

/* Toolbar styling */
:deep(.ql-toolbar) {
  background-color: var(--el-fill-color-lighter);
  border: none;
  border-bottom: 1px solid var(--el-border-color);
  padding: 8px;
}

:deep(.ql-toolbar .ql-stroke) {
  stroke: var(--el-text-color-primary);
}

:deep(.ql-toolbar .ql-fill) {
  fill: var(--el-text-color-primary);
}

:deep(.ql-toolbar button:hover) {
  color: var(--el-color-primary);
}

:deep(.ql-toolbar button:hover .ql-stroke) {
  stroke: var(--el-color-primary);
}

:deep(.ql-toolbar button:hover .ql-fill) {
  fill: var(--el-color-primary);
}

:deep(.ql-toolbar button.ql-active) {
  color: var(--el-color-primary);
}

:deep(.ql-toolbar button.ql-active .ql-stroke) {
  stroke: var(--el-color-primary);
}

:deep(.ql-toolbar button.ql-active .ql-fill) {
  fill: var(--el-color-primary);
}

/* Container styling */
:deep(.ql-container) {
  font-family: inherit;
  font-size: 14px;
  background-color: var(--el-bg-color);
  border: none;
}

:deep(.ql-editor) {
  min-height: 250px;
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
  line-height: 1.8;
  color: var(--el-text-color-primary);
}

:deep(.ql-editor:focus) {
  outline: none;
}

/* Placeholder styling */
:deep(.ql-editor.ql-blank::before) {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

/* Scrollbar styling */
:deep(.ql-editor::-webkit-scrollbar) {
  width: 6px;
}

:deep(.ql-editor::-webkit-scrollbar-track) {
  background: var(--el-fill-color-lighter);
}

:deep(.ql-editor::-webkit-scrollbar-thumb) {
  background: var(--el-border-color);
  border-radius: 3px;
}

:deep(.ql-editor::-webkit-scrollbar-thumb:hover) {
  background: var(--el-text-color-secondary);
}

/* Content styling */
:deep(.ql-editor h1) {
  font-size: 2em;
  margin: 0.67em 0;
}

:deep(.ql-editor h2) {
  font-size: 1.5em;
  margin: 0.75em 0;
}

:deep(.ql-editor h3) {
  font-size: 1.17em;
  margin: 0.83em 0;
}

:deep(.ql-editor p) {
  margin: 0.5em 0;
}

:deep(.ql-editor code) {
  background-color: var(--el-fill-color-light);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

:deep(.ql-editor pre.ql-syntax) {
  background-color: var(--el-fill-color-dark);
  color: var(--el-color-white);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

:deep(.ql-editor blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding-left: 16px;
  margin-left: 0;
  color: var(--el-text-color-secondary);
}

:deep(.ql-editor img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

/* Dark mode support */
html.dark :deep(.ql-toolbar) {
  background-color: var(--el-bg-color-overlay);
  border-bottom-color: var(--el-border-color-darker);
}

html.dark :deep(.ql-container) {
  background-color: var(--el-bg-color);
}

html.dark :deep(.ql-editor) {
  color: var(--el-text-color-primary);
}

html.dark :deep(.ql-toolbar .ql-stroke) {
  stroke: var(--el-text-color-regular);
}

html.dark :deep(.ql-toolbar .ql-fill) {
  fill: var(--el-text-color-regular);
}

/* Responsive */
@media (max-width: 768px) {
  :deep(.ql-toolbar) {
    padding: 6px;
  }

  :deep(.ql-editor) {
    padding: 12px;
    font-size: 13px;
  }
}
</style>
