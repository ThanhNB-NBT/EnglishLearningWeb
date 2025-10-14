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
  Alert,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  XMarkIcon,
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import toast from "react-hot-toast";

// Import child components
import QuestionFormBasicInfo from "../../../components/grammar/QuestionFormBasicInfo";
import QuestionAnswerSection from "../../../components/grammar/QuestionAnswerSection";
import QuestionFormSettings from "../../../components/grammar/QuestionFormSettings";

/**
 * Main Form Container - Tạo/Chỉnh sửa Question
 */
const GrammarQuestionForm = () => {
  const { lessonId, questionId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!questionId;

  // Form state
  const [formData, setFormData] = useState({
    lessonId: parseInt(lessonId),
    questionText: "",
    questionType: "MULTIPLE_CHOICE",
    correctAnswer: "",
    explanation: "",
    points: 5,
    orderIndex: 1,
  });

  const [options, setOptions] = useState([
    { optionText: "", isCorrect: false, orderIndex: 1 },
    { optionText: "", isCorrect: false, orderIndex: 2 },
  ]);

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  // ===== LIFECYCLE =====

  useEffect(() => {
    if (isEdit) {
      loadQuestionData();
    } else {
      getNextOrderIndex();
    }
  }, [lessonId, questionId]);

  // ===== DATA LOADING =====

  const getNextOrderIndex = async () => {
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      const questions = response.data.data || [];
      const maxOrder = questions.length > 0 ? Math.max(...questions.map((q) => q.orderIndex || 0)) : 0;
      setFormData((prev) => ({ ...prev, orderIndex: maxOrder + 1 }));
    } catch (error) {
      console.error("Get next order index error:", error);
    }
  };

  const loadQuestionData = async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      const question = response.data.data.find((q) => q.id === parseInt(questionId));

      if (question) {
        setFormData({
          lessonId: question.lessonId,
          questionText: question.questionText || "",
          questionType: question.questionType || "MULTIPLE_CHOICE",
          correctAnswer: question.correctAnswer || "",
          explanation: question.explanation || "",
          points: question.points || 5,
          orderIndex: question.orderIndex || 1,
        });

        if (question.options && question.options.length > 0) {
          setOptions(question.options);
        }
      }
    } catch (error) {
      console.error("Load question data error:", error);
      toast.error("Lỗi khi lấy thông tin câu hỏi");
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

    // Reset options when changing question type
    if (field === 'questionType' && value !== 'MULTIPLE_CHOICE') {
      setOptions([]);
    } else if (field === 'questionType' && value === 'MULTIPLE_CHOICE' && options.length === 0) {
      setOptions([
        { optionText: "", isCorrect: false, orderIndex: 1 },
        { optionText: "", isCorrect: false, orderIndex: 2 },
      ]);
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.questionText.trim()) {
      newErrors.questionText = "Nội dung câu hỏi không được để trống";
    } else if (formData.questionText.length > 500) {
      newErrors.questionText = "Nội dung không được vượt quá 500 ký tự";
    }

    if (formData.questionType === 'MULTIPLE_CHOICE') {
      if (options.length < 2) {
        newErrors.options = "Câu hỏi trắc nghiệm cần ít nhất 2 lựa chọn";
      } else if (options.some(opt => !opt.optionText.trim())) {
        newErrors.options = "Tất cả lựa chọn phải có nội dung";
      } else if (!options.some(opt => opt.isCorrect)) {
        newErrors.options = "Phải có ít nhất 1 đáp án đúng";
      }
    } else {
      if (!formData.correctAnswer.trim()) {
        newErrors.correctAnswer = "Đáp án đúng không được để trống";
      }
    }

    if (formData.points < 1 || formData.points > 100) {
      newErrors.points = "Điểm số phải từ 1-100";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin");
      return;
    }

    setSubmitting(true);
    try {
      const submitData = {
        ...formData,
        options: formData.questionType === 'MULTIPLE_CHOICE' ? options : null,
      };

      if (isEdit) {
        await grammarAdminAPI.updateQuestion(questionId, submitData);
        toast.success("Cập nhật câu hỏi thành công!");
      } else {
        await grammarAdminAPI.createQuestion(submitData);
        toast.success("Tạo câu hỏi thành công!");
      }

      navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId));
    } catch (error) {
      console.error("Submit question error:", error);
      toast.error(error.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setSubmitting(false);
    }
  };

  // ===== RENDER =====

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-purple-500" />
        <Typography variant="paragraph" color="blue-gray">
          Đang tải thông tin câu hỏi...
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
          onClick={() => navigate(-1)}
        >
          Bài học
        </Typography>
        <Typography
          className="opacity-60 cursor-pointer hover:opacity-100"
          onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId))}
        >
          Câu hỏi
        </Typography>
        <Typography color="blue-gray">
          {isEdit ? "Chỉnh sửa" : "Tạo mới"}
        </Typography>
      </Breadcrumbs>

      {/* Header */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="flex items-center space-x-3">
            <Button
              variant="outlined"
              size="sm"
              onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId))}
              className="border-gray-300"
            >
              <ArrowLeftIcon className="h-4 w-4 mr-2" />
              Quay lại
            </Button>
            <div>
              <Typography variant="h4" color="blue-gray" className="font-bold">
                {isEdit ? "Chỉnh sửa câu hỏi" : "Tạo câu hỏi mới"}
              </Typography>
              <Typography variant="small" color="blue-gray" className="opacity-70">
                Bài học thực hành
              </Typography>
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Info Alert */}
      <Alert color="blue" icon={<InformationCircleIcon className="h-5 w-5" />}>
        <Typography variant="small" className="font-medium mb-1">
          💡 Hướng dẫn tạo câu hỏi
        </Typography>
        <Typography variant="small" className="opacity-80">
          • <strong>Trắc nghiệm:</strong> Tạo 2-4 lựa chọn và đánh dấu đáp án đúng<br/>
          • <strong>Điền từ/Dịch câu:</strong> Nhập đáp án trực tiếp, có thể nhiều đáp án đúng (cách nhau bởi |)<br/>
          • <strong>Giải thích:</strong> Giúp học viên hiểu tại sao đáp án đúng
        </Typography>
      </Alert>

      {/* Form */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Info Component */}
            <QuestionFormBasicInfo
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
            />

            {/* Answer Section Component */}
            <QuestionAnswerSection
              questionType={formData.questionType}
              correctAnswer={formData.correctAnswer}
              options={options}
              errors={errors}
              onCorrectAnswerChange={(value) => handleInputChange('correctAnswer', value)}
              onOptionsChange={setOptions}
            />

            {/* Settings Component */}
            <QuestionFormSettings
              formData={formData}
              errors={errors}
              onChange={handleInputChange}
            />

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-blue-gray-100">
              <Button
                variant="outlined"
                onClick={() => navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId))}
                disabled={submitting}
                className="flex items-center"
              >
                <XMarkIcon className="h-4 w-4 mr-2" />
                Hủy
              </Button>
              <Button
                type="submit"
                color="purple"
                disabled={submitting}
                className="flex items-center"
              >
                {submitting ? (
                  <Spinner className="h-4 w-4 mr-2" />
                ) : (
                  <CheckCircleIcon className="h-4 w-4 mr-2" />
                )}
                {isEdit ? "Cập nhật" : "Tạo câu hỏi"}
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
};

export default GrammarQuestionForm;