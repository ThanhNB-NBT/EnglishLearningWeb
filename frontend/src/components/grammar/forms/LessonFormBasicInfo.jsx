import React from 'react';
import {
  Typography,
  Input,
  Select,
  Option,
} from '@material-tailwind/react';

/**
 * Component: Thông tin cơ bản của lesson (Title, Type)
 */
const LessonFormBasicInfo = ({ formData, errors, onChange }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Title */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Tiêu đề bài học <span className="text-red-500">*</span>
        </Typography>
        <Input
          value={formData.title}
          onChange={(e) => onChange("title", e.target.value)}
          placeholder="Ví dụ: Present Simple Tense"
          error={!!errors.title}
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        {errors.title && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.title}
          </Typography>
        )}
        <Typography variant="small" color="blue-gray" className="mt-1 opacity-60">
          {formData.title.length}/200 ký tự
        </Typography>
      </div>

      {/* Lesson Type */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Loại bài học <span className="text-red-500">*</span>
        </Typography>
        <Select
          value={formData.lessonType}
          onChange={(val) => onChange("lessonType", val)}
          className="!border-blue-gray-200 focus:!border-blue-500"
        >
          <Option value="THEORY">
            <div className="flex items-center space-x-2">
              <span>📖</span>
              <span>Lý thuyết</span>
            </div>
          </Option>
          <Option value="PRACTICE">
            <div className="flex items-center space-x-2">
              <span>✏️</span>
              <span>Thực hành</span>
            </div>
          </Option>
        </Select>
      </div>
    </div>
  );
};

export default LessonFormBasicInfo;