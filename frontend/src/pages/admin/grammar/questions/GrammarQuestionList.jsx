import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ADMIN_ROUTES } from '../../../../constants/routes';
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  Input,
  Select,
  Option,
  Chip,
} from '@material-tailwind/react';
import {
  PlusIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  QuestionMarkCircleIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline';

// ✨ Import Reusable Components & Hook
import PageAdminHeader from '../../../../components/common/PageAdminHeader';
import ConfirmDialog from '../../../../components/common/ConfirmDialog';
import QuestionTable from '../../../../components/grammar/tables/QuestionTable';
import { useQuestionList } from '../../../../hook/grammar/useGrammarQuestions';

const GrammarQuestionList = () => {
  const { lessonId } = useParams();
  const navigate = useNavigate();

  // ✨ USE HOOK
  const {
    filteredQuestions,
    questions,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    selectedQuestions,
    setSelectedQuestions,
    deleteQuestion,
    bulkDeleteQuestions,
    resetFilters,
  } = useQuestionList(lessonId);

  const [deleteDialog, setDeleteDialog] = useState({ open: false, question: null });
  const [bulkDeleteDialog, setBulkDeleteDialog] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const questionTypes = [
    { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
    { value: 'FILL_BLANK', label: 'Điền từ' },
    { value: 'TRANSLATE', label: 'Dịch câu' },
  ];

  // ===== SELECTION HANDLERS =====

  const handleSelectAll = (checked) => {
    if (checked) {
      setSelectedQuestions(filteredQuestions.map(q => q.id));
    } else {
      setSelectedQuestions([]);
    }
  };

  const handleSelectOne = (questionId, checked) => {
    if (checked) {
      setSelectedQuestions([...selectedQuestions, questionId]);
    } else {
      setSelectedQuestions(selectedQuestions.filter(id => id !== questionId));
    }
  };

  const isAllSelected = filteredQuestions.length > 0 && 
    selectedQuestions.length === filteredQuestions.length;

  const isSomeSelected = selectedQuestions.length > 0 && 
    selectedQuestions.length < filteredQuestions.length;

  // ===== CRUD OPERATIONS =====

  const handleEdit = (question) => {
    navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_EDIT(lessonId, question.id));
  };

  const handleDelete = async () => {
    if (!deleteDialog.question) return;

    setDeleting(true);
    try {
      await deleteQuestion(deleteDialog.question.id);
      setDeleteDialog({ open: false, question: null });
    } finally {
      setDeleting(false);
    }
  };

  const handleBulkDelete = async () => {
    if (selectedQuestions.length === 0) return;

    setDeleting(true);
    try {
      await bulkDeleteQuestions(selectedQuestions);
      setBulkDeleteDialog(false);
    } finally {
      setDeleting(false);
    }
  };

  const hasActiveFilter = searchTerm || filterType;

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-purple-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải danh sách câu hỏi...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6 p-4 md:p-6">
      {/* ✨ REUSABLE PAGE HEADER */}
      <PageAdminHeader
        title="Quản lý Câu hỏi"
        subtitle="Bài học thực hành"
        icon={QuestionMarkCircleIcon}
        iconBgColor="purple-500"
        iconColor="purple-400"
        showBackButton={true}
        onBack={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        actions={
          <Button
            size="lg"
            className="bg-purple-500 hover:bg-purple-600 shadow-lg hover:shadow-xl transition-all flex items-center gap-2 w-full lg:w-auto"
            onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_CREATE(lessonId))}
          >
            <PlusIcon className="h-5 w-5" />
            <span className="font-semibold">Tạo mới</span>
          </Button>
        }
      />

      {/* Filter Section */}
      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="lg:col-span-2">
              <Input
                label="Tìm kiếm câu hỏi"
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                color="purple"
                className="bg-secondary"
                containerProps={{ className: "!min-w-full" }}
              />
            </div>

            <div className="w-full">
              <Select
                label="Loại câu hỏi"
                value={filterType}
                onChange={val => setFilterType(val)}
                color="purple"
                className="bg-secondary"
                containerProps={{ className: "!min-w-full" }}
                menuProps={{ className: "bg-secondary border-primary" }}
              >
                <Option value="">Tất cả loại</Option>
                {questionTypes.map(type => (
                  <Option key={type.value} value={type.value}>
                    {type.label}
                  </Option>
                ))}
              </Select>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-primary flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 flex-wrap">
              <Typography variant="small" className="text-primary font-semibold">
                Hiển thị <span className="text-purple-500">{filteredQuestions.length}</span> / {questions.length} câu hỏi
              </Typography>
              {hasActiveFilter && (
                <Chip
                  size="sm"
                  value="Đang lọc"
                  color="purple"
                  className="text-xs"
                  icon={<MagnifyingGlassIcon className="h-3 w-3" />}
                />
              )}
              {selectedQuestions.length > 0 && (
                <Chip
                  size="sm"
                  value={`${selectedQuestions.length} đã chọn`}
                  color="orange"
                  className="text-xs font-semibold"
                />
              )}
            </div>

            <div className="flex gap-2 w-full sm:w-auto">
              {selectedQuestions.length > 0 && (
                <Button
                  size="sm"
                  color="red"
                  variant="gradient"
                  onClick={() => setBulkDeleteDialog(true)}
                  className="flex items-center gap-2"
                >
                  <TrashIcon className="h-4 w-4" />
                  Xóa {selectedQuestions.length} câu
                </Button>
              )}
              {hasActiveFilter && (
                <Button
                  variant="outlined"
                  size="sm"
                  onClick={resetFilters}
                  className="border-primary hover:bg-tertiary flex items-center gap-2"
                >
                  <ArrowPathIcon className="h-4 w-4" />
                  Reset
                </Button>
              )}
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Questions Table */}
      {filteredQuestions.length === 0 ? (
        <Card className="card-base border-primary">
          <CardBody className="p-12 text-center">
            <div className="max-w-md mx-auto">
              <div className="w-20 h-20 bg-purple-50 dark:bg-purple-900/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <QuestionMarkCircleIcon className="h-10 w-10 text-purple-500" />
              </div>
              <Typography variant="h5" className="text-primary mb-2 font-bold">
                {questions.length === 0 ? 'Chưa có câu hỏi nào' : 'Không tìm thấy câu hỏi'}
              </Typography>
              <Typography variant="small" className="text-secondary mb-6">
                {questions.length === 0
                  ? 'Hãy tạo câu hỏi đầu tiên cho bài học này.'
                  : 'Thử thay đổi bộ lọc để tìm thấy câu hỏi bạn cần.'
                }
              </Typography>
              {questions.length === 0 && (
                <Button
                  size="lg"
                  color="purple"
                  onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTION_CREATE(lessonId))}
                  className="flex items-center gap-2 mx-auto shadow-lg"
                >
                  <PlusIcon className="h-5 w-5" />
                  Tạo câu hỏi đầu tiên
                </Button>
              )}
            </div>
          </CardBody>
        </Card>
      ) : (
        <QuestionTable
          questions={filteredQuestions}
          selectedQuestions={selectedQuestions}
          onSelectAll={handleSelectAll}
          onSelectOne={handleSelectOne}
          isAllSelected={isAllSelected}
          isSomeSelected={isSomeSelected}
          onEdit={handleEdit}
          onDelete={(question) => setDeleteDialog({ open: true, question })}
        />
      )}

      {/* Single Delete Dialog */}
      <ConfirmDialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, question: null })}
        onConfirm={handleDelete}
        title="Xác nhận xóa câu hỏi"
        message="Bạn có chắc chắn muốn xóa câu hỏi này không?"
        warningMessage="⚠️ Hành động này không thể hoàn tác."
        confirmText="Xóa câu hỏi"
        type="delete"
        loading={deleting}
      />

      {/* Bulk Delete Dialog */}
      <ConfirmDialog
        open={bulkDeleteDialog}
        onClose={() => setBulkDeleteDialog(false)}
        onConfirm={handleBulkDelete}
        title="Xác nhận xóa nhiều câu hỏi"
        message={`Bạn có chắc chắn muốn xóa ${selectedQuestions.length} câu hỏi đã chọn không?`}
        warningMessage="⚠️ Hành động này không thể hoàn tác và sẽ xóa tất cả câu hỏi đã chọn."
        confirmText={`Xóa ${selectedQuestions.length} câu hỏi`}
        type="delete"
        loading={deleting}
      />
    </div>
  );
};

export default GrammarQuestionList;