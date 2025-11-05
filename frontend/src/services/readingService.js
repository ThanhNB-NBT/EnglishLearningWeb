import { readingAdminAPI } from "../api/modules/reading.api";
import toast from "react-hot-toast";

// ==================== LESSON SERVICES ====================
export const lessonService = {
  async fetchPaginated(params = {}) {
    try {
      const response = await readingAdminAPI.getAllLessons(params);
      return response.data.data;
    } catch (error) {
      toast.error(
        "Lỗi khi lấy danh sách bài đọc: " +
          (error.response?.data?.message || "Vui lòng thử lại.")
      );
      throw error;
    }
  },

  async fetchAll() {
    try {
      const response = await readingAdminAPI.getAllLessons({
        page: 0,
        size: 1000,
        sort: "orderIndex,desc",
      });

      if (response.data.data.content) {
        return response.data.data.content;
      }
      return response.data.data || [];
    } catch (error) {
      toast.error(
        "Lỗi khi lấy danh sách bài đọc: " +
          (error.response?.data?.message || "Vui lòng thử lại.")
      );
      throw error;
    }
  },

  async getNextOrderIndex() {
    try {
      const response = await readingAdminAPI.getNextLessonOrderIndex();
      const nextOrder = response.data.data.nextOrderIndex;
      return nextOrder;
    } catch (error) {
      console.error("Get next order index error:", error);
      return 1;
    }
  },

  async fetchById(lessonId) {
    try {
      const response = await readingAdminAPI.getLessonDetail(lessonId);
      return response.data.data;
    } catch (error) {
      console.error("Fetch lesson by ID error:", error);
      throw new Error("Lỗi khi lấy thông tin bài đọc");
    }
  },

  async create(lessonData) {
    try {
      const response = await readingAdminAPI.createLesson(lessonData);
      toast.success("Tạo bài đọc thành công!");
      return response.data.data;
    } catch (error) {
      console.error("Create lesson error:", error);
      throw new Error(error.response?.data?.message || "Lỗi khi tạo bài đọc");
    }
  },

  async update(lessonId, lessonData) {
    try {
      const response = await readingAdminAPI.updateLesson(lessonId, lessonData);
      toast.success("Cập nhật bài đọc thành công!");
      return response.data.data;
    } catch (error) {
      console.error("Update lesson error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi cập nhật bài đọc"
      );
    }
  },

  async delete(lessonId) {
    try {
      await readingAdminAPI.deleteLesson(lessonId);
      toast.success("Xóa bài đọc thành công!");
      return true;
    } catch (error) {
      console.error("Delete lesson error:", error);
      throw new Error(error.response?.data?.message || "Lỗi khi xóa bài đọc");
    }
  },

  async permanentlyDelete(lessonId) {
    try {
      await readingAdminAPI.permanentlyDeleteLesson(lessonId);
      toast.success("Xóa vĩnh viễn bài đọc thành công!");
      return true;
    } catch (error) {
      console.error("Permanently delete lesson error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi xóa vĩnh viễn bài đọc"
      );
    }
  },
};

// ==================== QUESTION SERVICES ====================
export const questionService = {
  async fetchPaginatedByLesson(lessonId, params = {}) {
    try {
      const response = await readingAdminAPI.getQuestionsByLesson(
        lessonId,
        params
      );
      return response.data.data;
    } catch (error) {
      console.error("Fetch paginated questions error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi lấy danh sách câu hỏi"
      );
    }
  },

  async fetchByLesson(lessonId) {
    try {
      const response = await readingAdminAPI.getQuestionsByLesson(lessonId, {
        page: 0,
        size: 1000,
        sort: "orderIndex,desc",
      });

      if (response.data.data.content) {
        return response.data.data.content;
      }
      return response.data.data || [];
    } catch (error) {
      console.error("Fetch questions error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi lấy danh sách câu hỏi"
      );
    }
  },

  async getNextOrderIndex(lessonId) {
    try {
      const response = await readingAdminAPI.getNextQuestionOrderIndex(
        lessonId
      );
      const nextOrder = response.data.data.nextOrderIndex;
      return nextOrder;
    } catch (error) {
      console.error("Get next order index error:", error);
      return 1;
    }
  },

  async create(questionData) {
    try {
      const response = await readingAdminAPI.createQuestion(questionData);
      toast.success("Tạo câu hỏi thành công!");
      return response.data.data;
    } catch (error) {
      console.error("Create question error:", error);
      throw new Error(error.response?.data?.message || "Lỗi khi tạo câu hỏi");
    }
  },

  async createBulk(lessonId, questions) {
    try {
      const response = await readingAdminAPI.createQuestionsInBulk(
        lessonId,
        questions
      );
      toast.success(`Tạo ${questions.length} câu hỏi thành công!`);
      return response.data.data;
    } catch (error) {
      console.error("Create bulk questions error:", error);
      throw new Error(error.response?.data?.message || "Lỗi khi tạo câu hỏi");
    }
  },

  async update(id, questionData) {
    try {
      const response = await readingAdminAPI.updateQuestion(id, questionData);
      toast.success("Cập nhật câu hỏi thành công!");
      return response.data.data;
    } catch (error) {
      console.error("Update question error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi cập nhật câu hỏi"
      );
    }
  },

  async delete(id) {
    try {
      await readingAdminAPI.deleteQuestion(id);
      toast.success("Xóa câu hỏi thành công!");
    } catch (error) {
      console.error("Delete question error:", error);
      throw new Error(error.response?.data?.message || "Lỗi khi xóa câu hỏi");
    }
  },

  async bulkDelete(questionIds) {
    try {
      await readingAdminAPI.bulkDeleteQuestions(questionIds);
      toast.success("Đã xóa " + questionIds.length + " câu hỏi thành công!");
    } catch (error) {
      console.error("Delete bulk questions error:", error);
      throw new Error(
        error.response?.data?.message || "Lỗi khi xóa nhiều câu hỏi"
      );
    }
  },
};

export default {
  lessons: lessonService,
  questions: questionService,
};
