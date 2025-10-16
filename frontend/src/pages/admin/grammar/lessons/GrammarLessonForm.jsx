import React from "react";
import { useParams } from "react-router-dom";
import { useLessonForm } from "../../../../hook/grammar/useGrammarLessons";
import { ADMIN_ROUTES } from "../../../../constants/routes";
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  Breadcrumbs,
  Progress,
  Alert,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  XMarkIcon,
  BookOpenIcon,
  PencilIcon,
} from "@heroicons/react/24/outline";

// Import child components
import LessonFormBasicInfo from "../../../../components/grammar/forms/LessonFormBasicInfo";
import LessonFormSettings from "../../../../components/grammar/forms/LessonFormSettings";
import LessonContentEditor from "../../../../components/grammar/editors/LessonContentEditor";
import LessonContentPreview from "../../../../components/grammar/editors/LessonContentPreview";
import GeminiParsedResultDialog from "../../../../components/grammar/dialog/GeminiParsedResultDialog";

const GrammarLessonForm = () => {
  const { topicId, lessonId } = useParams();
  const {
    formData,
    topicInfo,
    loading,
    submitting,
    pdfParsing,
    errors,
    hasChanges,
    isEdit,
    pdfDialog,
    setPdfDialog,
    handleInputChange,
    handleSubmit,
    handleCancel,
    handleFileUpload,
    handleUseParsedContent,
    getCompletionPercentage
  } = useLessonForm(topicId, lessonId);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải thông tin bài học...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6">
      {/* Breadcrumbs */}
      <Breadcrumbs className="bg-transparent p-0">
        <Typography
          className="text-tertiary hover:text-primary cursor-pointer transition-colors"
          onClick={() => window.history.back()}
        >
          Chủ đề ngữ pháp
        </Typography>
        <Typography
          className="text-tertiary hover:text-primary cursor-pointer transition-colors"
          onClick={handleCancel}
        >
          {topicInfo?.name || "Bài học"}
        </Typography>
        <Typography className="text-primary">
          {isEdit ? "Chỉnh sửa" : "Tạo mới"}
        </Typography>
      </Breadcrumbs>

      {/* Header */}
      <Card className="card-base border-primary">
        <CardBody className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Button
                variant="outlined"
                size="sm"
                onClick={handleCancel}
                className="border-primary hover:bg-tertiary"
              >
                <ArrowLeftIcon className="h-4 w-4 mr-2" />
                Quay lại
              </Button>
              <div className={`p-2 ${isEdit ? 'bg-orange-50 dark:bg-orange-900/30' : 'bg-blue-50 dark:bg-blue-900/30'} rounded-lg`}>
                {isEdit ? (
                  <PencilIcon className="h-6 w-6 text-orange-600 dark:text-orange-400" />
                ) : (
                  <BookOpenIcon className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                )}
              </div>
              <div>
                <Typography variant="h4" className="text-primary font-bold">
                  {isEdit ? "Chỉnh sửa bài học" : "Tạo bài học mới"}
                </Typography>
                <Typography variant="small" className="text-secondary mt-1">
                  Chủ đề: {topicInfo?.name}
                </Typography>
              </div>
            </div>
            {isEdit && hasChanges && (
              <Alert color="orange" className="py-2 px-3 w-auto">
                <Typography variant="small" className="font-medium">
                  Có thay đổi chưa lưu
                </Typography>
              </Alert>
            )}
          </div>
        </CardBody>
      </Card>

      {/* Progress */}
      <Card className="card-base border-primary">
        <CardBody className="p-4">
          <div className="flex items-center justify-between mb-2">
            <Typography variant="small" className="text-primary font-medium">
              Hoàn thành form
            </Typography>
            <Typography variant="small" className="text-secondary">
              {getCompletionPercentage()}%
            </Typography>
          </div>
          <Progress 
            value={getCompletionPercentage()} 
            color={isEdit ? "orange" : "blue"} 
            size="sm" 
          />
        </CardBody>
      </Card>

      {/* Form */}
      <Card className="card-base border-primary">
        <CardBody className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Info Component */}
            <LessonFormBasicInfo
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
              isEdit={isEdit}
            />

            {/* Content Editor Component (Only for THEORY) */}
            {formData.lessonType === "THEORY" && (
              <LessonContentEditor
                content={formData.content}
                error={errors.content}
                onChange={(value) => handleInputChange("content", value)}
                onFileUpload={handleFileUpload}
                isParsing={pdfParsing}
                isEdit={isEdit}
              />
            )}

            {/* Preview nội dung (Optional) */}
            {formData.content && formData.lessonType === "THEORY" && (
              <LessonContentPreview content={formData.content} />
            )}

            {/* Settings Component */}
            <LessonFormSettings
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
              isEdit={isEdit}
            />

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-primary">
              <Button
                variant="outlined"
                onClick={handleCancel}
                disabled={submitting}
                className="btn-secondary flex items-center"
              >
                <XMarkIcon className="h-4 w-4 mr-2" />
                Hủy
              </Button>
              <Button
                type="submit"
                color="green"
                disabled={submitting || Object.keys(errors).length > 0 || (isEdit && !hasChanges)}
                loading={submitting}
                className="flex items-center shadow-lg hover:shadow-xl transition-shadow"
              >
                {submitting ? (
                  isEdit ? "Đang lưu..." : "Đang tạo..."
                ) : (
                  <>
                    <CheckCircleIcon className="h-5 w-5 mr-2" />
                    {isEdit ? "Cập nhật" : "Tạo bài học"}
                  </>
                )}
              </Button>
            </div>
            
            {isEdit && !hasChanges && (
              <Typography variant="small" className="text-secondary text-center mt-2">
                Không có thay đổi để lưu
              </Typography>
            )}
          </form>
        </CardBody>
      </Card>

      {/* Gemini Parsed Result Dialog Component */}
      <GeminiParsedResultDialog
        open={pdfDialog.open}
        parsedData={pdfDialog.parsedData}
        summary={pdfDialog.summary}
        onClose={() =>
          setPdfDialog({ open: false, parsedData: null, summary: null })
        }
        onConfirm={handleUseParsedContent}
      />
    </div>
  );
};

export default GrammarLessonForm;