import React from 'react';
import {
  Card,
  Typography,
  IconButton,
  Chip,
  Spinner,
  Button,
} from "@material-tailwind/react";
import { useGrammarLearning } from '../../../hook/grammar/useGrammarLearning';
import LessonSidebar from '../../../components/grammar/sidebar/LessonSidebar';
import TheoryContent from '../../../components/grammar/content/TheoryContent';
import PracticeQuestions from '../../../components/grammar/content/PracticeQuestions';
import { 
  ChevronRightIcon, 
  ChevronLeftIcon,
  BookOpenIcon,
  PencilSquareIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';

const GrammarLearning = () => {
  const {
    topics,
    currentTopic,
    currentLesson,
    loading,
    submitting,
    expandedTopics,
    sidebarCollapsed,
    setSidebarCollapsed,
    toggleTopic,
    readingTime,
    showCompleteButton,
    handleScroll,
    answers,
    questionResults,
    hasSubmitted,
    handleAnswerChange,
    handleRetry,
    submitLesson,
    navigateToLesson,
  } = useGrammarLearning();

  if (loading && !currentLesson) {
    return (
      <div className="flex items-center justify-center h-screen bg-primary">
        <div className="text-center">
          <Spinner className="h-12 w-12 mx-auto mb-4" color="blue" />
          <Typography variant="h6" className="text-secondary">
            Đang tải bài học...
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
              <Typography variant="h5" className="font-bold text-primary truncate">
                {currentLesson?.title || 'Grammar Learning'}
              </Typography>
              {currentTopic && (
                <div className="flex items-center gap-3 mt-2 flex-wrap">
                  <Typography variant="small" className="text-secondary">
                    {currentTopic.name}
                  </Typography>
                  <Chip
                    size="sm"
                    value={`${currentTopic.completedLessons || 0}/${currentTopic.totalLessons || 0} hoàn thành`}
                    className="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 text-xs"
                  />
                </div>
              )}
            </div>
            
            {currentLesson && (
              <div className="flex items-center gap-2 ml-4 flex-shrink-0">
                {currentLesson.isCompleted && (
                  <Chip
                    size="sm"
                    value={`${currentLesson.userScore}% - ${currentLesson.userAttempts || 1} lần thử`}
                    className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300 text-xs"
                    icon={<CheckCircleIcon className="h-4 w-4" />}
                  />
                )}
                
                <Chip
                  size="sm"
                  value={currentLesson.lessonType === 'THEORY' ? '📖 Lý thuyết' : '✏️ Thực hành'}
                  className={`text-xs ${
                    currentLesson.lessonType === 'THEORY' 
                      ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
                      : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                  }`}
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
                  Chọn một bài học để bắt đầu
                </Typography>
              </Card>
            </div>
          ) : currentLesson.lessonType === 'THEORY' ? (
            // ✅ Theory - Show content even if completed, with "Next Lesson" button
            currentLesson.isCompleted ? (
              <div className="h-full flex flex-col">
                <TheoryContent
                  lesson={currentLesson}
                  readingTime={readingTime}
                  showCompleteButton={false}
                  onScroll={() => {}}
                  onComplete={() => {}}
                  submitting={false}
                  isCompleted={true}
                />
                
                {/* Next Lesson Button - Fixed at bottom */}
                <div className="bg-secondary border-t border-primary p-4">
                  <div className="max-w-4xl mx-auto">
                    <Button
                      color="blue"
                      onClick={() => {
                        const lessons = currentTopic?.lessons || [];
                        const currentIndex = lessons.findIndex(l => l.id === currentLesson.id);
                        const nextLesson = lessons[currentIndex + 1];
                        
                        if (nextLesson && nextLesson.isUnlocked) {
                          navigateToLesson(nextLesson.id, true);
                        } else {
                          toast.info("Bạn đã hoàn thành tất cả bài học có sẵn!");
                        }
                      }}
                      className="w-full"
                    >
                      BÀI TIẾP THEO
                    </Button>
                  </div>
                </div>
              </div>
            ) : (
              <TheoryContent
                lesson={currentLesson}
                readingTime={readingTime}
                showCompleteButton={showCompleteButton}
                onScroll={handleScroll}
                onComplete={submitLesson}
                submitting={submitting}
                isCompleted={false}
              />
            )
          ) : (
            // ✅ Practice
            currentLesson.isCompleted ? (
              <div className="flex items-center justify-center h-full p-8">
                <Card className="p-8 text-center card-base max-w-md">
                  <CheckCircleIcon className="h-16 w-16 text-green-500 mx-auto mb-4" />
                  
                  <Typography variant="h5" className="text-primary font-bold mb-2">
                    Bài học đã hoàn thành!
                  </Typography>
                  
                  <Typography variant="small" className="text-secondary mb-4">
                    ✏️ Thực hành
                  </Typography>

                  <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg mb-4">
                    <Typography variant="h3" className="text-green-600 dark:text-green-400 font-bold">
                      {currentLesson.userScore}%
                    </Typography>
                    <Typography variant="small" className="text-secondary">
                      Điểm số của bạn
                    </Typography>
                  </div>

                  <Typography variant="small" className="text-secondary mb-4">
                    Số lần thử: {currentLesson.userAttempts || 1}
                  </Typography>

                  <Button
                    color="blue"
                    onClick={() => {
                      const lessons = currentTopic?.lessons || [];
                      const currentIndex = lessons.findIndex(l => l.id === currentLesson.id);
                      const nextLesson = lessons[currentIndex + 1];
                      
                      if (nextLesson && nextLesson.isUnlocked) {
                        navigateToLesson(nextLesson.id, true);
                      } else {
                        toast.info("Bạn đã hoàn thành tất cả bài học có sẵn!");
                      }
                    }}
                    className="w-full"
                  >
                    BÀI TIẾP THEO
                  </Button>
                </Card>
              </div>
            ) : (
              <PracticeQuestions
                lesson={currentLesson}
                answers={answers}
                questionResults={questionResults}
                hasSubmitted={hasSubmitted}
                onAnswerChange={handleAnswerChange}
                onSubmit={submitLesson}
                onRetry={handleRetry}
                submitting={submitting}
              />
            )
          )}
        </div>
      </div>

      {/* Lesson Sidebar (Right) - Fixed positioning */}
      <div className={`relative transition-all duration-300 flex-shrink-0 ${
        sidebarCollapsed ? 'w-12' : 'w-80'
      }`}>
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
              topics={topics}
              currentTopic={currentTopic}
              currentLesson={currentLesson}
              expandedTopics={expandedTopics}
              onToggleTopic={toggleTopic}
              onSelectLesson={navigateToLesson}
            />
          </div>
        ) : (
          <Card className="h-full rounded-none shadow-xl card-base flex items-center justify-center border-l border-primary">
            <div className="rotate-90 whitespace-nowrap">
              <Typography variant="small" className="font-medium text-secondary">
                Lessons
              </Typography>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
};

export default GrammarLearning;