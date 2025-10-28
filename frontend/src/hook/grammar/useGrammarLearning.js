import { useState, useEffect, useCallback } from "react";
import { grammarUserAPI } from "../../api/modules/grammar.api";
import toast from "react-hot-toast";

export const useGrammarLearning = () => {
  const [topics, setTopics] = useState([]);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [currentLesson, setCurrentLesson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Lesson Sidebar state
  const [expandedTopics, setExpandedTopics] = useState([]);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Theory lesson tracking
  const [readingTime, setReadingTime] = useState(0);
  const [hasScrolledToEnd, setHasScrolledToEnd] = useState(false);
  const [showCompleteButton, setShowCompleteButton] = useState(false);

  // Practice lesson state
  const [answers, setAnswers] = useState({});
  const [questionResults, setQuestionResults] = useState(null);
  const [hasSubmitted, setHasSubmitted] = useState(false);

  // ✅ Load lesson content
  const loadLessonContent = useCallback(async (lessonId) => {
    setLoading(true);
    try {
      console.log("📥 Loading lesson:", lessonId);

      // Save to localStorage for reload persistence
      // (Lưu ID bài học hiện tại để reload trang không mất chỗ đang học)
      localStorage.setItem("currentLessonId", lessonId);

      const response = await grammarUserAPI.getLessonContent(lessonId);
      const lessonData = response.data.data;

      console.log("✅ Lesson loaded:", lessonData);
      console.log("📝 Questions:", lessonData.questions?.length || 0);
      console.log("✔️ Is completed:", lessonData.isCompleted);

      setCurrentLesson(lessonData);

      // Reset lesson state
      setReadingTime(0);
      setHasScrolledToEnd(false);
      setShowCompleteButton(false);
      setAnswers({});
      setQuestionResults(null);
      setHasSubmitted(false);
    } catch (error) {
      console.error("❌ Error loading lesson:", error);
      console.error("Error response:", error.response?.data);
      toast.error(
        error.response?.data?.message || "Lấy nội dung bài học thất bại."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  // ✅ useGrammarLearning.js - FIX

  const loadTopics = useCallback(async () => {
    setLoading(true);

    // ✅ Helper function - inline, không cần useCallback riêng
    const loadFirstLesson = (topicsData) => {
      if (topicsData.length > 0) {
        const firstTopic = topicsData[0];
        console.log("🎯 Setting first topic:", firstTopic.name);

        setExpandedTopics([firstTopic.id]);
        setCurrentTopic(firstTopic);

        if (firstTopic.lessons && firstTopic.lessons.length > 0) {
          const firstUnlockedLesson = firstTopic.lessons.find(
            (l) => l.isUnlocked
          );
          if (firstUnlockedLesson) {
            loadLessonContent(firstUnlockedLesson.id);
          } else {
            toast.info("Chưa có bài học nào được mở khóa.");
          }
        } else {
          toast.info("Chủ đề này chưa có bài học nào.");
        }
      }
    };

    try {
      console.log("🔥 Loading topics with lessons...");

      const response = await grammarUserAPI.getAccessibleTopics();
      const topicsData = response.data.data || [];

      console.log("✅ Topics loaded:", topicsData.length);
      setTopics(topicsData);

      // ✅ KIỂM TRA LOCALSTORAGE ĐỂ KHÔI PHỤC BÀI HỌC
      const savedLessonId = localStorage.getItem("currentLessonId");

      if (savedLessonId) {
        console.log("🔄 Restoring lesson from localStorage:", savedLessonId);

        // Tìm topic chứa lesson này
        let foundTopic = null;
        for (const topic of topicsData) {
          const lesson = topic.lessons?.find(
            (l) => l.id === parseInt(savedLessonId)
          );
          if (lesson) {
            foundTopic = topic;
            break;
          }
        }

        if (foundTopic) {
          setCurrentTopic(foundTopic);
          setExpandedTopics([foundTopic.id]);
          await loadLessonContent(parseInt(savedLessonId));
        } else {
          // Không tìm thấy -> load lesson đầu tiên
          console.warn("⚠️ Saved lesson not found, loading first lesson");
          loadFirstLesson(topicsData);
        }
      } else {
        // Chưa có lesson nào được lưu -> load lesson đầu tiên
        loadFirstLesson(topicsData);
      }
    } catch (error) {
      console.error("❌ Error loading topics:", error);
      toast.error("Lấy thông tin chủ đề thất bại.");
    } finally {
      setLoading(false);
    }
  }, [loadLessonContent]);

  // ✅ Load topic details (simplified)
  const loadTopicDetails = useCallback(
    async (topicId) => {
      try {
        console.log("📥 Loading topic details:", topicId);

        // Find topic from already loaded topics
        const topic = topics.find((t) => t.id === topicId);

        if (topic) {
          console.log("✅ Topic found in cache:", topic.name);
          setCurrentTopic(topic);

          // Load first unlocked lesson
          if (topic.lessons && topic.lessons.length > 0) {
            const firstUnlockedLesson = topic.lessons.find((l) => l.isUnlocked);
            if (firstUnlockedLesson) {
              await loadLessonContent(firstUnlockedLesson.id);
            }
          }
        } else {
          console.log("⚠️ Topic not in cache, fetching from API...");
          // Fallback: fetch from API
          const response = await grammarUserAPI.getTopicDetails(topicId);
          const topicData = response.data.data;

          console.log("✅ Topic loaded from API:", topicData);
          setCurrentTopic(topicData);

          if (topicData.lessons && topicData.lessons.length > 0) {
            const firstUnlockedLesson = topicData.lessons.find(
              (l) => l.isUnlocked
            );
            if (firstUnlockedLesson) {
              await loadLessonContent(firstUnlockedLesson.id);
            }
          }
        }
      } catch (error) {
        console.error("❌ Error loading topic details:", error);
        toast.error("Lỗi khi lấy thông tin chủ đề.");
      }
    },
    [topics, loadLessonContent]
  );

  // Reading timer effect
  useEffect(() => {
    if (currentLesson?.lessonType === "THEORY" && !currentLesson?.isCompleted) {
      const timer = setInterval(() => {
        setReadingTime((prev) => prev + 1);
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [currentLesson]);

  // Check if complete button should be shown
  useEffect(() => {
    if (currentLesson?.lessonType === "THEORY" && !currentLesson?.isCompleted) {
      const minTime = currentLesson.estimatedDuration || 0;
      const canComplete = hasScrolledToEnd && readingTime >= minTime;
      setShowCompleteButton(canComplete);
    }
  }, [currentLesson, hasScrolledToEnd, readingTime]);

  // Handle scroll to bottom for THEORY
  const handleScroll = (e) => {
    const element = e.target;
    const bottom =
      element.scrollHeight - element.scrollTop <= element.clientHeight + 50;
    if (bottom && !hasScrolledToEnd) {
      setHasScrolledToEnd(true);
    }
  };

  // Toggle topic expansion
  const toggleTopic = (topicId) => {
    console.log("🔄 Toggle topic:", topicId);
    setExpandedTopics((prev) =>
      prev.includes(topicId)
        ? prev.filter((id) => id !== topicId)
        : [...prev, topicId]
    );
  };

  // Handle answer change for Practice
  const handleAnswerChange = (questionId, answer) => {
    console.log("✏️ Answer changed:", { questionId, answer });
    setAnswers((prev) => ({
      ...prev,
      [questionId]: answer,
    }));

    // Clear result for this question if re-answering after submit
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
    loadLessonContent(currentLesson.id);
  };

  // Submit lesson
  const submitLesson = async () => {
    if (!currentLesson) {
      toast.error("Không tìm thấy thông tin bài học");
      return;
    }

    // Validate Practice answers
    if (currentLesson.lessonType === "PRACTICE") {
      const unanswered = currentLesson.questions?.filter((q) => !answers[q.id]);
      if (unanswered && unanswered.length > 0) {
        toast.error(
          `Vui lòng trả lời tất cả ${unanswered.length} câu hỏi còn lại.`
        );
        return;
      }
    }

    setSubmitting(true);
    try {
      const submitData = {
        lessonId: currentLesson.id,
        ...(currentLesson.lessonType === "THEORY"
          ? { readingTimeSecond: readingTime }
          : {
              answers: Object.entries(answers).map(([questionId, answer]) => ({
                questionId: parseInt(questionId),
                answer: answer,
              })),
            }),
      };

      console.log("📤 Submitting lesson:", submitData);

      const response = await grammarUserAPI.submitLesson(submitData);
      const result = response.data.data;

      console.log("✅ Submit response:", result);

      if (currentLesson.lessonType === "THEORY") {
        toast.success("Hoàn thành bài học!");

        if (result.hasUnlockedNext && result.nextLessonId) {
          toast.success("Bài học tiếp theo đã được mở khóa.", {
            duration: 3000,
          });
          setTimeout(() => {
            loadLessonContent(result.nextLessonId);
          }, 1500);
        } else {
          loadTopics();
        }
      } else if (currentLesson.lessonType === "PRACTICE") {
        // ✅ LUÔN hiển thị kết quả
        const resultsMap = {};
        result.questionResults?.forEach((qr) => {
          resultsMap[qr.questionId] = qr;
        });
        setQuestionResults(resultsMap);
        setHasSubmitted(true);

        if (result.isPassed) {
          toast.success(
            `🎉 Chính xác ${result.correctAnswers}/${
              result.totalQuestions
            } câu! (${Math.round(
              (result.correctAnswers / result.totalQuestions) * 100
            )}%)`
          );

          if (result.hasUnlockedNext && result.nextLessonId) {
            toast.success("Bài học tiếp theo đã được mở khóa.", {
              duration: 3000,
            });
          }
        } else {
          toast.error(
            `❌ Chính xác ${result.correctAnswers}/${
              result.totalQuestions
            } câu (${Math.round(
              (result.correctAnswers / result.totalQuestions) * 100
            )}%). Cần ít nhất 80% để hoàn thành.`
          );
        }

        // ✅ Reload topics để cập nhật sidebar NHƯNG KHÔNG load lại lesson
        refreshTopicsOnly();
      }
    } catch (error) {
      console.error("❌ Error submitting lesson:", error);
      console.error("Error response:", error.response?.data);

      const errorMsg =
        error.response?.data?.message ||
        "Gửi bài học thất bại. Vui lòng thử lại.";
      toast.error(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  // Chỉ refresh topics list, KHÔNG load lại lesson
  const refreshTopicsOnly = async () => {
    try {
      console.log("🔄 Refreshing topics list only...");

      const response = await grammarUserAPI.getAccessibleTopics();
      const topicsData = response.data.data || [];

      setTopics(topicsData);

      // ✅ Cập nhật currentTopic (để sidebar hiển thị đúng progress)
      if (currentTopic) {
        const updatedTopic = topicsData.find((t) => t.id === currentTopic.id);
        if (updatedTopic) {
          setCurrentTopic(updatedTopic);
        }
      }

      console.log("✅ Topics refreshed without reloading lesson");
    } catch (error) {
      console.error("❌ Error refreshing topics:", error);
    }
  };

  // Navigate to specific lesson
  const navigateToLesson = async (lessonId, isUnlocked) => {
    if (!isUnlocked) {
      toast.error("🔒 Bài học này chưa được mở khóa.");
      return;
    }
    await loadLessonContent(lessonId);
  };

  // Load topics on mount
  useEffect(() => {
    loadTopics();
  }, [loadTopics]);

  return {
    // Data
    topics,
    currentTopic,
    currentLesson,
    loading,
    submitting,

    // Sidebar
    expandedTopics,
    sidebarCollapsed,
    setSidebarCollapsed,
    toggleTopic,

    // Theory tracking
    readingTime,
    hasScrolledToEnd,
    showCompleteButton,
    handleScroll,

    // Practice
    answers,
    questionResults,
    hasSubmitted,
    handleAnswerChange,
    handleRetry,

    // Actions
    submitLesson,
    navigateToLesson,
    loadTopicDetails,
    loadTopics, // Export để có thể reload
    refreshTopicsOnly,
  };
};
