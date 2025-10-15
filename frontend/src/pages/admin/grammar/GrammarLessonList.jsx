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
  ArrowLeftIcon,
  DocumentTextIcon,
  PlayIcon,
} from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';

const GrammarLessonList = () => {
  const { topicId } = useParams();
  const [lessons, setLessons] = useState([]);
  const [filteredLessons, setFilteredLessons] = useState([]);
  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, lesson: null });
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (topicId) {
      loadLessons();
      loadTopicInfo();
    }
  }, [topicId]);

  useEffect(() => {
    filterLessons();
  }, [lessons, searchTerm, filterType, filterStatus]);

  const loadLessons = async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId);
      setLessons(response.data.data || []);
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách bài học');
      console.error('Load lessons error:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadTopicInfo = async () => {
    try {
      const response = await grammarAdminAPI.getAllTopics();
      const topic = response.data.data.find(t => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error('Load topic info error:', error);
    }
  };

  const filterLessons = () => {
    let filtered = lessons;

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(lesson =>
        lesson.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        lesson.content?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Type filter
    if (filterType) {
      filtered = filtered.filter(lesson => lesson.lessonType === filterType);
    }

    // Status filter
    if (filterStatus) {
      const isActive = filterStatus === 'active';
      filtered = filtered.filter(lesson => lesson.isActive === isActive);
    }

    // Sort by orderIndex
    filtered = filtered.sort((a, b) => a.orderIndex - b.orderIndex);

    setFilteredLessons(filtered);
  };

  const handleDelete = async () => {
    if (!deleteDialog.lesson) return;
    
    try {
      await grammarAdminAPI.deleteLesson(deleteDialog.lesson.id);
      setLessons(lessons.filter(lesson => lesson.id !== deleteDialog.lesson.id));
      toast.success('Xóa bài học thành công!');
    } catch (error) {
      toast.error((error.response?.data?.message || error.message));
      console.error('Delete lesson error:', error);
    } finally {
      setDeleteDialog({ open: false, lesson: null });
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
    setFilterStatus('');
  };

  // 🆕 Helper function: Lấy preview text từ HTML content
  const getContentPreview = (htmlContent, maxLength = 120) => {
    if (!htmlContent) return 'Chưa có nội dung';
    
    // Strip HTML tags
    const text = htmlContent.replace(/<[^>]+>/g, '');
    // Remove extra whitespace
    const cleanText = text.replace(/\s+/g, ' ').trim();
    
    if (cleanText.length <= maxLength) return cleanText;
    return cleanText.substring(0, maxLength) + '...';
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'THEORY': return 'blue';
      case 'PRACTICE': return 'green';
      default: return 'gray';
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'THEORY': return DocumentTextIcon;
      case 'PRACTICE': return PlayIcon;
      default: return BookOpenIcon;
    }
  };

  const getStatusColor = (isActive) => isActive ? 'green' : 'gray';

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" color="blue-gray">
          Đang tải danh sách bài học...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6">
      {/* Breadcrumbs */}
      <Breadcrumbs className="bg-transparent p-0">
        <Typography 
          className="opacity-60 cursor-pointer hover:opacity-100 transition-opacity"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        >
          Chủ đề ngữ pháp
        </Typography>
        <Typography color="blue-gray">{topicInfo?.name || 'Bài học'}</Typography>
      </Breadcrumbs>

      {/* Header Section */}
      <Card className="border border-blue-gray-100 shadow-sm">
        <CardBody className="p-6">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center space-y-4 lg:space-y-0">
            <div className="flex items-center space-x-3">
              <IconButton
                variant="outlined"
                size="sm"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
                className="border-gray-300 hover:bg-gray-50"
              >
                <ArrowLeftIcon className="h-4 w-4" />
              </IconButton>
              <div className="p-2 bg-blue-50 rounded-lg">
                <BookOpenIcon className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <Typography variant="h4" color="blue-gray" className="font-bold">
                  Bài học - {topicInfo?.name}
                </Typography>
                <Typography variant="small" color="blue-gray" className="opacity-70">
                  Quản lý các bài học trong chủ đề ngữ pháp
                </Typography>
              </div>
            </div>
            
            <Button
              size="lg"
              className="flex bg-gradient-to-r from-green-500 to-green-600 shadow-lg hover:shadow-xl transition-all duration-300"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))}
            >
              <PlusIcon className="h-6 w-6 mr-2" />
              <Typography variant="small" className="font-bold">
                Tạo bài học mới
              </Typography>
            </Button>
          </div>
        </CardBody>
      </Card>

      {/* Filter Section */}
      <Card className="border border-blue-gray-100 shadow-sm">
        <CardBody className="p-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Tìm kiếm
              </Typography>
              <Input
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                placeholder="Tìm theo tiêu đề hoặc nội dung"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-blue-500"
                labelProps={{ className: 'hidden' }}
              />
            </div>
            
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Loại bài học
              </Typography>
              <Select
                value={filterType}
                onChange={val => setFilterType(val)}
                className="!border-blue-gray-200 focus:!border-blue-500"
              >
                <Option value="">Tất cả loại</Option>
                <Option value="THEORY">Lý thuyết</Option>
                <Option value="PRACTICE">Thực hành</Option>
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
                className="border-gray-300 text-gray-800 hover:bg-gray-50"
              >
                <Typography variant="small" className="flex items-center">
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
                Hiển thị {filteredLessons.length} / {lessons.length} bài học
              </Typography>
              {(searchTerm || filterType || filterStatus) && (
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

      {/* Lessons Grid */}
      {filteredLessons.length === 0 ? (
        <Card className="border border-blue-gray-100 shadow-sm">
          <CardBody className="p-12 text-center">
            <BookOpenIcon className="h-16 w-16 text-blue-gray-300 mx-auto mb-4" />
            <Typography variant="h6" color="blue-gray" className="mb-2">
              {lessons.length === 0 ? 'Chưa có bài học nào' : 'Không tìm thấy bài học'}
            </Typography>
            <Typography variant="small" color="blue-gray" className="opacity-70 mb-4">
              {lessons.length === 0 
                ? 'Hãy tạo bài học đầu tiên cho chủ đề này.'
                : 'Thử thay đổi bộ lọc để tìm thấy bài học bạn cần.'
              }
            </Typography>
            {lessons.length === 0 && (
              <Button
                color="green"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))}
              >
                <PlusIcon className="h-4 w-4 mr-2" />
                Tạo bài học đầu tiên
              </Button>
            )}
          </CardBody>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredLessons.map((lesson) => {
            const TypeIcon = getTypeIcon(lesson.lessonType);
            return (
              <Card key={lesson.id} className="border border-blue-gray-100 hover:shadow-lg transition-shadow duration-300">
                <CardHeader floated={false} shadow={false} className="m-0 rounded-none border-b border-blue-gray-100">
                  <div className="p-4">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-2">
                          <Typography variant="small" color="blue-gray" className="font-medium opacity-70">
                            Bài {lesson.orderIndex}
                          </Typography>
                          <TypeIcon className="h-4 w-4 text-blue-gray-400" />
                        </div>
                        <Typography 
                          variant="h6" 
                          color="blue-gray" 
                          className="mb-2 line-clamp-2 cursor-pointer hover:text-blue-600 transition-colors duration-200"
                          onClick={() => {
                            if (lesson.lessonType === 'PRACTICE') {
                              navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id));
                            } else {
                              navigate(ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id));
                            }
                          }}
                        >
                          {lesson.title}
                        </Typography>
                        <div className="flex items-center space-x-2">
                          <Chip
                            size="sm"
                            value={lesson.lessonType === 'THEORY' ? 'Lý thuyết' : 'Thực hành'}
                            color={getTypeColor(lesson.lessonType)}
                            className="text-xs"
                          />
                          <Chip
                            size="sm"
                            value={lesson.isActive ? 'Hoạt động' : 'Tạm dừng'}
                            color={getStatusColor(lesson.isActive)}
                            icon={lesson.isActive ? 
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
                          {lesson.lessonType === 'PRACTICE' && (
                            <MenuItem 
                              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id))}
                              className="flex items-center"
                            >
                              <EyeIcon className="h-4 w-4 mr-2" />
                              <Typography variant="small" className="font-normal">
                                Xem câu hỏi
                              </Typography>
                            </MenuItem>
                          )}
                          <MenuItem 
                            onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id))}
                            className="flex items-center"
                          >
                            <PencilIcon className="h-4 w-4 mr-2" />
                            <Typography variant="small" className="font-normal">
                              Chỉnh sửa
                            </Typography>
                          </MenuItem>
                          <MenuItem 
                            onClick={() => setDeleteDialog({ open: true, lesson })}
                            className="flex items-center text-red-500 hover:bg-red-50"
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
                  {/* 🔧 FIX: Hiển thị preview text thay vì raw HTML */}
                  <Typography 
                    variant="small" 
                    color="blue-gray" 
                    className="opacity-70 mb-4 line-clamp-2"
                  >
                    {lesson.lessonType === 'THEORY' 
                      ? getContentPreview(lesson.content, 100)
                      : `Bài thực hành với ${lesson.questionCount || 0} câu hỏi`
                    }
                  </Typography>

                  <div className="flex items-center justify-between text-sm text-blue-gray-500 mb-4">
                    <div className="flex items-center space-x-4">
                      <span className="text-xs">⏱️ {lesson.estimatedDuration}s</span>
                      <span className="text-xs">🎁 {lesson.pointsReward} điểm</span>
                    </div>
                  </div>

                  <div className="flex space-x-2">
                    {lesson.lessonType === 'PRACTICE' && (
                      <Button
                        size="sm"
                        variant="outlined"
                        className="flex-1 border-green-500 text-green-500 hover:bg-green-50"
                        onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id))}
                      >
                        <EyeIcon className="h-4 w-4 mr-1" />
                        Câu hỏi
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outlined"
                      className="flex-1 border-blue-500 text-blue-500 hover:bg-blue-50"
                      onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id))}
                    >
                      <PencilIcon className="h-4 w-4 mr-1" />
                      Sửa
                    </Button>
                    <IconButton
                      size="sm"
                      variant="outlined"
                      color="red"
                      className="hover:bg-red-50"
                      onClick={() => setDeleteDialog({ open: true, lesson })}
                    >
                      <TrashIcon className="h-4 w-4" />
                    </IconButton>
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
        handler={() => setDeleteDialog({ open: false, lesson: null })}
        size="sm"
      >
        <DialogHeader className="flex items-center space-x-2">
          <TrashIcon className="h-6 w-6 text-red-500" />
          <span>Xác nhận xóa bài học</span>
        </DialogHeader>
        <DialogBody>
          <Typography variant="paragraph" color="blue-gray">
            Bạn có chắc chắn muốn xóa bài học <strong>"{deleteDialog.lesson?.title}"</strong> không?
          </Typography>
          <Typography variant="small" color="red" className="mt-2">
            ⚠️ Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan bao gồm các câu hỏi.
          </Typography>
        </DialogBody>
        <DialogFooter className="space-x-2">
          <Button
            variant="outlined"
            onClick={() => setDeleteDialog({ open: false, lesson: null })}
            className="hover:bg-gray-50"
          >
            Hủy
          </Button>
          <Button
            color="red"
            onClick={handleDelete}
            className="hover:shadow-lg transition-shadow"
          >
            <TrashIcon className="h-4 w-4 mr-2" />
            Xóa bài học
          </Button>
        </DialogFooter>
      </Dialog>
    </div>
  );
};

export default GrammarLessonList;