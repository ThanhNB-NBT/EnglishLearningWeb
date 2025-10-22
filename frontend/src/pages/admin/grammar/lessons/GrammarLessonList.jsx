import React, { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useLessonList } from "../../../../hook/grammar/useGrammarLessons";
import { ADMIN_ROUTES } from "../../../../constants/routes";
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
} from "@material-tailwind/react";
import {
  PlusIcon,
  MagnifyingGlassIcon,
  ArrowPathIcon,
  BookOpenIcon,
  PencilIcon,
  TrashIcon,
  EyeIcon,
  CheckCircleIcon,
  XCircleIcon,
  DocumentTextIcon,
  PlayCircleIcon,
  ClockIcon,
  TrophyIcon,
} from "@heroicons/react/24/outline";

// ✨ Import Reusable Components
import PageAdminHeader from "../../../../components/common/PageAdminHeader";
import ConfirmDialog from "../../../../components/common/ConfirmDialog";
import ResourceCard from "../../../../components/common/ResourceCard";

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
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    if (!deleteDialog.lesson) return;
    setDeleting(true);
    try {
      await deleteLesson(deleteDialog.lesson.id);
      setDeleteDialog({ open: false, lesson: null });
    } finally {
      setDeleting(false);
    }
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
        return PlayCircleIcon;
      default:
        return BookOpenIcon;
    }
  };

  const getStatusColor = (isActive) => (isActive ? "green" : "gray");

  const getContentPreview = (htmlContent, maxLength = 120) => {
    if (!htmlContent) return "Chưa có nội dung";
    const text = htmlContent.replace(/<[^>]+>/g, "");
    const cleanText = text.replace(/\s+/g, " ").trim();
    if (cleanText.length <= maxLength) return cleanText;
    return cleanText.substring(0, maxLength) + "...";
  };

  const hasActiveFilter = searchTerm || filterType || filterStatus;

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
    <div className="w-full space-y-6 p-4 md:p-6">
      {/* ✨ REUSABLE PAGE HEADER */}
      <PageAdminHeader
        title={topicInfo?.name || "Bài học"}
        subtitle="Quản lý các bài học trong chủ đề ngữ pháp"
        icon={BookOpenIcon}
        showBackButton={true}
        onBack={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        actions={
          <Button
            size="lg"
            className="bg-blue-500 hover:bg-blue-600 shadow-lg hover:shadow-xl transition-all flex items-center gap-2 w-full lg:w-auto"
            onClick={() =>
              navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
            }
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
                label="Tìm kiếm bài học"
                icon={<MagnifyingGlassIcon className="h-5 w-5" />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                color="blue"
                className="bg-secondary"
                containerProps={{ className: "!min-w-full" }}
              />
            </div>

            <div className="w-full">
              <Select
                label="Loại bài học"
                value={filterType}
                onChange={(val) => setFilterType(val)}
                color="blue"
                className="bg-secondary"
                containerProps={{ className: "!min-w-full" }}
                menuProps={{ className: "bg-secondary border-primary" }}
              >
                <Option value="">Tất cả loại</Option>
                <Option value="THEORY">Lý thuyết</Option>
                <Option value="PRACTICE">Thực hành</Option>
              </Select>
            </div>

            <div className="w-full">
              <Select
                value={filterStatus}
                onChange={(val) => setFilterStatus(val)}
                label="Trạng thái"
                color="blue"
                className="bg-secondary"
                containerProps={{ className: "!min-w-full" }}
                menuProps={{ className: "bg-secondary border-primary" }}
              >
                <Option value="">Tất cả trạng thái</Option>
                <Option value="active">Hoạt động</Option>
                <Option value="inactive">Không hoạt động</Option>
              </Select>
            </div>
          </div>

          {/* Filter Summary */}
          <div className="mt-6 pt-4 border-t border-primary flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 flex-wrap">
              <Typography variant="small" className="text-primary font-semibold">
                Hiển thị <span className="text-blue-500">{filteredLessons.length}</span> /{" "}
                {lessons.length} bài học
              </Typography>
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
                className="border-primary hover:bg-tertiary flex items-center gap-2 w-full sm:w-auto"
              >
                <ArrowPathIcon className="h-4 w-4" />
                Reset bộ lọc
              </Button>
            )}
          </div>
        </CardBody>
      </Card>

      {/* Lessons Grid */}
      {filteredLessons.length === 0 ? (
        <Card className="card-base border-primary">
          <CardBody className="p-12 text-center">
            <div className="max-w-md mx-auto">
              <div className="w-20 h-20 bg-blue-50 dark:bg-blue-900/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <BookOpenIcon className="h-10 w-10 text-blue-500" />
              </div>
              <Typography variant="h5" className="text-primary mb-2 font-bold">
                {lessons.length === 0
                  ? "Chưa có bài học nào"
                  : "Không tìm thấy bài học"}
              </Typography>
              <Typography variant="small" className="text-secondary mb-6">
                {lessons.length === 0
                  ? "Hãy tạo bài học đầu tiên cho chủ đề này."
                  : "Thử thay đổi bộ lọc để tìm thấy bài học bạn cần."}
              </Typography>
              {lessons.length === 0 && (
                <Button
                  size="lg"
                  color="blue"
                  onClick={() =>
                    navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
                  }
                  className="flex items-center gap-2 mx-auto shadow-lg"
                >
                  <PlusIcon className="h-5 w-5" />
                  Tạo bài học đầu tiên
                </Button>
              )}
            </div>
          </CardBody>
        </Card>
      ) : (
        /* ✨ USING RESOURCE CARD */
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {filteredLessons.map((lesson) => {
            const TypeIcon = getTypeIcon(lesson.lessonType);
            
            return (
              <ResourceCard
                key={lesson.id}
                item={lesson}
                title={lesson.title}
                description={
                  lesson.lessonType === "THEORY"
                    ? getContentPreview(lesson.content, 120)
                    : `Bài thực hành với ${lesson.questionCount || 0} câu hỏi`
                }
                orderLabel={`Bài ${lesson.orderIndex}`}
                icon={TypeIcon}
                iconBgColor={lesson.lessonType === "THEORY" ? "blue-500" : "green-500"}
                iconColor={lesson.lessonType === "THEORY" ? "blue-400" : "green-400"}
                chips={[
                  {
                    label: lesson.lessonType === "THEORY" ? "Lý thuyết" : "Thực hành",
                    color: getTypeColor(lesson.lessonType),
                  },
                  {
                    label: lesson.isActive ? "Hoạt động" : "Tạm dừng",
                    color: getStatusColor(lesson.isActive),
                    icon: lesson.isActive ? (
                      <CheckCircleIcon className="h-3 w-3" />
                    ) : (
                      <XCircleIcon className="h-3 w-3" />
                    ),
                  },
                ]}
                stats={[
                  {
                    icon: ClockIcon,
                    iconColor: "text-blue-500",
                    bgColor: "bg-blue-50 dark:bg-blue-900/20",
                    label: "",
                    value: `${lesson.estimatedDuration}s`,
                  },
                  {
                    icon: TrophyIcon,
                    iconColor: "text-amber-500",
                    bgColor: "bg-amber-50 dark:bg-amber-900/20",
                    label: "",
                    value: `${lesson.pointsReward} điểm`,
                  },
                ]}
                actions={
                  lesson.lessonType === "PRACTICE"
                    ? [
                        {
                          label: "Câu hỏi",
                          icon: EyeIcon,
                          className:
                            "border-green-500 text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20",
                          onClick: () =>
                            navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id)),
                        },
                        {
                          label: "Sửa",
                          icon: PencilIcon,
                          className:
                            "border-blue-500 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20",
                          onClick: () =>
                            navigate(
                              ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                            ),
                        },
                        {
                          type: "icon",
                          icon: TrashIcon,
                          color: "red",
                          className:
                            "hover:bg-red-50 dark:hover:bg-red-900/20 border-red-500",
                          onClick: () =>
                            setDeleteDialog({ open: true, lesson }),
                        },
                      ]
                    : [
                        {
                          label: "Sửa",
                          icon: PencilIcon,
                          className:
                            "border-blue-500 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20",
                          onClick: () =>
                            navigate(
                              ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                            ),
                        },
                        {
                          type: "icon",
                          icon: TrashIcon,
                          color: "red",
                          className:
                            "hover:bg-red-50 dark:hover:bg-red-900/20 border-red-500",
                          onClick: () =>
                            setDeleteDialog({ open: true, lesson }),
                        },
                      ]
                }
                menuActions={{
                  ...(lesson.lessonType === "PRACTICE" && {
                    view: {
                      label: "Xem câu hỏi",
                      onClick: () =>
                        navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id)),
                    },
                  }),
                  edit: {
                    label: "Chỉnh sửa",
                    onClick: () =>
                      navigate(
                        ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                      ),
                  },
                  delete: {
                    label: "Xóa bài học",
                    onClick: () => setDeleteDialog({ open: true, lesson }),
                  },
                }}
                onTitleClick={
                  lesson.lessonType === "PRACTICE"
                    ? () => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id))
                    : () =>
                        navigate(
                          ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                        )
                }
              />
            );
          })}
        </div>
      )}

      {/* ✨ REUSABLE CONFIRM DIALOG */}
      <ConfirmDialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, lesson: null })}
        onConfirm={handleDelete}
        title="Xác nhận xóa bài học"
        message="Bạn có chắc chắn muốn xóa bài học"
        itemName={deleteDialog.lesson?.title}
        warningMessage="Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan bao gồm các câu hỏi."
        confirmText="Xóa bài học"
        type="delete"
        loading={deleting}
      />
    </div>
  );
};

export default GrammarLessonList;