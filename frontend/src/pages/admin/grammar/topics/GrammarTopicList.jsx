import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTopicList } from '../../../../hook/grammar/useGrammarTopics';
import { ADMIN_ROUTES } from '../../../../constants/routes';
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  Chip,
  Input,
  Select,
  Option,
} from '@material-tailwind/react';
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  BookOpenIcon,
  CheckCircleIcon,
  XCircleIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline';

// ✨ Import Reusable Components
import PageAdminHeader from '../../../../components/common/PageAdminHeader';
import ConfirmDialog from '../../../../components/common/ConfirmDialog';
import ResourceCard from '../../../../components/common/ResourceCard';
import PaginationControls from '../../../../components/common/PaginationControls';
import PageSizeSelector from '../../../../components/common/PageSizeSelector';

const GrammarTopicList = () => {
  const navigate = useNavigate();
  const {
    topics,
    loading,
    searchTerm,
    setSearchTerm,
    filterLevel,
    setFilterLevel,
    filterStatus,
    setFilterStatus,
    pagination,
    handlePageChange,
    handlePageSizeChange,
    deleteTopic,
    resetFilters,
  } = useTopicList();

  const [deleteDialog, setDeleteDialog] = useState({ open: false, topic: null });
  const [deleting, setDeleting] = useState(false);

  const levels = [
    { value: 'BEGINNER', label: 'Cơ bản', icon: '🟢' },
    { value: 'INTERMEDIATE', label: 'Trung bình', icon: '🟠' },
    { value: 'ADVANCED', label: 'Nâng cao', icon: '🔴' },
  ];

  const handleDelete = async () => {
    if (!deleteDialog.topic) return;
    setDeleting(true);
    try {
      await deleteTopic(deleteDialog.topic.id);
      setDeleteDialog({ open: false, topic: null });
    } finally {
      setDeleting(false);
    }
  };

  const getLevelColor = (level) => {
    switch (level) {
      case 'BEGINNER':
        return 'green';
      case 'INTERMEDIATE':
        return 'orange';
      case 'ADVANCED':
        return 'red';
      default:
        return 'gray';
    }
  };

  const getLevelLabel = (level) => {
    const found = levels.find((l) => l.value === level);
    return found ? found.label : level;
  };

  const getStatusColor = (isActive) => (isActive ? 'green' : 'gray');
  const hasActiveFilter = searchTerm || filterLevel || filterStatus;

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải danh sách chủ đề...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6 p-4 md:p-6">
      {/* Page Header */}
      <PageAdminHeader
        title="Quản lý Chủ đề Ngữ pháp"
        subtitle="Quản lý và tổ chức các chủ đề ngữ pháp tiếng Anh"
        icon={BookOpenIcon}
        iconBgColor="blue-500"
        iconColor="blue-400"
        actions={
          <Button
            size="lg"
            className="bg-blue-500 hover:bg-blue-600 shadow-lg hover:shadow-xl transition-all flex items-center gap-2 w-full lg:w-auto"
            onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_CREATE)}
          >
            <PlusIcon className="h-5 w-5" />
            <span className="font-semibold">Tạo mới</span>
          </Button>
        }
      />

      {/* Filter Section */}
      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="lg:col-span-2">
              <Input
                label="Tìm kiếm chủ đề"
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                color="blue"
                className="bg-secondary"
                containerProps={{ className: '!min-w-full' }}
              />
            </div>

            <div className="w-full">
              <Select
                label="Cấp độ"
                value={filterLevel}
                onChange={(val) => setFilterLevel(val)}
                color="blue"
                className="bg-secondary"
                containerProps={{ className: '!min-w-full' }}
                menuProps={{ className: 'bg-secondary border-primary' }}
              >
                <Option value="">Tất cả cấp độ</Option>
                {levels.map((level) => (
                  <Option key={level.value} value={level.value}>
                    <div className="flex items-center gap-2">
                      <span className="text-base">{level.icon}</span>
                      <span>{level.label}</span>
                    </div>
                  </Option>
                ))}
              </Select>
            </div>

            <div className="w-full">
              <Select
                label="Trạng thái"
                value={filterStatus}
                onChange={(val) => setFilterStatus(val)}
                color="blue"
                className="bg-secondary"
                containerProps={{ className: '!min-w-full' }}
                menuProps={{ className: 'bg-secondary border-primary' }}
              >
                <Option value="">Tất cả trạng thái</Option>
                <Option value="active">Hoạt động</Option>
                <Option value="inactive">Không hoạt động</Option>
              </Select>
            </div>
          </div>

          {/* ✅ Filter Summary with Page Size Selector */}
          <div className="mt-6 pt-4 border-t border-primary flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 flex-wrap">
              <PageSizeSelector
                pageSize={pagination.pageSize}
                onPageSizeChange={handlePageSizeChange}
                options={[6, 12, 24, 48]}
              />
              {hasActiveFilter && (
                <Chip
                  size="sm"
                  value="Đang lọc"
                  color="blue"
                  className="text-xs"
                  icon={<MagnifyingGlassIcon className="h-3 w-3" />}
                />
              )}
            </div>

            {hasActiveFilter && (
              <Button
                variant="outlined"
                size="sm"
                onClick={resetFilters}
                className="border-primary dark:text-slate-400 hover:bg-tertiary flex items-center gap-2 w-full sm:w-auto"
              >
                <ArrowPathIcon className="h-4 w-4" />
                Reset bộ lọc
              </Button>
            )}
          </div>
        </CardBody>
      </Card>

      {/* Topics Grid */}
      {topics.length === 0 ? (
        <Card className="card-base border-primary">
          <CardBody className="p-12 text-center">
            <div className="max-w-md mx-auto">
              <div className="w-20 h-20 bg-blue-50 dark:bg-blue-900/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <BookOpenIcon className="h-10 w-10 text-blue-500" />
              </div>
              <Typography variant="h5" className="text-primary mb-2 font-bold">
                {pagination.totalElements === 0 ? 'Chưa có chủ đề nào' : 'Không tìm thấy chủ đề'}
              </Typography>
              <Typography variant="small" className="text-secondary mb-6">
                {pagination.totalElements === 0
                  ? 'Hãy tạo chủ đề ngữ pháp đầu tiên để bắt đầu.'
                  : 'Thử thay đổi bộ lọc để tìm thấy chủ đề bạn cần.'}
              </Typography>
              {pagination.totalElements === 0 && (
                <Button
                  size="lg"
                  color="blue"
                  onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_CREATE)}
                  className="flex items-center gap-2 mx-auto shadow-lg"
                >
                  <PlusIcon className="h-5 w-5" />
                  Tạo chủ đề đầu tiên
                </Button>
              )}
            </div>
          </CardBody>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            {topics.map((topic) => (
              <ResourceCard
                key={topic.id}
                item={topic}
                title={topic.name}
                orderLabel={`#${topic.orderIndex}`}
                icon={BookOpenIcon}
                iconBgColor="blue-500"
                iconColor="blue-400"
                chips={[
                  {
                    label: getLevelLabel(topic.levelRequired),
                    color: getLevelColor(topic.levelRequired),
                  },
                  {
                    label: topic.isActive ? 'Hoạt động' : 'Tạm dừng',
                    color: getStatusColor(topic.isActive),
                    icon: topic.isActive ? (
                      <CheckCircleIcon className="h-3 w-3" />
                    ) : (
                      <XCircleIcon className="h-3 w-3" />
                    ),
                  },
                ]}
                actions={[
                  {
                    label: 'Bài học',
                    icon: EyeIcon,
                    className:
                      'border-green-500 text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20',
                    onClick: () => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id)),
                  },
                  {
                    label: 'Sửa',
                    icon: PencilIcon,
                    className:
                      'border-blue-500 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20',
                    onClick: () => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_EDIT(topic.id)),
                  },
                  {
                    type: 'icon',
                    icon: TrashIcon,
                    color: 'red',
                    className: 'hover:bg-red-50 dark:hover:bg-red-900/20 border-red-500',
                    onClick: () => setDeleteDialog({ open: true, topic }),
                  },
                ]}
                menuActions={{
                  view: {
                    label: 'Xem bài học',
                    onClick: () => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id)),
                  },
                  edit: {
                    label: 'Chỉnh sửa',
                    onClick: () => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_EDIT(topic.id)),
                  },
                  delete: {
                    label: 'Xóa chủ đề',
                    onClick: () => setDeleteDialog({ open: true, topic }),
                  },
                }}
                onTitleClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id))}
              />
            ))}
          </div>

          {/* ✅ Pagination Controls */}
          <Card className="card-base border-primary">
            <PaginationControls
              currentPage={pagination.currentPage}
              totalPages={pagination.totalPages}
              totalElements={pagination.totalElements}
              pageSize={pagination.pageSize}
              hasNext={pagination.hasNext}
              hasPrevious={pagination.hasPrevious}
              onPageChange={handlePageChange}
            />
          </Card>
        </>
      )}

      {/* Confirm Dialog */}
      <ConfirmDialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, topic: null })}
        onConfirm={handleDelete}
        title="Xác nhận xóa chủ đề"
        message="Bạn có chắc chắn muốn xóa chủ đề"
        itemName={deleteDialog.topic?.name}
        warningMessage="Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan bao gồm các bài học."
        confirmText="Xóa chủ đề"
        type="delete"
        loading={deleting}
      />
    </div>
  );
};

export default GrammarTopicList;