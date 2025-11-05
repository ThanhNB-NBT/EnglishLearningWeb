import { useState } from 'react';
import { readingAdminAPI } from '../../api/modules/reading.api';
import toast from 'react-hot-toast';

export const useReadingParse = (onImportSuccess) => {
  const [uploading, setUploading] = useState(false);
  const [parseDialog, setParseDialog] = useState({
    open: false,
    parsedData: null,
    summary: null,
  });

  const handleFileSelect = (file) => {
    if (!file) return;

    const validTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'image/jpeg',
      'image/jpg',
      'image/png',
      'image/webp',
    ];

    if (!validTypes.includes(file.type)) {
      toast.error('Chỉ hỗ trợ file PDF, DOCX, JPG, PNG, WEBP');
      return;
    }

    if (file.size > 20 * 1024 * 1024) {
      toast.error('File không được vượt quá 20MB');
      return;
    }

    handleParseFile(file);
  };

  const handleParseFile = async (file) => {
    setUploading(true);
    const fileName = file.name.length > 30 
      ? file.name.substring(0, 27) + '...' 
      : file.name;
    
    const parsePromise = readingAdminAPI.parseFile(file);
    
    toast.promise(
      parsePromise,
      {
        loading: `Đang parse ${fileName}...`,
        success: (response) => {
          const { lesson, summary } = response.data.data;

          setParseDialog({
            open: true,
            parsedData: lesson,
            summary: {
              fileName: summary?.fileName ?? file.name,
              fileSize: summary?.fileSize ?? '',
              title: lesson?.title ?? '',
              contentLength: lesson?.content?.length ?? 0,
              translationLength: lesson?.contentTranslation?.length ?? 0,
              hasTranslation: !!lesson?.contentTranslation,
              questionCount: lesson?.questions?.length ?? 0,
              multipleChoice: lesson?.questions?.filter(q => q.questionType === 'MULTIPLE_CHOICE').length ?? 0,
              fillBlank: lesson?.questions?.filter(q => q.questionType === 'FILL_BLANK').length ?? 0,
            },
          });

          return `Parse thành công bài đọc: ${lesson?.title}`;
        },
        error: (error) => {
          console.error('Parse file error:', error);
          return error.response?.data?.message || 'Lỗi khi parse file.';
        },
      }
    );
    
    try {
      await parsePromise;
    } finally {
      setUploading(false);
    }
  };

  const handleImportLesson = async (parsedLesson) => {
    const importPromise = readingAdminAPI.saveParsedLesson(parsedLesson);
    
    toast.promise(
      importPromise,
      {
        loading: `Đang import bài đọc: ${parsedLesson.title}...`,
        success: () => {
          closeParseDialog();
          if (onImportSuccess) {
            onImportSuccess();
          }
          return `Import thành công bài đọc: ${parsedLesson.title}`;
        },
        error: (error) => {
          console.error('Import lesson error:', error);
          return error.response?.data?.message || 'Lỗi khi import bài đọc.';
        },
      }
    );
    
    try {
      await importPromise;
    } catch (error) {
      console.error('Import lesson failed:', error);
    }
  };

  const closeParseDialog = () => {
    setParseDialog({ open: false, parsedData: null, summary: null });
  };

  return {
    uploading,
    parseDialog,
    handleFileSelect,
    closeParseDialog,
    handleImportLesson,
  };
};