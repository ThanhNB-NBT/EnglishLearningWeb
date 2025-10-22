// hook/useGrammarTopics.js
// Hook tổng hợp cho quản lý Grammar Topics (List, Create, Edit)
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { topicService } from '../../services/grammarService';
import { ADMIN_ROUTES } from '../../constants/routes';
import toast from 'react-hot-toast';

// ==================== HOOK CHO LIST ====================
export const useTopicList = () => {
  const [topics, setTopics] = useState([]);
  const [filteredTopics, setFilteredTopics] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterLevel, setFilterLevel] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  const loadTopics = useCallback(async () => {
    setLoading(true);
    try {
      const data = await topicService.fetchAll();
      setTopics(data);
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách chủ đề ngữ pháp');
      console.error('Load topics error:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const applyFilters = useCallback(() => {
    let filtered = topics;

    if (searchTerm) {
      filtered = filtered.filter(topic =>
        topic.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        topic.description?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    if (filterLevel) {
      filtered = filtered.filter(topic => topic.levelRequired === filterLevel);
    }

    if (filterStatus) {
      const isActive = filterStatus === 'active';
      filtered = filtered.filter(topic => topic.isActive === isActive);
    }

    setFilteredTopics(filtered);
  }, [topics, searchTerm, filterLevel, filterStatus]);

  useEffect(() => {
    loadTopics();
  }, [loadTopics]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const deleteTopic = async (topicId) => {
    try {
      await topicService.delete(topicId);
      setTopics(topics.filter(topic => topic.id !== topicId));
      toast.success('Xóa chủ đề thành công!');
    } catch (error) {
      toast.error('Lỗi khi xóa chủ đề');
      console.error('Delete topic error:', error);
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterLevel('');
    setFilterStatus('');
  };

  return {
    topics,
    filteredTopics,
    loading,
    searchTerm,
    setSearchTerm,
    filterLevel,
    setFilterLevel,
    filterStatus,
    setFilterStatus,
    loadTopics,
    deleteTopic,
    resetFilters
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

  // ✅ NEW: Fetch next order index for create mode
  const fetchNextOrderIndex = useCallback(async () => {
    try {
      const topics = await topicService.fetchAll();
      
      if (topics.length === 0) {
        setFormData((prev) => ({ ...prev, orderIndex: 1 }));
      } else {
        const maxOrder = Math.max(...topics.map(t => t.orderIndex || 0));
        setFormData((prev) => ({ ...prev, orderIndex: maxOrder + 1 }));
      }
    } catch (error) {
      console.error('Fetch next order index error:', error);
      setFormData((prev) => ({ ...prev, orderIndex: 1 }));
    }
  }, []);

  // ✅ FIXED: Wrap validateForm trong useCallback
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

  // ✅ FIXED: Validate realtime với đầy đủ dependencies
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

  // ✅ UPDATED: Auto fetch next order index for create mode
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

  // ✅ FIXED: Clear error properly
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
        toast.success('Cập nhật chủ đề thành công!');
      } else {
        await topicService.create(submitData);
        toast.success('Tạo chủ đề thành công!');
      }
      
      navigate(ADMIN_ROUTES.GRAMMAR_TOPICS);
    } catch (error) {
      console.error('Submit topic error:', error);
      toast.error(error.response?.data?.message || 'Có lỗi xảy ra');
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