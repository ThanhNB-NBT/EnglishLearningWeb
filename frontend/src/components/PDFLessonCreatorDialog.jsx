import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
  Checkbox,
  Card,
  CardBody,
  Tabs,
  TabsHeader,
  TabsBody,
  Tab,
  TabPanel,
  Chip,
  Alert,
} from '@material-tailwind/react';
import {
  DocumentTextIcon,
  CheckCircleIcon,
  XCircleIcon,
  BookOpenIcon,
  AcademicCapIcon,
} from '@heroicons/react/24/outline';

const PDFLessonCreatorDialog = ({ open, analyzedContent, onClose, onCreateLessons }) => {
  const [selectedSections, setSelectedSections] = useState([]);
  const [activeTab, setActiveTab] = useState('preview');

  // Reset state khi dialog mở với nội dung mới
  useEffect(() => {
    if (open && analyzedContent) {
      setSelectedSections([]);
      setActiveTab('preview');
    }
  }, [open, analyzedContent]);

  // Early return chỉ khi dialog đóng HOẶC không có data
  if (!open) return null;
  
  // Destructure với default values an toàn
  const sections = analyzedContent?.sections || [];
  const exercises = analyzedContent?.exercises || [];
  const theorySections = sections.filter(s => s?.type === 'theory');

  const handleToggleSection = (index) => {
    setSelectedSections(prev => 
      prev.includes(index) 
        ? prev.filter(i => i !== index)
        : [...prev, index]
    );
  };

  const handleSelectAll = () => {
    if (selectedSections.length === theorySections.length) {
      setSelectedSections([]);
    } else {
      setSelectedSections(theorySections.map((_, index) => index));
    }
  };

  const handleCreate = () => {
    const selectedLessons = selectedSections.map(index => ({
      section: theorySections[index],
      includeExercises: exercises.length > 0
    }));
    onCreateLessons(selectedLessons);
  };

  return (
    <Dialog open={open} handler={onClose} size="xxl" className="max-h-[90vh] overflow-hidden">
      <DialogHeader className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <DocumentTextIcon className="h-6 w-6 text-blue-500" />
          <span>Tạo bài học từ PDF</span>
        </div>
        <Typography variant="small" color="gray">
          Đã phát hiện {theorySections.length} phần lý thuyết và {exercises.length} câu hỏi
        </Typography>
      </DialogHeader>

      <DialogBody className="overflow-y-auto">
        {!analyzedContent ? (
          <div className="flex items-center justify-center py-12">
            <Typography variant="paragraph" color="gray">
              Đang tải nội dung...
            </Typography>
          </div>
        ) : sections.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 space-y-4">
            <XCircleIcon className="h-16 w-16 text-red-300" />
            <Typography variant="h6" color="red">
              Không tìm thấy nội dung phù hợp
            </Typography>
            <Typography variant="small" color="gray">
              Vui lòng kiểm tra lại file PDF hoặc thử điều chỉnh cấu hình xử lý
            </Typography>
          </div>
        ) : (
          <Tabs value={activeTab} className="mb-4">
            <TabsHeader>
              <Tab value="preview" onClick={() => setActiveTab('preview')}>
                <div className="flex items-center space-x-2">
                  <BookOpenIcon className="h-4 w-4" />
                  <span>Xem trước nội dung</span>
                </div>
              </Tab>
              <Tab value="select" onClick={() => setActiveTab('select')}>
                <div className="flex items-center space-x-2">
                  <CheckCircleIcon className="h-4 w-4" />
                  <span>Chọn phần tạo bài học</span>
                </div>
              </Tab>
              {exercises.length > 0 && (
                <Tab value="exercises" onClick={() => setActiveTab('exercises')}>
                  <div className="flex items-center space-x-2">
                    <AcademicCapIcon className="h-4 w-4" />
                    <span>Bài tập ({exercises.length})</span>
                  </div>
                </Tab>
              )}
            </TabsHeader>

            <TabsBody>
              {/* Preview Tab */}
              <TabPanel value="preview" className="p-0 pt-4">
                <div className="space-y-4 max-h-[500px] overflow-y-auto">
                  {sections.map((section, index) => (
                    <Card key={index} className="border">
                      <CardBody>
                        <div className="flex items-start justify-between mb-3">
                          <Typography variant="h6" color="blue-gray">
                            {section.title}
                          </Typography>
                          <Chip
                            size="sm"
                            value={section.type === 'theory' ? 'Lý thuyết' : 'Bài tập'}
                            color={section.type === 'theory' ? 'blue' : 'green'}
                          />
                        </div>
                        <div className="bg-gray-50 p-4 rounded-lg max-h-60 overflow-y-auto">
                          {section.content.slice(0, 5).map((item, idx) => {
                            const text = typeof item === 'string' ? item : item.subsection;
                            return (
                              <Typography key={idx} variant="small" className="mb-2">
                                {text.substring(0, 200)}{text.length > 200 ? '...' : ''}
                              </Typography>
                            );
                          })}
                          {section.content.length > 5 && (
                            <Typography variant="small" color="gray" className="italic">
                              ... và {section.content.length - 5} nội dung khác
                            </Typography>
                          )}
                        </div>
                      </CardBody>
                    </Card>
                  ))}
                </div>
              </TabPanel>

              {/* Selection Tab */}
              <TabPanel value="select" className="p-0 pt-4">
                <div className="mb-4 flex justify-between items-center">
                  <Typography variant="h6" color="blue-gray">
                    Chọn các phần để tạo bài học
                  </Typography>
                  <Button
                    size="sm"
                    variant="outlined"
                    onClick={handleSelectAll}
                    className="flex items-center space-x-2"
                  >
                    {selectedSections.length === theorySections.length ? (
                      <>
                        <XCircleIcon className="h-4 w-4" />
                        <span>Bỏ chọn tất cả</span>
                      </>
                    ) : (
                      <>
                        <CheckCircleIcon className="h-4 w-4" />
                        <span>Chọn tất cả</span>
                      </>
                    )}
                  </Button>
                </div>

                <Alert color="blue" className="mb-4">
                  <Typography variant="small">
                    Mỗi phần được chọn sẽ tạo thành 1 bài học riêng biệt. Bạn có thể chỉnh sửa sau khi tạo.
                  </Typography>
                </Alert>

                <div className="space-y-3 max-h-[450px] overflow-y-auto">
                  {theorySections.map((section, index) => (
                    <Card 
                      key={index} 
                      className={`border-2 cursor-pointer transition-all ${
                        selectedSections.includes(index) 
                          ? 'border-blue-500 bg-blue-50' 
                          : 'border-gray-200 hover:border-blue-200'
                      }`}
                      onClick={() => handleToggleSection(index)}
                    >
                      <CardBody className="p-4">
                        <div className="flex items-start space-x-3">
                          <Checkbox
                            checked={selectedSections.includes(index)}
                            onChange={() => handleToggleSection(index)}
                            color="blue"
                            className="mt-1"
                          />
                          <div className="flex-1">
                            <Typography variant="h6" color="blue-gray" className="mb-2">
                              Bài {index + 1}: {section.title}
                            </Typography>
                            <Typography variant="small" color="gray">
                              {section.content.length} phần nội dung
                            </Typography>
                          </div>
                        </div>
                      </CardBody>
                    </Card>
                  ))}
                </div>
              </TabPanel>

              {/* Exercises Tab */}
              {exercises.length > 0 && (
                <TabPanel value="exercises" className="p-0 pt-4">
                  <Alert color="amber" className="mb-4">
                    <Typography variant="small">
                      Các câu hỏi này sẽ được tự động gán vào bài học thực hành. Bạn có thể chỉnh sửa sau.
                    </Typography>
                  </Alert>

                  <div className="space-y-4 max-h-[450px] overflow-y-auto">
                    {exercises.slice(0, 10).map((question, index) => (
                      <Card key={index} className="border">
                        <CardBody>
                          <div className="flex items-start space-x-3">
                            <Chip size="sm" value={`#${index + 1}`} color="green" />
                            <div className="flex-1">
                              <Typography variant="paragraph" className="mb-2 font-medium">
                                {question.questionText}
                              </Typography>
                              {question.options && question.options.length > 0 && (
                                <div className="space-y-1 ml-4">
                                  {question.options.map((option, optIdx) => (
                                    <div 
                                      key={optIdx}
                                      className={`flex items-center space-x-2 p-2 rounded ${
                                        option.isCorrect ? 'bg-green-50' : 'bg-gray-50'
                                      }`}
                                    >
                                      <Typography variant="small">
                                        {option.optionText}
                                      </Typography>
                                      {option.isCorrect && (
                                        <CheckCircleIcon className="h-4 w-4 text-green-600" />
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                              {question.explanation && (
                                <Typography variant="small" color="gray" className="mt-2 italic">
                                  Giải thích: {question.explanation}
                                </Typography>
                              )}
                            </div>
                          </div>
                        </CardBody>
                      </Card>
                    ))}
                    {exercises.length > 10 && (
                      <Typography variant="small" color="gray" className="text-center">
                        ... và {exercises.length - 10} câu hỏi khác
                      </Typography>
                    )}
                  </div>
                </TabPanel>
              )}
            </TabsBody>
          </Tabs>
        )}
      </DialogBody>

      <DialogFooter className="space-x-2">
        <Button variant="outlined" onClick={onClose}>
          Hủy
        </Button>
        <Button
          color="blue"
          onClick={handleCreate}
          disabled={selectedSections.length === 0 || !analyzedContent}
          className="flex items-center space-x-2"
        >
          <CheckCircleIcon className="h-4 w-4" />
          <span>Sử dụng nội dung đã chọn</span>
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default PDFLessonCreatorDialog;