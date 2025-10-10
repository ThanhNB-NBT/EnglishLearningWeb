import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { grammarAdminAPI } from '../../../api';
import { ADMIN_ROUTES } from '../../../constants/routes';
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Typography,
  Spinner,
  Chip,
  IconButton,
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Input,
  Select,
  Option,
  Menu,
  MenuHandler,
  MenuList,
  MenuItem,
  Breadcrumbs,
  Alert,
} from '@material-tailwind/react';
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  EllipsisVerticalIcon,
  QuestionMarkCircleIcon,
  ArrowPathIcon,
  ArrowLeftIcon,
  CheckCircleIcon,
  XCircleIcon,
  ChatBubbleLeftRightIcon,
  LanguageIcon,
  DocumentTextIcon,
} from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';

const GrammarQuestionList = () => {
  const { lessonId } = useParams();
  const [questions, setQuestions] = useState([]);
  const [filteredQuestions, setFilteredQuestions] = useState([]);
  const [setLessonInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, question: null });
  const [previewDialog, setPreviewDialog] = useState({ open: false, question: null });
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const navigate = useNavigate();

  const questionTypes = [
    { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
    { value: 'FILL_BLANK', label: 'Điền từ' },
    { value: 'TRANSLATION_VI_EN', label: 'Dịch Việt-Anh' },
    { value: 'TRANSLATION_EN_VI', label: 'Dịch Anh-Việt' },
  ];

  useEffect(() => {
    if (lessonId) {
      loadQuestions();
      loadLessonInfo();
    }
  }, [lessonId]);

  useEffect(() => {
    filterQuestions();
  }, [questions, searchTerm, filterType]);

  const loadQuestions = async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      setQuestions(response.data.data || []);
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách câu hỏi');
      console.error('Load questions error:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadLessonInfo = async () => {
    try {
      // Get lesson info by finding it in topic lessons
      // We need to get topicId first from URL or make another API call
      // For now, we'll make a simple approach
      setLessonInfo({ title: 'Bài học thực hành', id: lessonId });
    } catch (error) {
      console.error('Load lesson info error:', error);
    }
  };

  const filterQuestions = () => {
    let filtered = questions;

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(question =>
        question.questionText?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        question.explanation?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Type filter
    if (filterType) {
      filtered = filtered.filter(question => question.questionType === filterType);
    }

    setFilteredQuestions(filtered);
  };

  const handleDelete = async () => {
    if (!deleteDialog.question) return;
    
    try {
      await grammarAdminAPI.deleteQuestion(deleteDialog.question.id);
      setQuestions(questions.filter(question => question.id !== deleteDialog.question.id));
      toast.success('Xóa câu hỏi thành công!');
    } catch (error) {
      toast.error('Lỗi khi xóa câu hỏi: ' + (error.response?.data?.message || error.message));
      console.error('Delete question error:', error);
    } finally {
      setDeleteDialog({ open: false, question: null });
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return 'blue';
      case 'FILL_BLANK': return 'green';
      case 'TRANSLATION_VI_EN': return 'orange';
      case 'TRANSLATION_EN_VI': return 'purple';
      default: return 'gray';
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return ChatBubbleLeftRightIcon;
      case 'FILL_BLANK': return DocumentTextIcon;
      case 'TRANSLATION_VI_EN':
      case 'TRANSLATION_EN_VI': return LanguageIcon;
      default: return QuestionMarkCircleIcon;
    }
  };

  const formatQuestionType = (type) => {
    const typeObj = questionTypes.find(t => t.value === type);
    return typeObj ? typeObj.label : type;
  };

  const truncateText = (text, maxLength = 100) => {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" color="blue-gray">
          Đang tải danh sách câu hỏi...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6">
      {/* Breadcrumbs */}
      <Breadcrumbs className="bg-transparent p-0">
        <Typography 
          className="opacity-60 cursor-pointer hover:opacity-100"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        >
          Chủ đề ngữ pháp
        </Typography>
        <Typography 
          className="opacity-60 cursor-pointer hover:opacity-100"
          onClick={() => navigate(-1)}
        >
          Bài học
        </Typography>
        <Typography color="blue-gray">Câu hỏi</Typography>
      </Breadcrumbs>

      {/* Header Section */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center space-y-4 lg:space-y-0">
            <div className="flex items-center space-x-3">
              <IconButton
                variant="outlined"
                size="sm"
                onClick={() => navigate(-1)}
                className="border-gray-300"
              >
                <ArrowLeftIcon className="h-4 w-4" />
              </IconButton>
              <div className="p-2 bg-purple-50 rounded-lg">
                <QuestionMarkCircleIcon className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <Typography variant="h4" color="blue-gray" className="font-bold">
                  Câu hỏi bài học
                </Typography>
                <Typography variant="small" color="blue-gray" className="opacity-70">
                  Quản lý câu hỏi cho bài học thực hành
                </Typography>
              </div>
            </div>
            
            <Button
              size="lg"
              className="flex bg-gradient-to-r from-purple-500 to-purple-600 shadow-lg hover:shadow-xl transition-all duration-300"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_CREATE( lessonId))}
            >
              <PlusIcon className="h-6 w-6 mr-2" />
              <span className="font-bold">
                Tạo câu hỏi mới
              </span>
            </Button>
          </div>
        </CardBody>
      </Card>

      {/* Filter Section */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Tìm kiếm
              </Typography>
              <Input
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                placeholder="Tìm theo nội dung câu hỏi"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-blue-500"
                labelProps={{ className: 'hidden' }}
              />
            </div>
            
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Loại câu hỏi
              </Typography>
              <Select
                value={filterType}
                onChange={val => setFilterType(val)}
                className="!border-blue-gray-200 focus:!border-blue-500"
              >
                <Option value="">Tất cả loại</Option>
                {questionTypes.map(type => (
                  <Option key={type.value} value={type.value}>
                    {type.label}
                  </Option>
                ))}
              </Select>
            </div>

            <div className="flex">
              <Button
                variant="outlined"
                size="sm"
                onClick={resetFilters}
                className="border-gray-300 text-gray-800"
              >
                <span className="flex items-center hover:text-blue-500">
                  <ArrowPathIcon className="h-5 w-5 mr-2" />
                  Reset
                </span>
              </Button>
            </div>
          </div>

          {/* Filter Summary */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Typography variant="small" color="blue-gray" className="opacity-70">
                Hiển thị {filteredQuestions.length} / {questions.length} câu hỏi
              </Typography>
              {(searchTerm || filterType) && (
                <Chip
                  size="sm"
                  value="Đang lọc"
                  color="blue"
                  className="text-xs"
                />
              )}
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Questions List */}
      {filteredQuestions.length === 0 ? (
        <Card className="border border-blue-gray-100">
          <CardBody className="p-12 text-center">
            <QuestionMarkCircleIcon className="h-16 w-16 text-blue-gray-300 mx-auto mb-4" />
            <Typography variant="h6" color="blue-gray" className="mb-2">
              {questions.length === 0 ? 'Chưa có câu hỏi nào' : 'Không tìm thấy câu hỏi'}
            </Typography>
            <Typography variant="small" color="blue-gray" className="opacity-70 mb-4">
              {questions.length === 0 
                ? 'Hãy tạo câu hỏi đầu tiên cho bài học này.'
                : 'Thử thay đổi bộ lọc để tìm thấy câu hỏi bạn cần.'
              }
            </Typography>
            {questions.length === 0 && (
              <Button
                color="purple"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_CREATE(lessonId))}
              >
                <PlusIcon className="h-4 w-4 mr-2" />
                Tạo câu hỏi đầu tiên
              </Button>
            )}
          </CardBody>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredQuestions.map((question, index) => {
            const TypeIcon = getTypeIcon(question.questionType);
            return (
              <Card key={question.id} className="border border-blue-gray-100 hover:shadow-md transition-shadow duration-300">
                <CardBody className="p-6">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-3">
                        <Typography variant="small" color="blue-gray" className="font-medium opacity-70">
                          Câu {index + 1}
                        </Typography>
                        <Chip
                          size="sm"
                          value={formatQuestionType(question.questionType)}
                          color={getTypeColor(question.questionType)}
                          icon={<TypeIcon className="h-3 w-3" />}
                          className="text-xs"
                        />
                        <Chip
                          size="sm"
                          value={`${question.points || 10} điểm`}
                          color="gray"
                          className="text-xs"
                        />
                      </div>
                      
                      <Typography variant="paragraph" color="blue-gray" className="mb-3 font-medium">
                        {truncateText(question.questionText, 150)}
                      </Typography>

                      {/* Show options for multiple choice */}
                      {question.questionType === 'MULTIPLE_CHOICE' && question.options && (
                        <div className="grid grid-cols-2 gap-2 mb-3">
                          {question.options.slice(0, 4).map((option, idx) => (
                            <div key={idx} className="flex items-center space-x-2">
                              <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                                option.isCorrect ? 'bg-green-100 border-green-500' : 'border-gray-300'
                              }`}>
                                {option.isCorrect && <CheckCircleIcon className="h-3 w-3 text-green-600" />}
                              </div>
                              <Typography variant="small" color="blue-gray" className="opacity-80">
                                {String.fromCharCode(65 + idx)}. {truncateText(option.optionText, 50)}
                              </Typography>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Show correct answer for other types */}
                      {question.questionType !== 'MULTIPLE_CHOICE' && question.correctAnswer && (
                        <div className="mb-3">
                          <Typography variant="small" color="green" className="font-medium">
                            Đáp án: {truncateText(question.correctAnswer, 100)}
                          </Typography>
                        </div>
                      )}

                      {/* Show explanation if available */}
                      {question.explanation && (
                        <Typography variant="small" color="blue-gray" className="opacity-70 italic">
                          Giải thích: {truncateText(question.explanation, 100)}
                        </Typography>
                      )}
                    </div>

                    <div className="flex flex-col space-y-2 ml-4">
                      <Menu>
                        <MenuHandler>
                          <IconButton variant="text" size="sm">
                            <EllipsisVerticalIcon className="h-4 w-4" />
                          </IconButton>
                        </MenuHandler>
                        <MenuList>
                          <MenuItem 
                            onClick={() => setPreviewDialog({ open: true, question })}
                            className="flex items-center"
                          >
                            <EyeIcon className="h-4 w-4 mr-2" />
                            <Typography variant="small" className="font-normal">
                              Xem chi tiết
                            </Typography>
                          </MenuItem>
                          <MenuItem 
                            onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_EDIT(lessonId, question.id))}
                            className="flex items-center"
                          >
                            <PencilIcon className="h-4 w-4 mr-2" />
                            <Typography variant="small" className="font-normal">
                              Chỉnh sửa
                            </Typography>
                          </MenuItem>
                          <MenuItem 
                            onClick={() => setDeleteDialog({ open: true, question })}
                            className="flex items-center text-red-500"
                          >
                            <TrashIcon className="h-4 w-4 mr-2" />
                            Xóa
                          </MenuItem>
                        </MenuList>
                      </Menu>

                      <div className="flex space-x-1">
                        <IconButton
                          size="sm"
                          variant="outlined"
                          className="border-blue-500 text-blue-500 hover:bg-blue-50"
                          onClick={() => setPreviewDialog({ open: true, question })}
                        >
                          <EyeIcon className="h-3 w-3" />
                        </IconButton>
                        <IconButton
                          size="sm"
                          variant="outlined"
                          className="border-gray-500 text-gray-500 hover:bg-gray-50"
                          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_EDIT(lessonId, question.id))}
                        >
                          <PencilIcon className="h-3 w-3" />
                        </IconButton>
                        <IconButton
                          size="sm"
                          variant="outlined"
                          color="red"
                          onClick={() => setDeleteDialog({ open: true, question })}
                        >
                          <TrashIcon className="h-3 w-3" />
                        </IconButton>
                      </div>
                    </div>
                  </div>
                </CardBody>
              </Card>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog 
        open={deleteDialog.open} 
        handler={() => setDeleteDialog({ open: false, question: null })}
        size="sm"
      >
        <DialogHeader className="flex items-center space-x-2">
          <TrashIcon className="h-6 w-6 text-red-500" />
          <span>Xác nhận xóa câu hỏi</span>
        </DialogHeader>
        <DialogBody>
          <Typography variant="paragraph" color="blue-gray">
            Bạn có chắc chắn muốn xóa câu hỏi này không?
          </Typography>
          <Typography variant="small" color="red" className="mt-2">
            Hành động này không thể hoàn tác.
          </Typography>
        </DialogBody>
        <DialogFooter className="space-x-2">
          <Button
            variant="outlined"
            onClick={() => setDeleteDialog({ open: false, question: null })}
          >
            Hủy
          </Button>
          <Button
            color="red"
            onClick={handleDelete}
          >
            Xóa câu hỏi
          </Button>
        </DialogFooter>
      </Dialog>

      {/* Preview Question Dialog */}
      <Dialog 
        open={previewDialog.open} 
        handler={() => setPreviewDialog({ open: false, question: null })}
        size="lg"
      >
        <DialogHeader className="flex items-center space-x-2">
          <EyeIcon className="h-6 w-6 text-blue-500" />
          <span>Chi tiết câu hỏi</span>
        </DialogHeader>
        <DialogBody className="space-y-4 max-h-96 overflow-y-auto">
          {previewDialog.question && (
            <div className="space-y-4">
              <div>
                <Typography variant="h6" color="blue-gray" className="mb-2">
                  Loại câu hỏi:
                </Typography>
                <Chip
                  value={formatQuestionType(previewDialog.question.questionType)}
                  color={getTypeColor(previewDialog.question.questionType)}
                  className="text-sm"
                />
              </div>

              <div>
                <Typography variant="h6" color="blue-gray" className="mb-2">
                  Nội dung câu hỏi:
                </Typography>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <Typography variant="paragraph">
                    {previewDialog.question.questionText}
                  </Typography>
                </div>
              </div>

              {previewDialog.question.questionType === 'MULTIPLE_CHOICE' && previewDialog.question.options && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Các lựa chọn:
                  </Typography>
                  <div className="space-y-2">
                    {previewDialog.question.options.map((option, idx) => (
                      <div key={idx} className={`p-3 rounded-lg border-2 ${
                        option.isCorrect ? 'bg-green-50 border-green-200' : 'bg-gray-50 border-gray-200'
                      }`}>
                        <div className="flex items-center space-x-3">
                          <Typography variant="small" className="font-medium">
                            {String.fromCharCode(65 + idx)}.
                          </Typography>
                          <Typography variant="paragraph" className="flex-1">
                            {option.optionText}
                          </Typography>
                          {option.isCorrect && (
                            <CheckCircleIcon className="h-5 w-5 text-green-600" />
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {previewDialog.question.questionType !== 'MULTIPLE_CHOICE' && previewDialog.question.correctAnswer && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Đáp án đúng:
                  </Typography>
                  <div className="bg-green-50 p-4 rounded-lg border-2 border-green-200">
                    <Typography variant="paragraph" color="green" className="font-medium">
                      {previewDialog.question.correctAnswer}
                    </Typography>
                  </div>
                </div>
              )}

              {previewDialog.question.explanation && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Giải thích:
                  </Typography>
                  <div className="bg-blue-50 p-4 rounded-lg border-2 border-blue-200">
                    <Typography variant="paragraph">
                      {previewDialog.question.explanation}
                    </Typography>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-200">
                <div>
                  <Typography variant="small" color="blue-gray" className="font-medium">
                    Điểm số:
                  </Typography>
                  <Typography variant="paragraph" color="blue">
                    {previewDialog.question.points || 10} điểm
                  </Typography>
                </div>
                <div>
                  <Typography variant="small" color="blue-gray" className="font-medium">
                    Ngày tạo:
                  </Typography>
                  <Typography variant="paragraph">
                    {previewDialog.question.createdAt ? 
                      new Date(previewDialog.question.createdAt).toLocaleDateString('vi-VN') : 
                      'Không xác định'
                    }
                  </Typography>
                </div>
              </div>
            </div>
          )}
        </DialogBody>
        <DialogFooter>
          <Button
            variant="outlined"
            onClick={() => setPreviewDialog({ open: false, question: null })}
          >
            Đóng
          </Button>
        </DialogFooter>
      </Dialog>
    </div>
  );
};

export default GrammarQuestionList;