import React from 'react';
import {
  Typography,
  Input,
  Button,
  IconButton,
  Card,
  CardBody,
  Switch,
  Alert,
} from '@material-tailwind/react';
import {
  PlusIcon,
  TrashIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline';

/**
 * Component: Phần nhập đáp án và options
 */
const QuestionAnswerSection = ({ 
  questionType, 
  correctAnswer, 
  options, 
  errors,
  onCorrectAnswerChange,
  onOptionsChange 
}) => {
  const handleAddOption = () => {
    onOptionsChange([
      ...options,
      { optionText: '', isCorrect: false, orderIndex: options.length + 1 }
    ]);
  };

  const handleRemoveOption = (index) => {
    onOptionsChange(options.filter((_, i) => i !== index));
  };

  const handleOptionChange = (index, field, value) => {
    const newOptions = [...options];
    newOptions[index][field] = value;
    
    // If marking as correct, unmark others
    if (field === 'isCorrect' && value === true) {
      newOptions.forEach((opt, i) => {
        if (i !== index) opt.isCorrect = false;
      });
    }
    
    onOptionsChange(newOptions);
  };

  // For MULTIPLE_CHOICE
  if (questionType === 'MULTIPLE_CHOICE') {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <Typography variant="small" color="blue-gray" className="font-medium">
            Các lựa chọn <span className="text-red-500">*</span>
          </Typography>
          <Button
            size="sm"
            variant="outlined"
            onClick={handleAddOption}
            className="flex items-center border-blue-500 text-blue-500"
          >
            <PlusIcon className="h-4 w-4 mr-1" />
            Thêm lựa chọn
          </Button>
        </div>

        {errors.options && (
          <Alert color="red" className="py-2">
            <Typography variant="small">{errors.options}</Typography>
          </Alert>
        )}

        <div className="space-y-3">
          {options.map((option, index) => (
            <Card key={index} className={`border-2 ${option.isCorrect ? 'border-green-500 bg-green-50' : 'border-gray-200'}`}>
              <CardBody className="p-3">
                <div className="flex items-start space-x-3">
                  <div className="flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-600 rounded-full font-bold text-sm flex-shrink-0">
                    {String.fromCharCode(65 + index)}
                  </div>
                  
                  <div className="flex-1 space-y-2">
                    <Input
                      value={option.optionText}
                      onChange={(e) => handleOptionChange(index, 'optionText', e.target.value)}
                      placeholder={`Lựa chọn ${String.fromCharCode(65 + index)}`}
                      className="!border-blue-gray-200 focus:!border-blue-500"
                    />
                    
                    <div className="flex items-center space-x-2">
                      <Switch
                        checked={option.isCorrect}
                        onChange={(e) => handleOptionChange(index, 'isCorrect', e.target.checked)}
                        color="green"
                        label={
                          <Typography variant="small" color={option.isCorrect ? "green" : "blue-gray"}>
                            {option.isCorrect ? "✓ Đáp án đúng" : "Đánh dấu là đáp án đúng"}
                          </Typography>
                        }
                      />
                    </div>
                  </div>

                  <IconButton
                    size="sm"
                    variant="text"
                    color="red"
                    onClick={() => handleRemoveOption(index)}
                    disabled={options.length <= 2}
                  >
                    <TrashIcon className="h-4 w-4" />
                  </IconButton>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>

        {options.length < 2 && (
          <Alert color="amber">
            <Typography variant="small">
              ⚠️ Cần ít nhất 2 lựa chọn cho câu hỏi trắc nghiệm
            </Typography>
          </Alert>
        )}
      </div>
    );
  }

  // For other question types (FILL_BLANK, TRANSLATE, VERB_FORM)
  return (
    <div>
      <Typography
        variant="small"
        color="blue-gray"
        className="mb-2 font-medium"
      >
        Đáp án đúng <span className="text-red-500">*</span>
      </Typography>
      <Input
        value={correctAnswer}
        onChange={(e) => onCorrectAnswerChange(e.target.value)}
        placeholder="Nhập đáp án đúng. Nhiều đáp án cách nhau bởi dấu |"
        error={!!errors.correctAnswer}
        className="!border-blue-gray-200 focus:!border-blue-500"
      />
      {errors.correctAnswer && (
        <Typography variant="small" color="red" className="mt-1">
          {errors.correctAnswer}
        </Typography>
      )}
      <Typography variant="small" color="blue-gray" className="mt-1 opacity-60">
        💡 Ví dụ: "goes|is going|does go" (chấp nhận nhiều đáp án đúng)
      </Typography>
    </div>
  );
};

export default QuestionAnswerSection;