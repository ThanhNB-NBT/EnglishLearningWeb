import React from "react";
import {
  Card,
  Typography,
  Button,
  Progress,
  Radio,
  Input,
  Textarea,
  Alert,
  Chip,
} from "@material-tailwind/react";
import {
  CheckCircleIcon,
  XCircleIcon,
  ExclamationCircleIcon,
} from "@heroicons/react/24/outline";

const PracticeQuestions = ({
  lesson,
  answers,
  questionResults,
  hasSubmitted,
  onAnswerChange,
  onSubmit,
  onRetry,
  submitting,
}) => {
  const questions = lesson.questions || [];
  const answeredCount = Object.keys(answers).length;
  const totalCount = questions.length;

  React.useEffect(() => {
    if (!hasSubmitted && questions.length > 0) {
      setTimeout(() => {
        const firstInput = document.querySelector(
          'input[data-blank-index="0"]'
        );
        if (firstInput) {
          firstInput.focus();
        }
      }, 200);
    }
  }, [questions.length, hasSubmitted]);

  const renderFillBlankQuestion = (question, index) => {
    const userAnswerStr = answers[question.id] || "";
    const userAnswers = userAnswerStr ? userAnswerStr.split("|") : [];
    const result = questionResults?.[question.id];
    const isCorrect = result?.isCorrect;

    const parts = question.questionText.split(/_{2,}/);

    const handleBlankChange = (blankIndex, value) => {
      const newAnswers = [...userAnswers];
      newAnswers[blankIndex] = value;
      onAnswerChange(question.id, newAnswers.join("|"));
    };

    const handleKeyDown = (e, blankIndex) => {
      // Tab: chuyển sang ô tiếp theo
      if (e.key === "Tab" && !e.shiftKey && blankIndex < parts.length - 2) {
        e.preventDefault();
        focusNextInput(question.id, blankIndex + 1);
      }
      // Shift+Tab: quay lại ô trước
      else if (e.key === "Tab" && e.shiftKey && blankIndex > 0) {
        e.preventDefault();
        focusNextInput(question.id, blankIndex - 1);
      }
      // Enter: chuyển sang ô tiếp theo
      else if (e.key === "Enter" && blankIndex < parts.length - 2) {
        e.preventDefault();
        focusNextInput(question.id, blankIndex + 1);
      }
    };

    const focusNextInput = (questionId, blankIndex) => {
      const nextInput = document.querySelector(
        `input[data-blank-index="${blankIndex}"][data-question-id="${questionId}"]`
      );
      if (nextInput) {
        nextInput.focus();
        nextInput.select();

        setTimeout(() => {
          nextInput.scrollIntoView({
            behavior: "smooth",
            block: "center",
            inline: "center",
          });
        }, 50);
      }
    };

    return (
      <Card
        key={question.id}
        className={`p-4 mb-4 ${
          result
            ? isCorrect
              ? "border-2 border-green-500 bg-green-50 dark:bg-green-900/20"
              : "border-2 border-red-500 bg-red-50 dark:bg-red-900/20"
            : "card-base border border-gray-200"
        }`}
      >
        <div className="flex items-start gap-3">
          <Chip
            value={index + 1}
            size="sm"
            className={`${
              result
                ? isCorrect
                  ? "bg-green-500"
                  : "bg-red-500"
                : "bg-blue-500"
            } text-white font-bold mt-1`}
          />

          <div className="flex-1">
            <Typography
              variant="h6"
              className="text-primary mb-1 leading-relaxed"
              lang="en"
            >
              {parts.map((part, idx) => (
                <React.Fragment key={idx}>
                  {part}
                  {idx < parts.length - 1 && (
                    <input
                      type="text"
                      lang="en"
                      value={userAnswers[idx] || ""}
                      onChange={(e) => handleBlankChange(idx, e.target.value)}
                      onKeyDown={(e) => handleKeyDown(e, idx)}
                      onFocus={(e) => e.target.select()} // Select khi focus
                      disabled={hasSubmitted}
                      placeholder="..."
                      data-blank-index={idx}
                      data-question-id={question.id}
                      inputMode="text"
                      autoCapitalize="off"
                      autoCorrect="off"
                      spellCheck="false"
                      autoComplete="off"
                      className={`inline-block w-32 pb-0 mx-1 px-2 py-1 text-center border-b-2 focus:outline-none transition-colors bg-transparent ${
                        result
                          ? isCorrect
                            ? "border-green-500 bg-green-50 text-green-700"
                            : "border-red-500 bg-red-50 text-red-700"
                          : "border-gray-300 focus:border-blue-500"
                      } ${hasSubmitted ? "cursor-not-allowed" : ""}`}
                      style={{
                        borderTop: "none",
                        borderLeft: "none",
                        borderRight: "none",
                        borderRadius: "0",
                        fontFamily: "monospace", // Giúp dễ nhìn tiếng Anh hơn
                      }}
                    />
                  )}
                </React.Fragment>
              ))}
            </Typography>
            <Typography variant="small" className="text-secondary">
              {question.points} điểm • Nhấn Tab để chuyển ô
            </Typography>
          </div>

          {result && (
            <div className="flex-shrink-0">
              {isCorrect ? (
                <CheckCircleIcon className="h-6 w-6 text-green-500" />
              ) : (
                <XCircleIcon className="h-6 w-6 text-red-500" />
              )}
            </div>
          )}
        </div>

        {result && (
          <Alert
            color={isCorrect ? "green" : "red"}
            className="mt-4"
            icon={
              isCorrect ? (
                <CheckCircleIcon className="h-5 w-5" />
              ) : (
                <XCircleIcon className="h-5 w-5" />
              )
            }
          >
            {!isCorrect && result.correctAnswer && (
              <Typography variant="small" className="mb-2">
                <span className="font-semibold">Đáp án đúng:</span>{" "}
                <span className="font-medium">{result.correctAnswer}</span>
              </Typography>
            )}
            {result.explanation && (
              <Typography variant="small">
                <span className="font-semibold">Giải thích:</span>{" "}
                {result.explanation}
              </Typography>
            )}
          </Alert>
        )}
      </Card>
    );
  };

  const renderMultipleChoiceQuestion = (question, index) => {
    const userAnswer = answers[question.id] || "";
    const result = questionResults?.[question.id];
    const isCorrect = result?.isCorrect;

    return (
      <Card
        key={question.id}
        className={`p-6 mb-6 ${
          result
            ? isCorrect
              ? "border-2 border-green-500 bg-green-50 dark:bg-green-900/20"
              : "border-2 border-red-500 bg-red-50 dark:bg-red-900/20"
            : "card-base border border-gray-200"
        }`}
      >
        <div className="flex items-start gap-3 mb-4">
          <Chip
            value={index + 1}
            size="sm"
            className={`${
              result
                ? isCorrect
                  ? "bg-green-500"
                  : "bg-red-500"
                : "bg-blue-500"
            } text-white font-bold`}
          />

          <div className="flex-1">
            <Typography variant="h6" className="text-primary mb-1">
              {question.questionText}
            </Typography>
            <Typography variant="small" className="text-secondary">
              {question.points} điểm
            </Typography>
          </div>

          {result && (
            <div className="flex-shrink-0">
              {isCorrect ? (
                <CheckCircleIcon className="h-6 w-6 text-green-500" />
              ) : (
                <XCircleIcon className="h-6 w-6 text-red-500" />
              )}
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {question.options && question.options.length > 0 ? (
            question.options.map((option) => {
              const isSelected = userAnswer === option.optionText;
              const isCorrectOption = option.isCorrect;
              const showCorrect = result && isCorrectOption;
              const showWrong = result && isSelected && !isCorrectOption;

              return (
                <Card
                  key={option.id}
                  className={`p-3 cursor-pointer transition-all ${
                    showCorrect
                      ? "border-2 border-green-500 bg-green-50 dark:bg-green-900/30"
                      : showWrong
                      ? "border-2 border-red-500 bg-red-50 dark:bg-red-900/30"
                      : isSelected
                      ? "border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/30"
                      : "border border-gray-300 dark:border-gray-600 hover:border-blue-400"
                  } ${hasSubmitted ? "cursor-default" : ""}`}
                  onClick={() =>
                    !hasSubmitted &&
                    onAnswerChange(question.id, option.optionText)
                  }
                >
                  <div className="flex items-center gap-2">
                    <Radio
                      name={`question-${question.id}`}
                      checked={isSelected}
                      readOnly
                      color={showCorrect ? "green" : showWrong ? "red" : "blue"}
                    />
                    <Typography
                      className={`flex-1 text-sm ${
                        showCorrect || showWrong ? "font-medium" : ""
                      }`}
                    >
                      {option.optionText}
                    </Typography>
                    {showCorrect && (
                      <CheckCircleIcon className="h-5 w-5 text-green-500 flex-shrink-0" />
                    )}
                    {showWrong && (
                      <XCircleIcon className="h-5 w-5 text-red-500 flex-shrink-0" />
                    )}
                  </div>
                </Card>
              );
            })
          ) : (
            <div className="col-span-2 p-4 text-center">
              <Typography variant="small" className="text-secondary">
                Câu hỏi này chưa có đáp án
              </Typography>
            </div>
          )}
        </div>

        {result && (
          <Alert
            color={isCorrect ? "green" : "red"}
            className="mt-4"
            icon={
              isCorrect ? (
                <CheckCircleIcon className="h-5 w-5" />
              ) : (
                <XCircleIcon className="h-5 w-5" />
              )
            }
          >
            {!isCorrect && result.correctAnswer && (
              <Typography variant="small" className="mb-2">
                <span className="font-semibold">Đáp án đúng:</span>{" "}
                <span className="font-medium">{result.correctAnswer}</span>
              </Typography>
            )}
            {result.explanation && (
              <Typography variant="small">
                <span className="font-semibold">Giải thích:</span>{" "}
                {result.explanation}
              </Typography>
            )}
          </Alert>
        )}
      </Card>
    );
  };

  const renderTranslateQuestion = (question, index) => {
    const userAnswer = answers[question.id] || "";
    const result = questionResults?.[question.id];
    const isCorrect = result?.isCorrect;

    return (
      <Card
        key={question.id}
        className={`p-6 mb-6 ${
          result
            ? isCorrect
              ? "border-2 border-green-500 bg-green-50 dark:bg-green-900/20"
              : "border-2 border-red-500 bg-red-50 dark:bg-red-900/20"
            : "card-base border border-gray-200"
        }`}
      >
        <div className="flex items-start gap-3 mb-4">
          <Chip
            value={index + 1}
            size="sm"
            className={`${
              result
                ? isCorrect
                  ? "bg-green-500"
                  : "bg-red-500"
                : "bg-blue-500"
            } text-white font-bold`}
          />

          <div className="flex-1">
            <Typography variant="h6" className="text-primary mb-1">
              {question.questionText}
            </Typography>
            <Typography variant="small" className="text-secondary">
              {question.points} điểm
            </Typography>
          </div>

          {result && (
            <div className="flex-shrink-0">
              {isCorrect ? (
                <CheckCircleIcon className="h-6 w-6 text-green-500" />
              ) : (
                <XCircleIcon className="h-6 w-6 text-red-500" />
              )}
            </div>
          )}
        </div>

        <Textarea
          value={userAnswer}
          onChange={(e) => onAnswerChange(question.id, e.target.value)}
          disabled={hasSubmitted}
          label="Nhập câu dịch..."
          rows={3}
          className={`${
            result ? (isCorrect ? "border-green-500" : "border-red-500") : ""
          }`}
          color={result ? (isCorrect ? "green" : "red") : "blue"}
        />

        {result && (
          <Alert
            color={isCorrect ? "green" : "red"}
            className="mt-4"
            icon={
              isCorrect ? (
                <CheckCircleIcon className="h-5 w-5" />
              ) : (
                <XCircleIcon className="h-5 w-5" />
              )
            }
          >
            {!isCorrect && result.correctAnswer && (
              <Typography variant="small" className="mb-2">
                <span className="font-semibold">Đáp án gợi ý:</span>{" "}
                <span className="font-medium">{result.correctAnswer}</span>
              </Typography>
            )}
            {result.explanation && (
              <Typography variant="small">
                <span className="font-semibold">Giải thích:</span>{" "}
                {result.explanation}
              </Typography>
            )}
          </Alert>
        )}
      </Card>
    );
  };

  const renderQuestion = (question, index) => {
    switch (question.questionType) {
      case "FILL_BLANK":
        return renderFillBlankQuestion(question, index);
      case "MULTIPLE_CHOICE":
        return renderMultipleChoiceQuestion(question, index);
      case "TRANSLATE":
        return renderTranslateQuestion(question, index);
      default:
        return (
          <Card
            key={question.id}
            className="p-4 mb-4 card-base border border-gray-200"
          >
            <Typography variant="h6" className="text-primary mb-2">
              {index + 1}. {question.questionText}
            </Typography>
            <Input
              lang="en"
              spellCheck="false"
              autoComplete="off"
              type="text"
              value={answers[question.id] || ""}
              onChange={(e) => onAnswerChange(question.id, e.target.value)}
              disabled={hasSubmitted}
              label="Nhập câu trả lời..."
            />
          </Card>
        );
    }
  };

  return (
    <div className="relative h-full flex flex-col bg-white dark:bg-gray-900">
      {/* Progress Header */}
      <Card className="rounded-none shadow-sm card-base border-b border-primary p-3 flex-shrink-0">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-between mb-2">
            <Typography variant="small" className="text-secondary p-1">
              Tiến độ: {answeredCount}/{totalCount} {" "}
            </Typography>
            <Typography variant="small" className="font-medium text-primary p-1">
              Tổng: {questions.reduce((sum, q) => sum + (q.points || 0), 0)}{" "}
              điểm
            </Typography>
          </div>
          <Progress
            value={totalCount > 0 ? (answeredCount / totalCount) * 100 : 0}
            color="blue"
            className="h-2"
          />
        </div>
      </Card>

      {/* Questions List - Scrollable */}
      <div
        className={`flex-1 overflow-y-auto px-8 py-6 ${
          !hasSubmitted ? "pb-32" : "pb-6"
        }`}
      >
        <div className="max-w-4xl mx-auto">
          {lesson.description && (
            <Typography variant="h5" className="text-primary font-bold mb-6">
              {lesson.description}
            </Typography>
          )}

          {questions.length > 0 ? (
            questions.map((question, index) => renderQuestion(question, index))
          ) : (
            <Card className="p-8 text-center card-base">
              <Typography variant="h6" className="text-secondary">
                Bài học này chưa có câu hỏi
              </Typography>
            </Card>
          )}
        </div>
      </div>

      {/* ✅ Bottom Action Bar - Hiển thị khi CHƯA SUBMIT trong session hiện tại */}
      {!hasSubmitted && (
        <div className="flex-shrink-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 shadow-xl p-4">
          <div className="max-w-4xl mx-auto">
            {/* Warning */}
            {answeredCount < totalCount && (
              <Alert
                color="orange"
                className="mb-3 py-2"
                icon={<ExclamationCircleIcon className="h-5 w-5" />}
              >
                <Typography variant="small">
                  Còn {totalCount - answeredCount} câu chưa trả lời
                </Typography>
              </Alert>
            )}

            {/* Submit Button */}
            <Button
              onClick={onSubmit}
              disabled={submitting || answeredCount === 0}
              color="blue"
              className="w-full"
              size="lg"
            >
              {submitting ? (
                <span className="flex items-center gap-2 justify-center">
                  <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  Đang xử lý...
                </span>
              ) : (
                "Nộp bài"
              )}
            </Button>
          </div>
        </div>
      )}

      {/* ✅ Result Bar - Hiển thị SAU KHI SUBMIT */}
      {hasSubmitted && questionResults && (
        <div className="flex-shrink-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 shadow-xl p-4">
          <div className="max-w-4xl mx-auto">
            {/* Result message */}
            <Typography
              variant="small"
              className="text-center text-secondary mb-3"
            >
              {Object.values(questionResults).every((r) => r.isCorrect)
                ? "🎉 Hoàn hảo! Tất cả câu trả lời đều đúng!"
                : `Bạn đã hoàn thành bài làm. Xem lại các câu sai và nhấn "Làm lại" nếu muốn cải thiện điểm số.`}
            </Typography>

            {/* Retry button */}
            {!Object.values(questionResults).every((r) => r.isCorrect) && (
              <Button
                variant="outlined"
                color="orange"
                className="w-full"
                onClick={onRetry}
              >
                🔄 Làm lại
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default PracticeQuestions;
