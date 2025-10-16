import React, { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useLessonList } from "../../../../hook/grammar/useGrammarLessons";
import { ADMIN_ROUTES } from "../../../../constants/routes";
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
} from "@material-tailwind/react";
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  EllipsisVerticalIcon,
  BookOpenIcon,
  CheckCircleIcon,
  XCircleIcon,
  ArrowPathIcon,
  ArrowLeftIcon,
  DocumentTextIcon,
  PlayIcon,
} from "@heroicons/react/24/outline";

const GrammarLessonList = () => {
  const { topicId } = useParams();
  const navigate = useNavigate();

  const {
    filteredLessons,
    lessons,
    topicInfo,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    filterStatus,
    setFilterStatus,
    deleteLesson,
    resetFilters,
  } = useLessonList(topicId);

  const [deleteDialog, setDeleteDialog] = useState({
    open: false,
    lesson: null,
  });

  const handleDelete = async () => {
    if (!deleteDialog.lesson) return;
    await deleteLesson(deleteDialog.lesson.id);
    setDeleteDialog({ open: false, lesson: null });
  };

  const getContentPreview = (htmlContent, maxLength = 120) => {
    if (!htmlContent) return "Chưa có nội dung";
    const text = htmlContent.replace(/<[^>]+>/g, "");
    const cleanText = text.replace(/\s+/g, " ").trim();
    if (cleanText.length <= maxLength) return cleanText;
    return cleanText.substring(0, maxLength) + "...";
  };

  const getTypeColor = (type) => {
    switch (type) {
      case "THEORY":
        return "blue";
      case "PRACTICE":
        return "green";
      default:
        return "gray";
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case "THEORY":
        return DocumentTextIcon;
      case "PRACTICE":
        return PlayIcon;
      default:
        return BookOpenIcon;
    }
  };

  const getStatusColor = (isActive) => (isActive ? "green" : "gray");

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải danh sách bài học...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6">
      {/* Header Section */}
      <Card className="card-base border-primary">
        <CardBody className="p-3">
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center space-y-4 lg:space-y-0">
            <div className="flex items-center space-x-3">
              <IconButton
                variant="outlined"
                size="sm"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
                className="border-primary hover:bg-tertiary text-primary hidden md:block"
              >
                <ArrowLeftIcon className="h-4 w-4" />
              </IconButton>
              <div className="p-2 bg-blue-50 dark:bg-blue-900/30 rounded-lg">
                <BookOpenIcon className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <Typography variant="h4" className="text-primary font-bold">
                  Bài học - {topicInfo?.name}
                </Typography>
                <Typography variant="small" className="text-secondary">
                  Quản lý các bài học trong chủ đề ngữ pháp
                </Typography>
              </div>
            </div>

            <Button
              size="md"
              className="flex items-center dark:bg-blue-600 gap-2 shadow-lg hover:shadow-xl ml-auto"
              onClick={() =>
                navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
              }
            >
              <PlusIcon className="h-5 w-5" />
              <Typography variant="small" className="font-bold">
                Tạo mới
              </Typography>
            </Button>
          </div>
        </CardBody>
      </Card>

      {/* Filter Section */}
      <Card className="card-base border-primary">
        <CardBody className="p-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
            <div>
              <Input
                label="Tìm kiếm"
                icon={
                  <MagnifyingGlassIcon className="h-5 w-5 dark:text-gray-400" />
                }
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                color="blue"
              />
            </div>

            <div>
              <Select
                label="Loại bài học"
                value={filterType}
                onChange={(val) => setFilterType(val)}
                color="blue"
                animate={{
                  mount: { y: 0 },
                  unmount: { y: 25 },
                }}
                className="dark:text-gray-500"
              >
                <Option value="">Tất cả</Option>
                <Option value="THEORY">Lý thuyết</Option>
                <Option value="PRACTICE">Thực hành</Option>
              </Select>
            </div>

            <div>
              <Select
                value={filterStatus}
                onChange={(val) => setFilterStatus(val)}
                label="Trạng thái"
                color="blue"
                animate={{
                  mount: { y: 0 },
                  unmount: { y: 25 },
                }}
                className="dark:text-gray-500"
              >
                <Option value="">Tất cả</Option>
                <Option value="active">Hoạt động</Option>
                <Option value="inactive">Không hoạt động</Option>
              </Select>
            </div>

            <div className="flex">
              <Button
                variant="outlined"
                size="sm"
                onClick={resetFilters}
                className="border-primary hover:bg-tertiary ml-auto"
              >
                <Typography
                  variant="small"
                  className="flex items-center text-primary"
                >
                  <ArrowPathIcon className="h-5 w-5 mr-2" />
                  Reset
                </Typography>
              </Button>
            </div>
          </div>

          {/* Filter Summary */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Typography variant="small" className="text-secondary">
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
        <Card className="card-base border-primary">
          <CardBody className="p-12 text-center">
            <BookOpenIcon className="h-16 w-16 text-tertiary mx-auto mb-4" />
            <Typography variant="h6" className="text-primary mb-2">
              {lessons.length === 0
                ? "Chưa có bài học nào"
                : "Không tìm thấy bài học"}
            </Typography>
            <Typography variant="small" className="text-secondary mb-4">
              {lessons.length === 0
                ? "Hãy tạo bài học đầu tiên cho chủ đề này."
                : "Thử thay đổi bộ lọc để tìm thấy bài học bạn cần."}
            </Typography>
            {lessons.length === 0 && (
              <Button
                color="green"
                onClick={() =>
                  navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
                }
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
              <Card key={lesson.id} className="card-hover border-primary">
                <CardHeader
                  floated={false}
                  shadow={false}
                  className="m-0 rounded-none border-b border-primary"
                >
                  <div className="p-4">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-2">
                          <Typography
                            variant="small"
                            className="text-secondary font-medium"
                          >
                            Bài {lesson.orderIndex}
                          </Typography>
                          <TypeIcon className="h-4 w-4 text-tertiary" />
                        </div>
                        <Typography
                          variant="h6"
                          className="text-primary mb-2 line-clamp-2 cursor-pointer hover:text-blue-600 dark:hover:text-blue-400 transition-colors duration-200"
                          onClick={() => {
                            if (lesson.lessonType === "PRACTICE") {
                              navigate(
                                ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id)
                              );
                            } else {
                              navigate(
                                ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(
                                  topicId,
                                  lesson.id
                                )
                              );
                            }
                          }}
                        >
                          {lesson.title}
                        </Typography>
                        <div className="flex items-center space-x-2">
                          <Chip
                            size="sm"
                            value={
                              lesson.lessonType === "THEORY"
                                ? "Lý thuyết"
                                : "Thực hành"
                            }
                            color={getTypeColor(lesson.lessonType)}
                            className="text-xs"
                          />
                          <Chip
                            size="sm"
                            value={lesson.isActive ? "Hoạt động" : "Tạm dừng"}
                            color={getStatusColor(lesson.isActive)}
                            icon={
                              lesson.isActive ? (
                                <CheckCircleIcon className="h-3 w-3" />
                              ) : (
                                <XCircleIcon className="h-3 w-3" />
                              )
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
                        <MenuList className="bg-secondary border-primary">
                          {lesson.lessonType === "PRACTICE" && (
                            <MenuItem
                              onClick={() =>
                                navigate(
                                  ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id)
                                )
                              }
                              className="hover:bg-tertiary"
                            >
                              <EyeIcon className="h-4 w-4 mr-2" />
                              <Typography
                                variant="small"
                                className="text-primary font-normal"
                              >
                                Xem câu hỏi
                              </Typography>
                            </MenuItem>
                          )}
                          <MenuItem
                            onClick={() =>
                              navigate(
                                ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(
                                  topicId,
                                  lesson.id
                                )
                              )
                            }
                            className="hover:bg-tertiary"
                          >
                            <PencilIcon className="h-4 w-4 mr-2" />
                            <Typography
                              variant="small"
                              className="text-primary font-normal"
                            >
                              Chỉnh sửa
                            </Typography>
                          </MenuItem>
                          <MenuItem
                            onClick={() =>
                              setDeleteDialog({ open: true, lesson })
                            }
                            className="hover:bg-red-50 dark:hover:bg-red-900/20"
                          >
                            <TrashIcon className="h-4 w-4 mr-2 text-red-500" />
                            <Typography
                              variant="small"
                              className="text-red-500 font-normal"
                            >
                              Xóa
                            </Typography>
                          </MenuItem>
                        </MenuList>
                      </Menu>
                    </div>
                  </div>
                </CardHeader>

                <CardBody className="p-4">
                  <Typography
                    variant="small"
                    className="text-secondary mb-4 line-clamp-2"
                  >
                    {lesson.lessonType === "THEORY"
                      ? getContentPreview(lesson.content, 100)
                      : `Bài thực hành với ${
                          lesson.questionCount || 0
                        } câu hỏi`}
                  </Typography>

                  <div className="flex items-center justify-between text-sm text-secondary mb-4">
                    <div className="flex items-center space-x-4">
                      <span className="text-xs">
                        ⏱️ {lesson.estimatedDuration}s
                      </span>
                      <span className="text-xs">
                        🎯 {lesson.pointsReward} điểm
                      </span>
                    </div>
                  </div>

                  <div className="flex space-x-2">
                    {lesson.lessonType === "PRACTICE" && (
                      <Button
                        size="sm"
                        variant="outlined"
                        className="flex-1 border-green-500 text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20"
                        onClick={() =>
                          navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id))
                        }
                      >
                        <EyeIcon className="h-4 w-4 mr-1" />
                        Câu hỏi
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outlined"
                      className="flex-1 border-blue-500 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                      onClick={() =>
                        navigate(
                          ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                        )
                      }
                    >
                      <PencilIcon className="h-4 w-4 mr-1" />
                      Sửa
                    </Button>
                    <IconButton
                      size="sm"
                      variant="outlined"
                      color="red"
                      className="hover:bg-red-50 dark:hover:bg-red-900/20"
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
        className="bg-secondary"
      >
        <DialogHeader className="flex items-center space-x-2 border-b border-primary">
          <TrashIcon className="h-6 w-6 text-red-500" />
          <span className="text-primary">Xác nhận xóa bài học</span>
        </DialogHeader>
        <DialogBody className="border-b border-primary">
          <Typography variant="paragraph" className="text-primary">
            Bạn có chắc chắn muốn xóa bài học{" "}
            <strong>"{deleteDialog.lesson?.title}"</strong> không?
          </Typography>
          <Typography variant="small" color="red" className="mt-2">
            ⚠️ Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên
            quan bao gồm các câu hỏi.
          </Typography>
        </DialogBody>
        <DialogFooter className="space-x-2">
          <Button
            variant="outlined"
            onClick={() => setDeleteDialog({ open: false, lesson: null })}
            className="btn-secondary"
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
