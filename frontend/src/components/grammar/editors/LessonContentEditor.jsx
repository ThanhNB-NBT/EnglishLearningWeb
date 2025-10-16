import React from 'react';
import {
  Typography,
  Button,
  Alert,
  Progress,
  Spinner,
} from '@material-tailwind/react';
import {
  DocumentArrowUpIcon,
  SparklesIcon,
} from '@heroicons/react/24/outline';
import RichTextEditor from '../../editor/RichTextEditor';

/**
 * Component: Content editor với Gemini AI PDF parsing
 */
const LessonContentEditor = ({
  content,
  error,
  onChange,
  onFileUpload,
  isParsing,
}) => {
  return (
    <div>
      {/* Header with PDF Upload Button */}
      <div className="flex justify-between items-center mb-3">
        <div className="flex items-center space-x-2">
          <input
            type="file"
            id="pdf-upload"
            accept=".pdf,.docx"
            onChange={onFileUpload}
            className="hidden"
          />
          <Button
            size="sm"
            variant="gradient"
            color="purple"
            onClick={() => document.getElementById("pdf-upload").click()}
            disabled={isParsing}
            className="flex items-center shadow-lg"
          >
            {isParsing ? (
              <>
                <Spinner className="h-4 w-4 mr-2" />
                AI đang xử lý...
              </>
            ) : (
              <>
                <SparklesIcon className="h-4 w-4 mr-2" />
                Parse với Gemini AI
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Parsing Progress */}
      {isParsing && (
        <Alert color="blue" className="mb-4">
          <div className="flex items-center space-x-2 mb-2">
            <Spinner className="h-4 w-4" />
            <Typography variant="small" className="font-medium">
              🤖 Gemini AI đang phân tích file của bạn...
            </Typography>
          </div>
          <Typography variant="small" className="opacity-80 mb-2">
            AI đang đọc, phân tích cấu trúc và tạo lessons tự động
          </Typography>
          <Progress value={50} color="blue" size="sm" />
        </Alert>
      )}

      {/* Rich Text Editor - TipTap */}
      <RichTextEditor
        value={content}
        onChange={onChange}
        label="Nội dung bài học"
        placeholder="Nhập nội dung lý thuyết hoặc sử dụng Gemini AI để parse từ PDF..."
        error={error}
        required={true}
        height="450px"
      />

      {/* Hint */}
      <Typography variant="small" color="blue-gray" className="mt-2 opacity-60">
        💡 Mẹo: Bạn có thể upload file PDF/DOCX và Gemini AI sẽ tự động phân tích thành bài học
      </Typography>
    </div>
  );
};

export default LessonContentEditor;