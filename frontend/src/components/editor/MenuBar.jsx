import React, { useState } from 'react';
import {
  BoldIcon,
  ItalicIcon,
  UnderlineIcon,
  StrikethroughIcon,
  Heading1Icon,
  Heading2Icon,
  Heading3Icon,
  ListIcon,
  ListOrderedIcon,
  AlignLeftIcon,
  AlignCenterIcon,
  AlignRightIcon,
  AlignJustifyIcon,
  TableIcon,
  QuoteIcon,
  CodeIcon,
  ImageIcon,
  LinkIcon,
  YoutubeIcon,
  UndoIcon,
  RedoIcon,
  MinusIcon,
  PaletteIcon,
  HighlighterIcon,
  XIcon,
} from 'lucide-react';

const MenuBar = ({ editor }) => {
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showHighlightPicker, setShowHighlightPicker] = useState(false);

  if (!editor) return null;

  const colors = [
    '#000000', '#e60000', '#ff9900', '#ffff00', '#008a00', 
    '#0066cc', '#9933ff', '#ffffff', '#facccc', '#ffebcc',
    '#ffffcc', '#cce8cc', '#cce0f5', '#ebd6ff', '#bbbbbb',
    '#f06666', '#ffc266', '#ffff66', '#66b966', '#66a3e0',
    '#c285ff', '#888888', '#a10000', '#b26b00', '#b2b200',
    '#006100', '#0047b2', '#6b24b2', '#444444', '#5c0000',
  ];

  const addImage = () => {
    const url = window.prompt('Nhập URL hình ảnh:');
    if (url && url.trim() !== '') {
      editor.chain().focus().setImage({ src: url.trim() }).run();
    }
  };

  const addLink = () => {
    const previousUrl = editor.getAttributes('link').href;
    const url = window.prompt('Nhập URL:', previousUrl || '');
    
    if (url === null) return;
    
    if (url === '') {
      editor.chain().focus().unsetLink().run();
      return;
    }
    
    editor.chain().focus().setLink({ href: url.trim() }).run();
  };

  const addYoutube = () => {
    const url = window.prompt('Nhập URL YouTube:');
    if (url && url.trim() !== '') {
      editor.commands.setYoutubeVideo({ src: url.trim() });
    }
  };

  const addTable = () => {
    editor.chain().focus().insertTable({ 
      rows: 3, 
      cols: 3, 
      withHeaderRow: true 
    }).run();
  };

  const ToolbarButton = ({ onClick, isActive, disabled, title, children }) => (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={`toolbar-button ${isActive ? 'active' : ''} ${disabled ? 'disabled' : ''}`}
    >
      {children}
    </button>
  );

  const Divider = () => <div className="toolbar-divider" />;

  return (
    <div className="menubar">
      {/* Text Formatting */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBold().run()}
        isActive={editor.isActive('bold')}
        title="Bold (Ctrl+B)"
      >
        <BoldIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleItalic().run()}
        isActive={editor.isActive('italic')}
        title="Italic (Ctrl+I)"
      >
        <ItalicIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        isActive={editor.isActive('underline')}
        title="Underline (Ctrl+U)"
      >
        <UnderlineIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleStrike().run()}
        isActive={editor.isActive('strike')}
        title="Strikethrough"
      >
        <StrikethroughIcon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Headings */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
        isActive={editor.isActive('heading', { level: 1 })}
        title="Heading 1"
      >
        <Heading1Icon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        isActive={editor.isActive('heading', { level: 2 })}
        title="Heading 2"
      >
        <Heading2Icon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
        isActive={editor.isActive('heading', { level: 3 })}
        title="Heading 3"
      >
        <Heading3Icon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Color & Highlight */}
      <div className="color-picker-wrapper">
        <ToolbarButton
          onClick={() => setShowColorPicker(!showColorPicker)}
          title="Text Color"
        >
          <PaletteIcon size={16} />
        </ToolbarButton>
        {showColorPicker && (
          <>
            <div 
              className="color-picker-overlay" 
              onClick={() => setShowColorPicker(false)}
            />
            <div className="color-picker-dropdown">
              <div className="color-picker-header">
                <span>Màu chữ</span>
                <button
                  type="button"
                  className="color-picker-close"
                  onClick={() => setShowColorPicker(false)}
                >
                  <XIcon size={14} />
                </button>
              </div>
              <div className="color-grid">
                {colors.map((color) => (
                  <button
                    type="button"
                    key={color}
                    className="color-swatch"
                    style={{ backgroundColor: color }}
                    onClick={() => {
                      editor.chain().focus().setColor(color).run();
                      setShowColorPicker(false);
                    }}
                    title={color}
                  />
                ))}
              </div>
              <button
                type="button"
                className="color-remove"
                onClick={() => {
                  editor.chain().focus().unsetColor().run();
                  setShowColorPicker(false);
                }}
              >
                Xóa màu
              </button>
            </div>
          </>
        )}
      </div>

      <div className="color-picker-wrapper">
        <ToolbarButton
          onClick={() => setShowHighlightPicker(!showHighlightPicker)}
          title="Highlight"
        >
          <HighlighterIcon size={16} />
        </ToolbarButton>
        {showHighlightPicker && (
          <>
            <div 
              className="color-picker-overlay" 
              onClick={() => setShowHighlightPicker(false)}
            />
            <div className="color-picker-dropdown">
              <div className="color-picker-header">
                <span>Màu nền</span>
                <button
                  type="button"
                  className="color-picker-close"
                  onClick={() => setShowHighlightPicker(false)}
                >
                  <XIcon size={14} />
                </button>
              </div>
              <div className="color-grid">
                {colors.map((color) => (
                  <button
                    type="button"
                    key={color}
                    className="color-swatch"
                    style={{ backgroundColor: color }}
                    onClick={() => {
                      editor.chain().focus().toggleHighlight({ color }).run();
                      setShowHighlightPicker(false);
                    }}
                    title={color}
                  />
                ))}
              </div>
              <button
                type="button"
                className="color-remove"
                onClick={() => {
                  editor.chain().focus().unsetHighlight().run();
                  setShowHighlightPicker(false);
                }}
              >
                Xóa highlight
              </button>
            </div>
          </>
        )}
      </div>

      <Divider />

      {/* Lists */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        isActive={editor.isActive('bulletList')}
        title="Bullet List"
      >
        <ListIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        isActive={editor.isActive('orderedList')}
        title="Numbered List"
      >
        <ListOrderedIcon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Alignment */}
      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign('left').run()}
        isActive={editor.isActive({ textAlign: 'left' })}
        title="Align Left"
      >
        <AlignLeftIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign('center').run()}
        isActive={editor.isActive({ textAlign: 'center' })}
        title="Align Center"
      >
        <AlignCenterIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign('right').run()}
        isActive={editor.isActive({ textAlign: 'right' })}
        title="Align Right"
      >
        <AlignRightIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().setTextAlign('justify').run()}
        isActive={editor.isActive({ textAlign: 'justify' })}
        title="Justify"
      >
        <AlignJustifyIcon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Insert */}
      <ToolbarButton onClick={addTable} title="Insert Table">
        <TableIcon size={16} />
      </ToolbarButton>

      <ToolbarButton onClick={addImage} title="Insert Image">
        <ImageIcon size={16} />
      </ToolbarButton>

      <ToolbarButton onClick={addLink} title="Insert/Edit Link">
        <LinkIcon size={16} />
      </ToolbarButton>

      <ToolbarButton onClick={addYoutube} title="Insert YouTube">
        <YoutubeIcon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Others */}
      <ToolbarButton
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        isActive={editor.isActive('blockquote')}
        title="Blockquote"
      >
        <QuoteIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
        isActive={editor.isActive('codeBlock')}
        title="Code Block"
      >
        <CodeIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().setHorizontalRule().run()}
        title="Horizontal Rule"
      >
        <MinusIcon size={16} />
      </ToolbarButton>

      <Divider />

      {/* Undo/Redo */}
      <ToolbarButton
        onClick={() => editor.chain().focus().undo().run()}
        disabled={!editor.can().undo()}
        title="Undo (Ctrl+Z)"
      >
        <UndoIcon size={16} />
      </ToolbarButton>

      <ToolbarButton
        onClick={() => editor.chain().focus().redo().run()}
        disabled={!editor.can().redo()}
        title="Redo (Ctrl+Y)"
      >
        <RedoIcon size={16} />
      </ToolbarButton>

      {/* Table Controls */}
      {editor.isActive('table') && (
        <>
          <Divider />
          <div className="table-controls">
            <button
              type="button"
              onClick={() => editor.chain().focus().addRowBefore().run()}
              className="table-button"
              title="Add Row Before"
            >
              +Row↑
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().addRowAfter().run()}
              className="table-button"
              title="Add Row After"
            >
              +Row↓
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().deleteRow().run()}
              className="table-button"
              title="Delete Row"
            >
              -Row
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().addColumnBefore().run()}
              className="table-button"
              title="Add Column Before"
            >
              +Col←
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().addColumnAfter().run()}
              className="table-button"
              title="Add Column After"
            >
              +Col→
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().deleteColumn().run()}
              className="table-button"
              title="Delete Column"
            >
              -Col
            </button>
            <button
              type="button"
              onClick={() => editor.chain().focus().deleteTable().run()}
              className="table-button delete"
              title="Delete Table"
            >
              Xóa bảng
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default MenuBar;

