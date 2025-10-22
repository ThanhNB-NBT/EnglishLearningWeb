package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tối ưu để kiểm tra đáp án cho TẤT CẢ loại câu hỏi
 * Hỗ trợ: Grammar, Reading, Listening, Speaking (text-based)
 */
@Service
@Slf4j
public class AnswerCheckingService {

    // ===== MAIN CHECKING METHOD =====

    /**
     * Kiểm tra đáp án chính - routing đến method phù hợp
     */
    public boolean checkAnswer(Question question, SubmitAnswerRequest answerRequest) {
        if (question == null || answerRequest == null) {
            log.warn("Question hoặc answerRequest null");
            return false;
        }

        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE:
                return checkMultipleChoice(question, answerRequest);

            case FILL_BLANK:
                return checkFillBlank(question, answerRequest);

            case VERB_FORM:
                return checkVerbForm(question, answerRequest);

            case TRANSLATE:
                return checkTranslation(question, answerRequest);

            case TRUE_FALSE:
                return checkTrueFalse(question, answerRequest);

            case SHORT_ANSWER:
                return checkShortAnswer(question, answerRequest);

            default:
                log.warn("Không hỗ trợ loại câu: {}", question.getQuestionType());
                return false;
        }
    }

    // ===== GRAMMAR & READING METHODS =====

    /**
     * Kiểm tra câu trắc nghiệm (Multiple Choice)
     */
    /**
     * Kiểm tra câu trắc nghiệm (Multiple Choice)
     * Support both selectedOptionId và answer text
     */
    private boolean checkMultipleChoice(Question question, SubmitAnswerRequest answerRequest) {
        // ✅ Option 1: Check by selectedOptionId (if provided)
        if (answerRequest.getSelectedOptionId() != null) {
            boolean isCorrect = question.getOptions().stream()
                    .anyMatch(option -> option.getId().equals(answerRequest.getSelectedOptionId())
                            && option.getIsCorrect());

            log.debug("Multiple choice check (by ID): questionId={}, selectedOptionId={}, isCorrect={}",
                    question.getId(), answerRequest.getSelectedOptionId(), isCorrect);

            return isCorrect;
        }

        // ✅ Option 2: Check by answer text (if selectedOptionId not provided)
        if (answerRequest.getAnswer() != null && !answerRequest.getAnswer().trim().isEmpty()) {
            String userAnswer = answerRequest.getAnswer().trim();

            boolean isCorrect = question.getOptions().stream()
                    .anyMatch(option -> option.getIsCorrect() &&
                            option.getOptionText().trim().equalsIgnoreCase(userAnswer));

            log.debug("Multiple choice check (by text): questionId={}, answer='{}', isCorrect={}",
                    question.getId(), userAnswer, isCorrect);

            return isCorrect;
        }

        log.debug("No option selected or answer provided");
        return false;
    }

    /**
     * Kiểm tra điền từ vào chỗ trống
     * Support: multiple correct answers separated by "|"
     */
    private boolean checkFillBlank(Question question, SubmitAnswerRequest answerRequest) {
        String userAnswerStr = answerRequest.getAnswer();

        if (userAnswerStr == null || userAnswerStr.trim().isEmpty()) {
            return false;
        }

        String[] userAnswers = userAnswerStr.split("\\|");
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        // Check số lượng blank
        if (userAnswers.length != correctAnswers.length) {
            log.debug("Fill blank count mismatch: user={}, correct={}",
                    userAnswers.length, correctAnswers.length);
            return false;
        }

        // Check từng blank
        for (int i = 0; i < userAnswers.length; i++) {
            String userNormalized = smartNormalize(userAnswers[i]);
            String correctNormalized = smartNormalize(correctAnswers[i]);

            // ✅ So sánh sau khi expand contractions
            if (!areEquivalent(userNormalized, correctNormalized)) {
                log.debug("Fill blank #{} mismatch: user='{}', correct='{}'",
                        i + 1, userNormalized, correctNormalized);
                return false;
            }
        }

        log.debug("✅ Fill blank correct: {}", String.join("|", userAnswers));
        return true;
    }

    /**
     * ✅ Kiểm tra 2 chuỗi có tương đương không (bao gồm contracted forms)
     */
    private boolean areEquivalent(String s1, String s2) {
        // Exact match
        if (s1.equals(s2)) {
            return true;
        }

        // Expand contractions và so sánh lại
        String expanded1 = expandContractions(s1);
        String expanded2 = expandContractions(s2);

        return expanded1.equals(expanded2);
    }

    private String expandContractions(String text) {
        return text
                // Be verbs
                .replaceAll("\\bi'm\\b", "i am")
                .replaceAll("\\byou're\\b", "you are")
                .replaceAll("\\bhe's\\b", "he is")
                .replaceAll("\\bshe's\\b", "she is")
                .replaceAll("\\bit's\\b", "it is")
                .replaceAll("\\bwe're\\b", "we are")
                .replaceAll("\\bthey're\\b", "they are")

                // Negatives
                .replaceAll("\\bdon't\\b", "do not")
                .replaceAll("\\bdoesn't\\b", "does not")
                .replaceAll("\\bdidn't\\b", "did not")
                .replaceAll("\\bwon't\\b", "will not")
                .replaceAll("\\bcan't\\b", "cannot")
                .replaceAll("\\bcouldn't\\b", "could not")
                .replaceAll("\\bshouldn't\\b", "should not")
                .replaceAll("\\bisn't\\b", "is not")
                .replaceAll("\\baren't\\b", "are not")
                .replaceAll("\\bwasn't\\b", "was not")
                .replaceAll("\\bweren't\\b", "were not")

                // Modals
                .replaceAll("\\bi'll\\b", "i will")
                .replaceAll("\\byou'll\\b", "you will")
                .replaceAll("\\bhe'll\\b", "he will")
                .replaceAll("\\bshe'll\\b", "she'll")
                .replaceAll("\\bwe'll\\b", "we will")
                .replaceAll("\\bthey'll\\b", "they will")

                .replaceAll("\\bi'd\\b", "i would")
                .replaceAll("\\byou'd\\b", "you would")
                .replaceAll("\\bhe'd\\b", "he would")
                .replaceAll("\\bwe'd\\b", "we would")

                .replaceAll("\\bi've\\b", "i have")
                .replaceAll("\\byou've\\b", "you have")
                .replaceAll("\\bwe've\\b", "we have")
                .replaceAll("\\bthey've\\b", "they have");
    }

    /**
     * Kiểm tra chia động từ
     * Strict hơn fill_blank về format
     */
    private boolean checkVerbForm(Question question, SubmitAnswerRequest answerRequest) {
        if (answerRequest.getAnswer() == null || answerRequest.getAnswer().trim().isEmpty()) {
            return false;
        }

        String userAnswer = strictNormalize(answerRequest.getAnswer());
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        boolean isCorrect = Arrays.stream(correctAnswers)
                .anyMatch(correct -> userAnswer.equals(strictNormalize(correct)));

        if (!isCorrect) {
            log.debug("Verb form incorrect - User: '{}', Expected: {}",
                    userAnswer, Arrays.toString(correctAnswers));
        }

        return isCorrect;
    }

    // ===== TRANSLATION CHECKING =====

    /**
     * Kiểm tra bản dịch với 3 cấp độ:
     * 1. Exact match (100%)
     * 2. Keyword match (contains all important words)
     * 3. Fuzzy match (similarity(sự giống nhau) >= 85%)
     */
    private boolean checkTranslation(Question question, SubmitAnswerRequest answerRequest) {
        if (answerRequest.getAnswer() == null || answerRequest.getAnswer().trim().isEmpty()) {
            return false;
        }

        String userAnswer = smartNormalize(answerRequest.getAnswer());
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        // Level 1: Exact match (nhanh nhất)
        for (String correct : correctAnswers) {
            if (userAnswer.equals(smartNormalize(correct))) {
                log.debug("Translation exact match");
                return true;
            }
        }

        // Level 2: Keyword matching (So khớp từ khóa)
        String primaryAnswer = correctAnswers[0];
        Set<String> keywords = extractKeywords(primaryAnswer);

        long matchedKeywords = keywords.stream()
                .filter(userAnswer::contains)
                .count();

        // Có ít nhất 75% keywords và tối thiểu 2 keywords
        if (keywords.size() >= 2 && matchedKeywords >= Math.ceil(keywords.size() * 0.75)) {
            log.debug("Translation keyword match - found {}/{} keywords", matchedKeywords, keywords.size());
            return true;
        }

        // Level 3: Fuzzy match với similarity cao (>= 85%)
        double bestSimilarity = 0;
        for (String correct : correctAnswers) {
            double sim = calculateSimilarity(userAnswer, smartNormalize(correct));
            bestSimilarity = Math.max(bestSimilarity, sim);
        }

        if (bestSimilarity >= 0.85) {
            log.debug("Translation fuzzy match: similarity={}", bestSimilarity);
            return true;
        }

        log.debug("Translation incorrect - User: '{}', Keywords: {}/{}, Similarity: {}%",
                userAnswer, matchedKeywords, keywords.size(), (int) (bestSimilarity * 100));

        return false;
    }

    /**
     * Extract keywords (trích xuất từ khóa) từ câu dịch mẫu
     * Loại bỏ stop words, chỉ giữ từ quan trọng
     */
    private Set<String> extractKeywords(String sentence) {
        // Stop words phổ biến (các từ ít ý nghĩa)
        Set<String> stopWords = Set.of(
                "i", "you", "he", "she", "it", "we", "they",
                "am", "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did",
                "a", "an", "the",
                "to", "of", "in", "on", "at", "by", "for", "with", "from",
                "and", "or", "but",
                "very", "really", "so", "too", "this", "that");

        String normalized = smartNormalize(sentence);
        String[] words = normalized.split("\\s+");

        Set<String> keywords = new LinkedHashSet<>();
        for (String word : words) {
            // Chỉ giữ từ quan trọng: không phải stop word VÀ dài >= 3 ký tự
            if (!stopWords.contains(word) && word.length() >= 3) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * SCORING VERSION - Trả về điểm chi tiết và feedback
     * Dùng cho partial scoring và detailed feedback
     */
    public TranslationCheckResult checkTranslationWithScore(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return new TranslationCheckResult(false, 0, "Câu trả lời trống");
        }

        String userNormalized = smartNormalize(userAnswer);
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        // Check 1: Exact match = 100%
        for (String correct : correctAnswers) {
            if (userNormalized.equals(smartNormalize(correct))) {
                return new TranslationCheckResult(true, 100, "✅ Hoàn hảo! Bản dịch chính xác 100%");
            }
        }

        // Check 2: Keyword matching
        Set<String> keywords = extractKeywords(correctAnswers[0]);
        Set<String> missingKeywords = new HashSet<>();
        int foundKeywords = 0;

        for (String keyword : keywords) {
            if (userNormalized.contains(keyword)) {
                foundKeywords++;
            } else {
                missingKeywords.add(keyword);
            }
        }

        double keywordRatio = keywords.isEmpty() ? 0 : (double) foundKeywords / keywords.size();

        // Check 3: Similarity với đáp án gần nhất
        double bestSimilarity = 0;
        String closestAnswer = correctAnswers[0];

        for (String correct : correctAnswers) {
            double sim = calculateSimilarity(userNormalized, smartNormalize(correct));
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                closestAnswer = correct;
            }
        }

        // Tính điểm tổng hợp (weighted average)
        // Keywords: 70%, Similarity: 30%
        int keywordScore = (int) (keywordRatio * 100);
        int similarityScore = (int) (bestSimilarity * 100);
        int finalScore = (int) (keywordScore * 0.70 + similarityScore * 0.30);

        // Generate feedback chi tiết
        String feedback = generateTranslationFeedback(
                finalScore, foundKeywords, keywords.size(),
                similarityScore, closestAnswer, missingKeywords);

        boolean isPassed = finalScore >= 70; // 70% threshold

        return new TranslationCheckResult(isPassed, finalScore, feedback);
    }

    /**
     * Generate feedback CHI TIẾT và CỤ THỂ cho translation
     */
    private String generateTranslationFeedback(int score, int foundKeywords,
            int totalKeywords, int similarity, String closestAnswer, Set<String> missingKeywords) {

        if (score >= 95) {
            return "✅ Xuất sắc! Bản dịch rất chính xác.";
        }

        if (score >= 85) {
            return "✅ Rất tốt! Bản dịch gần như hoàn hảo.";
        }

        if (score >= 70) {
            String missing = missingKeywords.isEmpty() ? ""
                    : "\n💡 Có thể cải thiện thêm với các từ: " + String.join(", ", missingKeywords);
            return String.format(
                    "✅ Đạt yêu cầu! Bạn dùng đúng %d/%d từ khóa quan trọng.%s",
                    foundKeywords, totalKeywords, missing);
        }

        if (score >= 50) {
            return String.format(
                    "⚠️ Chưa đạt. Bản dịch thiếu %d từ khóa: [%s]\n" +
                            "💡 Tham khảo: \"%s\"",
                    missingKeywords.size(),
                    String.join(", ", missingKeywords),
                    closestAnswer);
        }

        return String.format(
                "❌ Bản dịch chưa chính xác (độ tương đồng: %d%%).\n" +
                        "💡 Gợi ý đáp án: \"%s\"",
                similarity, closestAnswer);
    }

    /**
     * Helper class cho kết quả check translation
     */
    public static class TranslationCheckResult {
        private final boolean isCorrect;
        private final int score; // 0-100
        private final String feedback;

        public TranslationCheckResult(boolean isCorrect, int score, String feedback) {
            this.isCorrect = isCorrect;
            this.score = score;
            this.feedback = feedback;
        }

        public boolean isCorrect() {
            return isCorrect;
        }

        public int getScore() {
            return score;
        }

        public String getFeedback() {
            return feedback;
        }
    }

    // ===== ADMIN HELPER - Preview Keywords =====

    /**
     * API cho Admin preview keywords sẽ được extract
     */
    public KeywordPreviewDTO previewTranslationKeywords(String correctAnswer) {
        String[] variants = correctAnswer.split("\\|");

        List<String> allKeywords = new ArrayList<>();
        for (String variant : variants) {
            Set<String> keywords = extractKeywords(variant);
            allKeywords.add(variant + " → Keywords: " + String.join(", ", keywords));
        }

        // Keywords từ answer đầu tiên (primary)
        Set<String> primaryKeywords = extractKeywords(variants[0]);

        return new KeywordPreviewDTO(
                correctAnswer,
                new ArrayList<>(primaryKeywords),
                allKeywords,
                "Hệ thống check: exact match HOẶC có >= 75% keywords HOẶC similarity >= 85%");
    }

    public static class KeywordPreviewDTO {
        private final String originalAnswer;
        private final List<String> primaryKeywords;
        private final List<String> variantDetails;
        private final String checkingLogic;

        public KeywordPreviewDTO(String originalAnswer, List<String> primaryKeywords,
                List<String> variantDetails, String checkingLogic) {
            this.originalAnswer = originalAnswer;
            this.primaryKeywords = primaryKeywords;
            this.variantDetails = variantDetails;
            this.checkingLogic = checkingLogic;
        }

        public String getOriginalAnswer() {
            return originalAnswer;
        }

        public List<String> getPrimaryKeywords() {
            return primaryKeywords;
        }

        public List<String> getVariantDetails() {
            return variantDetails;
        }

        public String getCheckingLogic() {
            return checkingLogic;
        }
    }

    // ===== TRUE/FALSE - ĐƠN GIẢN HÓA =====

    /**
     * Kiểm tra True/False - ĐƠN GIẢN
     * Frontend gửi: "true" hoặc "false" (string)
     * Backend chỉ cần so sánh trực tiếp
     */
    private boolean checkTrueFalse(Question question, SubmitAnswerRequest answerRequest) {
        if (answerRequest.getAnswer() == null || answerRequest.getAnswer().trim().isEmpty()) {
            return false;
        }

        String userAnswer = answerRequest.getAnswer().trim().toLowerCase();
        String correctAnswer = question.getCorrectAnswer().trim().toLowerCase();

        // So sánh trực tiếp "true" với "true" hoặc "false" với "false"
        boolean isCorrect = userAnswer.equals(correctAnswer);

        log.debug("True/False check: user='{}', correct='{}', isCorrect={}",
                userAnswer, correctAnswer, isCorrect);

        return isCorrect;
    }

    /**
     * Kiểm tra câu trả lời ngắn
     * Linh hoạt hơn fill_blank
     */
    private boolean checkShortAnswer(Question question, SubmitAnswerRequest answerRequest) {
        if (answerRequest.getAnswer() == null || answerRequest.getAnswer().trim().isEmpty()) {
            return false;
        }

        String userAnswer = smartNormalize(answerRequest.getAnswer());
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        // Check exact match hoặc contains all keywords
        boolean isCorrect = Arrays.stream(correctAnswers)
                .anyMatch(correct -> {
                    String normalizedCorrect = smartNormalize(correct);
                    return containsAllKeywords(userAnswer, normalizedCorrect);
                });

        if (!isCorrect) {
            log.debug("Short answer incorrect - User: '{}', Expected keywords from: {}",
                    userAnswer, Arrays.toString(correctAnswers));
        }

        return isCorrect;
    }

    // ===== NORMALIZATION METHODS =====

    /**
     * Smart normalization - cho translation, fill_blank, short_answer
     */
    private String smartNormalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        return input.toLowerCase()
                .trim()
                // Expand contractions - be verbs
                .replaceAll("\\bi'm\\b", "i am")
                .replaceAll("\\byou're\\b", "you are")
                .replaceAll("\\bhe's\\b", "he is")
                .replaceAll("\\bshe's\\b", "she is")
                .replaceAll("\\bit's\\b", "it is")
                .replaceAll("\\bwe're\\b", "we are")
                .replaceAll("\\bthey're\\b", "they are")

                // Negative contractions
                .replaceAll("\\bdon't\\b", "do not")
                .replaceAll("\\bdoesn't\\b", "does not")
                .replaceAll("\\bdidn't\\b", "did not")
                .replaceAll("\\bwon't\\b", "will not")
                .replaceAll("\\bcan't\\b", "cannot")
                .replaceAll("\\bcouldn't\\b", "could not")
                .replaceAll("\\bshouldn't\\b", "should not")
                .replaceAll("\\bisn't\\b", "is not")
                .replaceAll("\\baren't\\b", "are not")
                .replaceAll("\\bwasn't\\b", "was not")
                .replaceAll("\\bweren't\\b", "were not")

                // Modal contractions
                .replaceAll("\\bi'll\\b", "i will")
                .replaceAll("\\byou'll\\b", "you will")
                .replaceAll("\\bhe'll\\b", "he will")
                .replaceAll("\\bshe'll\\b", "she will")
                .replaceAll("\\bwe'll\\b", "we will")
                .replaceAll("\\bthey'll\\b", "they will")

                .replaceAll("\\bi'd\\b", "i would")
                .replaceAll("\\byou'd\\b", "you would")
                .replaceAll("\\bhe'd\\b", "he would")
                .replaceAll("\\bwe'd\\b", "we would")

                .replaceAll("\\bi've\\b", "i have")
                .replaceAll("\\byou've\\b", "you have")
                .replaceAll("\\bwe've\\b", "we have")
                .replaceAll("\\bthey've\\b", "they have")

                // Remove punctuation
                .replaceAll("[.,!?;:\"'`()\\[\\]{}]", "")

                // Normalize spaces and dashes
                .replaceAll("[-—–]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Strict normalization - cho verb forms
     */
    private String strictNormalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        return input.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }

    // ===== HELPER METHODS =====

    /**
     * Tính độ tương đồng giữa 2 chuỗi (Levenshtein distance)
     */
    private double calculateSimilarity(String s1, String s2) {

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0)
            return 1.0;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Levenshtein distance algorithm
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1, // deletion
                        dp[i][j - 1] + 1), // insertion
                        dp[i - 1][j - 1] + cost // substitution
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Kiểm tra user answer có chứa tất cả keywords không
     */
    private boolean containsAllKeywords(String userAnswer, String correctAnswer) {
        String[] keywords = correctAnswer.split("\\s+");

        for (String keyword : keywords) {
            if (!userAnswer.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    // ===== INPUT VALIDATION =====

    /**
     * Kiểm tra input hợp lệ theo loại câu hỏi
     */
    public boolean isValidUserInput(String userAnswer, QuestionType questionType) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String normalized = smartNormalize(userAnswer);

        switch (questionType) {
            case FILL_BLANK:
            case VERB_FORM:
                return normalized.length() > 0 && normalized.length() <= 100;

            case TRANSLATE:
                String[] words = normalized.split("\\s+");
                return words.length >= 2 && normalized.length() <= 500;

            case SHORT_ANSWER:
                String[] answerWords = normalized.split("\\s+");
                return answerWords.length >= 1 && answerWords.length <= 50;

            case TRUE_FALSE:
                String tfAnswer = userAnswer.toLowerCase().trim();
                return tfAnswer.equals("true") || tfAnswer.equals("false");

            case MULTIPLE_CHOICE:
                return true; // Validated by selectedOptionId

            default:
                return true;
        }
    }

    // ===== HINT GENERATION - TỐI ƯU =====

    /**
     * Tạo gợi ý ĐỘNG dựa trên lỗi cụ thể của user
     */
    public String generateHint(Question question, SubmitAnswerRequest answerRequest) {
        String userAnswer = answerRequest.getAnswer();

        switch (question.getQuestionType()) {
            case FILL_BLANK:
                return generateFillBlankHint(question, userAnswer);

            case VERB_FORM:
                return generateVerbFormHint(question, userAnswer);

            case TRANSLATE:
                return generateTranslationHint(question, userAnswer);

            case TRUE_FALSE:
                return "💡 Đọc lại đoạn văn và chú ý các chi tiết quan trọng";

            case SHORT_ANSWER:
                return "💡 Trả lời ngắn gọn, tập trung vào ý chính của câu hỏi";

            case MULTIPLE_CHOICE:
                return "💡 Đọc kỹ các lựa chọn và loại trừ những đáp án rõ ràng sai";

            default:
                return "💡 Đáp án chưa chính xác, hãy thử lại!";
        }
    }

    /**
     * Hint động cho Fill Blank
     */
    private String generateFillBlankHint(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return "💡 Hãy điền một từ hoặc cụm từ phù hợp";
        }

        String normalized = smartNormalize(userAnswer);
        String correct = smartNormalize(question.getCorrectAnswer().split("\\|")[0]);

        // Check số ít/số nhiều
        if (normalized.endsWith("s") && !correct.endsWith("s")) {
            return "💡 Kiểm tra lại số ít/số nhiều của danh từ";
        }
        if (!normalized.endsWith("s") && correct.endsWith("s")) {
            return "💡 Danh từ này cần ở dạng số nhiều";
        }

        // Check dạng động từ
        if (normalized.endsWith("ing") && !correct.endsWith("ing")) {
            return "💡 Kiểm tra lại dạng của động từ (V-ing, to V, hay V nguyên thể)";
        }

        return "💡 Kiểm tra chính tả và dạng từ (số ít/nhiều, thì động từ)";
    }

    /**
     * Hint động cho Verb Form
     */
    private String generateVerbFormHint(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return "💡 Hãy chia động từ theo chủ ngữ và thì";
        }

        String normalized = strictNormalize(userAnswer);
        String correct = strictNormalize(question.getCorrectAnswer().split("\\|")[0]);

        // Check quá khứ vs hiện tại
        if (normalized.endsWith("ed") && !correct.endsWith("ed")) {
            return "💡 Kiểm tra thì của câu - có phải quá khứ không?";
        }

        // Check tiếp diễn
        if (normalized.endsWith("ing") && !correct.endsWith("ing")) {
            return "💡 Câu này có cần dùng thì tiếp diễn (-ing) không?";
        }

        // Check trợ động từ
        if (normalized.split("\\s+").length < correct.split("\\s+").length) {
            return "💡 Có thể bạn thiếu trợ động từ (am/is/are/was/were/have/has...)";
        }

        return "💡 Chú ý chia động từ theo chủ ngữ (I/You/He/She...) và thì (hiện tại/quá khứ/tương lai)";
    }

    /**
     * Hint động cho Translation
     */
    private String generateTranslationHint(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return "💡 Hãy dịch câu sang tiếng Anh";
        }

        // Sử dụng kết quả scoring để đưa hint cụ thể
        TranslationCheckResult result = checkTranslationWithScore(question, userAnswer);

        if (result.getScore() >= 50) {
            return "💡 Bản dịch gần đúng rồi! Kiểm tra lại ngữ pháp và từ vựng";
        }

        return "💡 Chú ý: thì của động từ, thứ tự từ, và các từ khóa quan trọng";
    }

    /**
     * Tạo explanation chi tiết khi user trả lời sai
     */
    public String generateDetailedFeedback(Question question, SubmitAnswerRequest answerRequest, boolean isCorrect) {
        if (isCorrect) {
            return "✅ Chính xác! " + (question.getExplanation() != null ? question.getExplanation() : "");
        }

        StringBuilder feedback = new StringBuilder("❌ Chưa chính xác. ");

        if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            feedback.append(question.getExplanation()).append("\n");
        }

        feedback.append(generateHint(question, answerRequest));

        return feedback.toString();
    }

    // ===== PHÂN TÍCH LỖI - CẢI THIỆN =====

    /**
     * Phân tích lỗi cụ thể của user để tracking và cải thiện hints
     * 
     * VÍ DỤ HOẠT ĐỘNG:
     * 
     * 1. VERB_FORM:
     * Question: "I (be) happy yesterday."
     * Correct: "was"
     * User: "am" → ERROR_WRONG_TENSE (dùng hiện tại thay vì quá khứ)
     * User: "were" → ERROR_WRONG_SUBJECT_VERB (chủ ngữ "I" cần "was" không phải
     * "were")
     * User: "being" → ERROR_WRONG_VERB_FORM (dùng V-ing thay vì quá khứ)
     * 
     * 2. FILL_BLANK:
     * Question: "She has three _____ (cat)"
     * Correct: "cats"
     * User: "cat" → ERROR_MISSING_PLURAL (thiếu "s" số nhiều)
     * User: "cates" → ERROR_SPELLING (sai chính tả)
     * 
     * 3. TRANSLATE:
     * Question: "Tôi thích học tiếng Anh"
     * Correct: "I like learning English"
     * User: "I like learn English" → ERROR_MISSING_ING (thiếu -ing sau like)
     * User: "I am like learning English" → ERROR_EXTRA_WORD (thừa "am")
     * User: "Me like learning English" → ERROR_WRONG_PRONOUN (dùng "me" thay vì
     * "I")
     */
    public ErrorAnalysisResult analyzeError(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return new ErrorAnalysisResult("EMPTY_ANSWER", "Câu trả lời trống", null);
        }

        String normalized = smartNormalize(userAnswer);
        String correct = smartNormalize(question.getCorrectAnswer().split("\\|")[0]);

        // Phân tích theo loại câu hỏi
        switch (question.getQuestionType()) {
            case VERB_FORM:
                return analyzeVerbFormError(normalized, correct, userAnswer);

            case FILL_BLANK:
                return analyzeFillBlankError(normalized, correct);

            case TRANSLATE:
                return analyzeTranslationError(question, normalized, correct);

            default:
                // Generic similarity-based analysis
                double similarity = calculateSimilarity(normalized, correct);
                if (similarity >= 0.8) {
                    return new ErrorAnalysisResult("MINOR_MISTAKE",
                            "Lỗi nhỏ (độ tương đồng " + (int) (similarity * 100) + "%)",
                            "Gần đúng rồi, kiểm tra lại chính tả hoặc dạng từ");
                } else if (similarity >= 0.5) {
                    return new ErrorAnalysisResult("PARTIAL_CORRECT",
                            "Đúng một phần (độ tương đồng " + (int) (similarity * 100) + "%)",
                            "Có một số từ đúng nhưng còn thiếu hoặc sai");
                }
                return new ErrorAnalysisResult("COMPLETELY_WRONG",
                        "Sai hoàn toàn (độ tương đồng " + (int) (similarity * 100) + "%)", null);
        }
    }

    /**
     * Phân tích lỗi VERB_FORM cụ thể
     */
    private ErrorAnalysisResult analyzeVerbFormError(String userAnswer, String correct, String original) {
        // Check thì sai
        if (userAnswer.endsWith("ed") && !correct.endsWith("ed")) {
            return new ErrorAnalysisResult("WRONG_TENSE_PAST",
                    "Dùng quá khứ trong khi câu cần thì khác",
                    "Kiểm tra thì của câu (hiện tại/quá khứ/tương lai)");
        }

        if (!userAnswer.endsWith("ed") && correct.endsWith("ed")) {
            return new ErrorAnalysisResult("WRONG_TENSE_NOT_PAST",
                    "Câu cần thì quá khứ nhưng bạn dùng thì khác",
                    "Chú ý thời gian trong câu (yesterday, last week...)");
        }

        if (userAnswer.endsWith("ing") && !correct.endsWith("ing")) {
            return new ErrorAnalysisResult("WRONG_CONTINUOUS_FORM",
                    "Dùng thì tiếp diễn (-ing) trong khi câu không cần",
                    "Kiểm tra xem câu có cần dùng thì tiếp diễn không");
        }

        // Check thiếu/thừa trợ động từ
        String[] userWords = userAnswer.split("\\s+");
        String[] correctWords = correct.split("\\s+");

        if (userWords.length < correctWords.length) {
            return new ErrorAnalysisResult("MISSING_AUXILIARY",
                    "Thiếu trợ động từ",
                    "Có thể cần thêm am/is/are/was/were/have/has/do/does...");
        }

        if (userWords.length > correctWords.length) {
            return new ErrorAnalysisResult("EXTRA_AUXILIARY",
                    "Thừa trợ động từ hoặc từ không cần thiết",
                    "Kiểm tra lại cấu trúc câu");
        }

        // Check subject-verb agreement (I/You vs He/She/It)
        if (userAnswer.endsWith("s") && !correct.endsWith("s") && !correct.endsWith("ed")) {
            return new ErrorAnalysisResult("WRONG_SUBJECT_VERB_AGREEMENT",
                    "Sai sự hòa hợp giữa chủ ngữ và động từ",
                    "Chú ý chủ ngữ là số ít (he/she/it) hay số nhiều (I/you/we/they)");
        }

        return new ErrorAnalysisResult("VERB_FORM_INCORRECT",
                "Dạng động từ không đúng",
                "Kiểm tra chia động từ theo chủ ngữ và thì");
    }

    /**
     * Phân tích lỗi FILL_BLANK cụ thể
     */
    private ErrorAnalysisResult analyzeFillBlankError(String userAnswer, String correct) {
        // Check số ít/số nhiều
        if (userAnswer.equals(correct.substring(0, correct.length() - 1)) && correct.endsWith("s")) {
            return new ErrorAnalysisResult("MISSING_PLURAL_S",
                    "Thiếu 's' số nhiều",
                    "Danh từ này cần ở dạng số nhiều");
        }

        if (userAnswer.equals(correct + "s") && !correct.endsWith("s")) {
            return new ErrorAnalysisResult("EXTRA_PLURAL_S",
                    "Thừa 's' - từ này là số ít",
                    "Danh từ này không cần thêm 's'");
        }

        // Check lỗi chính tả gần đúng
        double similarity = calculateSimilarity(userAnswer, correct);
        if (similarity >= 0.8) {
            return new ErrorAnalysisResult("SPELLING_ERROR",
                    "Lỗi chính tả nhỏ",
                    "Gần đúng rồi, kiểm tra lại chính tả");
        }

        // Check case sensitivity (nếu chỉ khác hoa/thường)
        if (userAnswer.equalsIgnoreCase(correct)) {
            return new ErrorAnalysisResult("CASE_ERROR",
                    "Sai chữ hoa/thường",
                    "Chú ý chữ hoa/chữ thường");
        }

        return new ErrorAnalysisResult("FILL_BLANK_INCORRECT",
                "Từ điền không đúng",
                "Kiểm tra lại từ vựng và ngữ cảnh câu");
    }

    /**
     * Phân tích lỗi TRANSLATION cụ thể
     */
    private ErrorAnalysisResult analyzeTranslationError(Question question, String userAnswer, String correct) {
        // Check keywords
        Set<String> keywords = extractKeywords(correct);
        Set<String> missingKeywords = new HashSet<>();
        Set<String> foundKeywords = new HashSet<>();

        for (String keyword : keywords) {
            if (userAnswer.contains(keyword)) {
                foundKeywords.add(keyword);
            } else {
                missingKeywords.add(keyword);
            }
        }

        // Thiếu từ khóa quan trọng
        if (!missingKeywords.isEmpty() && foundKeywords.size() < keywords.size() / 2) {
            return new ErrorAnalysisResult("MISSING_KEYWORDS",
                    "Thiếu các từ khóa quan trọng: " + String.join(", ", missingKeywords),
                    "Bổ sung các từ: " + String.join(", ", missingKeywords));
        }

        // Check từ thừa
        String[] userWords = userAnswer.split("\\s+");
        String[] correctWords = correct.split("\\s+");

        if (userWords.length > correctWords.length + 3) {
            return new ErrorAnalysisResult("TOO_MANY_WORDS",
                    "Câu dịch dài hơn cần thiết",
                    "Cố gắng dịch súc tích hơn, loại bỏ từ thừa");
        }

        // Check cấu trúc câu cơ bản (subject-verb-object)
        // Tiếng Anh: subject → verb → object
        if (userAnswer.length() >= 10) {
            String[] words = userAnswer.split("\\s+");
            if (words.length >= 3) {
                // Check xem có động từ chính không (rất đơn giản)
                boolean hasVerb = Arrays.stream(words).anyMatch(w -> w.matches(
                        ".*(am|is|are|was|were|be|have|has|had|do|does|did|like|love|go|come|make|take|get).*"));

                if (!hasVerb) {
                    return new ErrorAnalysisResult("MISSING_MAIN_VERB",
                            "Có thể thiếu động từ chính",
                            "Kiểm tra xem câu có đủ chủ ngữ-động từ-tân ngữ không");
                }
            }
        }

        // Check similarity tổng thể
        double similarity = calculateSimilarity(userAnswer, correct);

        if (similarity >= 0.7) {
            return new ErrorAnalysisResult("TRANSLATION_ALMOST_CORRECT",
                    "Bản dịch gần đúng (độ tương đồng " + (int) (similarity * 100) + "%)",
                    "Kiểm tra lại ngữ pháp và từ vựng chi tiết");
        } else if (similarity >= 0.4) {
            return new ErrorAnalysisResult("TRANSLATION_PARTIAL",
                    "Bản dịch đúng một phần (độ tương đồng " + (int) (similarity * 100) + "%)",
                    "Có một số từ đúng nhưng cấu trúc câu chưa chính xác");
        }

        return new ErrorAnalysisResult("TRANSLATION_INCORRECT",
                "Bản dịch chưa chính xác",
                "Xem lại cấu trúc câu và các từ khóa quan trọng");
    }

    /**
     * Class chứa kết quả phân tích lỗi
     */
    public static class ErrorAnalysisResult {
        private final String errorCode;
        private final String errorDescription;
        private final String suggestion;

        public ErrorAnalysisResult(String errorCode, String errorDescription, String suggestion) {
            this.errorCode = errorCode;
            this.errorDescription = errorDescription;
            this.suggestion = suggestion;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getSuggestion() {
            return suggestion;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s%s",
                    errorCode,
                    errorDescription,
                    suggestion != null ? " → " + suggestion : "");
        }
    }

    // ===== SCORING HELPERS - TỐI ƯU =====

    /**
     * Tính điểm partial cho các câu dịch và short answer
     * Sử dụng weighted scoring: keywords (60%) + similarity (40%)
     */
    public int calculatePartialScore(String userAnswer, String correctAnswer, int maxPoints) {
        if (userAnswer == null || correctAnswer == null) {
            return 0;
        }

        String normalizedUser = smartNormalize(userAnswer);
        String normalizedCorrect = smartNormalize(correctAnswer);

        // Exact match = full points
        if (normalizedUser.equals(normalizedCorrect)) {
            return maxPoints;
        }

        // Keyword matching (60%)
        Set<String> keywords = extractKeywords(normalizedCorrect);
        long matchedKeywords = keywords.stream()
                .filter(normalizedUser::contains)
                .count();

        double keywordRatio = keywords.isEmpty() ? 0 : (double) matchedKeywords / keywords.size();

        // Similarity (40%)
        double similarity = calculateSimilarity(normalizedUser, normalizedCorrect);

        // Weighted score
        double weightedScore = (keywordRatio * 0.6) + (similarity * 0.4);

        // Convert to points with thresholds
        if (weightedScore >= 0.95) {
            return maxPoints;
        } else if (weightedScore >= 0.85) {
            return (int) (maxPoints * 0.9);
        } else if (weightedScore >= 0.70) {
            return (int) (maxPoints * 0.8);
        } else if (weightedScore >= 0.50) {
            return (int) (maxPoints * 0.5);
        } else if (weightedScore >= 0.30) {
            return (int) (maxPoints * 0.3);
        }

        return 0;
    }

    /**
     * Kiểm tra nếu đáp án "gần đúng" (cho warning/suggestion)
     * Dùng để hiển thị message "Gần đúng rồi, thử lại xem!"
     */
    public boolean isAlmostCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }

        String normalizedUser = smartNormalize(userAnswer);
        String normalizedCorrect = smartNormalize(correctAnswer);

        // Check similarity >= 75%
        double similarity = calculateSimilarity(normalizedUser, normalizedCorrect);
        if (similarity >= 0.75 && similarity < 1.0) {
            return true;
        }

        // Hoặc check keywords >= 80%
        Set<String> keywords = extractKeywords(normalizedCorrect);
        if (!keywords.isEmpty()) {
            long matched = keywords.stream()
                    .filter(normalizedUser::contains)
                    .count();
            double keywordRatio = (double) matched / keywords.size();
            return keywordRatio >= 0.8;
        }

        return false;
    }

    // ===== STATISTICS HELPERS =====

    /**
     * Thống kê loại lỗi phổ biến
     * Dùng để admin/teacher thấy học viên hay mắc lỗi gì
     */
    public Map<String, Integer> analyzeCommonErrors(List<ErrorAnalysisResult> errorResults) {
        Map<String, Integer> errorFrequency = new HashMap<>();

        for (ErrorAnalysisResult result : errorResults) {
            String code = result.getErrorCode();
            errorFrequency.put(code, errorFrequency.getOrDefault(code, 0) + 1);
        }

        // Sort by frequency
        return errorFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}

// ============================================
// VÍ DỤ SỬ DỤNG TRONG SERVICE
// ============================================

/*
 * EXAMPLE 1: Check đáp án cơ bản
 * 
 * Question question = questionRepository.findById(questionId).orElseThrow();
 * SubmitAnswerRequest request = new SubmitAnswerRequest(questionId, "was",
 * null);
 * 
 * boolean isCorrect = answerCheckingService.checkAnswer(question, request);
 * // → true nếu đúng, false nếu sai
 * 
 * 
 * EXAMPLE 2: Check translation với scoring chi tiết
 * 
 * Question translationQ =
 * questionRepository.findById(questionId).orElseThrow();
 * String userAnswer = "I like learning English";
 * 
 * TranslationCheckResult result =
 * answerCheckingService.checkTranslationWithScore(
 * translationQ, userAnswer);
 * 
 * System.out.println("Correct: " + result.isCorrect());
 * System.out.println("Score: " + result.getScore() + "%");
 * System.out.println("Feedback: " + result.getFeedback());
 * // Output:
 * // Correct: true
 * // Score: 100
 * // Feedback: ✅ Hoàn hảo! Bản dịch chính xác 100%
 * 
 * 
 * EXAMPLE 3: Phân tích lỗi chi tiết
 * 
 * Question verbQ = questionRepository.findById(questionId).orElseThrow();
 * String wrongAnswer = "am";
 * 
 * ErrorAnalysisResult errorAnalysis = answerCheckingService.analyzeError(
 * verbQ, wrongAnswer);
 * 
 * System.out.println(errorAnalysis);
 * // Output: [WRONG_TENSE_NOT_PAST] Câu cần thì quá khứ nhưng bạn dùng thì khác
 * // → Chú ý thời gian trong câu (yesterday, last week...)
 * 
 * 
 * EXAMPLE 4: Generate dynamic hint
 * 
 * SubmitAnswerRequest wrongRequest = new SubmitAnswerRequest(questionId, "am",
 * null);
 * String hint = answerCheckingService.generateHint(verbQ, wrongRequest);
 * 
 * System.out.println(hint);
 * // Output: 💡 Câu này có phải quá khứ không? Kiểm tra thì của câu
 * 
 * 
 * EXAMPLE 5: Preview keywords cho admin
 * 
 * String correctAnswer = "I like learning English|I love to learn English";
 * KeywordPreviewDTO preview = answerCheckingService.previewTranslationKeywords(
 * correctAnswer);
 * 
 * System.out.println("Primary keywords: " + preview.getPrimaryKeywords());
 * // Output: [like, learning, english]
 * 
 * System.out.println("Logic: " + preview.getCheckingLogic());
 * // Output: Hệ thống check: exact match HOẶC có >= 75% keywords HOẶC
 * similarity >= 85%
 * 
 * 
 * EXAMPLE 6: Check almost correct (cho UI feedback)
 * 
 * boolean almostCorrect = answerCheckingService.isAlmostCorrect(
 * "I likes learning English", "I like learning English");
 * 
 * if (almostCorrect) {
 * System.out.println("⚠️ Gần đúng rồi! Thử lại xem sao");
 * }
 * // Output: ⚠️ Gần đúng rồi! Thử lại xem sao (similarity = 95%)
 */