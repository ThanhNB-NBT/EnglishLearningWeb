import React from 'react';
import {
  Typography,
  Select,
  Option,
  Textarea,
} from '@material-tailwind/react';
import {
  ChatBubbleBottomCenterTextIcon,
  ChatBubbleLeftRightIcon,
  PencilSquareIcon,
  CheckBadgeIcon, // <-- THÊM ICON MỚI
} from '@heroicons/react/24/outline';
import FormInputError from '../../common/FormInputError';

const ReadingQuestionFormBasicInfo = ({ formData, errors, onChange }) => {
  const questionTypes = [
    {
      value: 'MULTIPLE_CHOICE',
      label: 'Trắc nghiệm (4 lựa chọn)',
      icon: ChatBubbleLeftRightIcon,
      color: 'cyan',
    },
    // <-- THÊM LOẠI CÂU HỎI MỚI DƯỚI ĐÂY -->
    {
      value: 'TRUE_FALSE',
      label: 'Đúng / Sai (True/False)',
      icon: CheckBadgeIcon,
      color: 'teal',
    },
    // <-- KẾT THÚC PHẦN THÊM MỚI -->
    {
      value: 'FILL_BLANK',
      label: 'Điền từ',
      icon: PencilSquareIcon,
      color: 'amber',
    },
    {
      value: 'SHORT_ANSWER',
      label: 'Trả lời ngắn',
      icon: ChatBubbleBottomCenterTextIcon,
      color: 'green',
    },
  ];

  const selectedType = questionTypes.find(
    (t) => t.value === formData.questionType
  );

  return (
    <div className="space-y-6">
      {/* Question Type */}
      <div>
        <Typography variant="small" className="mb-2 font-semibold text-primary">
          Loại câu hỏi <span className="text-red-500">*</span>
        </Typography>
        <Select
          value={formData.questionType}
          onChange={(value) => onChange('questionType', value)}
          error={!!errors.questionType}
          color={selectedType?.color || 'blue'}
          size="lg"
          className="bg-secondary"
          labelProps={{ className: 'hidden' }}
          menuProps={{ className: 'bg-secondary border-primary' }}
        >
          {questionTypes.map((type) => (
            <Option key={type.value} value={type.value} className="flex items-center gap-3">
              <type.icon className={`h-5 w-5 text-${type.color}-500`} />
              <span className="text-primary">{type.label}</span>
            </Option>
          ))}
        </Select>
        <FormInputError error={errors.questionType} />
      </div>

      {/* Question Text */}
      <div>
        <Typography variant="small" className="mb-2 font-semibold text-primary">
          Nội dung câu hỏi <span className="text-red-500">*</span>
        </Typography>
        <Textarea
          value={formData.questionText}
          onChange={(e) => onChange('questionText', e.target.value)}
          placeholder="Nhập nội dung câu hỏi..."
          error={!!errors.questionText}
          color="blue"
          size="lg"
          rows={4}
          className="bg-secondary"
        />
        <FormInputError error={errors.questionText} />
        <Typography variant="small" className="text-tertiary mt-1">
          💡 Đối với <strong>Điền từ</strong>, sử dụng <strong>___</strong> (3 dấu gạch dưới) để
          đánh dấu chỗ trống.
        </Typography>
      </div>

      {/* Explanation */}
      <div>
        <Typography variant="small" className="mb-2 font-semibold text-primary">
          Giải thích (Tùy chọn)
        </Typography>
        <Textarea
          value={formData.explanation}
          onChange={(e) => onChange('explanation', e.target.value)}
          placeholder="Nhập giải thích cho đáp án..."
          error={!!errors.explanation}
          color="blue"
          size="lg"
          rows={3}
          className="bg-secondary"
        />
        <FormInputError error={errors.explanation} />
        <Typography variant="small" className="text-tertiary mt-1">
          Giải thích sẽ được hiển thị cho học viên sau khi trả lời.
        </Typography>
      </div>
    </div>
  );
};

export default ReadingQuestionFormBasicInfo;