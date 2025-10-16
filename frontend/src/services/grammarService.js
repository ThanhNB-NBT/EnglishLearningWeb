// services/grammarService.js
import { grammarAdminAPI } from '../api/modules/grammar.api';
import toast from 'react-hot-toast';

// ==================== TOPIC SERVICES ====================
export const topicService = {
  /**
   * Lấy tất cả topics
   */
  async fetchAll() {
    try {
      const response = await grammarAdminAPI.getAllTopics();
      return response.data.data || [];
    } catch (error) {
      toast.error('Lỗi khi lấy danh sách topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
      throw error;
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
   * Lấy lessons theo topic
   */
  async fetchByTopic(topicId) {
    try {
      const response = await grammarAdminAPI.getLessonsByTopic(topicId);
      return response.data.data || [];
    } catch (error) {
      console.error('Fetch lessons by topic error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách bài học');
    }
  },

  /**
   * Lấy lesson theo ID
   */
  async fetchById(topicId, lessonId) {
    try {
      const lessons = await this.fetchByTopic(topicId);
      return lessons.find(lesson => lesson.id === parseInt(lessonId));
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
  async delete(lessonId) {
    try {
      await grammarAdminAPI.deleteLesson(lessonId);
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
   * Lấy questions theo lesson
   */
  async fetchByLesson(lessonId) {
    try {
      const response = await grammarAdminAPI.getQuestionsByLesson(lessonId);
      return response.data.data || [];
    } catch (error) {
      console.error('Fetch questions error:', error);
      throw new Error(error.response?.data?.message || 'Lỗi khi lấy danh sách câu hỏi');
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
};

// Export default
export default {
  topics: topicService,
  lessons: lessonService,
  questions: questionService,
};