package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.grammar.GrammarQuestion;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class AnswerCheckingService {
    
    // Kiểm tra đáp án chính
    public boolean checkAnswer(GrammarQuestion question, SubmitAnswerRequest answerRequest) {
        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE:
                return checkMultipleChoice(question, answerRequest);
            case FILL_BLANK:
                return checkFillBlank(question, answerRequest);
            case TRANSLATION_VI_EN:
            case TRANSLATION_EN_VI:
                return checkTranslation(question, answerRequest);
            default:
                log.warn("Không hỗ trợ loại câu: {}", question.getQuestionType());
                return false;
        }
    }

    // Kiểm tra đáp án cho câu hỏi trắc nghiệm
    private boolean checkMultipleChoice(GrammarQuestion question, SubmitAnswerRequest answerRequest) {
        if (answerRequest.getSelectedOptionId() == null) {
            return false;
        }

        return question.getOptions().stream()
                .anyMatch(option -> option.getId().equals(answerRequest.getSelectedOptionId())
                        && option.getIsCorrect());
    }

    // Kiểm tra đáp án cho câu hỏi điền từ
    private boolean checkFillBlank(GrammarQuestion question, SubmitAnswerRequest answerRequest) {
        String userAnswer = smartNormalize(answerRequest.getAnswer());
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        return Arrays.stream(correctAnswers)
                .anyMatch(correct -> userAnswer.equals(smartNormalize(correct)));
    }

    // Kiểm tra đáp án cho câu hỏi dịch
    private boolean checkTranslation(GrammarQuestion question, SubmitAnswerRequest answerRequest) {
        String userAnswer = smartNormalize(answerRequest.getAnswer());
        String[] correctAnswers = question.getCorrectAnswer().split("\\|");

        boolean isCorrect = Arrays.stream(correctAnswers)
                .anyMatch(correct -> userAnswer.equals(smartNormalize(correct)));

        // Ghi log 
        if (!isCorrect) {
            log.debug("Bản dịch không chính xác - User: '{}', Correct answers: {}", 
                userAnswer,
                Arrays.stream(correctAnswers)
                    .map(this::smartNormalize)
                    .toArray());
        }
        return isCorrect;
    }

    // Chuẩn hóa chuỗi để so sánh
    private String smartNormalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        return input.toLowerCase()
                .trim()
                
                // 1. Contractions chuẩn (không thay đổi ngữ pháp)
                .replace("i'm", "i am")
                .replace("you're", "you are")
                .replace("he's", "he is")
                .replace("she's", "she is") 
                .replace("it's", "it is")
                .replace("we're", "we are")
                .replace("they're", "they are")
                .replace("there's", "there is")
                .replace("here's", "here is")
                .replace("that's", "that is")
                .replace("what's", "what is")
                .replace("who's", "who is")
                .replace("where's", "where is")
                .replace("when's", "when is")
                .replace("how's", "how is")
                
                // Negative contractions
                .replace("don't", "do not")
                .replace("doesn't", "does not")
                .replace("didn't", "did not")
                .replace("won't", "will not")
                .replace("wouldn't", "would not")
                .replace("can't", "cannot")
                .replace("couldn't", "could not")
                .replace("shouldn't", "should not")
                .replace("mustn't", "must not")
                .replace("needn't", "need not")
                .replace("haven't", "have not")
                .replace("hasn't", "has not")
                .replace("hadn't", "had not")
                .replace("aren't", "are not")
                .replace("isn't", "is not")
                .replace("wasn't", "was not")
                .replace("weren't", "were not")
                
                // Modal contractions
                .replace("i'll", "i will")
                .replace("you'll", "you will")
                .replace("he'll", "he will")
                .replace("she'll", "she will")
                .replace("it'll", "it will")
                .replace("we'll", "we will")
                .replace("they'll", "they will")
                
                .replace("i'd", "i would")
                .replace("you'd", "you would")
                .replace("he'd", "he would")
                .replace("she'd", "she would")
                .replace("it'd", "it would")
                .replace("we'd", "we would")
                .replace("they'd", "they would")
                
                .replace("i've", "i have")
                .replace("you've", "you have")
                .replace("we've", "we have")
                .replace("they've", "they have")
                
                // 2. Bỏ punctuation (không ảnh hưởng nghĩa)
                .replaceAll("[.,!?;:\"'`()\\[\\]{}]", "")
                
                // 3. Normalize hyphens và dashes
                .replaceAll("[-–—]", " ")
                
                // 4. Normalize multiple spaces
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Kiểm tra input
    public boolean isValidUserInput(String userAnswer, QuestionType questionType) {
        if ( userAnswer == null || userAnswer.trim().isEmpty()){
            return false;
        }

        String normalized = smartNormalize(userAnswer);

        switch (questionType) {
            case FILL_BLANK:
                // Câu điền từ thường là từ đơn hoặc cụm từ ngắn
                return normalized.length() > 0 && normalized.length() <= 100;

            case TRANSLATION_EN_VI:
            case TRANSLATION_VI_EN:
                // Câu dịch cần ít nhất 2 từ
                String[] words = normalized.split("\\s+ ");
                return words.length >= 2 && normalized.length() <= 500;

            default:
                return true;
        }
    }

    // Tạo gợi ý(hint) cho người dùng khi sai
    public String generateHint(GrammarQuestion question, SubmitAnswerRequest answerRequest) {
        if (question.getQuestionType() == QuestionType.FILL_BLANK) {
            return "Kiểm tra lại dạng của từ (số ít/số nhiều, thì của động từ)";
        }

        if (question.getQuestionType() == QuestionType.TRANSLATION_EN_VI ||
            question.getQuestionType() == QuestionType.TRANSLATION_VI_EN) {
            return "Kiểm tra lại ngữ pháp và từ vựng. Lưu ý thì của động từ và chủ ngữ";
        }

        return "Đáp án chưa chính xác, hãy thử lại!";
    }

}
