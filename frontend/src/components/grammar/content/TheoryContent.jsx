import React from "react";
import { Card, Typography, Button, Progress } from "@material-tailwind/react";
import { ClockIcon, CheckCircleIcon } from "@heroicons/react/24/outline";
import "../../../styles/lessonPreview.css";

const TheoryContent = ({
  lesson,
  readingTime,
  showCompleteButton,
  onScroll,
  onComplete,
  submitting,
  isCompleted = false,
}) => {
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  const minRequiredTime = lesson.estimatedDuration || 0;
  const progress = Math.min((readingTime / minRequiredTime) * 100, 100);

  return (
    <div className="relative h-full flex flex-col">
      {/* Content Container - Scrollable */}
      <div
        className="flex-1 overflow-y-auto px-4 py-6 pb-32"
        onScroll={isCompleted ? undefined : onScroll}
      >
        <div className="max-w-4xl mx-auto">
          <Card className="p-8 card-base">
            <div className="lesson-preview-wrapper">
              <div
                className="lesson-preview-content"
                dangerouslySetInnerHTML={{ __html: lesson.content }}
              />
            </div>
          </Card>
        </div>
      </div>

      {/* Reading Progress Bar - Fixed at bottom, inside content area */}
      {!isCompleted && (
        <div className="absolute bottom-0 left-0 right-0 bg-secondary border-t border-primary shadow-xl p-4">
          <div className="max-w-4xl mx-auto">
            {/* Timer */}
            <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
              <div className="flex items-center gap-2 flex-wrap">
                <ClockIcon className="h-5 w-5 text-secondary" />
                <Typography variant="small" className="text-secondary">
                  Thời gian:{" "}
                  <span className="font-medium">{formatTime(readingTime)}</span>
                </Typography>
                {minRequiredTime > 0 && (
                  <Typography variant="small" className="text-secondary">
                    (Tối thiểu: {formatTime(minRequiredTime)})
                  </Typography>
                )}
              </div>

              {showCompleteButton && (
                <Button
                  onClick={onComplete}
                  disabled={submitting}
                  color="green"
                  className="flex items-center gap-2"
                  size="sm"
                >
                  {submitting ? (
                    <>
                      <div className="spinner"></div>
                      <span>Đang xử lý...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircleIcon className="h-5 w-5" />
                      <span>Hoàn thành</span>
                    </>
                  )}
                </Button>
              )}
            </div>

            {/* Progress Bar */}
            <div className="relative">
              <Progress
                value={progress}
                color={showCompleteButton ? "green" : "blue"}
                className="h-2"
              />
            </div>

            {!showCompleteButton && (
              <Typography
                variant="small"
                className="text-secondary mt-2 text-center"
              >
                {readingTime < minRequiredTime
                  ? `Vui lòng đọc ít nhất ${formatTime(
                      minRequiredTime - readingTime
                    )} nữa`
                  : "Cuộn xuống cuối trang để hoàn thành"}
              </Typography>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default TheoryContent;
