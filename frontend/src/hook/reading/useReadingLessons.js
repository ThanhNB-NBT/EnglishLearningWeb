import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { lessonService } from "../../services/readingService";
import { ADMIN_ROUTES } from "../../constants/routes";
import { readingAdminAPI } from "../../api/modules/reading.api";
import toast from "react-hot-toast";

// ==================== HOOK CHO LIST WITH PAGINATION ====================
export const useLessonList = () => {
  const [lessons, setLessons] = useState([]);
  const [loading, setLoading] = useState(false);

  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("");

  const [pagination, setPagination] = useState({
    currentPage: 0,
    pageSize: 6,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const loadLessons = useCallback(async () => {
    setLoading(true);
    try {
      const data = await lessonService.fetchPaginated({
        page: pagination.currentPage,
        size: pagination.pageSize,
        sort: "orderIndex,asc",
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
      toast.error("Lỗi khi lấy danh sách bài đọc");
      console.error("Load lessons error:", error);
    } finally {
      setLoading(false);
    }
  }, [pagination.currentPage, pagination.pageSize]);

  useEffect(() => {
    loadLessons();
  }, [loadLessons]);

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
      console.error("Delete lesson error:", error);
    }
  };

  const permanentlyDeleteLesson = async (lessonId) => {
    try {
      await lessonService.permanentlyDelete(lessonId);
      await loadLessons();
    } catch (error) {
      console.error("Permanently delete lesson error:", error);
    }
  };

  const resetFilters = () => {
    setSearchTerm("");
    setFilterStatus("");
  };

  const filteredLessons = lessons.filter((lesson) => {
    const matchSearch =
      !searchTerm ||
      lesson.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      lesson.content?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchStatus =
      !filterStatus ||
      (filterStatus === "active" ? lesson.isActive : !lesson.isActive);

    return matchSearch && matchStatus;
  });

  return {
    lessons: filteredLessons,
    loading,
    searchTerm,
    setSearchTerm,
    filterStatus,
    setFilterStatus,
    pagination,
    handlePageChange,
    handlePageSizeChange,
    deleteLesson,
    permanentlyDeleteLesson,
    resetFilters,
    reload: loadLessons,
  };
};

// ==================== HOOK CHO CREATE/EDIT ====================
export const useLessonForm = (lessonId = null) => {
  const navigate = useNavigate();
  const isEdit = !!lessonId;

  const [formData, setFormData] = useState({
    title: "",
    content: "",
    contentTranslation: "",
    orderIndex: 1,
    pointsReward: 25,
    isActive: true,
  });

  const [originalData, setOriginalData] = useState(null);
  const [allLessons, setAllLessons] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const [hasChanges, setHasChanges] = useState(false);

  const [manualOrderIndex, setManualOrderIndex] = useState(false);
  const [reorderDialog, setReorderDialog] = useState({
    open: false,
    affectedLessons: [],
    loading: false,
  });

  const validateForm = useCallback(() => {
    const newErrors = {};

    if (!formData.title.trim()) {
      newErrors.title = "Tiêu đề không được để trống";
    } else if (formData.title.length > 200) {
      newErrors.title = "Tiêu đề không được vượt quá 200 ký tự";
    }

    if (!formData.content.trim()) {
      newErrors.content = "Nội dung không được để trống";
    }

    if (formData.orderIndex < 1) {
      newErrors.orderIndex = "Thứ tự phải lớn hơn 0";
    }

    if (formData.pointsReward <= 0) {
      newErrors.pointsReward = "Điểm thưởng phải lớn hơn 0";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      validateForm();
    }
  }, [formData, errors, validateForm]);

  const loadAllLessons = useCallback(async () => {
    try {
      const lessons = await lessonService.fetchAll();
      setAllLessons(lessons);
    } catch (error) {
      console.error("Load all lessons error:", error);
    }
  }, []);

  const getNextOrderIndex = useCallback(async () => {
    try {
      const nextOrder = await lessonService.getNextOrderIndex();
      setFormData((prev) => ({ ...prev, orderIndex: nextOrder }));
    } catch (error) {
      console.error("Get next order index error:", error);
    }
  }, []);

  const loadLessonData = useCallback(async () => {
    setLoading(true);
    try {
      const lesson = await lessonService.fetchById(lessonId);

      if (lesson) {
        const lessonData = {
          title: lesson.title || "",
          content: lesson.content || "",
          contentTranslation: lesson.contentTranslation || "",
          orderIndex: lesson.orderIndex || 1,
          pointsReward: lesson.pointsReward || 25,
          isActive: lesson.isActive !== undefined ? lesson.isActive : true,
        };

        setFormData(lessonData);
        setOriginalData(lessonData);
      }
    } catch (error) {
      console.error("Load lesson data error:", error);
      toast.error("Lỗi khi lấy thông tin bài đọc");
      navigate(ADMIN_ROUTES.READING_LESSONS);
    } finally {
      setLoading(false);
    }
  }, [lessonId, navigate]);

  useEffect(() => {
    loadAllLessons();
    if (isEdit) {
      loadLessonData();
    } else {
      getNextOrderIndex();
    }
  }, [isEdit, loadAllLessons, loadLessonData, getNextOrderIndex]);

  useEffect(() => {
    if (originalData) {
      const hasDataChanged =
        JSON.stringify(formData) !== JSON.stringify(originalData);
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

    if (
      isEdit &&
      originalData &&
      formData.orderIndex === originalData.orderIndex
    ) {
      return false;
    }

    const targetOrder = formData.orderIndex;

    const affected = allLessons.filter((lesson) => {
      if (isEdit && lesson.id === parseInt(lessonId)) {
        return false;
      }

      return lesson.orderIndex >= targetOrder;
    });

    return affected.length > 0 ? affected : false;
  }, [
    formData.orderIndex,
    allLessons,
    manualOrderIndex,
    isEdit,
    lessonId,
    originalData,
  ]);

  const handleReorder = async () => {
    const affectedLessons = checkReorderNeeded();

    if (!affectedLessons) {
      return true;
    }

    

    return new Promise((resolve) => {
      setReorderDialog({
        open: true,
        affectedLessons,
        loading: false,
        onConfirm: async () => {
          setReorderDialog((prev) => ({ ...prev, loading: true }));

          try {
            await readingAdminAPI.reorderLesson(lessonId, formData.orderIndex);

            toast.success("Đã sắp xếp lại thứ tự các bài đọc");
            setReorderDialog({
              open: false,
              affectedLessons: [],
              loading: false,
            });

            await loadAllLessons();

            resolve(true);
          } catch (error) {
            console.error("Reorder error:", error);
            toast.error(
              "Lỗi khi sắp xếp lại: " +
                (error.response?.data?.message || error.message)
            );
            setReorderDialog((prev) => ({ ...prev, loading: false }));
            resolve(false);
          }
        },
        onCancel: () => {
          setReorderDialog({
            open: false,
            affectedLessons: [],
            loading: false,
          });
          resolve(false);
        },
      });
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin nhập vào");
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
      navigate(ADMIN_ROUTES.READING_LESSONS);
    } catch (error) {
      console.error("Submit lesson error:", error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    if (isEdit && hasChanges) {
      if (window.confirm("Bạn có chắc muốn hủy? Các thay đổi sẽ bị mất.")) {
        navigate(ADMIN_ROUTES.READING_LESSONS);
      }
    } else {
      navigate(ADMIN_ROUTES.READING_LESSONS);
    }
  };

  const getCompletionPercentage = () => {
    const requiredFields = ["title", "content"];
    const optionalFields = ["contentTranslation", "orderIndex", "pointsReward"];

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
    loading,
    submitting,
    errors,
    hasChanges,
    isEdit,
    handleInputChange,
    handleSubmit,
    handleCancel,
    getCompletionPercentage,
    manualOrderIndex,
    setManualOrderIndex,
    reorderDialog,
    totalLessons: allLessons.length,
    willCauseReorder: !!checkReorderNeeded(),
  };
};
