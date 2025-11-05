import React, { useEffect } from "react";
import {
  Card,
  Typography,
  IconButton,
  Chip,
  Spinner,
  Button,
} from "@material-tailwind/react";
import { useReadingLearning } from "../../../hook/reading/useReadingLearning";
import LessonSidebar from "../../../components/grammar/sidebar/LessonSidebar";
import ReadingContent from "../../../components/reading/content/ReadingContent";
import ReadingQuestions from "../../../components/reading/content/ReadingQuestions";
import {
  ChevronRightIcon,
  ChevronLeftIcon,
  CheckCircleIcon,
} from "@heroicons/react/24/outline";
import toast from "react-hot-toast";

const ReadingLearning = () => {
  const {
    lessons,
    currentLesson,
    loading,
    submitting,
    sidebarCollapsed,
    setSidebarCollapsed,
    answers,
    questionResults,
    hasSubmitted,
    showTranslation,
    setShowTranslation,
    handleAnswerChange,
    handleRetry,
    submitLesson,
    navigateToLesson,
  } = useReadingLearning();

  useEffect(() => {
    // Hàm kiểm tra và cập nhật trạng thái
    const handleResize = () => {
      // Sử dụng 768px (breakpoint 'md' của Tailwind) làm mốc
      if (window.innerWidth < 768) {
        setSidebarCollapsed(true); // Tự động thu gọn trên mobile
      } else {
        setSidebarCollapsed(false); // Tự động mở rộng trên desktop
      }
    };

    // Chạy hàm này 1 lần khi component mount
    handleResize();

    // Thêm listener để theo dõi thay đổi kích thước cửa sổ
    window.addEventListener("resize", handleResize);

    // Cleanup: gỡ bỏ listener khi component unmount
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [setSidebarCollapsed]);

  const goToNextLesson = () => {
    const currentIndex = lessons.findIndex((l) => l.id === currentLesson.id);
    const nextLesson = lessons[currentIndex + 1];

    if (nextLesson && nextLesson.isUnlocked) {
      navigateToLesson(nextLesson.id, true);
    } else {
      toast.info("Bạn đã hoàn thành tất cả bài đọc có sẵn!");
    }
  };

  if (loading && !currentLesson) {
    return (
      <div className="flex items-center justify-center h-screen bg-primary">
        <div className="text-center">
          <Spinner className="h-12 w-12 mx-auto mb-4" color="blue" />
          <Typography variant="h6" className="text-secondary">
            Đang tải bài đọc...
          </Typography>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full w-full overflow-hidden bg-primary">
      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <Card className="rounded-none shadow-sm card-base border-b border-primary p-4 flex-shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex-1 min-w-0">
              <Typography
                variant="h5"
                className="font-bold text-primary truncate"
              >
                {currentLesson?.title || "Reading Practice"}
              </Typography>
              <div className="flex items-center gap-3 mt-2 flex-wrap">
                <Typography variant="small" className="text-secondary">
                  Reading Lessons
                </Typography>
                <Chip
                  size="sm"
                  value={`${lessons.filter((l) => l.isCompleted).length}/${
                    lessons.length
                  } hoàn thành`}
                  className="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 text-xs"
                />
              </div>
            </div>

            {currentLesson && (
              <div className="flex items-center gap-2 ml-4 flex-shrink-0">
                {currentLesson.isCompleted && (
                  <Chip
                    size="sm"
                    value={`${currentLesson.userScore}% - ${
                      currentLesson.userAttempts || 1
                    } lần thử`}
                    className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300 text-xs"
                    icon={<CheckCircleIcon className="h-4 w-4" />}
                  />
                )}

                <Chip
                  size="sm"
                  value="📖 Reading"
                  className="bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 text-xs"
                />

                {currentLesson.pointsReward && (
                  <Chip
                    size="sm"
                    value={`🏆 ${currentLesson.pointsReward}`}
                    className="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300 text-xs"
                  />
                )}
              </div>
            )}
          </div>
        </Card>

        {/* Content */}
        <div className="flex-1 overflow-hidden bg-primary">
          {!currentLesson ? (
            <div className="flex items-center justify-center h-full">
              <Card className="p-8 text-center card-base">
                <Typography variant="h5" className="text-secondary">
                  Chọn một bài đọc để bắt đầu
                </Typography>
              </Card>
            </div>
          ) : (
            <div className="h-full flex flex-col overflow-hidden">
              {/* Completed Badge */}
              {currentLesson.isCompleted && (
                <div className="flex-shrink-0 bg-green-50 dark:bg-green-900/20 border-b border-green-200 dark:border-green-800 p-3">
                  <div className="flex items-center justify-between px-6">
                    <div className="flex items-center gap-3">
                      <CheckCircleIcon className="h-6 w-6 text-green-600 dark:text-green-400" />
                      <div>
                        <Typography
                          variant="small"
                          className="font-bold text-green-700 dark:text-green-300"
                        >
                          ĐÃ HOÀN THÀNH
                        </Typography>
                        <Typography
                          variant="small"
                          className="text-green-600 dark:text-green-400"
                        >
                          Điểm: {currentLesson.userScore}% • Lần thử:{" "}
                          {currentLesson.userAttempts || 1}
                        </Typography>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        color="green"
                        variant="outlined"
                        onClick={handleRetry}
                      >
                        Làm lại
                      </Button>
                      <Button color="blue" size="sm" onClick={goToNextLesson}>
                        BÀI TIẾP THEO
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {/* ✅ MAIN CONTENT: Full width, no padding, divider in middle */}
              <div className="flex-1 overflow-hidden flex flex-col md:flex-row">
                {/* Reading Content Section - LEFT */}
                <div className="flex-1 flex flex-col overflow-hidden">
                  <ReadingContent
                    lesson={currentLesson}
                    showTranslation={showTranslation}
                    onToggleTranslation={() =>
                      setShowTranslation(!showTranslation)
                    }
                    hasSubmitted={hasSubmitted}
                  />
                </div>

                {/* ✅ DIVIDER - Vertical black line */}
                <div className="w-px bg-gray-800 dark:bg-gray-600 flex-shrink-0 hidden md:block"></div>
                <div className="h-px bg-gray-800 dark:bg-gray-600 flex-shrink-0 md:hidden"></div>

                {/* Questions Section - RIGHT */}
                <div className="flex-1 flex flex-col overflow-hidden">
                  <ReadingQuestions
                    lesson={currentLesson}
                    answers={answers}
                    questionResults={questionResults}
                    hasSubmitted={hasSubmitted}
                    onAnswerChange={handleAnswerChange}
                    onSubmit={submitLesson}
                    onRetry={handleRetry}
                    submitting={submitting}
                  />
                </div>
              </div>

              {/* Next Button - Fixed at bottom (only show if completed) */}
              {currentLesson.isCompleted && (
                <div className="flex-shrink-0 bg-secondary border-t border-primary p-4 shadow-lg">
                  <div className="px-6">
                    <Button
                      color="blue"
                      size="lg"
                      onClick={goToNextLesson}
                      className="w-full"
                    >
                      BÀI TIẾP THEO
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Lesson Sidebar (Right) */}
      <div
        className={`relative transition-all duration-300 flex-shrink-0 ${
          sidebarCollapsed ? "w-12" : "w-80"
        }`}
      >
        {/* Toggle Button */}
        <IconButton
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          variant="filled"
          className="absolute -left-3 top-6 bg-secondary border border-primary rounded-full shadow-md z-50 hover:shadow-lg"
          size="sm"
        >
          {sidebarCollapsed ? (
            <ChevronLeftIcon className="h-4 w-4 text-primary" />
          ) : (
            <ChevronRightIcon className="h-4 w-4 text-primary" />
          )}
        </IconButton>

        {!sidebarCollapsed ? (
          <div className="h-full">
            <LessonSidebar
              topics={[
                {
                  id: 1,
                  name: "Reading Practice",
                  lessons: lessons,
                  completedLessons: lessons.filter((l) => l.isCompleted).length,
                  totalLessons: lessons.length,
                },
              ]}
              currentTopic={{
                id: 1,
                name: "Reading Practice",
                lessons: lessons,
                completedLessons: lessons.filter((l) => l.isCompleted).length,
                totalLessons: lessons.length,
              }}
              currentLesson={currentLesson}
              expandedTopics={[1]}
              onToggleTopic={() => {}}
              onSelectLesson={navigateToLesson}
            />
          </div>
        ) : (
          <Card className="h-full rounded-none shadow-xl card-base flex items-center justify-center border-l border-primary">
            <div className="rotate-90 whitespace-nowrap">
              <Typography
                variant="small"
                className="font-medium text-secondary"
              >
                Lessons
              </Typography>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
};

export default ReadingLearning;
