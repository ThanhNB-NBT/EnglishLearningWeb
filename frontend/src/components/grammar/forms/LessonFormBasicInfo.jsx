import React from 'react';
import {
  Typography,
  Input,
  InputProps,
  Select,
  Option,
  Alert,
} from '@material-tailwind/react';
import {
  DocumentTextIcon,
  PlayCircleIcon,
} from '@heroicons/react/24/outline';

const LessonFormBasicInfo = ({ formData, errors, onChange }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Title */}
      <div className="md:col-span-2">
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Tiêu đề bài học <span className="text-red-500">*</span>
        </Typography>
        <Input
          value={formData.title}
          onChange={(e) => onChange("title", e.target.value)}
          placeholder="Ví dụ: Present Simple Tense - Thì hiện tại đơn"
          error={!!errors.title}
          className={"!border-blue-gray-200 focus:!border-blue-500 placeholder:text-gray-500 placeholder:opacity-60"}
          size="lg"
          labelProps={{
            className: "hidden",
          }}
        />
        {errors.title && (
          <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
            <span>⚠️</span> {errors.title}
          </Typography>
        )}
        <div className="flex items-center justify-between mt-2">
          <Typography variant="small" className="text-tertiary">
            {formData.title.length}/200 ký tự
          </Typography>
          {formData.title.length > 0 && formData.title.length < 10 && (
            <Typography variant="small" className="text-amber-500">
              💡 Tiêu đề nên dài hơn 10 ký tự
            </Typography>
          )}
        </div>
      </div>

      {/* Lesson Type */}
      <div className="md:col-span-2">
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Loại bài học <span className="text-red-500">*</span>
        </Typography>
        <Select
          value={formData.lessonType}
          onChange={(val) => onChange("lessonType", val)}
          className="bg-secondary border-blue-gray-200 focus:border-blue-500"
          size="lg"
          labelProps={{
            className: "hidden",
          }}
        >
          <Option value="THEORY">
            <div className="flex items-center gap-3 py-1">
              <div className="p-2 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                <DocumentTextIcon className="h-4 w-4 text-blue-500" />
              </div>
              <div>
                <Typography variant="small" className="font-semibold text-primary">
                  Lý thuyết
                </Typography>
                <Typography variant="small" className="text-tertiary">
                  Bài học có nội dung giảng dạy
                </Typography>
              </div>
            </div>
          </Option>
          <Option value="PRACTICE">
            <div className="flex items-center gap-3 py-1">
              <div className="p-2 bg-green-50 dark:bg-green-900/20 rounded-lg">
                <PlayCircleIcon className="h-4 w-4 text-green-500" />
              </div>
              <div>
                <Typography variant="small" className="font-semibold text-primary">
                  Thực hành
                </Typography>
                <Typography variant="small" className="text-tertiary">
                  Bài học có câu hỏi trắc nghiệm
                </Typography>
              </div>
            </div>
          </Option>
        </Select>
        
        {formData.lessonType === "PRACTICE" && (
          <Alert color="blue" className="mt-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
            <Typography variant="small" className="text-primary">
              💡 Bạn sẽ thêm câu hỏi sau khi tạo bài học này
            </Typography>
          </Alert>
        )}
      </div>
    </div>
  );
};

export default LessonFormBasicInfo;