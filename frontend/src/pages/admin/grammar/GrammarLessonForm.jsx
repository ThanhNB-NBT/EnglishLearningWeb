import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { grammarAdminAPI } from "../../../api";
import { ADMIN_ROUTES } from "../../../constants/routes";
import * as mammoth from "mammoth";
import {
  extractTextFromPDF,
  extractTextFromDOCX,
  analyzeContent,
} from "../../../utils/pdfReader";
import PDFLessonCreatorDialog from "../../../components/PDFLessonCreatorDialog";
import PDFUploadConfig from "../../../components/PDFUploadConfig";
import {
  Button,
  Card,
  CardBody,
  Typography,
  Input,
  Textarea,
  Select,
  Option,
  Switch,
  Spinner,
  Breadcrumbs,
  Progress,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  DocumentArrowUpIcon,
  CheckCircleIcon,
  AdjustmentsHorizontalIcon,
} from "@heroicons/react/24/outline";
import toast from "react-hot-toast";

const GrammarLessonForm = () => {
  const { topicId, lessonId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!lessonId;

  const [showPdfConfig, setShowPdfConfig] = useState(false);
  const [pdfConfig, setPdfConfig] = useState({
    skipFirstPages: 2,
    skipLastPages: 0,
    removeHeaders: true,
    removeFooters: true,
    minSectionLength: 50,
    minQuestionLength: 20,
  });

  const [formData, setFormData] = useState({
    topicId: parseInt(topicId),
    title: "",
    lessonType: "THEORY",
    content: "",
    orderIndex: 1,
    pointsRequired: 0,
    pointsReward: 10,
    isActive: true,
  });

  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pdfProcessing, setPdfProcessing] = useState(false);
  const [pdfDialog, setPdfDialog] = useState({
    open: false,
    analyzedContent: null,
  });

  const [errors, setErrors] = useState({});

  useEffect(() => {
    loadTopicInfo();
    if (isEdit) {
      loadLessonData();
    } else {
      getNextOrderIndex();
    }
  }, [topicId, lessonId]);

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
      const response = await grammarAdminAPI.getLessonsByTopic(topicId);
      const lesson = response.data.data.find(
        (l) => l.id === parseInt(lessonId)
      );
      if (lesson) {
        setFormData({
          topicId: lesson.topicId,
          title: lesson.title || "",
          lessonType: lesson.lessonType || "THEORY",
          content: lesson.content || "",
          orderIndex: lesson.orderIndex || 1,
          pointsRequired: lesson.pointsRequired || 0,
          pointsReward: lesson.pointsReward || 10,
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
    }

    if (formData.title.length > 200) {
      newErrors.title = "Tiêu đề không được vượt quá 200 ký tự";
    }

    if (formData.orderIndex < 1) {
      newErrors.orderIndex = "Thứ tự phải lớn hơn 0";
    }

    if (formData.pointsRequired < 0) {
      newErrors.pointsRequired = "Điểm yêu cầu không được âm";
    }

    if (formData.pointsReward <= 0) {
      newErrors.pointsReward = "Điểm thưởng phải lớn hơn 0";
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
      const errorMessage =
        error.response?.data?.message || error.message || "Có lỗi xảy ra";
      toast.error(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const isPDF = file.type === "application/pdf";
    const isDOCX = file.name.toLowerCase().endsWith(".docx");

    if (!isPDF && !isDOCX) {
      toast.error("Chỉ hỗ trợ file PDF và DOCX");
      return;
    }

    setPdfProcessing(true);

    try {
      console.log("Processing file:", file.name, "Size:", file.size);

      const arrayBuffer = await file.arrayBuffer();
      let extractedText = "";

      if (isPDF) {
        console.log("Extracting PDF with config:", pdfConfig);
        extractedText = await extractTextFromPDF(arrayBuffer, {
          skipFirstPages: pdfConfig.skipFirstPages,
          skipLastPages: pdfConfig.skipLastPages,
          removeHeaders: pdfConfig.removeHeaders,
          removeFooters: pdfConfig.removeFooters,
        });
      } else if (isDOCX) {
        extractedText = await extractTextFromDOCX(arrayBuffer, mammoth);
      }

      console.log("Extracted text length:", extractedText.length);
      console.log("First 500 chars:", extractedText.substring(0, 500));

      if (!extractedText || extractedText.trim().length < 50) {
        toast.error(
          "Không tìm thấy đủ nội dung trong file. Vui lòng thử điều chỉnh cấu hình."
        );
        setShowPdfConfig(true); // SỬA: Dùng setShowPdfConfig thay vì setShowPdfConfig
        return;
      }

      const analyzedContent = analyzeContent(extractedText, {
        minSectionLength: pdfConfig.minSectionLength,
        minQuestionLength: pdfConfig.minQuestionLength,
      });

      console.log("Analyzed content:", {
        sections: analyzedContent.sections.length,
        exercises: analyzedContent.exercises.length,
      });

      if (analyzedContent.sections.length === 0) {
        toast.error(
          "Không tìm thấy nội dung phù hợp. Thử điều chỉnh cấu hình hoặc kiểm tra định dạng file."
        );
        setShowPdfConfig(true);
        return;
      }

      setPdfDialog({
        open: true,
        analyzedContent: analyzedContent,
      });

      console.log("Dialog state after set:", {
        open: true,
        sectionsCount: analyzedContent.sections.length,
        exercisesCount: analyzedContent.exercises.length,
      });

      toast.success(
        `Phân tích thành công: ${analyzedContent.sections.length} phần lý thuyết, ${analyzedContent.exercises.length} câu hỏi`
      );
    } catch (error) {
      console.error("File processing error:", error);
      toast.error(error.message || "Lỗi khi xử lý file. Vui lòng thử lại.");
    } finally {
      setPdfProcessing(false);
      event.target.value = "";
    }
  };

  const handleCreateLessonsFromPDF = (selectedLessons) => {
    if (selectedLessons.length === 0) {
      toast.error("Vui lòng chọn ít nhất một phần để tạo bài học");
      return;
    }

    // Get the first selected lesson's content
    const firstLesson = selectedLessons[0];
    const section = firstLesson.section;

    // Format content from section
    let content = "";
    section.content.forEach((item) => {
      if (typeof item === "string") {
        content += item + "\n\n";
      } else if (item.subsection) {
        content += `${item.subsection}\n`;
        item.details.forEach((detail) => {
          content += `  ${detail}\n`;
        });
        content += "\n";
      }
    });

    // Update form with extracted content
    setFormData((prev) => ({
      ...prev,
      title: section.title,
      content: content.trim(),
    }));

    setPdfDialog({ open: false, analyzedContent: null });

    toast.success(`Đã thêm nội dung từ file vào bài học`);
  };

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
          className="opacity-60 cursor-pointer hover:opacity-100"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_TOPICS)}
        >
          Chủ đề ngữ pháp
        </Typography>
        <Typography
          className="opacity-60 cursor-pointer hover:opacity-100"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
        >
          {topicInfo?.name || "Bài học"}
        </Typography>
        <Typography color="blue-gray">
          {isEdit ? "Chỉnh sửa bài học" : "Tạo bài học mới"}
        </Typography>
      </Breadcrumbs>

      {/* Header */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="flex items-center space-x-3">
            <Button
              variant="outlined"
              size="sm"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
              className="border-gray-300"
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
                className="opacity-70"
              >
                Chủ đề: {topicInfo?.name}
              </Typography>
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Form */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Information */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Typography
                  variant="small"
                  color="blue-gray"
                  className="mb-2 font-medium"
                >
                  Tiêu đề bài học *
                </Typography>
                <Input
                  value={formData.title}
                  onChange={(e) => handleInputChange("title", e.target.value)}
                  placeholder="Nhập tiêu đề bài học"
                  error={!!errors.title}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                />
                {errors.title && (
                  <Typography variant="small" color="red" className="mt-1">
                    {errors.title}
                  </Typography>
                )}
              </div>

              <div>
                <Typography
                  variant="small"
                  color="blue-gray"
                  className="mb-2 font-medium"
                >
                  Loại bài học *
                </Typography>
                <Select
                  value={formData.lessonType}
                  onChange={(val) => handleInputChange("lessonType", val)}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                >
                  <Option value="THEORY">Lý thuyết</Option>
                  <Option value="PRACTICE">Thực hành</Option>
                </Select>
              </div>
            </div>

            {/* Content Section - Only show for THEORY */}
            {formData.lessonType === "THEORY" && (
              <div>
                <div className="flex justify-between items-center mb-2">
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="font-medium"
                  >
                    Nội dung bài học *
                  </Typography>
                  <div className="flex space-x-2">
                    <Button
                      size="sm"
                      variant="outlined"
                      onClick={() => setShowPdfConfig(true)}
                      className="border-gray-500 text-gray-500"
                    >
                      <AdjustmentsHorizontalIcon className="h-4 w-4 mr-2" />
                      Cấu hình
                    </Button>
                    <input
                      type="file"
                      id="pdf-upload"
                      accept=".pdf,.docx"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                    <Button
                      size="sm"
                      variant="outlined"
                      onClick={() =>
                        document.getElementById("pdf-upload").click()
                      }
                      disabled={pdfProcessing}
                      className="border-blue-500 text-blue-500"
                    >
                      <DocumentArrowUpIcon className="h-4 w-4 mr-2" />
                      {pdfProcessing ? "Đang xử lý..." : "Tải file PDF/DOCX"}
                    </Button>
                  </div>
                </div>
                <Textarea
                  value={formData.content}
                  onChange={(e) => handleInputChange("content", e.target.value)}
                  placeholder="Nhập nội dung lý thuyết của bài học..."
                  rows={10}
                  error={!!errors.content}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                />
                {errors.content && (
                  <Typography variant="small" color="red" className="mt-1">
                    {errors.content}
                  </Typography>
                )}
                {pdfProcessing && (
                  <div className="mt-2">
                    <Progress value={50} color="blue" className="h-2" />
                    <Typography variant="small" color="blue" className="mt-1">
                      Đang xử lý file, vui lòng đợi...
                    </Typography>
                  </div>
                )}
              </div>
            )}

            {/* Settings */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <Typography
                  variant="small"
                  color="blue-gray"
                  className="mb-2 font-medium"
                >
                  Thứ tự *
                </Typography>
                <Input
                  type="number"
                  value={formData.orderIndex}
                  onChange={(e) =>
                    handleInputChange(
                      "orderIndex",
                      parseInt(e.target.value) || 1
                    )
                  }
                  min="1"
                  error={!!errors.orderIndex}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                />
                {errors.orderIndex && (
                  <Typography variant="small" color="red" className="mt-1">
                    {errors.orderIndex}
                  </Typography>
                )}
              </div>

              <div>
                <Typography
                  variant="small"
                  color="blue-gray"
                  className="mb-2 font-medium"
                >
                  Điểm yêu cầu *
                </Typography>
                <Input
                  type="number"
                  value={formData.pointsRequired}
                  onChange={(e) =>
                    handleInputChange(
                      "pointsRequired",
                      parseInt(e.target.value) || 0
                    )
                  }
                  min="0"
                  error={!!errors.pointsRequired}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                />
                {errors.pointsRequired && (
                  <Typography variant="small" color="red" className="mt-1">
                    {errors.pointsRequired}
                  </Typography>
                )}
              </div>

              <div>
                <Typography
                  variant="small"
                  color="blue-gray"
                  className="mb-2 font-medium"
                >
                  Điểm thưởng *
                </Typography>
                <Input
                  type="number"
                  value={formData.pointsReward}
                  onChange={(e) =>
                    handleInputChange(
                      "pointsReward",
                      parseInt(e.target.value) || 10
                    )
                  }
                  min="1"
                  error={!!errors.pointsReward}
                  className="!border-blue-gray-200 focus:!border-blue-500"
                />
                {errors.pointsReward && (
                  <Typography variant="small" color="red" className="mt-1">
                    {errors.pointsReward}
                  </Typography>
                )}
              </div>
            </div>

            {/* Active Status */}
            <div className="flex items-center space-x-3">
              <Switch
                checked={formData.isActive}
                onChange={(e) =>
                  handleInputChange("isActive", e.target.checked)
                }
                color="green"
              />
              <Typography variant="small" color="blue-gray">
                Bài học hoạt động
              </Typography>
            </div>

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-blue-gray-100">
              <Button
                variant="outlined"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId))}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                color="green"
                disabled={submitting}
                className="flex items-center"
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

      {/* PDF Upload Config Dialog */}
      <PDFUploadConfig
        open={showPdfConfig}
        onClose={() => setShowPdfConfig(false)}
        onConfirm={(config) => {
          setPdfConfig(config);
          setShowPdfConfig(false); // Đóng dialog
        }}
      />

      {/* PDF Lesson Creator Dialog */}
      <PDFLessonCreatorDialog
        open={pdfDialog.open}
        analyzedContent={pdfDialog.analyzedContent}
        onClose={() => setPdfDialog({ open: false, analyzedContent: null })}
        onCreateLessons={handleCreateLessonsFromPDF}
      />
    </div>
  );
};

export default GrammarLessonForm;
