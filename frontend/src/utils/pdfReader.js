import * as pdfjsLib from 'pdfjs-dist';

// SỬ DỤNG CDN - Đảm bảo hoạt động
pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.mjs`;

// Debug
console.log('PDF.js version:', pdfjsLib.version);
console.log('Worker src:', pdfjsLib.GlobalWorkerOptions.workerSrc);

/**
 * Extract text from PDF file
 * @param {ArrayBuffer} arrayBuffer - PDF file as ArrayBuffer
 * @param {Object} options - Extraction options
 * @returns {Promise<string>} - Extracted text content
 */
export const extractTextFromPDF = async (arrayBuffer, options = {}) => {
  try {
    console.log('Starting PDF extraction...');
    console.log('ArrayBuffer size:', arrayBuffer.byteLength);
    
    const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer });
    const pdf = await loadingTask.promise;
    
    console.log('PDF loaded successfully. Pages:', pdf.numPages);
    
    const {
      skipFirstPages = 2,
      skipLastPages = 0,
      removeHeaders = true,
      removeFooters = true,
    } = options;
    
    // Đảm bảo không skip hết tất cả các trang
    const startPage = Math.min(skipFirstPages + 1, pdf.numPages);
    const endPage = Math.max(pdf.numPages - skipLastPages, startPage);
    
    console.log(`Processing pages from ${startPage} to ${endPage}`);
    
    let fullText = '';
    const headerFooterPatterns = new Set();
    
    // First pass: Collect potential headers/footers (chỉ khi có đủ trang)
    if ((removeHeaders || removeFooters) && pdf.numPages >= 3) {
      const samplePages = Math.min(3, endPage - startPage + 1);
      const lineFrequency = {};
      
      for (let i = 0; i < samplePages; i++) {
        const pageNum = startPage + i;
        const page = await pdf.getPage(pageNum);
        const textContent = await page.getTextContent();
        const lines = textContent.items
          .map(item => item.str.trim())
          .filter(line => line.length > 0);
        
        const checkLines = [
          ...lines.slice(0, 2),
          ...lines.slice(-2)
        ];
        
        checkLines.forEach(line => {
          lineFrequency[line] = (lineFrequency[line] || 0) + 1;
        });
      }
      
      // Lines appearing in 2+ pages are likely headers/footers
      Object.entries(lineFrequency).forEach(([line, count]) => {
        if (count >= 2) {
          headerFooterPatterns.add(line);
        }
      });
      
      console.log('Detected header/footer patterns:', headerFooterPatterns.size);
    }
    
    // Second pass: Extract content
    for (let pageNum = startPage; pageNum <= endPage; pageNum++) {
      try {
        const page = await pdf.getPage(pageNum);
        const textContent = await page.getTextContent();
        
        const pageLines = textContent.items
          .map(item => item.str.trim())
          .filter(line => line.length > 0);
        
        console.log(`Page ${pageNum}: ${pageLines.length} lines`);
        
        // Filter với điều kiện lỏng hơn
        const filteredLines = pageLines.filter(line => {
          if (headerFooterPatterns.has(line)) return false;
          if (isHeaderFooterPattern(line)) return false;
          if (/^[\d\s]+$/.test(line) && line.length < 5) return false;
          return true;
        });
        
        console.log(`Page ${pageNum}: ${filteredLines.length} lines after filtering`);
        
        const pageText = filteredLines.join(' ');
        if (pageText.trim().length > 0) {
          fullText += pageText + '\n\n';
        }
      } catch (pageError) {
        console.error(`Error processing page ${pageNum}:`, pageError);
        // Continue with next page
      }
    }
    
    console.log('Total extracted text length:', fullText.length);
    
    if (fullText.trim().length === 0) {
      throw new Error('Không thể trích xuất được nội dung từ PDF. File có thể là ảnh scan hoặc bị bảo vệ.');
    }
    
    return fullText.trim();
  } catch (error) {
    console.error('PDF extraction error:', error);
    throw new Error(`Không thể đọc file PDF: ${error.message}`);
  }
};

/**
 * Check if a line matches common header/footer patterns
 * @param {string} line - Line to check
 * @returns {boolean}
 */
const isHeaderFooterPattern = (line) => {
  const patterns = [
    /^(Trung tâm|Website|Hotline|Fanpage|Email)/i,  // Contact info
    /^(Page|Trang)\s*\d+/i,                          // Page numbers
    /^https?:\/\//i,                                 // URLs
    /^\d{1,3}\s*$/,                                  // Standalone numbers
    /^(Chapter|Chương|Unit|Bài)\s*\d+\s*$/i,        // Chapter headers only
    /^[-=_]{3,}$/,                                   // Decorative lines
  ];
  
  return patterns.some(pattern => pattern.test(line));
};

/**
 * Extract text from DOCX file using mammoth
 * @param {ArrayBuffer} arrayBuffer - DOCX file as ArrayBuffer
 * @param {Object} mammoth - Mammoth library instance
 * @returns {Promise<string>} - Extracted text content
 */
export const extractTextFromDOCX = async (arrayBuffer, mammoth) => {
  try {
    const result = await mammoth.extractRawText({ arrayBuffer });
    return result.value;
  } catch (error) {
    console.error('DOCX extraction error:', error);
    throw new Error('Không thể đọc file DOCX. Vui lòng kiểm tra file có hợp lệ không.');
  }
};

/**
 * Analyze content to separate into structured sections
 * @param {string} text - Extracted text content
 * @param {Object} options - Analysis options
 * @returns {Object} - Object containing structured content
 */
export const analyzeContent = (text, options = {}) => {
  const {
    minSectionLength = 50,  // Bỏ qua section quá ngắn
    minQuestionLength = 20, // Bỏ qua câu hỏi quá ngắn
  } = options;
  
  const lines = text.split('\n').filter(line => {
    const trimmed = line.trim();
    // Bỏ qua dòng rỗng và dòng chỉ chứa ký tự đặc biệt
    if (trimmed.length === 0) return false;
    if (/^[^\w\sÀ-ỹ]+$/.test(trimmed)) return false;
    return true;
  });
  
  // Detect sections based on patterns
  const sections = [];
  let currentSection = null;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    
    // Skip if line is too short or looks like noise
    if (line.length < 3) continue;
    if (isNoiseLine(line)) continue;
    
    // Detect main section headers (UPPERCASE with potential Roman numerals)
    if (line.match(/^[IVX]+\.\s+[A-ZÀÁẢÃẠÂẦẤẨẪẬĂẰẮẲẴẶÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ\s]+$/)) {
      if (currentSection && currentSection.content.length > 0) {
        sections.push(currentSection);
      }
      currentSection = {
        title: line,
        content: [],
        type: 'theory'
      };
    }
    // Detect section headers (Title Case or specific patterns)
    else if (line.match(/^(UNIT|BÀI|PHẦN)\s+\d+/i) || 
             line.match(/^[A-ZÀÁẢÃẠ][a-zàáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ\s]+–\s+[A-Z]/)) {
      if (currentSection && currentSection.content.length > 0) {
        sections.push(currentSection);
      }
      currentSection = {
        title: line,
        content: [],
        type: 'theory'
      };
    }
    // Detect exercise sections
    else if (line.match(/^(BÀI TẬP|EXERCISE|PRACTICE|LUYỆN TẬP)/i)) {
      if (currentSection && currentSection.content.length > 0) {
        sections.push(currentSection);
      }
      currentSection = {
        title: line,
        content: [],
        type: 'exercise'
      };
    }
    // Detect subsections (numbered patterns)
    else if (line.match(/^\d+\.\s+[A-ZÀÁẢÃẠ]/)) {
      if (currentSection) {
        currentSection.content.push({
          subsection: line,
          details: []
        });
      }
    }
    // Regular content
    else if (line.length > 0 && currentSection) {
      if (currentSection.content.length > 0 && 
          typeof currentSection.content[currentSection.content.length - 1] === 'object' &&
          currentSection.content[currentSection.content.length - 1].details) {
        currentSection.content[currentSection.content.length - 1].details.push(line);
      } else {
        currentSection.content.push(line);
      }
    }
  }
  
  if (currentSection && currentSection.content.length > 0) {
    sections.push(currentSection);
  }
  
  // Filter out sections that are too short (likely noise)
  const filteredSections = sections.filter(section => {
    const contentLength = JSON.stringify(section.content).length;
    return contentLength >= minSectionLength;
  });
  
  // Parse exercises from exercise sections
  const exerciseQuestions = parseExercises(
    filteredSections.filter(s => s.type === 'exercise'),
    { minQuestionLength }
  );
  
  // Combine theory sections
  const theorySections = filteredSections.filter(s => s.type === 'theory');
  const theoryContent = formatTheoryContent(theorySections);
  
  return {
    theory: theoryContent,
    sections: theorySections,
    exercises: exerciseQuestions,
    rawSections: filteredSections
  };
};

/**
 * Check if a line is noise (contact info, URLs, etc.)
 * @param {string} line - Line to check
 * @returns {boolean}
 */
const isNoiseLine = (line) => {
  const noisePatterns = [
    /^(Trung tâm|Website|Hotline|Fanpage|Email|Phone|Tel)/i,
    /^https?:\/\//i,
    /^www\./i,
    /\d{10,}/, // Long numbers (phone numbers)
    /\d{3}-\d{3}-\d{4}/, // Formatted phone numbers  // Formatted phone numbers
    /^[^\w\sÀ-ỹ]+$/,  // Only special characters
  ];
  
  return noisePatterns.some(pattern => pattern.test(line));
};

/**
 * Format theory content into readable structure
 */
const formatTheoryContent = (sections) => {
  let formatted = '';
  
  sections.forEach(section => {
    formatted += `\n${section.title}\n`;
    formatted += '='.repeat(section.title.length) + '\n\n';
    
    section.content.forEach(item => {
      if (typeof item === 'string') {
        formatted += item + '\n';
      } else if (item.subsection) {
        formatted += `\n${item.subsection}\n`;
        item.details.forEach(detail => {
          formatted += `  ${detail}\n`;
        });
      }
    });
    formatted += '\n';
  });
  
  return formatted.trim();
};

/**
 * Parse exercises into structured format
 * @param {Array} exerciseSections - Exercise sections to parse
 * @param {Object} options - Parsing options
 * @returns {Array} - Parsed questions
 */
const parseExercises = (exerciseSections, options = {}) => {
  const { minQuestionLength = 20 } = options;
  const questions = [];
  
  exerciseSections.forEach(section => {
    let currentQuestion = null;
    
    section.content.forEach(item => {
      const line = typeof item === 'string' ? item : item.subsection;
      
      // Skip noise lines
      if (isNoiseLine(line)) return;
      
      // Detect question start (numbered questions)
      if (line.match(/^\d+\.\s+/)) {
        if (currentQuestion && currentQuestion.questionText.length >= minQuestionLength) {
          questions.push(currentQuestion);
        }
        currentQuestion = {
          questionText: line.replace(/^\d+\.\s+/, ''),
          options: [],
          correctAnswer: '',
          explanation: ''
        };
      }
      // Detect options (A. B. C. D. or a. b. c. d.)
      else if (currentQuestion && line.match(/^[A-Da-d]\.|^[○●]\s/)) {
        currentQuestion.options.push({
          optionText: line,
          isCorrect: false
        });
      }
      // Detect answer pattern
      else if (currentQuestion && line.match(/^(Answer|Đáp án|ĐÁP ÁN):/i)) {
        const answerMatch = line.match(/[A-Da-d]/);
        if (answerMatch) {
          currentQuestion.correctAnswer = answerMatch[0].toUpperCase();
          // Mark correct option
          const correctIndex = currentQuestion.correctAnswer.charCodeAt(0) - 65;
          if (currentQuestion.options[correctIndex]) {
            currentQuestion.options[correctIndex].isCorrect = true;
          }
        }
      }
      // Add to explanation or question text
      else if (currentQuestion) {
        if (currentQuestion.options.length > 0) {
          currentQuestion.explanation += (currentQuestion.explanation ? ' ' : '') + line;
        } else {
          currentQuestion.questionText += ' ' + line;
        }
      }
    });
    
    // Don't forget the last question
    if (currentQuestion && currentQuestion.questionText.length >= minQuestionLength) {
      questions.push(currentQuestion);
    }
  });
  
  // Clean up questions
  return questions.map(q => ({
    ...q,
    questionText: q.questionText.trim(),
    explanation: q.explanation.trim(),
  })).filter(q => q.options.length > 0 || q.correctAnswer);
};

/**
 * Create lessons from analyzed content
 * Intelligently split content into multiple lessons based on sections
 */
export const createLessonsFromContent = (analyzedContent, topicId) => {
  const lessons = [];
  let orderIndex = 1;
  
  // Group sections into logical lessons
  const theorySections = analyzedContent.sections.filter(s => s.type === 'theory');
  
  theorySections.forEach(section => {
    // Create theory lesson for each major section
    lessons.push({
      topicId: parseInt(topicId),
      title: section.title,
      lessonType: 'THEORY',
      content: formatSectionContent(section),
      orderIndex: orderIndex++,
      pointsRequired: (orderIndex - 2) * 10, // Progressive difficulty
      pointsReward: 10,
      isActive: true
    });
    
    // If there are exercises, create a practice lesson
    if (analyzedContent.exercises.length > 0) {
      const relatedExercises = analyzedContent.exercises.slice(0, 10); // Take first 10 questions
      if (relatedExercises.length > 0) {
        lessons.push({
          topicId: parseInt(topicId),
          title: `Bài tập - ${section.title}`,
          lessonType: 'PRACTICE',
          content: '',
          orderIndex: orderIndex++,
          pointsRequired: (orderIndex - 2) * 10,
          pointsReward: 15,
          isActive: true,
          questions: relatedExercises.map(q => ({
            questionText: q.questionText,
            questionType: 'MULTIPLE_CHOICE',
            options: q.options,
            correctAnswer: q.correctAnswer,
            explanation: q.explanation,
            points: 10
          }))
        });
      }
    }
  });
  
  return lessons;
};

/**
 * Format a single section content
 */
const formatSectionContent = (section) => {
  let content = '';
  
  section.content.forEach(item => {
    if (typeof item === 'string') {
      content += item + '\n\n';
    } else if (item.subsection) {
      content += `${item.subsection}\n`;
      item.details.forEach(detail => {
        content += `  ${detail}\n`;
      });
      content += '\n';
    }
  });
  
  return content.trim();
};