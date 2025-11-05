// frontend/src/components/reading/editors/ReadingContentEditor.jsx
import React from 'react';
import {
  Typography,
} from '@material-tailwind/react';
import RichTextEditor from '../../editor/RichTextEditor';

const ReadingContentEditor = ({
  content,
  translation,
  contentError,
  translationError,
  onContentChange,
  onTranslationChange,
}) => {
  return (
    <div className="space-y-6">
      {/* Main Content */}
      <div>
        <RichTextEditor
          value={content}
          onChange={onContentChange}
          label="Nội dung bài đọc (tiếng Anh)"
          placeholder="Nhập hoặc paste nội dung bài đọc tiếng Anh..."
          error={contentError}
          required={true}
          height="400px"
        />
      </div>

      {/* Translation */}
      <div>
        <RichTextEditor
          value={translation}
          onChange={onTranslationChange}
          label="Bản dịch (tiếng Việt) - Tùy chọn"
          placeholder="Nhập bản dịch tiếng Việt của bài đọc (nếu có)..."
          error={translationError}
          required={false}
          height="300px"
        />
        <Typography variant="small" color="blue-gray" className="mt-2 opacity-60">
          💡 Mẹo: Bản dịch sẽ giúp người học hiểu rõ hơn nội dung bài đọc
        </Typography>
      </div>
    </div>
  );
};

export default ReadingContentEditor;