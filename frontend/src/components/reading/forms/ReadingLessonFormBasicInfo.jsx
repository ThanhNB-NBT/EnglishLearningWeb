// frontend/src/components/reading/forms/ReadingLessonFormBasicInfo.jsx
import React from "react";
import { Typography, Input } from "@material-tailwind/react";
import { BookOpenIcon } from "@heroicons/react/24/outline";

const ReadingLessonFormBasicInfo = ({ formData, errors, onChange }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Title */}
      <div className="md:col-span-2">
        <Typography variant="small" className="mb-2 font-semibold text-primary">
          Tiêu đề bài đọc <span className="text-red-500">*</span>
        </Typography>
        <Input
          value={formData.title}
          onChange={(e) => onChange("title", e.target.value)}
          placeholder="Ví dụ: A Trip to London - Chuyến đi đến London"
          error={!!errors.title}
          className={
            "!border-blue-gray-200 focus:!border-blue-500 placeholder:text-gray-500 placeholder:opacity-60"
          }
          size="lg"
          labelProps={{
            className: "hidden",
          }}
          icon={<BookOpenIcon className="h-5 w-5" />}
        />
        {errors.title && (
          <Typography
            variant="small"
            color="red"
            className="mt-1 flex items-center gap-1"
          >
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
    </div>
  );
};

export default ReadingLessonFormBasicInfo;
