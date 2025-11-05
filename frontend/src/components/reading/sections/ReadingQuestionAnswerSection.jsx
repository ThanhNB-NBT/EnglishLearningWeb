import React from 'react';
import {
  Typography,
  Input,
  Radio,
} from '@material-tailwind/react';
import FormInputError from '../../common/FormInputError';

// Component con cho loại Trắc nghiệm
const MultipleChoiceSection = ({ options, errors, onOptionsChange }) => {

  const handleOptionTextChange = (index, value) => {
    const newOptions = [...options];
    newOptions[index].optionText = value;
    onOptionsChange(newOptions);
  };

  const handleCorrectOptionChange = (index) => {
    const newOptions = options.map((option, i) => ({
      ...option,
      isCorrect: i === index,
    }));
    onOptionsChange(newOptions);
  };

  return (
    <div className="space-y-4">
      <Typography variant="small" className="font-semibold text-primary">
        Nhập 4 lựa chọn và chọn 1 đáp án đúng:
      </Typography>
      
      {options.map((option, index) => (
        <div key={index} className="flex items-center gap-3">
          <Radio
            name="correctAnswer"
            checked={option.isCorrect}
            onChange={() => handleCorrectOptionChange(index)}
            color="green"
            className="border-primary"
          />
          <div className="flex-1">
            <Input
              value={option.optionText}
              onChange={(e) => handleOptionTextChange(index, e.target.value)}
              placeholder={`Lựa chọn ${index + 1}`}
              error={!!errors[`option${index}`]}
              color="blue"
              className="bg-secondary"
            />
            <FormInputError error={errors[`option${index}`]} />
          </div>
        </div>
      ))}
      {errors.correctAnswer && !errors.option0 && !errors.option1 && !errors.option2 && !errors.option3 && (
        <FormInputError error={errors.correctAnswer} />
      )}
    </div>
  );
};

// Component con cho loại Điền từ & Trả lời ngắn
const DirectAnswerSection = ({ correctAnswer, error, onChange, type }) => {
  const isFillBlank = type === 'FILL_BLANK';
  const placeholder = isFillBlank 
    ? "Nhập đáp án đúng (ví dụ: answer)"
    : "Nhập câu trả lời mẫu (ví dụ: It means...)";
  
  return (
    <div className="space-y-2">
      <Typography variant="small" className="font-semibold text-primary">
        Đáp án đúng <span className="text-red-500">*</span>
      </Typography>
      <Input
        value={correctAnswer}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        error={!!error}
        color="blue"
        className="bg-secondary"
      />
      <FormInputError error={error} />
      <Typography variant="small" className="text-tertiary">
        💡 Hỗ trợ nhiều đáp án đúng, cách nhau bởi dấu <strong>|</strong> (ví dụ: answer|a good answer)
      </Typography>
    </div>
  );
};

// <-- COMPONENT MỚI CHO TRUE/FALSE -->
const TrueFalseSection = ({ options, errors, onOptionsChange }) => {
  
  // Xác định xem "True" có đang được chọn hay không
  // Mặc định là false nếu 'options' rỗng
  const isTrueCorrect = options && options.length > 0 ? options[0].isCorrect : false;

  const handleCorrectChange = (isTrueSelected) => {
    // Luôn tạo 1 mảng options cố định cho True/False
    const newOptions = [
      { optionText: 'True', isCorrect: isTrueSelected, orderIndex: 1 },
      { optionText: 'False', isCorrect: !isTrueSelected, orderIndex: 2 }
    ];
    onOptionsChange(newOptions);
  };

  return (
    <div className="space-y-2">
      <Typography variant="small" className="font-semibold text-primary">
        Chọn đáp án đúng:
      </Typography>
      
      <div className="flex flex-col sm:flex-row gap-4">
        {/* Lựa chọn TRUE */}
        <div 
          className={`flex-1 p-3 rounded-lg border-2 cursor-pointer transition-all ${
            isTrueCorrect 
              ? 'bg-green-50 dark:bg-green-900/20 border-green-500' 
              : 'bg-tertiary border-primary hover:bg-secondary'
          }`}
          onClick={() => handleCorrectChange(true)}
        >
          <Radio
            name="trueFalseAnswer"
            label={<Typography className="text-primary font-medium">True (Đúng)</Typography>}
            checked={isTrueCorrect}
            onChange={() => handleCorrectChange(true)}
            color="green"
            className="border-primary"
          />
        </div>
        
        {/* Lựa chọn FALSE */}
        <div 
          className={`flex-1 p-3 rounded-lg border-2 cursor-pointer transition-all ${
            !isTrueCorrect 
              ? 'bg-green-50 dark:bg-green-900/20 border-green-500' 
              : 'bg-tertiary border-primary hover:bg-secondary'
          }`}
          onClick={() => handleCorrectChange(false)}
        >
          <Radio
            name="trueFalseAnswer"
            label={<Typography className="text-primary font-medium">False (Sai)</Typography>}
            checked={!isTrueCorrect}
            onChange={() => handleCorrectChange(false)}
            color="green"
            className="border-primary"
          />
        </div>
      </div>
      <FormInputError error={errors.correctAnswer} />
    </div>
  );
};
// <-- KẾT THÚC COMPONENT MỚI -->


// Component chính
const ReadingQuestionAnswerSection = ({
  questionType,
  correctAnswer,
  options,
  errors,
  onCorrectAnswerChange,
  onOptionsChange,
}) => {
  switch (questionType) {
    case 'MULTIPLE_CHOICE':
      return (
        <MultipleChoiceSection
          options={options}
          errors={errors}
          onOptionsChange={onOptionsChange}
        />
      );
    // <-- THÊM CASE MỚI CHO TRUE/FALSE -->
    case 'TRUE_FALSE':
      return (
        <TrueFalseSection
          options={options}
          errors={errors}
          onOptionsChange={onOptionsChange}
        />
      );
    // <-- KẾT THÚC CASE MỚI -->
    case 'FILL_BLANK':
      return (
        <DirectAnswerSection
          correctAnswer={correctAnswer}
          error={errors.correctAnswer}
          onChange={onCorrectAnswerChange}
          type="FILL_BLANK"
        />
      );
    case 'SHORT_ANSWER':
      return (
        <DirectAnswerSection
          correctAnswer={correctAnswer}
          error={errors.correctAnswer}
          onChange={onCorrectAnswerChange}
          type="SHORT_ANSWER"
        />
      );
    default:
      return (
        <Typography variant="small" className="text-tertiary">
          Vui lòng chọn loại câu hỏi ở mục "Thông tin cơ bản"
        </Typography>
      );
  }
};

export default ReadingQuestionAnswerSection;