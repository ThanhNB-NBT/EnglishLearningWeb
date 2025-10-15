import React from 'react';
import {
  Typography,
  Input,
  Switch,
} from '@material-tailwind/react';

/**
 * Component: Cài đặt lesson (Order, Points, Duration, Status)
 * 🔧 Fixed: Responsive layout với breakpoints tốt hơn
 */
const LessonFormSettings = ({ formData, errors, onChange }) => {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-4 gap-6">
      {/* Order Index */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Thứ tự <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.orderIndex}
          onChange={(e) =>
            onChange("orderIndex", parseInt(e.target.value) || 1)
          }
          min="1"
          error={!!errors.orderIndex}
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        {errors.orderIndex && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.orderIndex}
          </Typography>
        )}
      </div>

      {/* Points Reward */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Điểm thưởng <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.pointsReward}
          onChange={(e) =>
            onChange("pointsReward", parseInt(e.target.value) || 10)
          }
          min="1"
          error={!!errors.pointsReward}
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        {errors.pointsReward && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.pointsReward}
          </Typography>
        )}
      </div>

      {/* Estimated Duration */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Thời gian (giây) <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.estimatedDuration}
          onChange={(e) =>
            onChange("estimatedDuration", parseInt(e.target.value) || 180)
          }
          min="10"
          error={!!errors.estimatedDuration}
          className="!border-blue-gray-200 focus:!border-blue-500"
        />
        {errors.estimatedDuration && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.estimatedDuration}
          </Typography>
        )}
      </div>

      {/* Active Status */}
      <div>
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          Trạng thái
        </Typography>
        <div className="flex items-center h-10">
          <Switch
            checked={formData.isActive}
            onChange={(e) => onChange("isActive", e.target.checked)}
            color="green"
            label={
              <Typography variant="small" color="blue-gray">
                {formData.isActive ? "Hoạt động" : "Tạm dừng"}
              </Typography>
            }
          />
        </div>
      </div>
    </div>
  );
};

export default LessonFormSettings;