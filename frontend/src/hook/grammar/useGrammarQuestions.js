// hook/useGrammarQuestions.js
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { questionService } from '../../services/grammarService';
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST WITH PAGINATION ====================
export const useQuestionList = (lessonId) => {
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // Search & Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [selectedQuestions, setSelectedQuestions] = useState([]);

  // Pagination state
  const [pagination, setPagination] = useState({
    currentPage: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const loadQuestions = useCallback(async () => {
    if (!lessonId) return;
    
    setLoading(true);
    try {
      const data = await questionService.fetchPaginatedByLesson(lessonId, {
        page: pagination.currentPage,
        size: pagination.pageSize,
        sort: 'orderIndex,asc',
      });

      if (data.content) {
        setQuestions(data.content);
        setPagination({
          currentPage: data.pagination.currentPage,
          pageSize: data.pagination.pageSize,
          totalElements: data.pagination.totalElements,
          totalPages: data.pagination.totalPages,
          hasNext: data.pagination.hasNext,
          hasPrevious: data.pagination.hasPrevious,
        });
      } else {
        setQuestions(data || []);
      }
      
      setSelectedQuestions([]);
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách câu hỏi');
      console.error('Load questions error:', error);
    } finally {
      setLoading(false);
    }
  }, [lessonId, pagination.currentPage, pagination.pageSize]);

  useEffect(() => {
    loadQuestions();
  }, [loadQuestions]);

  const handlePageChange = (newPage) => {
    setPagination((prev) => ({ ...prev, currentPage: newPage }));
  };

  const handlePageSizeChange = (newSize) => {
    setPagination((prev) => ({ ...prev, pageSize: newSize, currentPage: 0 }));
  };

  const deleteQuestion = async (questionId) => {
    try {
      await questionService.delete(questionId);
      await loadQuestions();
    } catch (error) {
      console.error('Delete question error:', error);
      throw error;
    }
  };

  const bulkDeleteQuestions = async (questionIds) => {
    try {
      await questionService.bulkDelete(questionIds);
      await loadQuestions();
      setSelectedQuestions([]);
    } catch (error) {
      console.error('Bulk delete error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi xóa nhiều câu hỏi');
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
  };

  const filteredQuestions = questions.filter((question) => {
    const matchSearch =
      !searchTerm ||
      question.questionText?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      question.explanation?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchType = !filterType || question.questionType === filterType;

    return matchSearch && matchType;
  });

  return {
    questions: filteredQuestions,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    selectedQuestions,
    setSelectedQuestions,
    pagination,
    handlePageChange,
    handlePageSizeChange,
    deleteQuestion,
    bulkDeleteQuestions,
    resetFilters,
    reload: loadQuestions,
  };
};

// ==================== HOOK CHO CREATE/EDIT ====================
export const useQuestionForm = (lessonId, questionId = null) => {
  const navigate = useNavigate();
  const isEdit = !!questionId;

  const [formData, setFormData] = useState({
    lessonId: parseInt(lessonId),
    questionText: '',
    questionType: 'MULTIPLE_CHOICE',
    correctAnswer: '',
    explanation: '',
    points: 5,
    orderIndex: 1,
  });

  const [options, setOptions] = useState([
    { optionText: '', isCorrect: false, orderIndex: 1 },
    { optionText: '', isCorrect: false, orderIndex: 2 },
    { optionText: '', isCorrect: false, orderIndex: 3 },
    { optionText: '', isCorrect: false, orderIndex: 4 },
  ]);

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  const validateForm = useCallback(() => {
    const newErrors = {};

    if (!formData.questionText.trim()) {
      newErrors.questionText = 'Nội dung câu hỏi không được để trống';
    } else if (formData.questionText.length > 500) {
      newErrors.questionText = 'Nội dung không được vượt quá 500 ký tự';
    }

    if (formData.questionType === 'MULTIPLE_CHOICE') {
      if (options.some(opt => !opt.optionText.trim())) {
        newErrors.options = 'Tất cả lựa chọn phải có nội dung';
      } else if (!options.some(opt => opt.isCorrect)) {
        newErrors.options = 'Phải có ít nhất 1 đáp án đúng';
      }
    } else {
      if (!formData.correctAnswer.trim()) {
        newErrors.correctAnswer = 'Đáp án đúng không được để trống';
      }
    }

    if (formData.points < 1 || formData.points > 100) {
      newErrors.points = 'Điểm số phải từ 1-100';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData, options]);

  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      validateForm();
    }
  }, [formData, options, errors, validateForm]);

  const getNextOrderIndex = useCallback(async () => {
    try {
      const nextOrder = await questionService.getNextOrderIndex(lessonId);
      console.log('✅ Next order index for question:', nextOrder);
      setFormData(prev => ({ ...prev, orderIndex: nextOrder }));
    } catch (error) {
      console.error('Get next order index error:', error);
    }
  }, [lessonId]);

  const loadQuestionData = useCallback(async () => {
    setLoading(true);
    try {
      const questions = await questionService.fetchByLesson(lessonId);
      const question = questions.find(q => q.id === parseInt(questionId));

      if (question) {
        setFormData({
          lessonId: question.lessonId,
          questionText: question.questionText || '',
          questionType: question.questionType || 'MULTIPLE_CHOICE',
          correctAnswer: question.correctAnswer || '',
          explanation: question.explanation || '',
          points: question.points || 5,
          orderIndex: question.orderIndex || 1,
        });

        if (question.questionType === 'MULTIPLE_CHOICE' && question.options) {
          const loadedOptions = question.options.slice(0, 4);
          while (loadedOptions.length < 4) {
            loadedOptions.push({ 
              optionText: '', 
              isCorrect: false, 
              orderIndex: loadedOptions.length + 1 
            });
          }
          setOptions(loadedOptions);
        }
      }
    } catch (error) {
      console.error('Load question data error:', error);
      toast.error('Lỗi khi lấy thông tin câu hỏi');
      navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId));
    } finally {
      setLoading(false);
    }
  }, [lessonId, questionId, navigate]);

  useEffect(() => {
    if (isEdit) {
      loadQuestionData();
    } else {
      getNextOrderIndex();
    }
  }, [isEdit, loadQuestionData, getNextOrderIndex]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    
    if (errors[field]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }

    if (field === 'questionType') {
      if (value === 'MULTIPLE_CHOICE') {
        setOptions([
          { optionText: '', isCorrect: false, orderIndex: 1 },
          { optionText: '', isCorrect: false, orderIndex: 2 },
          { optionText: '', isCorrect: false, orderIndex: 3 },
          { optionText: '', isCorrect: false, orderIndex: 4 },
        ]);
      } else {
        setOptions([]);
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error('Vui lòng kiểm tra lại thông tin');
      return;
    }

    setSubmitting(true);
    try {
      const submitData = {
        ...formData,
        options: formData.questionType === 'MULTIPLE_CHOICE' ? options : null,
      };

      if (isEdit) {
        await questionService.update(questionId, submitData);
      } else {
        await questionService.create(submitData);
      }

      navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId));
    } catch (error) {
      console.error('Submit question error:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId));
  };

  return {
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
  };
};