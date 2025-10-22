import React from 'react';
import {
  Typography,
  Input,
  Textarea,
  Select,
  Option,
  Chip,
} from '@material-tailwind/react';
import {
  ChatBubbleLeftRightIcon,
  LanguageIcon,
  PencilSquareIcon,
} from '@heroicons/react/24/outline';

const QuestionFormBasicInfo = ({ formData, errors, onChange }) => {
  const questionTypes = [
    { 
      value: 'MULTIPLE_CHOICE', 
      label: 'Trắc nghiệm', 
      icon: ChatBubbleLeftRightIcon,
      color: 'blue',
      description: 'Chọn 1 đáp án đúng từ 4 lựa chọn'
    },
    { 
      value: 'FILL_BLANK', 
      label: 'Điền từ', 
      icon: PencilSquareIcon,
      color: 'green',
      description: 'Điền từ vào chỗ trống'
    },
    { 
      value: 'TRANSLATE', 
      label: 'Dịch câu', 
      icon: LanguageIcon,
      color: 'orange',
      description: 'Dịch câu tiếng Anh/Việt'
    },
  ];

  const selectedType = questionTypes.find(t => t.value === formData.questionType);

  return (
    <div className="space-y-6">
      {/* Question Type */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Loại câu hỏi <span className="text-red-500">*</span>
        </Typography>
        <Select
          value={formData.questionType}
          onChange={(val) => onChange("questionType", val)}
          className="bg-secondary"
          color="purple"
          containerProps={{ className: "!min-w-full" }}
          menuProps={{ className: "bg-secondary border-primary" }}
        >
          {questionTypes.map((type) => {
            const Icon = type.icon;
            return (
              <Option key={type.value} value={type.value}>
                <div className="flex items-start space-x-3 py-1">
                  <Icon className={`h-5 w-5 text-${type.color}-500 mt-0.5`} />
                  <div>
                    <Typography variant="small" className="font-medium text-primary">
                      {type.label}
                    </Typography>
                    <Typography variant="small" className="text-tertiary text-xs">
                      {type.description}
                    </Typography>
                  </div>
                </div>
              </Option>
            );
          })}
        </Select>
        {selectedType && (
          <div className="mt-2 flex items-center space-x-2">
            <Chip
              size="sm"
              value={selectedType.label}
              color={selectedType.color}
              icon={<selectedType.icon className="h-3 w-3" />}
            />
            <Typography variant="small" className="text-tertiary">
              {selectedType.description}
            </Typography>
          </div>
        )}
      </div>

      {/* Question Text */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Nội dung câu hỏi <span className="text-red-500">*</span>
        </Typography>
        <Textarea
          value={formData.questionText}
          onChange={(e) => onChange("questionText", e.target.value)}
          placeholder="Ví dụ: She _____ to school every day. (go)"
          error={!!errors.questionText}
          className="bg-secondary dark:text-slate-200"
          color="purple"
          rows={3}
        />
        {errors.questionText && (
          <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
            <span>⚠️</span> {errors.questionText}
          </Typography>
        )}
        <Typography variant="small" className="text-tertiary mt-1">
          {formData.questionText.length}/500 ký tự
        </Typography>
      </div>

      {/* Explanation */}
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Giải thích đáp án
        </Typography>
        <Textarea
          value={formData.explanation}
          onChange={(e) => onChange("explanation", e.target.value)}
          placeholder="Giải thích tại sao đáp án này đúng..."
          className="bg-secondary dark:text-slate-200"
          color="purple"
          rows={2}
        />
        <Typography variant="small" className="text-tertiary mt-1">
          💡 Gợi ý: Giải thích giúp học viên hiểu rõ hơn
        </Typography>
      </div>
    </div>
  );
};

export default QuestionFormBasicInfo;