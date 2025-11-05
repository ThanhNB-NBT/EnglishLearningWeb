import React from 'react';
import {
  Typography,
  Input,
} from '@material-tailwind/react';
import { HashtagIcon, TrophyIcon } from '@heroicons/react/24/outline';
import FormInputError from '../../common/FormInputError';

const ReadingQuestionFormSettings = ({ formData, errors, onChange, isEdit }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
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
            onChange('orderIndex', parseInt(e.target.value) || 1)
          }
          min="1"
          error={!!errors.orderIndex}
          className="bg-secondary"
          color="blue"
          size="lg"
        />
        <FormInputError error={errors.orderIndex} />
        {!isEdit && (
          <Typography variant="small" className="text-tertiary mt-1">
            Mặc định sẽ được thêm vào cuối.
          </Typography>
        )}
      </div>

      {/* Points */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary flex items-center gap-2"
        >
          <TrophyIcon className="h-4 w-4 text-amber-500" />
          Điểm <span className="text-red-500">*</span>
        </Typography>
        <Input
          type="number"
          value={formData.points}
          onChange={(e) =>
            onChange('points', parseInt(e.target.value) || 5)
          }
          min="1"
          error={!!errors.points}
          className="bg-secondary"
          color="blue"
          size="lg"
        />
        <FormInputError error={errors.points} />
        <Typography variant="small" className="text-tertiary mt-1">
          Khuyến nghị: 5-10 điểm.
        </Typography>
      </div>
    </div>
  );
};

export default ReadingQuestionFormSettings;