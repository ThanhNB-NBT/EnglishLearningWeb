import React from "react";
import { useParams } from "react-router-dom";
import { useLessonForm } from "../../../../hook/reading/useReadingLessons";
import { ADMIN_ROUTES } from "../../../../constants/routes";
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  IconButton,
  Progress,
  Alert,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  XMarkIcon,
  BookOpenIcon,
  PencilIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";

import ConfirmDialog from "../../../../components/common/ConfirmDialog";
import ReadingLessonFormBasicInfo from "../../../../components/reading/forms/ReadingLessonFormBasicInfo";
import ReadingLessonFormSettings from "../../../../components/reading/forms/ReadingLessonFormSettings";
import ReadingContentEditor from "../../../../components/reading/editors/ReadingContentEditor";
import ReadingContentPreview from "../../../../components/reading/editors/ReadingContentPreview";

const ReadingLessonForm = () => {
  const { id } = useParams();
  const {
    formData,
    loading,
    submitting,
    errors,
    hasChanges,
    isEdit,
    handleInputChange,
    handleSubmit,
    handleCancel,
    getCompletionPercentage,
    manualOrderIndex,
    setManualOrderIndex,
    reorderDialog,
    totalLessons,
    willCauseReorder,
  } = useLessonForm(id);

  const handleManualOrderToggle = (e) => {
    setManualOrderIndex(e.target.checked);
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải thông tin bài đọc...
        </Typography>
      </div>
    );
  }

  const completionPercentage = getCompletionPercentage();
  const hasErrors = Object.keys(errors).length > 0;

  return (
    <div className="w-full space-y-6 p-4 md:p-6">
      <Card className="bg-gradient-to-r from-slate-800 to-slate-900 dark:from-slate-900 dark:to-black border border-slate-700 shadow-xl">
        <CardBody className="p-4 md:p-6">
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
            <div className="flex items-center gap-3 flex-1">
              <IconButton
                variant="outlined"
                size="lg"
                onClick={handleCancel}
                className="text-slate-300 hover:bg-slate-700 border-slate-500 hidden md:flex"
              >
                <ArrowLeftIcon className="h-4 w-4" />
              </IconButton>

              <div
                className={`p-3 rounded-xl border ${
                  isEdit
                    ? "bg-orange-500/20 border-orange-500/30"
                    : "bg-blue-500/20 border-blue-500/30"
                }`}
              >
                {isEdit ? (
                  <PencilIcon className="h-6 w-6 text-orange-400" />
                ) : (
                  <BookOpenIcon className="h-6 w-6 text-blue-400" />
                )}
              </div>

              <div>
                <Typography
                  variant="h4"
                  className="text-slate-100 font-bold mb-1"
                >
                  {isEdit ? "Chỉnh sửa bài đọc" : "Tạo bài đọc mới"}
                </Typography>
                <Typography variant="small" className="text-slate-400">
                  Quản lý nội dung bài đọc hiểu
                </Typography>
              </div>
            </div>

            {isEdit && hasChanges && (
              <Alert
                color="orange"
                className="py-2 px-4 bg-orange-500/20 border border-orange-500/30"
                icon={<ExclamationTriangleIcon className="h-5 w-5" />}
              >
                <Typography
                  variant="small"
                  className="font-medium text-orange-400"
                >
                  Có thay đổi chưa lưu
                </Typography>
              </Alert>
            )}
          </div>
        </CardBody>
      </Card>

      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Typography
                variant="small"
                className="text-primary font-semibold"
              >
                Hoàn thành form
              </Typography>
              {completionPercentage === 100 && (
                <CheckCircleIcon className="h-5 w-5 text-green-500" />
              )}
            </div>
            <Typography
              variant="small"
              className={`font-bold ${
                completionPercentage === 100
                  ? "text-green-500"
                  : "text-secondary"
              }`}
            >
              {completionPercentage}%
            </Typography>
          </div>
          <Progress
            value={completionPercentage}
            color={isEdit ? "orange" : "blue"}
            size="lg"
            className="bg-tertiary"
          />
          <Typography variant="small" className="text-tertiary mt-2">
            {completionPercentage < 100
              ? "Vui lòng điền đầy đủ thông tin bắt buộc"
              : "Form đã hoàn thiện, sẵn sàng lưu"}
          </Typography>
        </CardBody>
      </Card>

      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <form onSubmit={handleSubmit} className="space-y-8">
            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                <div className="w-1 h-6 bg-blue-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Thông tin cơ bản
                </Typography>
              </div>
              <ReadingLessonFormBasicInfo
                formData={formData}
                errors={errors}
                onChange={handleInputChange}
                isEdit={isEdit}
              />
            </div>

            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                <div className="w-1 h-6 bg-purple-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Nội dung bài đọc
                </Typography>
              </div>
              <ReadingContentEditor
                content={formData.content}
                translation={formData.contentTranslation}
                contentError={errors.content}
                translationError={errors.contentTranslation}
                onContentChange={(value) => handleInputChange("content", value)}
                onTranslationChange={(value) =>
                  handleInputChange("contentTranslation", value)
                }
                isEdit={isEdit}
              />
            </div>

            {formData.content && (
              <ReadingContentPreview
                content={formData.content}
                translation={formData.contentTranslation}
              />
            )}

            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                <div className="w-1 h-6 bg-green-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Cài đặt bài đọc
                </Typography>
              </div>
              <ReadingLessonFormSettings
                formData={formData}
                errors={errors}
                onChange={handleInputChange}
                isEdit={isEdit}
                manualOrderIndex={manualOrderIndex}
                onManualOrderToggle={handleManualOrderToggle}
                totalLessons={totalLessons}
                willCauseReorder={willCauseReorder}
              />
            </div>

            <div className="pt-6 border-t border-primary">
              <div className="flex flex-col sm:flex-row justify-end gap-3">
                <Button
                  variant="outlined"
                  onClick={handleCancel}
                  disabled={submitting}
                  className="btn-secondary flex items-center justify-center"
                >
                  <XMarkIcon className="h-4 w-4 mr-2" />
                  Hủy bỏ
                </Button>
                <Button
                  type="submit"
                  color="green"
                  disabled={submitting || hasErrors || (isEdit && !hasChanges)}
                  loading={submitting}
                  className="flex items-center justify-center shadow-lg hover:shadow-xl transition-all"
                >
                  {submitting ? (
                    isEdit ? (
                      "Đang lưu..."
                    ) : (
                      "Đang tạo..."
                    )
                  ) : (
                    <>
                      <CheckCircleIcon className="h-5 w-5 mr-2" />
                      {isEdit ? "Cập nhật bài đọc" : "Tạo bài đọc"}
                    </>
                  )}
                </Button>
              </div>

              {isEdit && !hasChanges && (
                <Typography
                  variant="small"
                  className="text-secondary text-center mt-3"
                >
                  Không có thay đổi để lưu
                </Typography>
              )}
              {hasErrors && (
                <Typography
                  variant="small"
                  className="text-red-500 text-center mt-3"
                >
                  Vui lòng sửa các lỗi trước khi lưu
                </Typography>
              )}
            </div>
          </form>
        </CardBody>
      </Card>

      <ConfirmDialog
        open={reorderDialog.open}
        onClose={() => reorderDialog.onCancel?.()}
        onConfirm={() => reorderDialog.onConfirm?.()}
        title="Xác nhận sắp xếp lại"
        confirmText="Xác nhận sắp xếp lại"
        type="reorder"
        loading={reorderDialog.loading}
        reorderData={{
          targetPosition: formData.orderIndex,
          affectedItems: reorderDialog.affectedLessons.map((lesson) => ({
            id: lesson.id,
            title: lesson.title,
            subtitle: `Bài đọc ${lesson.orderIndex}`,
            orderIndex: lesson.orderIndex,
          })),
          isEdit: isEdit,
        }}
      />
    </div>
  );
};

export default ReadingLessonForm;