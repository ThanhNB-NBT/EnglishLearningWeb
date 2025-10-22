import React from 'react';
import {
  Typography,
  Input,
} from '@material-tailwind/react';
import {
  TrophyIcon,
  HashtagIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';

const QuestionFormSettings = ({ formData, errors, onChange, isEdit }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
      {/* Points */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <TrophyIcon className="h-4 w-4 text-amber-500" />
          Điểm số <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.points}
          onChange={(e) => onChange("points", parseInt(e.target.value) || 5)}
          min="1"
          max="100"
          error={!!errors.points}
          className="bg-secondary"
          color="purple"
          size="lg"
        />
        {errors.points && (
          <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
            <span>⚠️</span> {errors.points}
          </Typography>
        )}
        <Typography variant="small" className="text-tertiary mt-1">
          Điểm thưởng khi trả lời đúng (1-100)
        </Typography>
      </div>

      {/* Order Index */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <HashtagIcon className="h-4 w-4 text-purple-500" />
          Thứ tự hiển thị
        </Typography>
        <div className="relative">
          <Input
            type="number"
            value={formData.orderIndex}
            onChange={(e) => onChange("orderIndex", parseInt(e.target.value) || 1)}
            min="1"
            className="bg-secondary"
            color="purple"
            size="lg"
            disabled={!isEdit}
          />
          {!isEdit && (
            <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
              <CheckCircleIcon className="h-5 w-5 text-green-500" />
            </div>
          )}
        </div>
        <Typography variant="small" className="text-tertiary mt-1">
          {isEdit 
            ? "Thứ tự xuất hiện trong bài học" 
            : `✅ Tự động: Câu hỏi thứ ${formData.orderIndex}`}
        </Typography>
      </div>
    </div>
  );
};

export default QuestionFormSettings;