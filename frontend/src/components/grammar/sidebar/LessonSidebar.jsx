import React from 'react';
import {
  Card,
  Typography,
  List,
  ListItem,
  ListItemPrefix,
  Accordion,
  AccordionHeader,
  AccordionBody,
  Progress,
  Chip,
} from "@material-tailwind/react";
import {
  ChevronDownIcon,
  ChevronRightIcon,
  LockClosedIcon,
  CheckCircleIcon,
  DocumentTextIcon,
  PlayCircleIcon,
} from '@heroicons/react/24/outline';

const LessonSidebar = ({ 
  topics = [],
  currentTopic,
  currentLesson, 
  expandedTopics = [],
  onToggleTopic, 
  onSelectLesson 
}) => {
  console.log("📊 LessonSidebar props:", { topics, currentTopic, expandedTopics });

  return (
    <Card className="h-full rounded-none shadow-xl card-base flex flex-col overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-primary flex-shrink-0">
        <Typography variant="h6" className="font-bold text-primary">
          Danh sách bài học
        </Typography>
        {currentTopic && (
          <div className="mt-3">
            <div className="flex justify-between mb-2">
              <Typography variant="small" className="text-secondary">
                Tiến độ:
              </Typography>
              <Typography variant="small" className="font-medium text-primary">
                {currentTopic.completedLessons || 0}/{currentTopic.totalLessons || 0}
              </Typography>
            </div>
            <Progress 
              value={currentTopic.totalLessons > 0 
                ? ((currentTopic.completedLessons || 0) / currentTopic.totalLessons) * 100 
                : 0}
              color="green"
              className="h-2"
            />
          </div>
        )}
      </div>

      {/* Topics List */}
      <div className="flex-1 overflow-y-auto">
        {topics && topics.length > 0 ? (
          topics.map((topic) => {
            const isExpanded = expandedTopics.includes(topic.id);
            const lessons = topic.lessons || [];
            
            return (
              <Accordion
                key={topic.id}
                open={isExpanded}
                animate={{
                  mount: { scale: 1 },
                  unmount: { scale: 0.95 },
                }}
                className="border-b border-primary"
              >
                <AccordionHeader
                  onClick={() => onToggleTopic(topic.id)}
                  className="border-b-0 p-4 hover:bg-tertiary transition-colors"
                >
                  <div className="flex items-center justify-between w-full">
                    <div className="flex-1 min-w-0">
                      <Typography variant="small" className="font-semibold text-primary truncate">
                        {topic.name}
                      </Typography>
                      <Typography variant="small" className="text-secondary text-xs mt-1">
                        {topic.completedLessons || 0}/{topic.totalLessons || 0} bài
                      </Typography>
                    </div>
                    {isExpanded ? (
                      <ChevronDownIcon className="h-4 w-4 text-secondary ml-2 flex-shrink-0" />
                    ) : (
                      <ChevronRightIcon className="h-4 w-4 text-secondary ml-2 flex-shrink-0" />
                    )}
                  </div>
                </AccordionHeader>

                <AccordionBody className="py-0">
                  {/* ✅ Always render List with content */}
                  <div className="bg-tertiary">
                    {lessons.length > 0 ? (
                      <List className="p-0">
                        {lessons.map((lesson, index) => {
                          const isActive = currentLesson?.id === lesson.id;
                          const isLocked = !lesson.isUnlocked;

                          return (
                            <ListItem
                              key={lesson.id}
                              onClick={() => !isLocked && onSelectLesson(lesson.id, lesson.isUnlocked)}
                              className={`p-3 pl-6 ${
                                isActive 
                                  ? 'bg-blue-50 dark:bg-blue-900/30 border-l-4 border-blue-500' 
                                  : isLocked
                                  ? 'opacity-50 cursor-not-allowed'
                                  : 'hover:bg-secondary cursor-pointer'
                              }`}
                            >
                              <ListItemPrefix className="mr-3">
                                {isLocked ? (
                                  <LockClosedIcon className="h-5 w-5 text-gray-400" />
                                ) : lesson.isCompleted ? (
                                  <CheckCircleIcon className="h-5 w-5 text-green-500" />
                                ) : lesson.lessonType === 'THEORY' ? (
                                  <DocumentTextIcon className="h-5 w-5 text-blue-500" />
                                ) : (
                                  <PlayCircleIcon className="h-5 w-5 text-green-500" />
                                )}
                              </ListItemPrefix>

                              <div className="flex-1 min-w-0">
                                <Typography
                                  variant="small"
                                  className={`font-medium truncate ${
                                    isActive ? 'text-blue-600 dark:text-blue-400' : 'text-primary'
                                  }`}
                                >
                                  {index + 1}. {lesson.title}
                                </Typography>
                                
                                <div className="flex items-center gap-2 mt-1 flex-wrap">
                                  <Chip
                                    size="sm"
                                    value={lesson.lessonType === 'THEORY' ? 'Lý thuyết' : 'Thực hành'}
                                    className={`text-xs ${
                                      lesson.lessonType === 'THEORY'
                                        ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
                                        : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                                    }`}
                                  />

                                  {lesson.estimatedDuration && (
                                    <Typography variant="small" className="text-secondary text-xs">
                                      {Math.ceil(lesson.estimatedDuration / 60)}p
                                    </Typography>
                                  )}

                                  {lesson.isCompleted && lesson.userScore > 0 && (
                                    <Typography variant="small" className="text-green-600 dark:text-green-400 font-medium text-xs">
                                      {lesson.userScore}%
                                    </Typography>
                                  )}
                                </div>
                              </div>
                            </ListItem>
                          );
                        })}
                      </List>
                    ) : (
                      <div className="p-4 text-center">
                        <Typography variant="small" className="text-secondary">
                          Chưa có bài học
                        </Typography>
                      </div>
                    )}
                  </div>
                </AccordionBody>
              </Accordion>
            );
          })
        ) : (
          <div className="p-8 text-center">
            <Typography variant="small" className="text-secondary">
              Đang tải danh sách bài học...
            </Typography>
          </div>
        )}
      </div>
    </Card>
  );
};

export default LessonSidebar;