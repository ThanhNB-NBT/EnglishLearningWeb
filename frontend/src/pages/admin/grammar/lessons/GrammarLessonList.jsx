import React, { useState, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useLessonList } from "../../../../hook/grammar/useGrammarLessons";
import { useGrammarPDFParse } from "../../../../hook/grammar/useGrammarPDFParse";
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
  DocumentPlusIcon, // ✅ NEW
} from "@heroicons/react/24/outline";

// ✨ Import Reusable Components
import PageAdminHeader from "../../../../components/common/PageAdminHeader";
import ConfirmDialog from "../../../../components/common/ConfirmDialog";
import ResourceCard from "../../../../components/common/ResourceCard";
import PaginationControls from "../../../../components/common/PaginationControls";
import PageSizeSelector from "../../../../components/common/PageSizeSelector";

// ✅ NEW: Import PDF/File parsing components
import PDFPageSelector from "../../../../components/grammar/dialog/PDFPageSelector";
import GeminiParsedResultDialog from "../../../../components/grammar/dialog/GeminiParsedResultDialog";

const GrammarLessonList = () => {
  const { topicId } = useParams();
  const navigate = useNavigate();
  const fileInputRef = useRef(null); // ✅ NEW

  const {
    lessons,
    topicInfo,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    filterStatus,
    setFilterStatus,
    pagination,
    handlePageChange,
    handlePageSizeChange,
    deleteLesson,
    resetFilters,
    reload, // ✅ NEW: Add reload function
  } = useLessonList(topicId);

  // ✅ NEW: PDF/File parsing hook
  const {
    uploading,
    pdfDialog,
    pageSelector,
    handleFileSelect,
    handlePageSelectionConfirm,
    closePdfDialog,
    handleImportLessons,
  } = useGrammarPDFParse(topicId, reload);

  const [deleteDialog, setDeleteDialog] = useState({
    open: false,
    lesson: null,
  });
  const [deleting, setDeleting] = useState(false);

  // ✅ NEW: Handle upload button click
  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  // ✅ NEW: Handle file input change
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleFileSelect(file);
    }
    // Reset input để có thể chọn lại cùng file
    e.target.value = "";
  };

  const handleDelete = async () => {
    if (!deleteDialog.lesson) return;
    setDeleting(true);
    try {
      await deleteLesson(deleteDialog.lesson.id,
        { cascade: deleteDialog.lesson.lessonType === "PRACTICE" }
      );
      setDeleteDialog({ open: false, lesson: null });
    } finally {
      setDeleting(false);
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
      {/* ✅ HIDDEN FILE INPUT */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.docx,.jpg,.jpeg,.png,.webp"
        onChange={handleFileChange}
        className="hidden"
      />

      {/* REUSABLE PAGE HEADER - Updated with Parse button */}
      <PageAdminHeader
        title={topicInfo?.name || "Bài học"}
        subtitle="Quản lý các bài học trong chủ đề ngữ pháp"
        icon={BookOpenIcon}
        showBackButton={true}
        onBack={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        actions={
          <div className="flex flex-col sm:flex-row gap-3 w-full lg:w-auto">
            {/* Parse File Button */}
            <Button
              size="lg"
              color="purple"
              className="shadow-lg hover:shadow-xl transition-all flex items-center justify-center gap-2 w-full lg:w-auto"
              onClick={handleUploadClick}
              disabled={uploading}
              loading={uploading}
            >
              <DocumentPlusIcon className="h-5 w-5" />
              <span className="font-semibold">
                {uploading ? "Đang parse..." : "Parse PDF/DOCX/Image"}
              </span>
            </Button>

            {/* Create New Button */}
            <Button
              size="lg"
              className="bg-blue-500 hover:bg-blue-600 shadow-lg hover:shadow-xl transition-all flex items-center justify-center gap-2 w-full lg:w-auto"
              onClick={() =>
                navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
              }
            >
              <PlusIcon className="h-5 w-5" />
              <span className="font-semibold">Thêm mới</span>
            </Button>
          </div>
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

          {/* Filter Summary with Page Size Selector */}
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
      {lessons.length === 0 ? (
        <Card className="card-base border-primary">
          <CardBody className="p-12 text-center">
            <div className="max-w-md mx-auto">
              <div className="w-20 h-20 bg-blue-50 dark:bg-blue-900/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <BookOpenIcon className="h-10 w-10 text-blue-500" />
              </div>
              <Typography variant="h5" className="text-primary mb-2 font-bold">
                {pagination.totalElements === 0
                  ? "Chưa có bài học nào"
                  : "Không tìm thấy bài học"}
              </Typography>
              <Typography variant="small" className="text-secondary mb-6">
                {pagination.totalElements === 0
                  ? "Hãy tạo bài học bằng cách tạo thủ công hoặc parse từ file PDF/DOCX."
                  : "Thử thay đổi bộ lọc để tìm thấy bài học bạn cần."}
              </Typography>
              {pagination.totalElements === 0 && (
                <div className="flex flex-col sm:flex-row gap-3 justify-center">
                  <Button
                    size="lg"
                    color="purple"
                    onClick={handleUploadClick}
                    className="flex items-center gap-2 shadow-lg"
                  >
                    <DocumentPlusIcon className="h-5 w-5" />
                    Parse từ file
                  </Button>
                  <Button
                    size="lg"
                    color="blue"
                    onClick={() =>
                      navigate(ADMIN_ROUTES.GRAMMAR_LESSON_CREATE(topicId))
                    }
                    className="flex items-center gap-2 shadow-lg"
                  >
                    <PlusIcon className="h-5 w-5" />
                    Tạo thủ công
                  </Button>
                </div>
              )}
            </div>
          </CardBody>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            {lessons.map((lesson) => {
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
                  iconBgColor={
                    lesson.lessonType === "THEORY" ? "blue" : "cyan"
                  }
                  iconColor={
                    lesson.lessonType === "THEORY" ? "blue" : "amber"
                  }
                  chips={[
                    {
                      label:
                        lesson.lessonType === "THEORY"
                          ? "Lý thuyết"
                          : "Thực hành",
                      color: lesson.lessonType === "THEORY" ? "blue" : "amber",
                    },
                    {
                      label: lesson.isActive ? "Hoạt động" : "Tạm dừng",
                      color: lesson.isActive ? "green" : "gray",
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
                              navigate(
                                ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id)
                              ),
                          },
                          {
                            label: "Sửa",
                            icon: PencilIcon,
                            className:
                              "border-blue-500 text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20",
                            onClick: () =>
                              navigate(
                                ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(
                                  topicId,
                                  lesson.id
                                )
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
                                ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(
                                  topicId,
                                  lesson.id
                                )
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
                      ? () =>
                          navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lesson.id))
                      : () =>
                          navigate(
                            ADMIN_ROUTES.GRAMMAR_LESSON_EDIT(topicId, lesson.id)
                          )
                  }
                />
              );
            })}
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

      {/* ✅ NEW: PDF Page Selector Dialog */}
      <PDFPageSelector
        open={pageSelector.open}
        file={pageSelector.file}
        onClose={() => pageSelector.setOpen?.(false)}
        onConfirm={handlePageSelectionConfirm}
      />

      {/* ✅ NEW: Parsed Result Dialog */}
      {pdfDialog.open && pdfDialog.parsedData && (
        <GeminiParsedResultDialog
          open={pdfDialog.open}
          parsedData={pdfDialog.parsedData}
          summary={pdfDialog.summary}
          onClose={closePdfDialog}
          onConfirm={handleImportLessons}
        />
      )}
    </div>
  );
};

export default GrammarLessonList;
