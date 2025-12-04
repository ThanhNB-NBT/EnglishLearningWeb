<template>
  <div class="quill-editor-wrapper" :class="{ 'is-disabled': readOnly }">
    <div ref="editorRef" :style="{ height: height, width: width }"></div>

    <div v-if="showWordCount" class="editor-footer">
      <div class="word-count">
        <el-text size="small" type="info">
          {{ wordCount }} từ • {{ charCount }} ký tự
        </el-text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

// Modules
import BlotFormatter from 'quill-blot-formatter'
import MarkdownShortcuts from 'quill-markdown-shortcuts'

// --- 1. CẤU HÌNH FONT CHỮ ---
const Font = Quill.import('attributors/class/font')
const fontList = [
  'arial', 'times-new-roman', 'verdana', 'tahoma',
  'courier-new', 'georgia', 'trebuchet-ms', 'impact'
]
Font.whitelist = fontList
Quill.register(Font, true)

// --- 2. ĐĂNG KÝ MODULES ---
if (!Quill.imports['modules/blotFormatter']) {
  Quill.register('modules/blotFormatter', BlotFormatter)
}
if (!Quill.imports['modules/markdownShortcuts']) {
  Quill.register('modules/markdownShortcuts', MarkdownShortcuts)
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  theme: { type: String, default: 'snow' },
  height: { type: String, default: '300px' },
  width: { type: String, default: '100%' },
  placeholder: { type: String, default: 'Nhập nội dung...' },
  toolbar: { type: [Array, String], default: 'full' },
  readOnly: { type: Boolean, default: false },
  showWordCount: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'text-change', 'ready'])

const editorRef = ref(null)
let quillInstance = null
let toolbarContainer = null

const wordCount = ref(0)
const charCount = ref(0)
const isLocalChange = ref(false)

// --- HTML TOOLBAR BẢNG ---
const getTableToolbarHTML = () => {
  return `
    <span class="ql-formats table-controls">
      <button type="button" class="ql-table-insert" title="Chèn bảng 3x3">
        <svg viewBox="0 0 18 18"><rect class="ql-stroke" height="12" width="12" x="3" y="3"></rect><rect class="ql-fill" height="2" width="3" x="5" y="5"></rect><rect class="ql-fill" height="2" width="3" x="10" y="5"></rect><rect class="ql-fill" height="2" width="3" x="5" y="11"></rect><rect class="ql-fill" height="2" width="3" x="10" y="11"></rect></svg>
      </button>
      <button type="button" class="ql-table-row-add" title="Thêm dòng dưới">
        <svg viewBox="0 0 18 18"><path class="ql-fill" d="M14,4H4A2,2,0,0,0,2,6V12a2,2,0,0,0,2,2H14a2,2,0,0,0,2-2V6A2,2,0,0,0,14,4ZM4,6H14V8H4V6Zm0,6V10H14v2Z"/><rect class="ql-fill" height="2" width="8" x="5" y="15"></rect></svg>
      </button>
      <button type="button" class="ql-table-col-add" title="Thêm cột phải">
        <svg viewBox="0 0 18 18"><path class="ql-fill" d="M12,2H6A2,2,0,0,0,4,4V14a2,2,0,0,0,2,2h6a2,2,0,0,0,2-2V4A2,2,0,0,0,12,2ZM6,4H8V14H6V4Zm6,10H10V4h2Z"/><rect class="ql-fill" height="8" width="2" x="15" y="5"></rect></svg>
      </button>
      <button type="button" class="ql-table-delete-row" title="Xóa dòng">
        <svg viewBox="0 0 18 18"><line class="ql-stroke" x1="2" x2="16" y1="9" y2="9"></line><path class="ql-fill" d="M14,4H4A2,2,0,0,0,2,6V12a2,2,0,0,0,2,2H14a2,2,0,0,0,2-2V6A2,2,0,0,0,14,4ZM4,6H14V8H4V6Zm0,6V10H14v2Z"/></svg>
      </button>
      <button type="button" class="ql-table-delete-col" title="Xóa cột">
        <svg viewBox="0 0 18 18"><line class="ql-stroke" x1="9" x2="9" y1="2" y2="16"></line><path class="ql-fill" d="M12,2H6A2,2,0,0,0,4,4V14a2,2,0,0,0,2,2h6a2,2,0,0,0,2-2V4A2,2,0,0,0,12,2ZM6,4H8V14H6V4Zm6,10H10V4h2Z"/></svg>
      </button>
    </span>
  `
}

// --- CẤU HÌNH TOOLBAR CHÍNH ---
const toolbarOptions = {
  full: [
    // FONT & SIZE (Đầy đủ)
    [{ 'font': [false, ...fontList] }, { 'size': ['small', false, 'large', 'huge'] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ 'color': [] }, { 'background': [] }], // Bảng màu
    [{ 'script': 'sub' }, { 'script': 'super' }],
    [{ 'header': 1 }, { 'header': 2 }, 'blockquote', 'code-block'],
    [{ 'list': 'ordered' }, { 'list': 'bullet' }, { 'list': 'check' }],
    [{ 'indent': '-1' }, { 'indent': '+1' }],
    [{ 'align': [] }],
    ['link', 'image', 'video', 'formula'],
    ['clean']
  ],
  question: [
    [{ 'header': [2, 3, false] }],
    [{ 'font': [false, 'arial', 'times-new-roman'] }],
    ['bold', 'italic', 'underline', 'strike', 'code-block'],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
    ['link', 'image'],
    ['clean']
  ]
}

onMounted(() => {
  if (!editorRef.value) return

  // Tạo container cho toolbar
  toolbarContainer = document.createElement('div')
  editorRef.value.parentNode.insertBefore(toolbarContainer, editorRef.value)

  let baseToolbarConfig = Array.isArray(props.toolbar)
    ? props.toolbar
    : (toolbarOptions[props.toolbar] || toolbarOptions.full)

  // Init Quill (Native Table)
  quillInstance = new Quill(editorRef.value, {
    theme: props.theme,
    modules: {
      toolbar: { container: toolbarContainer },
      table: true,
      blotFormatter: {},
      markdownShortcuts: {}
    },
    placeholder: props.placeholder,
    readOnly: props.readOnly
  })

  // Re-init (Trick để render toolbar chuẩn từ mảng config)
  toolbarContainer.remove()
  quillInstance = new Quill(editorRef.value, {
    theme: props.theme,
    modules: {
      toolbar: baseToolbarConfig,
      table: true,
      blotFormatter: {},
      markdownShortcuts: {}
    },
    placeholder: props.placeholder,
    readOnly: props.readOnly
  })

  // Inject Custom Buttons
  injectTableButtons()

  if (props.modelValue) {
    quillInstance.root.innerHTML = props.modelValue
    updateCounts()
  }

  // Handle Changes
  quillInstance.on('text-change', (delta, oldDelta, source) => {
    isLocalChange.value = true
    const html = quillInstance.root.innerHTML
    const finalHtml = (html === '<p><br></p>') ? '' : html

    emit('update:modelValue', finalHtml)
    emit('text-change', { delta, oldDelta, source })
    updateCounts()

    nextTick(() => { isLocalChange.value = false })
  })

  // Fix button types (tránh reload form)
  const allButtons = quillInstance.getModule('toolbar').container.querySelectorAll('button')
  allButtons.forEach(btn => btn.setAttribute('type', 'button'))

  emit('ready', quillInstance)
})

const injectTableButtons = () => {
  const toolbarModule = quillInstance.getModule('toolbar')
  if (!toolbarModule) return

  const container = toolbarModule.container
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = getTableToolbarHTML()
  const tableGroup = tempDiv.firstElementChild
  container.appendChild(tableGroup)

  const tableModule = quillInstance.getModule('table')

  const actions = {
    '.ql-table-insert': () => tableModule.insertTable(3, 3),
    '.ql-table-row-add': () => tableModule.insertRowBelow(),
    '.ql-table-col-add': () => tableModule.insertColumnRight(),
    '.ql-table-delete-row': () => tableModule.deleteRow(),
    '.ql-table-delete-col': () => tableModule.deleteColumn()
  }

  Object.keys(actions).forEach(selector => {
    const btn = tableGroup.querySelector(selector)
    if (btn) {
      btn.addEventListener('mousedown', (e) => {
        e.preventDefault()
        try { actions[selector]() } catch (err) { }
      })
    }
  })
}

// Watchers
watch(() => props.modelValue, (newVal) => {
  if (!quillInstance) return
  if (isLocalChange.value) return // Chặn vòng lặp
  if (newVal === quillInstance.root.innerHTML) return

  const range = quillInstance.getSelection()
  quillInstance.root.innerHTML = newVal || ''
  if (range) {
    try { quillInstance.setSelection(range) } catch (e) { }
  }
  updateCounts()
})

watch(() => props.readOnly, (val) => {
  if (quillInstance) quillInstance.enable(!val)
})

const updateCounts = () => {
  if (!quillInstance) return
  const text = quillInstance.getText().trim()
  if (!text) { wordCount.value = 0; charCount.value = 0; return }
  charCount.value = text.length
  wordCount.value = text.split(/\s+/).filter(w => w.length > 0).length
}

onBeforeUnmount(() => {
  quillInstance = null
})

defineExpose({
  getEditor: () => quillInstance,
  setContent: (content) => { if (quillInstance) { quillInstance.root.innerHTML = content; updateCounts() } },
  clear: () => { if (quillInstance) { quillInstance.root.innerHTML = ''; updateCounts() } }
})
</script>

<style scoped>
/* =======================================================
   BASE STYLES (Chế độ Sáng)
   ======================================================= */
.quill-editor-wrapper {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  width: 100% !important;
  background: var(--el-bg-color);
  display: flex; flex-direction: column;
}

/* Toolbar */
:deep(.ql-toolbar) {
  border: none !important;
  border-bottom: 1px solid var(--el-border-color-lighter) !important;
  background: var(--el-fill-color-light);
  border-radius: 8px 8px 0 0;
  padding: 8px 12px;
}

/* Editor */
:deep(.ql-container) {
  border: none !important;
  font-family: inherit; font-size: 15px; flex: 1;
}
:deep(.ql-editor) {
  padding: 20px; min-height: inherit; line-height: 1.6;
}

/* Dropdown Menu (Font, Size, Color) */
:deep(.ql-picker-options) {
  max-height: 250px;
  overflow-y: auto;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  z-index: 9999 !important;
  border-radius: 4px;
}

/* Font Faces & Display Names */
:deep(.ql-font-arial) { font-family: 'Arial', sans-serif; }
:deep(.ql-font-times-new-roman) { font-family: 'Times New Roman', serif; }
:deep(.ql-font-verdana) { font-family: 'Verdana', sans-serif; }
:deep(.ql-font-tahoma) { font-family: 'Tahoma', sans-serif; }
:deep(.ql-font-courier-new) { font-family: 'Courier New', monospace; }
:deep(.ql-font-georgia) { font-family: 'Georgia', serif; }
:deep(.ql-font-trebuchet-ms) { font-family: 'Trebuchet MS', sans-serif; }
:deep(.ql-font-impact) { font-family: 'Impact', sans-serif; }

:deep(.ql-picker.ql-font .ql-picker-label[data-value="arial"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="arial"]::before) { content: 'Arial'; font-family: 'Arial'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="times-new-roman"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="times-new-roman"]::before) { content: 'Times New Roman'; font-family: 'Times New Roman'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="verdana"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="verdana"]::before) { content: 'Verdana'; font-family: 'Verdana'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="tahoma"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="tahoma"]::before) { content: 'Tahoma'; font-family: 'Tahoma'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="courier-new"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="courier-new"]::before) { content: 'Courier New'; font-family: 'Courier New'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="georgia"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="georgia"]::before) { content: 'Georgia'; font-family: 'Georgia'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="trebuchet-ms"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="trebuchet-ms"]::before) { content: 'Trebuchet MS'; font-family: 'Trebuchet MS'; }
:deep(.ql-picker.ql-font .ql-picker-label[data-value="impact"]::before), :deep(.ql-picker.ql-font .ql-picker-item[data-value="impact"]::before) { content: 'Impact'; font-family: 'Impact'; }

/* Table Styles */
:deep(.ql-editor table) { width: 100%; border-collapse: collapse; margin: 10px 0; table-layout: fixed; }
:deep(.ql-editor td) { border: 1px solid #dcdfe6; padding: 8px; min-width: 50px; }

/* Custom Buttons (Chế độ Sáng) */
:deep(.table-controls button) {
  width: 28px !important; height: 24px !important; padding: 2px !important; margin-right: 2px;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: none; cursor: pointer;
  color: #606266; /* Màu xám chuẩn */
}
:deep(.table-controls button svg) { width: 18px; height: 18px; fill: currentColor; }
:deep(.table-controls button .ql-stroke) { stroke: currentColor; stroke-width: 1.5; fill: none; }
:deep(.table-controls button .ql-fill) { fill: currentColor; }
:deep(.table-controls button:hover) { color: var(--el-color-primary); }
:deep(.ql-table-delete-row:hover), :deep(.ql-table-delete-col:hover) { color: #f56c6c !important; }

/* Footer */
.editor-footer { border-top: 1px solid var(--el-border-color-lighter); padding: 8px 16px; background-color: var(--el-fill-color-lighter); display: flex; justify-content: flex-end; font-size: 12px; color: var(--el-text-color-secondary); }
.word-count { display: flex; align-items: center; gap: 8px; }

/* =======================================================
   DARK MODE STYLES (ĐÃ ĐỒNG BỘ MÀU)
   ======================================================= */

/* 1. Nền và Viền */
html.dark .quill-editor-wrapper { background: var(--el-bg-color); border-color: var(--el-border-color-darker); }
html.dark :deep(.ql-toolbar) { background-color: var(--el-bg-color-overlay); border-bottom-color: var(--el-border-color-darker) !important; }
html.dark .editor-footer { background-color: var(--el-bg-color-overlay); border-top-color: var(--el-border-color-darker); }
html.dark :deep(.ql-editor td) { border-color: #4c4d4f; }

/* 2. Đồng bộ màu Icon (Cả nút mặc định & nút bảng) */
/* Ép tất cả nút có màu xám sáng (tương tự var(--el-text-color-regular)) */
html.dark :deep(.ql-toolbar button),
html.dark :deep(.ql-picker-label) {
  color: #A3A6AD !important; /* Màu xám sáng chuẩn Dark Mode */
}

/* Đảm bảo SVG dùng màu currentColor */
html.dark :deep(.ql-toolbar .ql-stroke) { stroke: currentColor !important; }
html.dark :deep(.ql-toolbar .ql-fill) { fill: currentColor !important; }

/* Hover & Active States (Chuyển sang màu xanh) */
html.dark :deep(.ql-toolbar button:hover),
html.dark :deep(.ql-toolbar button.ql-active),
html.dark :deep(.ql-picker-label:hover),
html.dark :deep(.ql-picker-label.ql-active) {
  color: var(--el-color-primary) !important;
}

/* 3. FIX DROPDOWN LIST (Nền đen, Chữ trắng) */
html.dark :deep(.ql-picker-options) {
  background-color: #1d1e1f !important; /* Nền tối */
  border-color: #414243 !important;      /* Viền tối */
  color: #E5EAF3 !important;             /* Chữ sáng */
}
html.dark :deep(.ql-picker-item) {
  color: #E5EAF3 !important;
}
html.dark :deep(.ql-picker-item:hover),
html.dark :deep(.ql-picker-item.ql-selected) {
  background-color: #303133 !important;  /* Nền hover xám nhẹ */
  color: var(--el-color-primary) !important;
}
</style>
