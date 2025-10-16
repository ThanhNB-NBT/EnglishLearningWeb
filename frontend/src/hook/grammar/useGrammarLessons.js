// hook/useGrammarLessons.js
// Hook tổng hợp cho quản lý Grammar Lessons (List, Create, Edit)
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { grammarAdminAPI } from '../../api/modules/grammar.api';
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST ====================
export const useLessonList = (topicId) => {
  const [lessons, setLessons] = useState([]);
  const [filteredLessons, setFilteredLessons] = useState([]);
  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  const loadLessons = useCallback(async () => {
    if (!topicId) return;
    
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
  }, [topicId]);

  const loadTopicInfo = useCallback(async () => {
    if (!topicId) return;
    
    try {
      const response = await grammarAdminAPI.getAllTopics();
      const topic = response.data.data.find(t => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error('Load topic info error:', error);
    }
  }, [topicId]);

  const applyFilters = useCallback(() => {
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
  }, [lessons, searchTerm, filterType, filterStatus]);

  useEffect(() => {
    loadLessons();
    loadTopicInfo();
  }, [loadLessons, loadTopicInfo]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const deleteLesson = async (lessonId) => {
    try {
      await grammarAdminAPI.deleteLesson(lessonId);
      setLessons(lessons.filter(lesson => lesson.id !== lessonId));
      toast.success('Xóa bài học thành công!');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Lỗi khi xóa bài học');
      console.error('Delete lesson error:', error);
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
    setFilterStatus('');
  };

  return {
    lessons,
    filteredLessons,
    topicInfo,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    filterStatus,
    setFilterStatus,
    loadLessons,
    deleteLesson,
    resetFilters
  };
};

// ==================== HOOK CHO CREATE/EDIT ====================
export const useLessonForm = (topicId, lessonId = null) => {
  const navigate = useNavigate();
  const isEdit = !!lessonId;
  
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
  
  const [originalData, setOriginalData] = useState(null);
  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pdfParsing, setPdfParsing] = useState(false);
  const [errors, setErrors] = useState({});
  const [hasChanges, setHasChanges] = useState(false);

  // PDF Dialog state
  const [pdfDialog, setPdfDialog] = useState({
    open: false,
    parsedData: null,
    summary: null,
  });

  // Load topic info
  const loadTopicInfo = useCallback(async () => {
    try {
      const response = await grammarAdminAPI.getAllTopics();
      const topic = response.data.data.find((t) => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error('Load topic info error:', error);
      toast.error('Lỗi khi lấy thông tin chủ đề');
    }
  }, [topicId]);

  // Get next order index for new lesson
  const getNextOrderIndex = useCallback(async () => {
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId);
      const lessons = response.data.data || [];
      const maxOrder = lessons.length > 0 
        ? Math.max(...lessons.map((l) => l.orderIndex)) 
        : 0;
      setFormData((prev) => ({ ...prev, orderIndex: maxOrder + 1 }));
    } catch (error) {
      console.error('Get next order index error:', error);
    }
  }, [topicId]);

  // Load lesson data for edit mode
  const loadLessonData = useCallback(async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getLessonDetail(lessonId);
      const lesson = response.data.data;

      if (lesson) {
        const lessonData = {
          topicId: lesson.topicId,
          title: lesson.title || "",
          lessonType: lesson.lessonType || "THEORY",
          content: lesson.content || "",
          orderIndex: lesson.orderIndex || 1,
          pointsReward: lesson.pointsReward || 10,
          estimatedDuration: lesson.estimatedDuration || 180,
          isActive: lesson.isActive !== undefined ? lesson.isActive : true,
        };
        
        setFormData(lessonData);
        setOriginalData(lessonData);
      }
    } catch (error) {
      console.error('Load lesson data error:', error);
      toast.error('Lỗi khi lấy thông tin bài học');
      navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
    } finally {
      setLoading(false);
    }
  }, [lessonId, topicId, navigate]);

  useEffect(() => {
    loadTopicInfo();
    if (isEdit) {
      loadLessonData();
    } else {
      getNextOrderIndex();
    }
  }, [isEdit, loadTopicInfo, loadLessonData, getNextOrderIndex]);

  useEffect(() => {
    if (originalData) {
      const hasDataChanged = JSON.stringify(formData) !== JSON.stringify(originalData);
      setHasChanges(hasDataChanged);
    }
  }, [formData, originalData]);

  // Validation
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

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error('Vui lòng kiểm tra lại thông tin nhập vào');
      return;
    }

    setSubmitting(true);
    try {
      if (isEdit) {
        await grammarAdminAPI.updateLesson(lessonId, formData);
        toast.success('Cập nhật bài học thành công!');
      } else {
        await grammarAdminAPI.createLesson(formData);
        toast.success('Tạo bài học thành công!');
      }
      navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
    } catch (error) {
      console.error('Submit lesson error:', error);
      toast.error(error.response?.data?.message || 'Có lỗi xảy ra');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    if (isEdit && hasChanges) {
      if (window.confirm('Bạn có chắc muốn hủy? Các thay đổi sẽ bị mất.')) {
        navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
      }
    } else {
      navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
    }
  };

  // PDF Parsing with Gemini AI
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const isPDF = file.type === 'application/pdf';
    const isDOCX = file.name.toLowerCase().endsWith('.docx');

    if (!isPDF && !isDOCX) {
      toast.error('Chỉ hỗ trợ file PDF và DOCX');
      return;
    }

    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      toast.error('File không được vượt quá 10MB');
      return;
    }

    setPdfParsing(true);
    const toastId = toast.loading('🤖 Gemini AI đang phân tích file...');

    try {
      const response = await grammarAdminAPI.parsePDF(topicId, file);
      const result = response.data.data;

      setPdfDialog({
        open: true,
        parsedData: result.parsedData,
        summary: result.summary,
      });

      toast.success('Phân tích thành công với Gemini AI!', { id: toastId });
    } catch (error) {
      console.error('Gemini parsing error:', error);
      const errorMsg = error.response?.data?.message || 'Lỗi khi phân tích file';
      toast.error(errorMsg, { id: toastId });
    } finally {
      setPdfParsing(false);
      event.target.value = '';
    }
  };

  const handleUseParsedContent = async (finalData) => {
    if (!finalData || !finalData.lessons || finalData.lessons.length === 0) {
      toast.error('Không có dữ liệu để sử dụng');
      return;
    }

    try {
      // Single lesson: use directly in form
      if (finalData.lessons.length === 1) {
        const lesson = finalData.lessons[0];
        setFormData((prev) => ({
          ...prev,
          title: lesson.title,
          content: lesson.content,
          lessonType: lesson.lessonType || 'THEORY',
          orderIndex: lesson.orderIndex || prev.orderIndex,
          pointsReward: lesson.pointsReward || prev.pointsReward,
          estimatedDuration: lesson.estimatedDuration || prev.estimatedDuration,
        }));
        setPdfDialog({ open: false, parsedData: null, summary: null });
        toast.success('Đã áp dụng nội dung từ Gemini AI!');
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
      console.error('Save parsed lessons error:', error);
      const errorMsg = error.response?.data?.message || error.message || 'Có lỗi xảy ra';
      toast.error('Lỗi khi lưu: ' + errorMsg);
    }
  };

  const getCompletionPercentage = () => {
    const requiredFields = ['title', 'lessonType'];
    const optionalFields = ['content', 'orderIndex', 'pointsReward', 'estimatedDuration'];

    let completed = 0;
    let total = requiredFields.length + optionalFields.length;

    requiredFields.forEach((field) => {
      if (formData[field] && formData[field].toString().trim()) completed++;
    });

    optionalFields.forEach((field) => {
      if (formData[field] && formData[field].toString().trim()) completed++;
    });

    return Math.round((completed / total) * 100);
  };

  return {
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
  };
};