// services/grammarService.js
import { grammarAdminAPI } from '../api/modules/grammar.api';
import toast from 'react-hot-toast';

// ==================== TOPIC SERVICES ====================
export const topicService = {
  /**
   * ✅ NEW: Lấy topics với phân trang
   */
  async fetchPaginated(params = {}) {
    try {
      const response = await grammarAdminAPI.getAllTopics(params);
      return response.data.data; // Return full paginated data
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
    }
  },

  /**
   * ✅ FIXED: Lấy TẤT CẢ topics (không phân trang) - for getting max orderIndex
   */
  async fetchAll() {
    try {
      const response = await grammarAdminAPI.getAllTopics({ 
        page: 0, 
        size: 1000,  // Lấy max 1000 records
        sort: 'orderIndex,desc' // ✅ Sort desc để lấy max order ngay
      });
      
      // Handle both paginated and non-paginated response
      if (response.data.data.content) {
        return response.data.data.content;
      }
      return response.data.data || [];
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
    }
  },

  /**
   * ✅ NEW: Lấy orderIndex tiếp theo (tối ưu hơn)
   */
  async getNextOrderIndex() {
    try {
      console.log('🔍 Fetching next order index for topics...');
      
      const response = await grammarAdminAPI.getNextTopicOrderIndex();
      const nextOrder = response.data.data.nextOrderIndex;
      
      console.log('✅ Next topic orderIndex:', nextOrder);
      return nextOrder;
    } catch (error) {
      console.error('❌ Get next order index error:', error);
      return 1;
    }
  },

  /**
   * Tạo topic mới
   */
  async create(topicData) {
    try {
      const response = await grammarAdminAPI.createTopic(topicData);
      toast.success('Tạo topic thành công!');
      return response.data.data;
    } catch (error) {
      toast.error('Lỗi khi tạo topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
    }
  },

  /**
   * Cập nhật topic
   */
  async update(id, topicData) {
    try {
      const response = await grammarAdminAPI.updateTopic(id, topicData);
      toast.success('Cập nhật topic thành công!');
      return response.data.data;
    } catch (error) {
      toast.error('Lỗi khi cập nhật topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
    }
  },

  /**
   * Xóa topic
   */
  async delete(id) {
    try {
      await grammarAdminAPI.deleteTopic(id);
      toast.success('Xóa topic thành công!');
    } catch (error) {
      toast.error('Lỗi khi xóa topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
    }
  },
};

// ==================== LESSON SERVICES ====================
export const lessonService = {
  /**
   * Lấy lessons với phân trang
   */
  async fetchPaginatedByTopic(topicId, params = {}) {
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId, params);
      return response.data.data;
    } catch (error) {
      console.error('Fetch paginated lessons error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách bài học');
    }
  },

  /**
   * Lấy TẤT CẢ lessons theo topic (không phân trang)
   */
  async fetchByTopic(topicId) {
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId, { 
        page: 0, 
        size: 1000,
        sort: 'orderIndex,desc' // ✅ Sort desc
      });
      
      if (response.data.data.content) {
        return response.data.data.content;
      }
      return response.data.data || [];
    } catch (error) {
      console.error('Fetch lessons by topic error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách bài học');
    }
  },

  /**
   * Lấy orderIndex tiếp theo cho lesson
   */
  async getNextOrderIndex(topicId) {
    try {
      console.log(`🔍 Fetching next order index for lessons in topic ${topicId}...`);
      
      const response = await grammarAdminAPI.getNextLessonOrderIndex(topicId);
      const nextOrder = response.data.data.nextOrderIndex;
      
      console.log('✅ Next lesson orderIndex:', nextOrder);
      return nextOrder;
    } catch (error) {
      console.error('❌ Get next order index error:', error);
      return 1;
    }
  },

  /**
   * Lấy lesson theo ID
   */
  async fetchById(lessonId) {
    try {
      const response = await grammarAdminAPI.getLessonDetail(lessonId);
      return response.data.data;
    } catch (error) {
      console.error('Fetch lesson by ID error:', error);
      throw new Error('Lỗi khi lấy thông tin bài học');
    }
  },

  /**
   * Tạo lesson mới
   */
  async create(lessonData) {
    try {
      const response = await grammarAdminAPI.createLesson(lessonData);
      toast.success('Tạo bài học thành công!');
      return response.data.data;
    } catch (error) {
      console.error('Create lesson error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi tạo bài học');
    }
  },

  /**
   * Cập nhật lesson
   */
  async update(lessonId, lessonData) {
    try {
      const response = await grammarAdminAPI.updateLesson(lessonId, lessonData);
      toast.success('Cập nhật bài học thành công!');
      return response.data.data;
    } catch (error) {
      console.error('Update lesson error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi cập nhật bài học');
    }
  },

  /**
   * Xóa lesson
   */
  async delete(lessonId, params = {}) {
    try {
      await grammarAdminAPI.deleteLesson(lessonId, params); // params = { cascade: true } khi cần xóa cả questions
      toast.success('Xóa bài học thành công!');
      return true;
    } catch (error) {
      console.error('Delete lesson error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi xóa bài học');
    }
  },
};

// ==================== QUESTION SERVICES ====================
export const questionService = {
  /**
   * Lấy questions với phân trang
   */
  async fetchPaginatedByLesson(lessonId, params = {}) {
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId, params);
      return response.data.data;
    } catch (error) {
      console.error('Fetch paginated questions error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách câu hỏi');
    }
  },

  /**
   * Lấy TẤT CẢ questions theo lesson (không phân trang)
   */
  async fetchByLesson(lessonId) {
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId, { 
        page: 0, 
        size: 1000,
        sort: 'orderIndex,desc' // ✅ Sort desc
      });
      
      if (response.data.data.content) {
        return response.data.data.content;
      }
      return response.data.data || [];
    } catch (error) {
      console.error('Fetch questions error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách câu hỏi');
    }
  },

  /**
   * Lấy orderIndex tiếp theo cho question
   */
  async getNextOrderIndex(lessonId) {
    try {
      console.log(`🔍 Fetching next order index for questions in lesson ${lessonId}...`);
      
      const response = await grammarAdminAPI.getNextQuestionOrderIndex(lessonId);
      const nextOrder = response.data.data.nextOrderIndex;
      
      console.log('✅ Next question orderIndex:', nextOrder);
      return nextOrder;
    } catch (error) {
      console.error('❌ Get next order index error:', error);
      return 1;
    }
  },

  /**
   * Tạo question mới
   */
  async create(questionData) {
    try {
      const response = await grammarAdminAPI.createQuestion(questionData);
      toast.success('Tạo câu hỏi thành công!');
      return response.data.data;
    } catch (error) {
      console.error('Create question error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi tạo câu hỏi');
    }
  },

  /**
   * Tạo nhiều questions
   */
  async createBulk(lessonId, questions) {
    try {
      const response = await grammarAdminAPI.createQuestionsInBulk(lessonId, questions);
      toast.success(`Tạo ${questions.length} câu hỏi thành công!`);
      return response.data.data;
    } catch (error) {
      console.error('Create bulk questions error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi tạo câu hỏi');
    }
  },

  /**
   * Cập nhật question
   */
  async update(id, questionData) {
    try {
      const response = await grammarAdminAPI.updateQuestion(id, questionData);
      toast.success('Cập nhật câu hỏi thành công!');
      return response.data.data;
    } catch (error) {
      console.error('Update question error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi cập nhật câu hỏi');
    }
  },

  /**
   * Xóa question
   */
  async delete(id) {
    try {
      await grammarAdminAPI.deleteQuestion(id);
      toast.success('Xóa câu hỏi thành công!');
    } catch (error) {
      console.error('Delete question error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi xóa câu hỏi');
    }
  },

  /**
   * Xóa nhiều questions
   */
  async bulkDelete(questionIds) {
    try {
      await grammarAdminAPI.bulkDeleteQuestions(questionIds);
      toast.success('Đã xóa ' + questionIds.length + ' câu hỏi thành công!');
    } catch (error) {
      console.error('Delete bulk questions error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi xóa nhiều câu hỏi');
    }
  },
};

// Export default
export default {
  topics: topicService,
  lessons: lessonService,
  questions: questionService,
};