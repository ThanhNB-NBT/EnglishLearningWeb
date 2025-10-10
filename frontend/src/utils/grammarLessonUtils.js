import { grammarAdminAPI } from '../../../services/api';

// Fetch all lessons by topic
export const fetchLessonsByTopic = async (topicId) => {
  try {
    const response = await grammarAdminAPI.getLessonsByTopic(topicId);
    return response.data.data || [];
  } catch (error) {
    console.error('Fetch lessons by topic error:', error);
    throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách bài học');
  }
};

// Create new lesson
export const createLesson = async (lessonData) => {
  try {
    const response = await grammarAdminAPI.createLesson(lessonData);
    return response.data.data;
  } catch (error) {
    console.error('Create lesson error:', error);
    throw new Error(error.response?.data?.message || 'Lỗi khi tạo bài học');
  }
};

// Update lesson
export const updateLesson = async (lessonId, lessonData) => {
  try {
    const response = await grammarAdminAPI.updateLesson(lessonId, lessonData);
    return response.data.data;
  } catch (error) {
    console.error('Update lesson error:', error);
    throw new Error(error.response?.data?.message || 'Lỗi khi cập nhật bài học');
  }
};

// Delete lesson
export const deleteLesson = async (lessonId) => {
  try {
    await grammarAdminAPI.deleteLesson(lessonId);
    return true;
  } catch (error) {
    console.error('Delete lesson error:', error);
    throw new Error(error.response?.data?.message || 'Lỗi khi xóa bài học');
  }
};

// Get lesson by ID
export const fetchLessonById = async (topicId, lessonId) => {
  try {
    const lessons = await fetchLessonsByTopic(topicId);
    return lessons.find(lesson => lesson.id === parseInt(lessonId));
  } catch (error) {
    console.error('Fetch lesson by ID error:', error);
    throw new Error('Lỗi khi lấy thông tin bài học');
  }
};

// Validate lesson data
export const validateLessonData = (lessonData) => {
  const errors = {};

  if (!lessonData.title || !lessonData.title.trim()) {
    errors.title = 'Tiêu đề không được để trống';
  }

  if (lessonData.title && lessonData.title.length > 200) {
    errors.title = 'Tiêu đề không được vượt quá 200 ký tự';
  }

  if (!lessonData.lessonType) {
    errors.lessonType = 'Loại bài học không được để trống';
  }

  if (lessonData.lessonType === 'THEORY' && (!lessonData.content || !lessonData.content.trim())) {
    errors.content = 'Nội dung lý thuyết không được để trống';
  }

  if (!lessonData.orderIndex || lessonData.orderIndex < 1) {
    errors.orderIndex = 'Thứ tự phải lớn hơn 0';
  }

  if (lessonData.pointsRequired < 0) {
    errors.pointsRequired = 'Điểm yêu cầu không được âm';
  }

  if (!lessonData.pointsReward || lessonData.pointsReward <= 0) {
    errors.pointsReward = 'Điểm thưởng phải lớn hơn 0';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

// Format lesson type for display
export const formatLessonType = (lessonType) => {
  switch (lessonType) {
    case 'THEORY':
      return 'Lý thuyết';
    case 'PRACTICE':
      return 'Thực hành';
    default:
      return lessonType;
  }
};

// Get lesson type color
export const getLessonTypeColor = (lessonType) => {
  switch (lessonType) {
    case 'THEORY':
      return 'blue';
    case 'PRACTICE':
      return 'green';
    default:
      return 'gray';
  }
};

// Sort lessons by order index
export const sortLessonsByOrder = (lessons) => {
  return [...lessons].sort((a, b) => a.orderIndex - b.orderIndex);
};

// Filter lessons by search term
export const filterLessonsBySearch = (lessons, searchTerm) => {
  if (!searchTerm) return lessons;
  
  const term = searchTerm.toLowerCase();
  return lessons.filter(lesson => 
    lesson.title?.toLowerCase().includes(term) ||
    lesson.content?.toLowerCase().includes(term)
  );
};

// Filter lessons by type
export const filterLessonsByType = (lessons, lessonType) => {
  if (!lessonType) return lessons;
  return lessons.filter(lesson => lesson.lessonType === lessonType);
};

// Filter lessons by status
export const filterLessonsByStatus = (lessons, isActive) => {
  if (isActive === undefined || isActive === null) return lessons;
  return lessons.filter(lesson => lesson.isActive === isActive);
};

// Get next order index for new lesson
export const getNextOrderIndex = (lessons) => {
  if (!lessons || lessons.length === 0) return 1;
  const maxOrder = Math.max(...lessons.map(lesson => lesson.orderIndex || 0));
  return maxOrder + 1;
};

// Calculate lesson statistics
export const calculateLessonStats = (lessons) => {
  const total = lessons.length;
  const active = lessons.filter(lesson => lesson.isActive).length;
  const theory = lessons.filter(lesson => lesson.lessonType === 'THEORY').length;
  const practice = lessons.filter(lesson => lesson.lessonType === 'PRACTICE').length;
  
  return {
    total,
    active,
    inactive: total - active,
    theory,
    practice
  };
};

// Export lesson data to JSON
export const exportLessonsToJSON = (lessons) => {
  const exportData = {
    exportDate: new Date().toISOString(),
    totalLessons: lessons.length,
    lessons: lessons.map(lesson => ({
      id: lesson.id,
      title: lesson.title,
      lessonType: lesson.lessonType,
      content: lesson.content,
      orderIndex: lesson.orderIndex,
      pointsRequired: lesson.pointsRequired,
      pointsReward: lesson.pointsReward,
      isActive: lesson.isActive,
      questionCount: lesson.questionCount || 0,
      createdAt: lesson.createdAt
    }))
  };

  const dataStr = JSON.stringify(exportData, null, 2);
  const dataBlob = new Blob([dataStr], { type: 'application/json' });
  const url = URL.createObjectURL(dataBlob);
  
  const link = document.createElement('a');
  link.href = url;
  link.download = `grammar-lessons-${new Date().toISOString().split('T')[0]}.json`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};