// hook/useGrammarQuestions.js
// Hook tổng hợp cho quản lý Grammar Questions (List, Create, Edit)
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { grammarAdminAPI } from '../../api/modules/grammar.api';
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST ====================
export const useQuestionList = (lessonId) => {
  const [questions, setQuestions] = useState([]);
  const [filteredQuestions, setFilteredQuestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [selectedQuestions, setSelectedQuestions] = useState([]);

  const loadQuestions = useCallback(async () => {
    if (!lessonId) return;
    
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      setQuestions(response.data.data || []);
      setSelectedQuestions([]); // Reset selection
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách câu hỏi');
      console.error('Load questions error:', error);
    } finally {
      setLoading(false);
    }
  }, [lessonId]);

  const applyFilters = useCallback(() => {
    let filtered = questions;

    if (searchTerm) {
      filtered = filtered.filter(question =>
        question.questionText?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        question.explanation?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    if (filterType) {
      filtered = filtered.filter(question => question.questionType === filterType);
    }

    filtered = filtered.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));

    setFilteredQuestions(filtered);
  }, [questions, searchTerm, filterType]);

  useEffect(() => {
    loadQuestions();
  }, [loadQuestions]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const deleteQuestion = async (questionId) => {
    try {
      await grammarAdminAPI.deleteQuestion(questionId);
      setQuestions(questions.filter(q => q.id !== questionId));
      toast.success('Xóa câu hỏi thành công!');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Lỗi khi xóa câu hỏi');
      console.error('Delete question error:', error);
      throw error;
    }
  };

  const bulkDeleteQuestions = async (questionIds) => {
    try {
      await Promise.all(
        questionIds.map(id => grammarAdminAPI.deleteQuestion(id))
      );
      setQuestions(questions.filter(q => !questionIds.includes(q.id)));
      setSelectedQuestions([]);
      toast.success(`Đã xóa ${questionIds.length} câu hỏi thành công!`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Lỗi khi xóa câu hỏi');
      console.error('Bulk delete error:', error);
      throw error;
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('');
  };

  return {
    questions,
    filteredQuestions,
    loading,
    searchTerm,
    setSearchTerm,
    filterType,
    setFilterType,
    selectedQuestions,
    setSelectedQuestions,
    loadQuestions,
    deleteQuestion,
    bulkDeleteQuestions,
    resetFilters,
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

  // ✅ FIXED: Wrap validateForm trong useCallback
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

  // ✅ Validate realtime
  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      validateForm();
    }
  }, [formData, options, errors, validateForm]);

  const getNextOrderIndex = useCallback(async () => {
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      const questions = response.data.data || [];
      const maxOrder = questions.length > 0 
        ? Math.max(...questions.map(q => q.orderIndex || 0)) 
        : 0;
      setFormData(prev => ({ ...prev, orderIndex: maxOrder + 1 }));
    } catch (error) {
      console.error('Get next order index error:', error);
    }
  }, [lessonId]);

  const loadQuestionData = useCallback(async () => {
    setLoading(true);
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      const question = response.data.data.find(q => q.id === parseInt(questionId));

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
          // Ensure always 4 options
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

    // Reset options when changing question type
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
        await grammarAdminAPI.updateQuestion(questionId, submitData);
        toast.success('Cập nhật câu hỏi thành công!');
      } else {
        await grammarAdminAPI.createQuestion(submitData);
        toast.success('Tạo câu hỏi thành công!');
      }

      navigate(ADMIN_ROUTES.GRAMMAR_QUESTIONS(lessonId));
    } catch (error) {
      console.error('Submit question error:', error);
      toast.error(error.response?.data?.message || 'Có lỗi xảy ra');
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