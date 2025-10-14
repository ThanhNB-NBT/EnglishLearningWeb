import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { grammarAdminAPI } from '../../../api';
import { ADMIN_ROUTES } from '../../../constants/routes';
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  IconButton,
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Input,
  Select,
  Option,
  Breadcrumbs,
  Chip,
} from '@material-tailwind/react';
import {
  PlusIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  QuestionMarkCircleIcon,
  ArrowPathIcon,
  ArrowLeftIcon,
} from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';
import QuestionTable from '../../../components/grammar/QuestionTable';

const GrammarQuestionList = () => {
  const { lessonId } = useParams();
  const navigate = useNavigate();

  const [questions, setQuestions] = useState([]);
  const [filteredQuestions, setFilteredQuestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, question: null });
  
  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');

  const questionTypes = [
    { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
    { value: 'FILL_BLANK', label: 'Điền từ' },
    { value: 'TRANSLATE', label: 'Dịch câu' },
    { value: 'VERB_FORM', label: 'Chia động từ' },
  ];

  useEffect(() => {
    if (lessonId) {
      loadQuestions();
    }
  }, [lessonId]);

  useEffect(() => {
    filterQuestions();
  }, [questions, searchTerm, filterType]);

  // ===== DATA LOADING =====

  const loadQuestions = async () => {
    setLoading(true);
    try {
      // ✅ API này đã filter theo ParentType.GRAMMAR trong backend
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      setQuestions(response.data.data || []);
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách câu hỏi');
      console.error('Load questions error:', error);
    } finally {
      setLoading(false);
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

    // Sort by orderIndex
    filtered = filtered.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));

    setFilteredQuestions(filtered);
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
  };

  // ===== CRUD OPERATIONS =====

  const handleEdit = (question) => {
    navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_EDIT(lessonId, question.id));
  };

  const handleDelete = async () => {
    if (!deleteDialog.question) return;

    try {
      await grammarAdminAPI.deleteQuestion(deleteDialog.question.id);
      setQuestions(questions.filter(q => q.id !== deleteDialog.question.id));
      toast.success('Xóa câu hỏi thành công!');
    } catch (error) {
      toast.error('Lỗi khi xóa câu hỏi: ' + (error.response?.data?.message || error.message));
      console.error('Delete question error:', error);
    } finally {
      setDeleteDialog({ open: false, question: null });
    }
  };

  // ✅ FIX: Loại bỏ unused handlePreview - preview được handle trong QuestionTable

  // ===== RENDER =====

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-purple-500" />
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

      {/* Header */}
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
                  Quản lý Câu hỏi
                </Typography>
                <Typography variant="small" color="blue-gray" className="opacity-70">
                  Bài học thực hành
                </Typography>
              </div>
            </div>

            <Button
              size="lg"
              className="flex bg-gradient-to-r from-purple-500 to-purple-600 shadow-lg hover:shadow-xl transition-all duration-300"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_CREATE(lessonId))}
            >
              <PlusIcon className="h-6 w-6 mr-2" />
              <span className="font-bold">Tạo câu hỏi mới</span>
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
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-purple-500"
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
                className="!border-blue-gray-200 focus:!border-purple-500"
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
                <span className="flex items-center hover:text-purple-500">
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
                  color="purple"
                  className="text-xs"
                />
              )}
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Questions Table */}
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
        <QuestionTable
          questions={filteredQuestions}
          onEdit={handleEdit}
          onDelete={(question) => setDeleteDialog({ open: true, question })}
        />
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
          <div className="mt-3 p-3 bg-gray-50 rounded-lg">
            <Typography variant="small" color="blue-gray" className="line-clamp-2">
              {deleteDialog.question?.questionText}
            </Typography>
          </div>
          <Typography variant="small" color="red" className="mt-3">
            ⚠️ Hành động này không thể hoàn tác.
          </Typography>
        </DialogBody>
        <DialogFooter className="space-x-2">
          <Button
            variant="outlined"
            onClick={() => setDeleteDialog({ open: false, question: null })}
          >
            Hủy
          </Button>
          <Button color="red" onClick={handleDelete}>
            Xóa câu hỏi
          </Button>
        </DialogFooter>
      </Dialog>
    </div>
  );
};

export default GrammarQuestionList;