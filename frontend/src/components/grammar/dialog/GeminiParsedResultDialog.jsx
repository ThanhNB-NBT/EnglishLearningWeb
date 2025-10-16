import React, { useState } from 'react';
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Card,
  CardBody,
  Typography,
  Alert,
  Chip,
  Checkbox,
  Collapse,
  Textarea,
} from '@material-tailwind/react';
import {
  SparklesIcon,
  CheckCircleIcon,
  XMarkIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  EyeIcon,
  PencilIcon,
} from '@heroicons/react/24/outline';

/**
 * ✨ Component: Enhanced Dialog với khả năng xem chi tiết và chọn lessons
 */
const GeminiParsedResultDialog = ({
  open,
  parsedData,
  summary,
  onClose,
  onConfirm,
}) => {
  // State cho việc chọn lessons
  const [selectedLessons, setSelectedLessons] = useState([]);
  
  // State cho việc mở rộng chi tiết lesson
  const [expandedLessons, setExpandedLessons] = useState([]);
  
  // State cho việc edit lesson
  const [editingLesson, setEditingLesson] = useState(null);
  const [editedLessons, setEditedLessons] = useState({});

  // Initialize: Chọn tất cả lessons khi mở dialog
  React.useEffect(() => {
    if (parsedData?.lessons) {
      setSelectedLessons(parsedData.lessons.map((_, index) => index));
      setExpandedLessons([]);
      setEditedLessons({});
      setEditingLesson(null);
    }
  }, [parsedData]);

  // Toggle chọn lesson
  const toggleSelectLesson = (index) => {
    setSelectedLessons(prev => 
      prev.includes(index) 
        ? prev.filter(i => i !== index)
        : [...prev, index]
    );
  };

  // Toggle chọn tất cả
  const toggleSelectAll = () => {
    if (selectedLessons.length === parsedData?.lessons?.length) {
      setSelectedLessons([]);
    } else {
      setSelectedLessons(parsedData.lessons.map((_, index) => index));
    }
  };

  // Toggle mở rộng chi tiết
  const toggleExpand = (index) => {
    setExpandedLessons(prev =>
      prev.includes(index)
        ? prev.filter(i => i !== index)
        : [...prev, index]
    );
  };

  // Start editing
  const startEditing = (index) => {
    const lesson = getLesson(index);
    setEditingLesson(index);
    setEditedLessons(prev => ({
      ...prev,
      [index]: {
        title: lesson.title,
        content: lesson.content,
      }
    }));
  };

  // Save edit
  const saveEdit = () => {
    setEditingLesson(null);
  };

  // Cancel edit
  const cancelEdit = (index) => {
    setEditingLesson(null);
    setEditedLessons(prev => {
      const newEdited = { ...prev };
      delete newEdited[index];
      return newEdited;
    });
  };

  // Update edited content
  const updateEditedLesson = (index, field, value) => {
    setEditedLessons(prev => ({
      ...prev,
      [index]: {
        ...prev[index],
        [field]: value
      }
    }));
  };

  // Get lesson (edited or original)
  const getLesson = (index) => {
    return editedLessons[index] || parsedData.lessons[index];
  };

  // Handle confirm with selected and edited lessons
  const handleConfirmSelection = () => {
    if (selectedLessons.length === 0) {
      return;
    }

    // Build final data with only selected lessons
    const finalData = {
      lessons: selectedLessons.map(index => {
        const original = parsedData.lessons[index];
        const edited = editedLessons[index];
        
        return {
          ...original,
          ...(edited && {
            title: edited.title,
            content: edited.content,
          })
        };
      })
    };

    onConfirm(finalData);
  };

  if (!parsedData?.lessons) return null;

  const allSelected = selectedLessons.length === parsedData.lessons.length;

  return (
    <Dialog open={open} handler={onClose} size="xl" className="max-h-[90vh] overflow-hidden">
      <DialogHeader className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <SparklesIcon className="h-6 w-6 text-purple-500" />
          <span>Kết quả phân tích từ Gemini AI</span>
        </div>
        <Chip
          value={`${selectedLessons.length}/${parsedData.lessons.length} được chọn`}
          color={selectedLessons.length > 0 ? "green" : "gray"}
          size="sm"
        />
      </DialogHeader>

      <DialogBody className="max-h-[calc(90vh-200px)] overflow-y-auto">
        {/* Summary */}
        {summary && (
          <Alert color="blue" icon={<SparklesIcon className="h-5 w-5" />} className="mb-4">
            <Typography variant="h6" className="mb-3 font-bold">
              📊 Tóm tắt kết quả:
            </Typography>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-3 bg-white/50 rounded-lg">
                <Typography className="text-2xl font-bold text-blue-600">
                  {summary.totalLessons}
                </Typography>
                <Typography variant="small" className="opacity-70">
                  Tổng bài học
                </Typography>
              </div>
              <div className="text-center p-3 bg-white/50 rounded-lg">
                <Typography className="text-2xl font-bold text-blue-500">
                  {summary.theoryLessons}
                </Typography>
                <Typography variant="small" className="opacity-70">
                  Bài lý thuyết
                </Typography>
              </div>
              <div className="text-center p-3 bg-white/50 rounded-lg">
                <Typography className="text-2xl font-bold text-green-600">
                  {summary.practiceLessons}
                </Typography>
                <Typography variant="small" className="opacity-70">
                  Bài thực hành
                </Typography>
              </div>
              <div className="text-center p-3 bg-white/50 rounded-lg">
                <Typography className="text-2xl font-bold text-purple-600">
                  {summary.totalQuestions}
                </Typography>
                <Typography variant="small" className="opacity-70">
                  Tổng câu hỏi
                </Typography>
              </div>
            </div>
            <Typography variant="small" className="mt-3 opacity-70">
              ⚡ File size: {summary.fileSize} | Tên file: {summary.fileName}
            </Typography>
          </Alert>
        )}

        {/* Select All Checkbox */}
        <div className="flex items-center justify-between mb-4 p-3 bg-gray-50 rounded-lg">
          <Checkbox
            checked={allSelected}
            onChange={toggleSelectAll}
            label={
              <Typography variant="small" className="font-medium">
                {allSelected ? "Bỏ chọn tất cả" : "Chọn tất cả"}
              </Typography>
            }
          />
          <Typography variant="small" className="text-gray-600">
            {selectedLessons.length} bài học được chọn
          </Typography>
        </div>

        {/* Lessons List */}
        <div className="space-y-3">
          <Typography variant="h6" color="blue-gray" className="mb-2">
            📚 Danh sách bài học:
          </Typography>
          
          {parsedData.lessons.map((lesson, index) => {
            const isSelected = selectedLessons.includes(index);
            const isExpanded = expandedLessons.includes(index);
            const isEditing = editingLesson === index;
            const currentLesson = getLesson(index);

            return (
              <Card 
                key={index} 
                className={`border transition-all ${
                  isSelected ? 'border-green-500 shadow-md' : 'border-gray-300'
                }`}
              >
                <CardBody className="p-4">
                  {/* Header Row */}
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-start space-x-3 flex-1">
                      <Checkbox
                        checked={isSelected}
                        onChange={() => toggleSelectLesson(index)}
                      />
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-1">
                          <Typography variant="small" className="font-bold text-blue-gray-600">
                            Bài {index + 1}
                          </Typography>
                          <Chip
                            size="sm"
                            value={lesson.lessonType === "THEORY" ? "Lý thuyết" : "Thực hành"}
                            color={lesson.lessonType === "THEORY" ? "blue" : "green"}
                            className="text-xs"
                          />
                        </div>
                        
                        {/* Title - Editable */}
                        {isEditing ? (
                          <Textarea
                            value={editedLessons[index]?.title || ''}
                            onChange={(e) => updateEditedLesson(index, 'title', e.target.value)}
                            className="mb-2"
                            label="Tiêu đề"
                          />
                        ) : (
                          <Typography variant="h6" color="blue-gray" className="mb-2">
                            {currentLesson.title}
                          </Typography>
                        )}
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center space-x-2">
                      {isEditing ? (
                        <>
                          <Button
                            size="sm"
                            color="green"
                            variant="text"
                            onClick={() => saveEdit(index)}
                            className="flex items-center"
                          >
                            <CheckCircleIcon className="h-4 w-4 mr-1" />
                            Lưu
                          </Button>
                          <Button
                            size="sm"
                            color="red"
                            variant="text"
                            onClick={() => cancelEdit(index)}
                          >
                            Hủy
                          </Button>
                        </>
                      ) : (
                        <Button
                          size="sm"
                          color="blue"
                          variant="text"
                          onClick={() => startEditing(index)}
                          className="flex items-center"
                        >
                          <PencilIcon className="h-4 w-4 mr-1" />
                          Sửa
                        </Button>
                      )}
                      
                      <Button
                        size="sm"
                        variant="text"
                        onClick={() => toggleExpand(index)}
                        className="flex items-center"
                      >
                        {isExpanded ? (
                          <>
                            <ChevronUpIcon className="h-4 w-4 mr-1" />
                            Ẩn
                          </>
                        ) : (
                          <>
                            <ChevronDownIcon className="h-4 w-4 mr-1" />
                            Xem
                          </>
                        )}
                      </Button>
                    </div>
                  </div>

                  {/* Content Preview (always visible) */}
                  {!isExpanded && !isEditing && (
                    <div className="bg-gray-50 p-3 rounded-lg mb-2">
                      <Typography variant="small" color="gray" className="line-clamp-2">
                        {currentLesson.content?.replace(/<[^>]*>/g, '').substring(0, 150)}...
                      </Typography>
                    </div>
                  )}

                  {/* Expanded Content - Editable */}
                  <Collapse open={isExpanded}>
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <Typography variant="small" className="font-bold mb-2">
                        Nội dung chi tiết:
                      </Typography>
                      {isEditing ? (
                        <Textarea
                          value={editedLessons[index]?.content || ''}
                          onChange={(e) => updateEditedLesson(index, 'content', e.target.value)}
                          rows={8}
                          label="Nội dung"
                        />
                      ) : (
                        <div className="bg-gray-50 p-4 rounded-lg max-h-96 overflow-y-auto">
                          <Typography variant="small" className="whitespace-pre-wrap">
                            {currentLesson.content?.replace(/\\n/g, '\n')}
                          </Typography>
                        </div>
                      )}

                      {/* Questions Preview */}
                      {lesson.questions && lesson.questions.length > 0 && (
                        <div className="mt-3">
                          <Typography variant="small" className="font-bold mb-2">
                            📝 Câu hỏi ({lesson.questions.length}):
                          </Typography>
                          <div className="space-y-2">
                            {lesson.questions.slice(0, 3).map((q, qIndex) => (
                              <div key={qIndex} className="bg-white p-2 rounded border">
                                <Typography variant="small" className="font-medium">
                                  {qIndex + 1}. {q.questionText}
                                </Typography>
                                <Typography variant="small" className="text-green-600">
                                  ✓ {q.correctAnswer}
                                </Typography>
                              </div>
                            ))}
                            {lesson.questions.length > 3 && (
                              <Typography variant="small" className="text-gray-600 text-center">
                                ... và {lesson.questions.length - 3} câu hỏi khác
                              </Typography>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </Collapse>

                  {/* Metadata */}
                  <div className="flex items-center space-x-4 text-xs text-gray-600 mt-2">
                    <span>⏱️ {lesson.estimatedDuration}s</span>
                    <span>🎯 {lesson.pointsReward} điểm</span>
                    <span>📍 Thứ tự: {lesson.orderIndex}</span>
                    {lesson.questions && lesson.questions.length > 0 && (
                      <span className="text-green-600 font-medium">
                        ✅ {lesson.questions.length} câu hỏi
                      </span>
                    )}
                  </div>
                </CardBody>
              </Card>
            );
          })}
        </div>

        {/* Warning */}
        <Alert color="amber" className="mt-4">
          <Typography variant="small">
            ⚠️ <strong>Lưu ý:</strong> Hãy kiểm tra kỹ nội dung trước khi lưu. 
            Bạn có thể chỉnh sửa sau khi import.
          </Typography>
        </Alert>
      </DialogBody>

      <DialogFooter className="space-x-2">
        <Button
          variant="outlined"
          onClick={onClose}
          className="flex items-center"
        >
          <XMarkIcon className="h-4 w-4 mr-2" />
          Hủy
        </Button>
        <Button
          color="purple"
          onClick={handleConfirmSelection}
          disabled={selectedLessons.length === 0}
          className="flex items-center shadow-lg"
        >
          <CheckCircleIcon className="h-4 w-4 mr-2" />
          Lưu {selectedLessons.length} bài học đã chọn
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default GeminiParsedResultDialog;