import React from 'react';
import {
  Typography,
  Input,
} from '@material-tailwind/react';

/**
 * Component: Cài đặt question (Points, Order)
 */
const QuestionFormSettings = ({ formData, errors, onChange }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Points */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Điểm số <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.points}
          onChange={(e) => onChange("points", parseInt(e.target.value) || 5)}
          min="1"
          max="100"
          error={!!errors.points}
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        {errors.points && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.points}
          </Typography>
        )}
        <Typography variant="small" color="blue-gray" className="mt-1 opacity-60">
          Điểm thưởng khi trả lời đúng
        </Typography>
      </div>

      {/* Order Index */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Thứ tự hiển thị
        </Typography>
        <Input
          type="number"
          value={formData.orderIndex}
          onChange={(e) => onChange("orderIndex", parseInt(e.target.value) || 1)}
          min="1"
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        <Typography variant="small" color="blue-gray" className="mt-1 opacity-60">
          Thứ tự xuất hiện trong bài học
        </Typography>
      </div>
    </div>
  );
};

export default QuestionFormSettings;