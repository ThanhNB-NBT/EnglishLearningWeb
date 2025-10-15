import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { grammarAdminAPI } from "../../../api";
import { ADMIN_ROUTES } from "../../../constants/routes";
import {
  Button,
  Card,
  CardBody,
  Typography,
  Spinner,
  Breadcrumbs,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import toast from "react-hot-toast";

// Import child components
import LessonFormBasicInfo from "../../../components/grammar/LessonFormBasicInfo";
import LessonFormSettings from "../../../components/grammar/LessonFormSettings";
import LessonContentEditor from "../../../components/grammar/LessonContentEditor";
import LessonContentPreview from "../../../components/grammar/LessonContentPreview";
import GeminiParsedResultDialog from "../../../components/grammar/GeminiParsedResultDialog";

/**
 * Main Form Container - Tạo/Chỉnh sửa Lesson
 * Chia nhỏ thành các component con để dễ đọc và maintain
 */
const GrammarLessonForm = () => {
  const { topicId, lessonId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!lessonId;

  // Form state
  const [formData, setFormData] = useState({
    topicId: parseInt(topicId),
    title: "",
    lessonType: "THEORY",
    content: "",
    orderIndex: 1,
    pointsReward: 10,
    estimatedDuration: 180,
    isActive: true,
  });

  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pdfParsing, setPdfParsing] = useState(false);
  const [errors, setErrors] = useState({});

  // PDF Dialog state
  const [pdfDialog, setPdfDialog] = useState({
    open: false,
    parsedData: null,
    summary: null,
  });

  // ===== LIFECYCLE =====

  useEffect(() => {
    loadTopicInfo();
    if (isEdit) {
      loadLessonData();
    } else {
      getNextOrderIndex();
    }
  }, [topicId, lessonId]);

  // ===== DATA LOADING =====

  const loadTopicInfo = async () => {
    try {
      const response = await grammarAdminAPI.getAllTopics();
      const topic = response.data.data.find((t) => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error("Load topic info error:", error);
      toast.error("Lỗi khi lấy thông tin chủ đề");
    }
  };

  const getNextOrderIndex = async () => {
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId);
      const lessons = response.data.data || [];
      const maxOrder =
        lessons.length > 0 ? Math.max(...lessons.map((l) => l.orderIndex)) : 0;
      setFormData((prev) => ({ ...prev, orderIndex: maxOrder + 1 }));
    } catch (error) {
      console.error("Get next order index error:", error);
    }
  };

  const loadLessonData = async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getLessonDetail(lessonId);
      const lesson = response.data.data;

      if (lesson) {
        setFormData({
          topicId: lesson.topicId,
          title: lesson.title || "",
          lessonType: lesson.lessonType || "THEORY",
          content: lesson.content || "",
          orderIndex: lesson.orderIndex || 1,
          pointsReward: lesson.pointsReward || 10,
          estimatedDuration: lesson.estimatedDuration || 180,
          isActive: lesson.isActive !== undefined ? lesson.isActive : true,
        });
      }
    } catch (error) {
      console.error("Load lesson data error:", error);
      toast.error("Lỗi khi lấy thông tin bài học");
    } finally {
      setLoading(false);
    }
  };

  // ===== FORM HANDLERS =====

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: null }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.title.trim()) {
      newErrors.title = "Tiêu đề không được để trống";
    } else if (formData.title.length > 200) {
      newErrors.title = "Tiêu đề không được vượt quá 200 ký tự";
    }

    if (formData.orderIndex < 1) {
      newErrors.orderIndex = "Thứ tự phải lớn hơn 0";
    }

    if (formData.pointsReward <= 0) {
      newErrors.pointsReward = "Điểm thưởng phải lớn hơn 0";
    }

    if (formData.estimatedDuration < 10) {
      newErrors.estimatedDuration = "Thời gian ước tính phải >= 10 giây";
    }

    if (formData.lessonType === "THEORY" && !formData.content.trim()) {
      newErrors.content = "Nội dung lý thuyết không được để trống";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin nhập vào");
      return;
    }

    setSubmitting(true);
    try {
      if (isEdit) {
        await grammarAdminAPI.updateLesson(lessonId, formData);
        toast.success("Cập nhật bài học thành công!");
      } else {
        await grammarAdminAPI.createLesson(formData);
        toast.success("Tạo bài học thành công!");
      }
      navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
    } catch (error) {
      console.error("Submit lesson error:", error);
      toast.error(error.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setSubmitting(false);
    }
  };

  // ===== GEMINI PDF PARSING =====

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const isPDF = file.type === "application/pdf";
    const isDOCX = file.name.toLowerCase().endsWith(".docx");

    if (!isPDF && !isDOCX) {
      toast.error("Chỉ hỗ trợ file PDF và DOCX");
      return;
    }

    // 🔧 FIX: Kiểm tra kích thước file (max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      toast.error("File không được vượt quá 10MB");
      return;
    }

    setPdfParsing(true);
    const toastId = toast.loading("🤖 Gemini AI đang phân tích file...");

    try {
      const response = await grammarAdminAPI.parsePDF(topicId, file);
      const result = response.data.data;

      setPdfDialog({
        open: true,
        parsedData: result.parsedData,
        summary: result.summary,
      });

      toast.success("Phân tích thành công với Gemini AI!", { id: toastId });
    } catch (error) {
      console.error("Gemini parsing error:", error);
      const errorMsg = error.response?.data?.message || "Lỗi khi phân tích file";
      toast.error(errorMsg, { id: toastId });
    } finally {
      setPdfParsing(false);
      event.target.value = ""; // Reset input
    }
  };

  const handleUseParsedContent = async (finalData) => {
    if (!finalData || !finalData.lessons || finalData.lessons.length === 0) {
      toast.error("Không có dữ liệu để sử dụng");
      return;
    }

    try {
      // Single lesson: use directly in form
      if (finalData.lessons.length === 1) {
        const lesson = finalData.lessons[0];
        setFormData((prev) => ({
          ...prev,
          title: lesson.title,
          content: lesson.content, // 🔧 Đây là HTML từ TipTap
          lessonType: lesson.lessonType || "THEORY",
          orderIndex: lesson.orderIndex || prev.orderIndex,
          pointsReward: lesson.pointsReward || prev.pointsReward,
          estimatedDuration: lesson.estimatedDuration || prev.estimatedDuration,
        }));
        setPdfDialog({ open: false, parsedData: null, summary: null });
        toast.success("Đã áp dụng nội dung từ Gemini AI!");
      }
      // Multiple lessons: save all to database
      else {
        const toastId = toast.loading(`Đang lưu ${finalData.lessons.length} bài học...`);

        await grammarAdminAPI.saveParsedLessons(topicId, finalData);

        toast.success(
          `Đã tạo ${finalData.lessons.length} bài học thành công!`,
          { id: toastId }
        );

        setPdfDialog({ open: false, parsedData: null, summary: null });
        navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
      }
    } catch (error) {
      console.error("Save parsed lessons error:", error);
      const errorMsg = error.response?.data?.message || error.message || "Có lỗi xảy ra";
      toast.error("Lỗi khi lưu: " + errorMsg);
    }
  };

  // ===== RENDER =====

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" color="blue-gray">
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
          className="opacity-60 cursor-pointer hover:opacity-100 transition-opacity"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        >
          Chủ đề ngữ pháp
        </Typography>
        <Typography
          className="opacity-60 cursor-pointer hover:opacity-100 transition-opacity"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
        >
          {topicInfo?.name || "Bài học"}
        </Typography>
        <Typography color="blue-gray">
          {isEdit ? "Chỉnh sửa" : "Tạo mới"}
        </Typography>
      </Breadcrumbs>

      {/* Header */}
      <Card className="border border-blue-gray-100 shadow-sm">
        <CardBody className="p-6">
          <div className="flex items-center space-x-3">
            <Button
              variant="outlined"
              size="sm"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
              className="border-gray-300 hover:bg-gray-50"
            >
              <ArrowLeftIcon className="h-4 w-4 mr-2" />
              Quay lại
            </Button>
            <div>
              <Typography variant="h4" color="blue-gray" className="font-bold">
                {isEdit ? "Chỉnh sửa bài học" : "Tạo bài học mới"}
              </Typography>
              <Typography
                variant="small"
                color="blue-gray"
                className="opacity-70 mt-1"
              >
                Chủ đề: {topicInfo?.name}
              </Typography>
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Form */}
      <Card className="border border-blue-gray-100 shadow-sm">
        <CardBody className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Info Component */}
            <LessonFormBasicInfo
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
            />

            {/* Content Editor Component (Only for THEORY) */}
            {formData.lessonType === "THEORY" && (
              <LessonContentEditor
                content={formData.content}
                error={errors.content}
                onChange={(value) => handleInputChange("content", value)}
                onFileUpload={handleFileUpload}
                isParsing={pdfParsing}
              />
            )}

            {/* 🆕 Preview nội dung (Optional) */}
            {formData.content && formData.lessonType === "THEORY" && (
              <LessonContentPreview content={formData.content} />
            )}

            {/* Settings Component */}
            <LessonFormSettings
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
            />

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-blue-gray-100">
              <Button
                variant="outlined"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
                disabled={submitting}
                className="flex items-center hover:bg-gray-50"
              >
                <XMarkIcon className="h-4 w-4 mr-2" />
                Hủy
              </Button>
              <Button
                type="submit"
                color="green"
                disabled={submitting}
                className="flex items-center shadow-lg hover:shadow-xl transition-shadow"
              >
                {submitting ? (
                  <Spinner className="h-4 w-4 mr-2" />
                ) : (
                  <CheckCircleIcon className="h-4 w-4 mr-2" />
                )}
                {isEdit ? "Cập nhật" : "Tạo bài học"}
              </Button>
            </div>
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