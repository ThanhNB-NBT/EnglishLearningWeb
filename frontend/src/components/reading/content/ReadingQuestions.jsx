import React from "react";
import {
  Typography,
  Button,
  Input,
  Radio,
  Chip,
} from "@material-tailwind/react";
import {
  CheckCircleIcon,
  XCircleIcon,
  QuestionMarkCircleIcon,
  PaperAirplaneIcon,
} from "@heroicons/react/24/outline";

const ReadingQuestions = ({
  lesson,
  answers,
  questionResults,
  hasSubmitted,
  onAnswerChange,
  onSubmit,
  onRetry,
  submitting,
}) => {
  if (!lesson) return null;

  const questions = lesson.questions || [];
  const allAnswered = questions.every((q) => answers[q.id]);

  const questionsByType = questions.reduce((acc, q) => {
    const type = q.questionType;
    if (!acc[type]) acc[type] = [];
    acc[type].push(q);
    return acc;
  }, {});

  const typeLabels = {
    MULTIPLE_CHOICE: "Trắc nghiệm",
    TRUE_FALSE: "Đúng/Sai",
    FILL_BLANK: "Điền vào chỗ trống",
    SHORT_ANSWER: "Trả lời ngắn",
  };

  const typeColors = {
    MULTIPLE_CHOICE: "blue",
    TRUE_FALSE: "green",
    FILL_BLANK: "purple",
    SHORT_ANSWER: "amber",
  };

  const optionLetters = ["A", "B", "C", "D", "E", "F"];

  const renderQuestion = (question, localIndex) => {
    const result = questionResults?.[question.id];
    const userAnswer = answers[question.id];
    const showResult = hasSubmitted && result;

    return (
      <div key={question.id} className="mb-6">
        {/* Question Header */}
        <div className="flex items-start gap-2 mb-3">
          <span className="font-semibold text-primary flex-shrink-0">
            {localIndex}.
          </span>
          <div className="flex-1">
            <Typography className="text-primary font-medium">
              {question.questionText}
            </Typography>
          </div>
          {showResult && (
            <div className="flex-shrink-0">
              {result.isCorrect ? (
                <CheckCircleIcon className="h-5 w-5 text-green-600" />
              ) : (
                <XCircleIcon className="h-5 w-5 text-red-600" />
              )}
            </div>
          )}
        </div>

        {/* Question Options based on type */}
        <div className="ml-6 space-y-2">
          {question.questionType === "MULTIPLE_CHOICE" && (
            <div className="space-y-2">
              {question.options?.map((option, idx) => {
                const optionText =
                  typeof option === "string" ? option : option.optionText;

                const optionId =
                  typeof option === "string" ? option : option.id;

                const letter = optionLetters[idx];
                const isSelected = userAnswer === optionText;
                const isCorrect = optionText === question.correctAnswer;

                return (
                  <div
                    key={optionId}
                    className={`flex items-center gap-2 py-1 ${
                      showResult && isCorrect
                        ? "text-green-600 dark:text-green-400 font-medium"
                        : showResult && isSelected && !result.isCorrect
                        ? "text-red-600 dark:text-red-400"
                        : "text-primary"
                    }`}
                  >
                    <Radio
                      name={`question-${question.id}`}
                      value={optionText}
                      checked={isSelected}
                      disabled={hasSubmitted}
                      onChange={() => onAnswerChange(question.id, optionText)}
                      color="blue"
                      className="hover:before:opacity-0"
                    />
                    <label
                      className="flex-1 cursor-pointer flex items-center gap-2"
                      onClick={() =>
                        !hasSubmitted && onAnswerChange(question.id, optionText)
                      }
                    >
                      <span className="font-semibold">{letter}.</span>
                      <span>{optionText}</span>
                      {showResult && isCorrect && (
                        <CheckCircleIcon className="h-4 w-4 text-green-600 ml-auto" />
                      )}
                    </label>
                  </div>
                );
              })}
            </div>
          )}

          {question.questionType === "TRUE_FALSE" && (
            <div className="border border-gray-300 dark:border-gray-600 rounded">
              <div className="flex items-stretch">
                {/* Question Text Column */}
                <div className="flex-1 p-3 border-r border-gray-300 dark:border-gray-600">
                  <Typography className="text-primary">
                    {question.questionText}
                  </Typography>
                </div>

                {/* True Column */}
                <div className="w-16 flex items-center justify-center border-r border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-800/50">
                  <Radio
                    name={`question-${question.id}`}
                    value="TRUE"
                    checked={userAnswer === "TRUE"}
                    disabled={hasSubmitted}
                    onChange={() => onAnswerChange(question.id, "TRUE")}
                    color={
                      showResult && question.correctAnswer === "TRUE"
                        ? "green"
                        : showResult &&
                          userAnswer === "TRUE" &&
                          !result.isCorrect
                        ? "red"
                        : "blue"
                    }
                    className="hover:before:opacity-0"
                  />
                </div>

                {/* False Column */}
                <div className="w-16 flex items-center justify-center bg-gray-50 dark:bg-gray-800/50">
                  <Radio
                    name={`question-${question.id}`}
                    value="FALSE"
                    checked={userAnswer === "FALSE"}
                    disabled={hasSubmitted}
                    onChange={() => onAnswerChange(question.id, "FALSE")}
                    color={
                      showResult && question.correctAnswer === "FALSE"
                        ? "green"
                        : showResult &&
                          userAnswer === "FALSE" &&
                          !result.isCorrect
                        ? "red"
                        : "blue"
                    }
                    className="hover:before:opacity-0"
                  />
                </div>
              </div>
            </div>
          )}

          {(question.questionType === "FILL_BLANK" ||
            question.questionType === "SHORT_ANSWER") && (
            <div className="space-y-2">
              <Input
                type="text"
                value={userAnswer || ""}
                onChange={(e) => onAnswerChange(question.id, e.target.value)}
                disabled={hasSubmitted}
                placeholder="Nhập câu trả lời..."
                variant="standard"
                className={`${
                  showResult
                    ? result.isCorrect
                      ? "!border-green-500 text-green-700"
                      : "!border-red-500 text-red-700"
                    : ""
                }`}
                color={
                  showResult ? (result.isCorrect ? "green" : "red") : "blue"
                }
              />
              {showResult && !result.isCorrect && (
                <Typography
                  variant="small"
                  className="text-blue-700 dark:text-blue-300 ml-1"
                >
                  <strong>Đáp án:</strong> {question.correctAnswer}
                </Typography>
              )}
            </div>
          )}
        </div>

        {/* Hint/Explanation */}
        {showResult && (result.hint || question.explanation) && (
          <div className="ml-6 mt-3 p-3 bg-amber-50 dark:bg-amber-900/20 rounded border-l-4 border-amber-500">
            <Typography
              variant="small"
              className="text-amber-900 dark:text-amber-100"
            >
              <strong>💡 Gợi ý:</strong> {result.hint || question.explanation}
            </Typography>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-primary">
      {/* Questions - Scrollable */}
      <div className="flex-1 overflow-y-auto p-4 md:p-6">
        {questions.length === 0 ? (
          <div className="text-center py-12">
            <QuestionMarkCircleIcon className="h-16 w-16 text-secondary mx-auto mb-4 opacity-50" />
            <Typography variant="h6" className="text-secondary">
              Chưa có câu hỏi nào
            </Typography>
          </div>
        ) : (
          <div className="space-y-8">
            {Object.entries(questionsByType).map(([type, typeQuestions]) => (
              <div key={type}>
                {/* Section Header */}
                <div className="flex items-center gap-2 mb-4 pb-2 border-b border-primary">
                  <Chip
                    value={typeLabels[type] || type}
                    color={typeColors[type] || "gray"}
                    className="font-semibold"
                    size="sm"
                  />
                  <Typography variant="small" className="text-secondary">
                    ({typeQuestions.length} câu)
                  </Typography>
                </div>

                {/* Questions in this type */}
                <div>
                  {typeQuestions.map((q, idx) => renderQuestion(q, idx + 1))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Footer - Submit/Retry Buttons */}
      <div className="flex-shrink-0 border-t border-primary p-3 md:p-4 bg-secondary">
        {!hasSubmitted ? (
          <Button
            color="blue"
            size="lg"
            onClick={onSubmit}
            disabled={!allAnswered || submitting}
            loading={submitting}
            className="w-full flex items-center justify-center gap-2"
          >
            <PaperAirplaneIcon className="h-5 w-5" />
            <span className="hidden sm:inline">Nộp bài</span>
            <span className="sm:hidden">Nộp</span>
            <span>
              ({questions.filter((q) => answers[q.id]).length}/
              {questions.length})
            </span>
          </Button>
        ) : (
          <Button
            color="green"
            variant="outlined"
            size="lg"
            onClick={onRetry}
            className="w-full"
          >
            Làm lại
          </Button>
        )}
      </div>
    </div>
  );
};

export default ReadingQuestions;
