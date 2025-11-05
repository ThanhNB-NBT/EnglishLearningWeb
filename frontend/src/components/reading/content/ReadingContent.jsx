import React from "react";
import {
  Card,
  CardBody,
  Typography,
  Button,
} from "@material-tailwind/react";
import {
  EyeIcon,
  EyeSlashIcon,
  BookOpenIcon,
} from "@heroicons/react/24/outline";

const ReadingContent = ({
  lesson,
  showTranslation,
  onToggleTranslation,
  hasSubmitted,
}) => {
  if (!lesson) return null;

  return (
    <div className="h-full flex flex-col bg-primary">
      {/* ✅ REMOVED HEADER - Only show toggle button in content */}
      
      {/* Content - Scrollable */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* Toggle Translation Button - Floating top right */}
        {hasSubmitted && lesson.contentTranslation && (
          <div className="flex justify-end mb-4">
            <Button
              size="sm"
              variant={showTranslation ? "filled" : "outlined"}
              color="purple"
              onClick={onToggleTranslation}
              className="flex items-center gap-2"
            >
              {showTranslation ? (
                <>
                  <EyeSlashIcon className="h-4 w-4" />
                  Ẩn bản dịch
                </>
              ) : (
                <>
                  <EyeIcon className="h-4 w-4" />
                  Hiển thị bản dịch
                </>
              )}
            </Button>
          </div>
        )}

        {/* Reading Passage */}
        <div className="prose prose-lg dark:prose-invert max-w-none">
          <div
            className="text-primary leading-relaxed whitespace-pre-wrap"
            dangerouslySetInnerHTML={{ __html: lesson.content }}
          />
        </div>

        {/* Translation - Show after submit if toggled */}
        {hasSubmitted && showTranslation && lesson.contentTranslation && (
          <div className="mt-8 pt-6 border-t-2 border-purple-200 dark:border-purple-800">
            <div className="flex items-center gap-2 mb-4">
              <div className="h-1 w-12 bg-purple-500 rounded"></div>
              <Typography
                variant="h6"
                className="text-purple-700 dark:text-purple-300 font-bold"
              >
                Bản dịch tiếng Việt
              </Typography>
            </div>
            <div className="prose prose-lg dark:prose-invert max-w-none bg-purple-50 dark:bg-purple-900/10 p-6 rounded-lg border border-purple-200 dark:border-purple-800">
              <div
                className="text-secondary leading-relaxed whitespace-pre-wrap"
                dangerouslySetInnerHTML={{ __html: lesson.contentTranslation }}
              />
            </div>
          </div>
        )}

        {/* Placeholder if no content */}
        {!lesson.content && (
          <div className="text-center py-12">
            <BookOpenIcon className="h-16 w-16 text-secondary mx-auto mb-4 opacity-50" />
            <Typography variant="h6" className="text-secondary">
              Nội dung bài đọc đang được cập nhật
            </Typography>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReadingContent;