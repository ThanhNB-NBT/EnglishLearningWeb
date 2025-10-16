import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTopicList } from '../../../../hook/grammar/useGrammarTopics';
import { ADMIN_ROUTES } from '../../../../constants/routes';
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
} from '@material-tailwind/react';
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  EllipsisVerticalIcon,
  BookOpenIcon,
  AcademicCapIcon,
  CheckCircleIcon,
  XCircleIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline';

const GrammarTopicList = () => {
  const navigate = useNavigate();
  const {
    filteredTopics,
    topics,
    loading,
    searchTerm,
    setSearchTerm,
    filterLevel,
    setFilterLevel,
    filterStatus,
    setFilterStatus,
    deleteTopic,
    resetFilters
  } = useTopicList();

  const [deleteDialog, setDeleteDialog] = useState({ open: false, topic: null });

  const levels = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

  const handleDelete = async () => {
    if (!deleteDialog.topic) return;
    await deleteTopic(deleteDialog.topic.id);
    setDeleteDialog({ open: false, topic: null });
  };

  const getLevelColor = (level) => {
    switch (level) {
      case 'BEGINNER': return 'green';
      case 'INTERMEDIATE': return 'orange';
      case 'ADVANCED': return 'red';
      default: return 'gray';
    }
  };

  const getStatusColor = (isActive) => isActive ? 'green' : 'gray';

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" color="blue-gray">
          Đang tải danh sách chủ đề...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6">
      {/* Header Section */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center space-y-4 lg:space-y-0">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-blue-50 rounded-lg">
                <BookOpenIcon className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <Typography variant="h4" color="blue-gray" className="font-bold">
                  Quản lý Chủ đề Ngữ pháp
                </Typography>
                <Typography variant="small" color="blue-gray" className="opacity-70">
                  Quản lý và tổ chức các chủ đề ngữ pháp tiếng Anh
                </Typography>
              </div>
            </div>
            
            <Button
              size="lg"
              className="flex bg-gradient-to-r from-blue-500 to-blue-600 shadow-lg hover:shadow-xl transition-all duration-300"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_CREATE)}
            >
              <PlusIcon className="h-6 w-6 mr-2" />
              <Typography variant="small" className="font-bold">
                Tạo chủ đề mới
              </Typography>
            </Button>
          </div>
        </CardBody>
      </Card>

      {/* Filter Section */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Tìm kiếm
              </Typography>
              <Input
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                placeholder="Tìm theo tên hoặc mô tả"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-blue-500"
                labelProps={{ className: 'hidden' }}
              />
            </div>
            
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Cấp độ
              </Typography>
              <Select
                value={filterLevel}
                onChange={val => setFilterLevel(val)}
                className="!border-blue-gray-200 focus:!border-blue-500"
              >
                <Option value="">Tất cả cấp độ</Option>
                {levels.map(level => (
                  <Option key={level} value={level}>
                    {level}
                  </Option>
                ))}
              </Select>
            </div>

            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Trạng thái
              </Typography>
              <Select
                value={filterStatus}
                onChange={val => setFilterStatus(val)}
                className="!border-blue-gray-200 focus:!border-blue-500"
              >
                <Option value="">Tất cả trạng thái</Option>
                <Option value="active">Hoạt động</Option>
                <Option value="inactive">Không hoạt động</Option>
              </Select>
            </div>

            <div className="flex">
              <Button
                variant="outlined"
                size="sm"
                onClick={resetFilters}
                className="border-gray-300 text-gray-800"
              >
                <Typography variant="small" className="flex items-center hover:text-blue-500">
                  <ArrowPathIcon className="h-5 w-5 mr-2" />
                  Reset
                </Typography>
              </Button>
            </div>
          </div>

          {/* Filter Summary */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Typography variant="small" color="blue-gray" className="opacity-70">
                Hiển thị {filteredTopics.length} / {topics.length} chủ đề
              </Typography>
              {(searchTerm || filterLevel || filterStatus) && (
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

      {/* Topics Grid */}
      {filteredTopics.length === 0 ? (
        <Card className="border border-blue-gray-100">
          <CardBody className="p-12 text-center">
            <BookOpenIcon className="h-16 w-16 text-blue-gray-300 mx-auto mb-4" />
            <Typography variant="h6" color="blue-gray" className="mb-2">
              {topics.length === 0 ? 'Chưa có chủ đề nào' : 'Không tìm thấy chủ đề'}
            </Typography>
            <Typography variant="small" color="blue-gray" className="opacity-70 mb-4">
              {topics.length === 0 
                ? 'Hãy tạo chủ đề ngữ pháp đầu tiên để bắt đầu.'
                : 'Thử thay đổi bộ lọc để tìm thấy chủ đề bạn cần.'
              }
            </Typography>
            {topics.length === 0 && (
              <Button
                color="blue"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_CREATE)}
              >
                <PlusIcon className="h-4 w-4 mr-2" />
                Tạo chủ đề đầu tiên
              </Button>
            )}
          </CardBody>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredTopics.map((topic) => (
            <Card key={topic.id} className="border border-blue-gray-100 hover:shadow-lg transition-shadow duration-300">
              <CardHeader floated={false} shadow={false} className="m-0 rounded-none border-b border-blue-gray-100">
                <div className="p-4">
                  <div className="flex justify-between items-start mb-3">
                    <div className="flex-1">
                      <Typography 
                        variant="h6" 
                        color="blue-gray" 
                        className="mb-1 line-clamp-2 cursor-pointer hover:text-blue-600 transition-colors duration-200"
                        onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id))}
                      >
                        {topic.name}
                      </Typography>
                      <div className="flex items-center space-x-2">
                        <Chip
                          size="sm"
                          value={topic.levelRequired}
                          color={getLevelColor(topic.levelRequired)}
                          className="text-xs"
                        />
                        <Chip
                          size="sm"
                          value={topic.isActive ? 'Hoạt động' : 'Tạm dừng'}
                          color={getStatusColor(topic.isActive)}
                          icon={topic.isActive ? 
                            <CheckCircleIcon className="h-3 w-3" /> : 
                            <XCircleIcon className="h-3 w-3" />
                          }
                          className="text-xs"
                        />
                      </div>
                    </div>
                    
                    <Menu>
                      <MenuHandler>
                        <IconButton variant="text" size="sm">
                          <EllipsisVerticalIcon className="h-4 w-4" />
                        </IconButton>
                      </MenuHandler>
                      <MenuList>
                        <MenuItem 
                          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id))}
                          className="flex items-center"
                        >
                          <EyeIcon className="h-4 w-4 mr-2" />
                          <Typography variant="small" className="font-normal">
                            Xem bài học
                          </Typography>
                        </MenuItem>
                        <MenuItem 
                          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_EDIT(topic.id))}
                          className="flex items-center"
                        >
                          <PencilIcon className="h-4 w-4 mr-2" />
                          <Typography variant="small" className="font-normal">
                            Chỉnh sửa
                          </Typography>
                        </MenuItem>
                        <MenuItem 
                          onClick={() => setDeleteDialog({ open: true, topic })}
                          className="flex items-center text-red-500"
                        >
                          <TrashIcon className="h-4 w-4 mr-2" />
                          Xóa
                        </MenuItem>
                      </MenuList>
                    </Menu>
                  </div>
                </div>
              </CardHeader>

              <CardBody className="p-4">
                <Typography variant="small" color="blue-gray" className="opacity-70 mb-4 line-clamp-3">
                  {topic.description || 'Chưa có mô tả'}
                </Typography>

                <div className="flex items-center justify-between text-sm text-blue-gray-500 mb-4">
                  <div className="flex items-center space-x-4">
                    <span>Thứ tự: {topic.orderIndex}</span>
                    <span className="flex items-center">
                      <AcademicCapIcon className="h-4 w-4 mr-1" />
                      {topic.levelRequired}
                    </span>
                  </div>
                </div>

                <div className="flex space-x-2">
                  <IconButton
                    size="sm"
                    variant="outlined"
                    className="border-green-500 text-green-500 hover:bg-green-50"
                    onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topic.id))}
                  >
                    <EyeIcon className="h-4 w-4" />
                  </IconButton>
                  <IconButton
                    size="sm"
                    variant="outlined"
                    className="border-blue-500 text-blue-500 hover:bg-blue-50"
                    onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPIC_EDIT(topic.id))}
                  >
                    <PencilIcon className="h-4 w-4" />
                  </IconButton>
                  <IconButton
                    size="sm"
                    variant="outlined"
                    color="red"
                    onClick={() => setDeleteDialog({ open: true, topic })}
                  >
                    <TrashIcon className="h-4 w-4" />
                  </IconButton>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog 
        open={deleteDialog.open} 
        handler={() => setDeleteDialog({ open: false, topic: null })}
        size="sm"
      >
        <DialogHeader className="flex items-center space-x-2">
          <TrashIcon className="h-6 w-6 text-red-500" />
          <span>Xác nhận xóa chủ đề</span>
        </DialogHeader>
        <DialogBody>
          <Typography variant="paragraph" color="blue-gray">
            Bạn có chắc chắn muốn xóa chủ đề <strong>"{deleteDialog.topic?.name}"</strong> không?
          </Typography>
          <Typography variant="small" color="red" className="mt-2">
            Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan.
          </Typography>
        </DialogBody>
        <DialogFooter className="space-x-2">
          <Button
            variant="outlined"
            onClick={() => setDeleteDialog({ open: false, topic: null })}
          >
            Hủy
          </Button>
          <Button
            color="red"
            onClick={handleDelete}
          >
            Xóa chủ đề
          </Button>
        </DialogFooter>
      </Dialog>
    </div>
  );
};

export default GrammarTopicList;