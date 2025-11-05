import { useState, useEffect, useCallback } from "react";
import { readingUserAPI } from "../../api/modules/reading.api";
import toast from "react-hot-toast";

export const useReadingLearning = () => {
  const [lessons, setLessons] = useState([]);
  const [currentLesson, setCurrentLesson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  const [answers, setAnswers] = useState({});
  const [questionResults, setQuestionResults] = useState(null);
  const [hasSubmitted, setHasSubmitted] = useState(false);
  const [showTranslation, setShowTranslation] = useState(false);

  const loadLessonContent = useCallback(async (lessonId) => {
    setLoading(true);
    try {
      localStorage.setItem("currentReadingLessonId", lessonId);

      const response = await readingUserAPI.getLessonDetail(lessonId);
      const lessonData = response.data.data;

      setCurrentLesson(lessonData);

      setAnswers({});
      setQuestionResults(null);
      setHasSubmitted(false);
      setShowTranslation(lessonData.isCompleted || false);
    } catch (error) {
      console.error("Error loading lesson:", error);
      toast.error(
        error.response?.data?.message || "Lấy nội dung bài đọc thất bại."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadLessons = useCallback(async () => {
    setLoading(true);

    const loadFirstLesson = (lessonsData) => {
      if (lessonsData.length > 0) {
        const firstUnlockedLesson = lessonsData.find((l) => l.isUnlocked);
        if (firstUnlockedLesson) {
          loadLessonContent(firstUnlockedLesson.id);
        } else {
          toast.info("Chưa có bài đọc nào được mở khóa.");
        }
      }
    };

    try {
      const response = await readingUserAPI.getLessons({
        page: 0,
        size: 1000,
        sort: "orderIndex,asc",
      });

      const lessonsData =
        response.data.data.content || response.data.data || [];
      setLessons(lessonsData);

      const savedLessonId = localStorage.getItem("currentReadingLessonId");

      if (savedLessonId) {
        const lesson = lessonsData.find(
          (l) => l.id === parseInt(savedLessonId)
        );
        if (lesson) {
          await loadLessonContent(parseInt(savedLessonId));
        } else {
          loadFirstLesson(lessonsData);
        }
      } else {
        loadFirstLesson(lessonsData);
      }
    } catch (error) {
      console.error("Error loading lessons:", error);
      toast.error("Lấy thông tin bài đọc thất bại.");
    } finally {
      setLoading(false);
    }
  }, [loadLessonContent]);

  useEffect(() => {
    loadLessons();
  }, [loadLessons]);

  const handleAnswerChange = (questionId, answer) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: answer,
    }));

    if (hasSubmitted && questionResults) {
      setQuestionResults((prev) => ({
        ...prev,
        [questionId]: null,
      }));
    }
  };

  const handleRetry = () => {
    setAnswers({});
    setQuestionResults(null);
    setHasSubmitted(false);
    setShowTranslation(false);
    loadLessonContent(currentLesson.id);
  };

  const submitLesson = async () => {
    if (!currentLesson) {
      toast.error("Không tìm thấy thông tin bài đọc");
      return;
    }

    const unanswered = currentLesson.questions?.filter((q) => !answers[q.id]);
    if (unanswered && unanswered.length > 0) {
      toast.error(
        `Vui lòng trả lời tất cả ${unanswered.length} câu hỏi còn lại.`
      );
      return;
    }

    setSubmitting(true);
    try {
      const submitData = {
        answers: Object.entries(answers).map(([questionId, answer]) => ({
          questionId: parseInt(questionId),
          answer: answer,
        })),
      };

      const response = await readingUserAPI.submitLesson(
        currentLesson.id,
        submitData
      );
      const result = response.data.data;

      console.log("📊 Submit result:", result);

      const resultsMap = {};
      result.questionResults?.forEach((qr) => {
        resultsMap[qr.questionId] = qr;
      });
      setQuestionResults(resultsMap);
      setHasSubmitted(true);

      // ✅ Extract data
      const correctCount = result.correctCount || 0;
      const totalQuestions = result.totalQuestions || 0;
      const percentage = result.scorePercentage || 0;
      const isPassed = result.isPassed || false;
      const pointsEarned = result.pointsEarned || 0;

      // ✅ Show result messages
      if (isPassed) {
        // 1. Main success message
        if (percentage === 100) {
          toast.success(
            `🎉 Xuất sắc! Hoàn hảo ${totalQuestions}/${totalQuestions} câu đúng!`,
            {
              duration: 5000,
              icon: "🌟",
              style: {
                background: "#10B981",
                color: "#fff",
              },
            }
          );
        } else {
          toast.success(
            `✅ Chúc mừng! Bạn đã đạt ${percentage.toFixed(
              1
            )}% (${correctCount}/${totalQuestions} câu đúng)`,
            { duration: 5000 }
          );
        }

        // 2. Points earned message (delay 1s)
        if (pointsEarned > 0) {
          setTimeout(() => {
            toast.success(`⭐ +${pointsEarned} điểm`, {
              duration: 3000,
              icon: "🎁",
            });
          }, 1000);
        }

        // 3. Unlock next message (delay 2s)
        if (result.hasUnlockedNext && result.nextLessonId) {
          setTimeout(() => {
            toast.success(`🔓 Bài đọc tiếp theo đã mở khóa!`, {
              duration: 3000,
              icon: "🚀",
            });
          }, 2000);
        }
      } else {
        // Failed message
        toast.error(
          `📚 Chưa đạt! Bạn đạt ${percentage.toFixed(
            1
          )}% (${correctCount}/${totalQuestions} câu). Cần ≥80% để hoàn thành.`,
          {
            duration: 5000,
            icon: "💪",
          }
        );
      }

      refreshLessonsOnly();
    } catch (error) {
      console.error("Error submitting lesson:", error);

      const errorMsg =
        error.response?.data?.message ||
        "Gửi bài đọc thất bại. Vui lòng thử lại.";
      toast.error(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  const refreshLessonsOnly = async () => {
    try {
      const response = await readingUserAPI.getLessons({
        page: 0,
        size: 1000,
        sort: "orderIndex,asc",
      });

      const lessonsData =
        response.data.data.content || response.data.data || [];
      setLessons(lessonsData);
    } catch (error) {
      console.error("Error refreshing lessons:", error);
    }
  };

  const navigateToLesson = async (lessonId, isUnlocked) => {
    if (!isUnlocked) {
      toast.error("Bài đọc này chưa được mở khóa.");
      return;
    }
    await loadLessonContent(lessonId);
  };

  return {
    lessons,
    currentLesson,
    loading,
    submitting,

    sidebarCollapsed,
    setSidebarCollapsed,

    answers,
    questionResults,
    hasSubmitted,
    showTranslation,
    setShowTranslation,
    handleAnswerChange,
    handleRetry,

    submitLesson,
    navigateToLesson,
    loadLessons,
    refreshLessonsOnly,
  };
};
