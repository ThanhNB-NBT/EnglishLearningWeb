// hook/useGrammarLessons.js
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { lessonService, topicService } from '../../services/grammarService'; // ✅ Import cả topicService
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST WITH PAGINATION ====================
export const useLessonList = (topicId) => {
  const [lessons, setLessons] = useState([]);
  const [topicInfo, setTopicInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  
  // Search & Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  // Pagination state
  const [pagination, setPagination] = useState({
    currentPage: 0,
    pageSize: 6,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const loadLessons = useCallback(async () => {
    if (!topicId) return;
    
    setLoading(true);
    try {
      const data = await lessonService.fetchPaginatedByTopic(topicId, {
        page: pagination.currentPage,
        size: pagination.pageSize,
        sort: 'orderIndex,asc',
      });

      if (data.content) {
        setLessons(data.content);
        setPagination({
          currentPage: data.pagination.currentPage,
          pageSize: data.pagination.pageSize,
          totalElements: data.pagination.totalElements,
          totalPages: data.pagination.totalPages,
          hasNext: data.pagination.hasNext,
          hasPrevious: data.pagination.hasPrevious,
        });
      } else {
        setLessons(data || []);
      }
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách bài học');
      console.error('Load lessons error:', error);
    } finally {
      setLoading(false);
    }
  }, [topicId, pagination.currentPage, pagination.pageSize]);

  // ✅ CLEANED: Dùng topicService thay vì grammarAdminAPI
  const loadTopicInfo = useCallback(async () => {
    if (!topicId) return;
    
    try {
      const topics = await topicService.fetchAll();
      const topic = topics.find(t => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error('Load topic info error:', error);
    }
  }, [topicId]);

  useEffect(() => {
    loadLessons();
    loadTopicInfo();
  }, [loadLessons, loadTopicInfo]);

  const handlePageChange = (newPage) => {
    setPagination((prev) => ({ ...prev, currentPage: newPage }));
  };

  const handlePageSizeChange = (newSize) => {
    setPagination((prev) => ({ ...prev, pageSize: newSize, currentPage: 0 }));
  };

  const deleteLesson = async (lessonId) => {
    try {
      await lessonService.delete(lessonId);
      await loadLessons();
    } catch (error) {
      console.error('Delete lesson error:', error);
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
    setFilterStatus('');
  };

  const filteredLessons = lessons.filter((lesson) => {
    const matchSearch =
      !searchTerm ||
      lesson.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      lesson.content?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchType = !filterType || lesson.lessonType === filterType;
    const matchStatus =
      !filterStatus || (filterStatus === 'active' ? lesson.isActive : !lesson.isActive);

    return matchSearch && matchType && matchStatus;
  });

  return {
    lessons: filteredLessons,
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
    reload: loadLessons,
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
  const [allLessons, setAllLessons] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pdfParsing, setPdfParsing] = useState(false);
  const [errors, setErrors] = useState({});
  const [hasChanges, setHasChanges] = useState(false);

  const [manualOrderIndex, setManualOrderIndex] = useState(false);
  const [reorderDialog, setReorderDialog] = useState({
    open: false,
    affectedLessons: [],
    loading: false,
  });

  const [pdfDialog, setPdfDialog] = useState({
    open: false,
    parsedData: null,
    summary: null,
  });

  const validateForm = useCallback(() => {
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
  }, [formData]);

  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      validateForm();
    }
  }, [formData, errors, validateForm]);

  // ✅ CLEANED: Dùng topicService
  const loadTopicInfo = useCallback(async () => {
    try {
      const topics = await topicService.fetchAll();
      const topic = topics.find((t) => t.id === parseInt(topicId));
      setTopicInfo(topic);
    } catch (error) {
      console.error('Load topic info error:', error);
      toast.error('Lỗi khi lấy thông tin chủ đề');
    }
  }, [topicId]);

  const loadAllLessons = useCallback(async () => {
    try {
      const lessons = await lessonService.fetchByTopic(topicId);
      setAllLessons(lessons);
    } catch (error) {
      console.error('Load all lessons error:', error);
    }
  }, [topicId]);

  const getNextOrderIndex = useCallback(async () => {
    try {
      const nextOrder = await lessonService.getNextOrderIndex(topicId);
      console.log('✅ Next order index for lesson:', nextOrder);
      setFormData((prev) => ({ ...prev, orderIndex: nextOrder }));
    } catch (error) {
      console.error('Get next order index error:', error);
    }
  }, [topicId]);

  const loadLessonData = useCallback(async () => {
    setLoading(true);
    try {
      const lesson = await lessonService.fetchById(lessonId);

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
    loadAllLessons();
    if (isEdit) {
      loadLessonData();
    } else {
      getNextOrderIndex();
    }
  }, [isEdit, loadTopicInfo, loadAllLessons, loadLessonData, getNextOrderIndex]);

  useEffect(() => {
    if (originalData) {
      const hasDataChanged = JSON.stringify(formData) !== JSON.stringify(originalData);
      setHasChanges(hasDataChanged);
    }
  }, [formData, originalData]);

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const checkReorderNeeded = useCallback(() => {
    if (!isEdit && !manualOrderIndex) {
      return false;
    }
    
    if (isEdit && originalData && formData.orderIndex === originalData.orderIndex) {
      return false;
    }
    
    const targetOrder = formData.orderIndex;
    
    const affected = allLessons.filter(lesson => {
      if (isEdit && lesson.id === parseInt(lessonId)) {
        return false;
      }
      
      return lesson.orderIndex >= targetOrder;
    });

    return affected.length > 0 ? affected : false;
  }, [formData.orderIndex, allLessons, manualOrderIndex, isEdit, lessonId, originalData]);

  // ✅ NOTE: Giữ lại grammarAdminAPI ONLY cho reorderLessons vì chưa có trong service
  const handleReorder = async () => {
    const affectedLessons = checkReorderNeeded();
    
    if (!affectedLessons) {
      return true;
    }

    // ⚠️ Import grammarAdminAPI only when needed
    const { grammarAdminAPI } = await import('../../api/modules/grammar.api');

    return new Promise((resolve) => {
      setReorderDialog({
        open: true,
        affectedLessons,
        loading: false,
        onConfirm: async () => {
          setReorderDialog(prev => ({ ...prev, loading: true }));
          
          try {
            await grammarAdminAPI.reorderLessons(topicId, {
              insertPosition: formData.orderIndex,
              excludeLessonId: isEdit ? parseInt(lessonId) : null
            });
            
            toast.success('Đã sắp xếp lại thứ tự các bài học');
            setReorderDialog({ open: false, affectedLessons: [], loading: false });
            
            await loadAllLessons();
            
            resolve(true);
          } catch (error) {
            console.error('Reorder error:', error);
            toast.error('Lỗi khi sắp xếp lại: ' + (error.response?.data?.message || error.message));
            setReorderDialog(prev => ({ ...prev, loading: false }));
            resolve(false);
          }
        },
        onCancel: () => {
          setReorderDialog({ open: false, affectedLessons: [], loading: false });
          resolve(false);
        }
      });
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error('Vui lòng kiểm tra lại thông tin nhập vào');
      return;
    }

    const needsReorder = checkReorderNeeded();
    
    if (needsReorder) {
      const reorderSuccess = await handleReorder();
      if (!reorderSuccess) {
        return;
      }
    }

    setSubmitting(true);
    try {
      if (isEdit) {
        await lessonService.update(lessonId, formData);
      } else {
        await lessonService.create(formData);
      }
      navigate(ADMIN_ROUTES.GRAMMAR_LESSONS(topicId));
    } catch (error) {
      console.error('Submit lesson error:', error);
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

  // ✅ NOTE: Giữ lại grammarAdminAPI cho PDF parsing & saveParsedLessons
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const isPDF = file.type === 'application/pdf';
    const isDOCX = file.name.toLowerCase().endsWith('.docx');

    if (!isPDF && !isDOCX) {
      toast.error('Chỉ hỗ trợ file PDF và DOCX');
      return;
    }

    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      toast.error('File không được vượt quá 10MB');
      return;
    }

    setPdfParsing(true);
    const toastId = toast.loading('🤖 Gemini AI đang phân tích file...');

    try {
      // ⚠️ Dynamic import for PDF features
      const { grammarAdminAPI } = await import('../../api/modules/grammar.api');
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
      } else {
        const toastId = toast.loading(`Đang lưu ${finalData.lessons.length} bài học...`);

        // ⚠️ Dynamic import
        const { grammarAdminAPI } = await import('../../api/modules/grammar.api');
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
    getCompletionPercentage,
    manualOrderIndex,
    setManualOrderIndex,
    reorderDialog,
    totalLessons: allLessons.length,
    willCauseReorder: !!checkReorderNeeded(),
  };
};