import { useState } from 'react';
import { grammarAdminAPI } from '../../api/modules/grammar.api';
import toast from 'react-hot-toast'; // ✅ FIX: Import từ react-hot-toast

export const useGrammarPDFParse = (topicId, onImportSuccess) => {
  const [uploading, setUploading] = useState(false);
  const [pdfDialog, setPdfDialog] = useState({
    open: false,
    parsedData: null,
    summary: null,
  });
  const [pageSelector, setPageSelector] = useState({
    open: false,
    file: null,
    setOpen: (open) => setPageSelector(prev => ({ ...prev, open })),
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

    if (file.type === 'application/pdf') {
      setPageSelector({ 
        open: true, 
        file, 
        setOpen: (open) => setPageSelector(prev => ({ ...prev, open }))
      });
    } else {
      handleParseFile(file, null);
    }
  };

  const handlePageSelectionConfirm = async (selectedPages) => {
    await handleParseFile(pageSelector.file, selectedPages);
    setPageSelector({ open: false, file: null, setOpen: () => {} });
  };

  const handleParseFile = async (file, pages = null) => {
    setUploading(true);
    const fileName = file.name.length > 30 
      ? file.name.substring(0, 27) + '...' 
      : file.name;
    
    // ✅ FIX: Use react-hot-toast promise API
    const parsePromise = grammarAdminAPI.parseFile(topicId, file, pages);
    
    toast.promise(
      parsePromise,
      {
        loading: `Đang parse ${fileName}... ${pages ? `(${pages.length} trang)` : ''}`,
        success: (response) => {
          const { parsedData, summary } = response.data.data;

          setPdfDialog({
            open: true,
            parsedData,
            summary: {
              totalLessons: summary?.totalLessons ?? parsedData?.lessons?.length ?? 0,
              theoryLessons: summary?.theoryLessons ?? 0,
              practiceLessons: summary?.practiceLessons ?? 0,
              totalQuestions: summary?.totalQuestions ?? 0,
              fileSize: summary?.fileSize ?? '',
              fileName: summary?.fileName ?? '',
            },
          });

          return `✅ Parse thành công ${summary?.totalLessons ?? parsedData?.lessons?.length ?? 0} bài học!`;
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

  const handleImportLessons = async (selectedLessons) => {
    const importPromise = grammarAdminAPI.saveParsedLessons(topicId, selectedLessons);
    
    toast.promise(
      importPromise,
      {
        loading: `Đang import ${selectedLessons.length} bài học...`,
        success: () => {
          closePdfDialog();
          if (onImportSuccess) {
            onImportSuccess();
          }
          return `✅ Import thành công ${selectedLessons.length} bài học!`;
        },
        error: (error) => {
          console.error('Import lessons error:', error);
          return error.response?.data?.message || 'Lỗi khi import bài học.';
        },
      }
    );
    
    try {
      await importPromise;
    } catch (error) {
      console.error('Import lessons failed:', error);
    }
  };

  const closePdfDialog = () => {
    setPdfDialog({ open: false, parsedData: null, summary: null });
  };

  return {
    uploading,
    pdfDialog,
    pageSelector,
    handleFileSelect,
    handlePageSelectionConfirm,
    closePdfDialog,
    handleImportLessons,
  };
};