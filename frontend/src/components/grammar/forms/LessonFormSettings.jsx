import React from 'react';
import {
  Typography,
  Input,
  Switch,
  Chip,
} from '@material-tailwind/react';
import {
  HashtagIcon,
  TrophyIcon,
  ClockIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline';

const LessonFormSettings = ({ formData, errors, onChange, isEdit }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
      {/* Order Index */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <HashtagIcon className="h-4 w-4 text-blue-500" />
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
          className="bg-secondary"
          color="blue"
          size="lg"
          disabled={!isEdit}
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
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <TrophyIcon className="h-4 w-4 text-amber-500" />
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
          className="bg-secondary"
          color="blue"
          size="lg"
        />
        {errors.pointsReward && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.pointsReward}
          </Typography>
        )}
        <Typography variant="small" className="text-tertiary mt-1">
          Khuyến nghị: 10-50 điểm
        </Typography>
      </div>

      {/* Estimated Duration */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <ClockIcon className="h-4 w-4 text-blue-500" />
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
          className="bg-secondary"
          color="blue"
          size="lg"
        />
        {errors.estimatedDuration && (
          <Typography variant="small" color="red" className="mt-1">
            {errors.estimatedDuration}
          </Typography>
        )}
        <Typography variant="small" className="text-tertiary mt-1">
          {Math.floor(formData.estimatedDuration / 60)} phút {formData.estimatedDuration % 60} giây
        </Typography>
      </div>

      {/* Active Status */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          {formData.isActive ? (
            <CheckCircleIcon className="h-4 w-4 text-green-500" />
          ) : (
            <XCircleIcon className="h-4 w-4 text-gray-500" />
          )}
          Trạng thái
        </Typography>
        <div className="flex items-center justify-between h-[42px] px-3 bg-secondary border border-primary rounded-lg">
          <Switch
            checked={formData.isActive}
            onChange={(e) => onChange("isActive", e.target.checked)}
            color="green"
            className="checked:bg-green-500"
          />
          <Chip
            size="sm"
            value={formData.isActive ? "Hoạt động" : "Tạm dừng"}
            color={formData.isActive ? "green" : "gray"}
            className="font-semibold"
          />
        </div>
        <Typography variant="small" className="text-tertiary mt-1">
          {formData.isActive 
            ? "Bài học sẽ hiển thị cho học viên"
            : "Bài học sẽ bị ẩn"}
        </Typography>
      </div>
    </div>
  );
};

export default LessonFormSettings;