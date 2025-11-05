import React from "react";
import { useParams } from "react-router-dom";
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  Alert,
} from "@material-tailwind/react";
import {
  CheckCircleIcon,
  XMarkIcon,
  InformationCircleIcon,
  DocumentTextIcon, // ✨ Đổi icon
} from "@heroicons/react/24/outline";

// ✨ Import Components & Hook
import PageAdminHeader from "../../../../components/common/PageAdminHeader";
import ReadingQuestionFormBasicInfo from "../../../../components/reading/forms/ReadingQuestionFormBasicInfo";
import ReadingQuestionAnswerSection from "../../../../components/reading/sections/ReadingQuestionAnswerSection";
import ReadingQuestionFormSettings from "../../../../components/reading/forms/ReadingQuestionFormSettings";
import { useQuestionForm } from "../../../../hook/reading/useReadingQuestions";

const ReadingQuestionForm = () => {
  const { lessonId, questionId } = useParams();
  
  // ✨ USE HOOK
  const {
    formData,
    options,
    loading,
    submitting,
    errors,
    isEdit,
    setOptions,
    handleInputChange,
    handleSubmit,
    handleCancel,
  } = useQuestionForm(lessonId, questionId);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải thông tin câu hỏi...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full space-y-6 p-4 md:p-6">
      {/* ✨ REUSABLE PAGE HEADER */}
      <PageAdminHeader
        title={isEdit ? "Chỉnh sửa câu hỏi" : "Tạo câu hỏi mới"}
        subtitle="Bài đọc hiểu" // ✨ Đổi subtitle
        icon={DocumentTextIcon} // ✨ Đổi icon
        iconBgColor="blue-500" // ✨ Đổi màu
        iconColor="blue-400" // ✨ Đổi màu
        showBackButton={true}
        onBack={handleCancel}
      />

      {/* Info Alert */}
      <Alert color="blue" icon={<InformationCircleIcon className="h-5 w-5" />} className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
        <Typography variant="small" className="font-medium mb-1 text-primary">
          💡 Hướng dẫn tạo câu hỏi
        </Typography>
        {/* ✨ Cập nhật hướng dẫn */}
        <Typography variant="small" className="text-secondary">
          • <strong>Trắc nghiệm:</strong> 4 lựa chọn cố định, đánh dấu đáp án đúng<br/>
          • <strong>Điền từ:</strong> Nhập đáp án trực tiếp (nhiều đáp án cách nhau bởi |)<br/>
          • <strong>Trả lời ngắn:</strong> Nhập câu trả lời mẫu (nhiều đáp án cách nhau bởi |)<br/>
          • <strong>Giải thích:</strong> Giúp học viên hiểu tại sao đáp án đúng
        </Typography>
      </Alert>

      {/* Form */}
      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* Basic Info */}
            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                {/* ✨ Đổi màu */}
                <div className="w-1 h-6 bg-blue-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Thông tin cơ bản
                </Typography>
              </div>
              {/* ✨ Đổi component */}
              <ReadingQuestionFormBasicInfo
                formData={formData}
                errors={errors}
                onChange={handleInputChange}
              />
            </div>

            {/* Answer Section */}
            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                <div className="w-1 h-6 bg-green-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Đáp án
                </Typography>
              </div>
              {/* ✨ Đổi component */}
              <ReadingQuestionAnswerSection
                questionType={formData.questionType}
                correctAnswer={formData.correctAnswer}
                options={options}
                errors={errors}
                onCorrectAnswerChange={(value) => handleInputChange('correctAnswer', value)}
                onOptionsChange={setOptions}
              />
            </div>

            {/* Settings */}
            <div>
              <div className="flex items-center gap-2 mb-4 pb-3 border-b border-primary">
                {/* ✨ Đổi màu */}
                <div className="w-1 h-6 bg-cyan-500 rounded-full"></div>
                <Typography variant="h6" className="text-primary font-bold">
                  Cài đặt
                </Typography>
              </div>
              {/* ✨ Đổi component */}
              <ReadingQuestionFormSettings
                formData={formData}
                errors={errors}
                onChange={handleInputChange}
                isEdit={isEdit}
              />
            </div>

            {/* Form Actions */}
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
                  color="blue"
                  disabled={submitting}
                  loading={submitting}
                  className="flex items-center justify-center shadow-lg hover:shadow-xl transition-all"
                >
                  {!submitting && <CheckCircleIcon className="h-4 w-4 mr-2" />}
                  {isEdit ? "Cập nhật" : "Tạo câu hỏi"}
                </Button>
              </div>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
};

export default ReadingQuestionForm;