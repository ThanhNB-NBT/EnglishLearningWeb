// hook/useGrammarTopics.js
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { topicService } from '../../services/grammarService';
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST WITH PAGINATION ====================
export const useTopicList = () => {
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // Search & Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [filterLevel, setFilterLevel] = useState('');
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

  const loadTopics = useCallback(async () => {
    setLoading(true);
    try {
      const data = await topicService.fetchPaginated({
        page: pagination.currentPage,
        size: pagination.pageSize,
        sort: 'orderIndex,asc',
      });

      if (data.content) {
        setTopics(data.content);
        setPagination({
          currentPage: data.pagination.currentPage,
          pageSize: data.pagination.pageSize,
          totalElements: data.pagination.totalElements,
          totalPages: data.pagination.totalPages,
          hasNext: data.pagination.hasNext,
          hasPrevious: data.pagination.hasPrevious,
        });
      } else {
        setTopics(data || []);
      }
    } catch (error) {
      console.error('Load topics error:', error);
    } finally {
      setLoading(false);
    }
  }, [pagination.currentPage, pagination.pageSize]);

  useEffect(() => {
    loadTopics();
  }, [loadTopics]);

  const handlePageChange = (newPage) => {
    setPagination((prev) => ({ ...prev, currentPage: newPage }));
  };

  const handlePageSizeChange = (newSize) => {
    setPagination((prev) => ({ ...prev, pageSize: newSize, currentPage: 0 }));
  };

  const deleteTopic = async (topicId) => {
    try {
      await topicService.delete(topicId);
      await loadTopics();
    } catch (error) {
      console.error('Delete topic error:', error);
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterLevel('');
    setFilterStatus('');
  };

  const filteredTopics = topics.filter((topic) => {
    const matchSearch =
      !searchTerm ||
      topic.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      topic.description?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchLevel = !filterLevel || topic.levelRequired === filterLevel;
    const matchStatus =
      !filterStatus || (filterStatus === 'active' ? topic.isActive : !topic.isActive);

    return matchSearch && matchLevel && matchStatus;
  });

  return {
    topics: filteredTopics,
    loading,
    searchTerm,
    setSearchTerm,
    filterLevel,
    setFilterLevel,
    filterStatus,
    setFilterStatus,
    pagination,
    handlePageChange,
    handlePageSizeChange,
    deleteTopic,
    resetFilters,
    reload: loadTopics,
  };
};

// ==================== HOOK CHO CREATE/EDIT ====================
export const useTopicForm = (topicId = null) => {
  const navigate = useNavigate();
  const isEdit = !!topicId;
  
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    levelRequired: "BEGINNER",
    orderIndex: 1,
    isActive: true,
  });
  const [originalData, setOriginalData] = useState(null);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [hasChanges, setHasChanges] = useState(false);

  const fetchNextOrderIndex = useCallback(async () => {
    try {
      const nextOrder = await topicService.getNextOrderIndex();
      console.log('✅ Next order index for topic:', nextOrder);
      setFormData((prev) => ({ ...prev, orderIndex: nextOrder }));
    } catch (error) {
      console.error('Fetch next order index error:', error);
      setFormData((prev) => ({ ...prev, orderIndex: 1 }));
    }
  }, []);

  const validateForm = useCallback(() => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = "Tên chủ đề là bắt buộc";
    } else if (formData.name.length < 3) {
      newErrors.name = "Tên chủ đề phải có ít nhất 3 ký tự";
    } else if (formData.name.length > 100) {
      newErrors.name = "Tên chủ đề không được vượt quá 100 ký tự";
    }

    if (formData.description && formData.description.length > 500) {
      newErrors.description = "Mô tả không được vượt quá 500 ký tự";
    }

    if (!formData.levelRequired) {
      newErrors.levelRequired = "Cấp độ là bắt buộc";
    }

    if (!formData.orderIndex || formData.orderIndex < 1) {
      newErrors.orderIndex = "Thứ tự phải là số nguyên dương";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      validateForm();
    }
  }, [formData, errors, validateForm]);

  const fetchTopic = useCallback(async () => {
    try {
      const topics = await topicService.fetchAll();
      const topic = topics.find(t => t.id.toString() === topicId);
      
      if (!topic) {
        toast.error("Không tìm thấy chủ đề");
        navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
        return;
      }

      const topicData = {
        name: topic.name || "",
        description: topic.description || "",
        levelRequired: topic.levelRequired || "BEGINNER",
        orderIndex: topic.orderIndex || 1,
        isActive: topic.isActive !== undefined ? topic.isActive : true,
      };

      setFormData(topicData);
      setOriginalData(topicData);
    } catch (err) {
      toast.error(err.message || "Lấy thông tin thất bại");
      navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
    } finally {
      setLoading(false);
    }
  }, [topicId, navigate]);

  useEffect(() => {
    if (isEdit) {
      fetchTopic();
    } else {
      fetchNextOrderIndex();
    }
  }, [isEdit, fetchTopic, fetchNextOrderIndex]);

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

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin");
      return;
    }

    setSaving(true);
    try {
      const submitData = {
        ...formData,
        orderIndex: parseInt(formData.orderIndex),
      };

      if (isEdit) {
        await topicService.update(topicId, submitData);
      } else {
        await topicService.create(submitData);
      }
      
      navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
    } catch (error) {
      console.error('Submit topic error:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (isEdit && hasChanges) {
      if (window.confirm("Bạn có chắc muốn hủy? Các thay đổi sẽ bị mất.")) {
        navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
      }
    } else {
      navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
    }
  };

  const getCompletionPercentage = () => {
    const requiredFields = ["name", "levelRequired"];
    const optionalFields = ["description", "orderIndex"];

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
    setFormData,
    loading,
    saving,
    errors,
    hasChanges,
    isEdit,
    handleInputChange,
    handleSubmit,
    handleCancel,
    getCompletionPercentage
  };
};