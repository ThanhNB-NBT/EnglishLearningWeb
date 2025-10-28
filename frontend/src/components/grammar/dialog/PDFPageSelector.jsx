import React, { useState, useEffect } from 'react';
import { Viewer, Worker } from '@react-pdf-viewer/core';
import { defaultLayoutPlugin } from '@react-pdf-viewer/default-layout';
import '@react-pdf-viewer/core/lib/styles/index.css';
import '@react-pdf-viewer/default-layout/lib/styles/index.css';
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
  Checkbox,
  Card,
  CardBody,
  Spinner,
  Input,
  IconButton,
} from '@material-tailwind/react';
import { XMarkIcon } from '@heroicons/react/24/outline';

const PDFPageSelector = ({ open, file, onClose, onConfirm }) => {
  const [numPages, setNumPages] = useState(0);
  const [selectedPages, setSelectedPages] = useState(new Set());
  const [loading, setLoading] = useState(false);
  const [rangeStart, setRangeStart] = useState('');
  const [rangeEnd, setRangeEnd] = useState('');
  const [fileUrl, setFileUrl] = useState('');

  const defaultLayoutPluginInstance = defaultLayoutPlugin();

  // Create file URL when dialog opens
  useEffect(() => {
    if (open && file) {
      const url = URL.createObjectURL(file);
      setFileUrl(url);
      console.log('📄 File URL created:', url);
      
      return () => {
        URL.revokeObjectURL(url);
        setFileUrl('');
      };
    }
  }, [open, file]);

  const handleDocumentLoad = (e) => {
    const pages = e.doc.numPages;
    console.log('✅ PDF loaded successfully:', pages, 'pages');
    setNumPages(pages);
    
    // Auto select first 20 pages
    const initial = new Set();
    for (let i = 1; i <= Math.min(20, pages); i++) {
      initial.add(i);
    }
    setSelectedPages(initial);
    console.log('✅ Auto-selected', initial.size, 'pages');
  };

  const handlePageToggle = (pageNum) => {
    const newSelected = new Set(selectedPages);
    if (newSelected.has(pageNum)) {
      newSelected.delete(pageNum);
    } else {
      newSelected.add(pageNum);
    }
    setSelectedPages(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedPages.size === numPages) {
      setSelectedPages(new Set());
    } else {
      const all = new Set();
      for (let i = 1; i <= numPages; i++) {
        all.add(i);
      }
      setSelectedPages(all);
    }
  };

  const handleSelectRange = () => {
    const start = parseInt(rangeStart);
    const end = parseInt(rangeEnd);

    if (isNaN(start) || isNaN(end) || start < 1 || end > numPages || start > end) {
      alert(`Vui lòng nhập khoảng hợp lệ (1-${numPages})`);
      return;
    }

    const newSelected = new Set(selectedPages);
    for (let i = start; i <= end; i++) {
      newSelected.add(i);
    }
    setSelectedPages(newSelected);
    setRangeStart('');
    setRangeEnd('');
  };

  const handleConfirm = async () => {
    if (selectedPages.size === 0) {
      alert('Vui lòng chọn ít nhất 1 trang');
      return;
    }

    setLoading(true);
    try {
      const pagesArray = Array.from(selectedPages).sort((a, b) => a - b);
      console.log('✅ Confirming pages:', pagesArray);
      await onConfirm(pagesArray);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    console.log('🔒 Closing dialog');
    setNumPages(0);
    setSelectedPages(new Set());
    setRangeStart('');
    setRangeEnd('');
    onClose();
  };

  return (
    <Dialog 
      open={open} 
      handler={handleClose} 
      size="xxl" 
      className="bg-secondary border border-primary"
      dismiss={{ enabled: !loading }}
    >
      <DialogHeader className="border-b border-primary flex items-center justify-between">
        <div className="flex-1">
          <Typography variant="h5" className="text-primary font-bold">
            📄 Chọn trang cần parse
          </Typography>
          <Typography variant="small" className="text-secondary font-normal">
            {file?.name} • {numPages ? `${numPages} trang` : 'Đang tải...'}
          </Typography>
        </div>
        
        <IconButton
          variant="text"
          color="gray"
          onClick={handleClose}
          disabled={loading}
        >
          <XMarkIcon className="h-5 w-5" />
        </IconButton>
      </DialogHeader>

      <DialogBody className="space-y-4 p-6 max-h-[75vh] overflow-y-auto">
        {/* Quick Actions */}
        {numPages > 0 && (
          <Card className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
            <CardBody className="p-4 space-y-3">
              {/* Row 1: Select all & Quick ranges */}
              <div className="flex items-center justify-between flex-wrap gap-3">
                <Checkbox
                  checked={selectedPages.size === numPages}
                  onChange={handleSelectAll}
                  label={
                    <Typography variant="small" className="font-semibold text-primary">
                      {selectedPages.size === numPages ? 'Bỏ chọn tất cả' : `Chọn tất cả (${numPages} trang)`}
                    </Typography>
                  }
                  color="blue"
                />

                <div className="flex gap-2 flex-wrap">
                  <Button
                    size="sm"
                    variant="outlined"
                    onClick={() => {
                      const pages = new Set();
                      for (let i = 1; i <= Math.min(10, numPages); i++) pages.add(i);
                      setSelectedPages(pages);
                    }}
                  >
                    1-10
                  </Button>
                  <Button
                    size="sm"
                    variant="outlined"
                    onClick={() => {
                      const pages = new Set();
                      for (let i = 1; i <= Math.min(20, numPages); i++) pages.add(i);
                      setSelectedPages(pages);
                    }}
                  >
                    1-20
                  </Button>
                  <Button
                    size="sm"
                    variant="outlined"
                    onClick={() => {
                      const pages = new Set();
                      for (let i = 1; i <= Math.min(50, numPages); i++) pages.add(i);
                      setSelectedPages(pages);
                    }}
                  >
                    1-50
                  </Button>
                  <Button
                    size="sm"
                    variant="outlined"
                    onClick={() => setSelectedPages(new Set())}
                    color="red"
                  >
                    Xóa hết
                  </Button>
                </div>
              </div>

              {/* Row 2: Custom range */}
              <div className="flex items-end gap-3 flex-wrap">
                <div className="flex-1 min-w-[100px]">
                  <Typography variant="small" className="text-secondary mb-1">
                    Từ trang:
                  </Typography>
                  <Input
                    type="number"
                    min="1"
                    max={numPages}
                    value={rangeStart}
                    onChange={(e) => setRangeStart(e.target.value)}
                    placeholder="1"
                    className="bg-secondary"
                  />
                </div>
                
                <div className="flex-1 min-w-[100px]">
                  <Typography variant="small" className="text-secondary mb-1">
                    Đến trang:
                  </Typography>
                  <Input
                    type="number"
                    min="1"
                    max={numPages}
                    value={rangeEnd}
                    onChange={(e) => setRangeEnd(e.target.value)}
                    placeholder={String(numPages || '')}
                    className="bg-secondary"
                  />
                </div>
                
                <Button
                  size="sm"
                  color="blue"
                  onClick={handleSelectRange}
                  disabled={!rangeStart || !rangeEnd}
                >
                  Chọn khoảng
                </Button>
              </div>

              {/* Row 3: Summary */}
              <div className="pt-2 border-t border-blue-200 dark:border-blue-800">
                <div className="flex items-center justify-between mb-2">
                  <Typography variant="small" className="text-blue-600 dark:text-blue-400 font-semibold">
                    ✅ Đã chọn: {selectedPages.size} trang
                  </Typography>
                  <Typography variant="small" className="text-secondary">
                    💡 Ước tính: ~{Math.ceil(selectedPages.size * 0.5)} phút
                  </Typography>
                </div>

                {/* Page checkboxes grid */}
                <div>
                  <Typography variant="small" className="text-secondary mb-2">
                    Chọn/bỏ chọn từng trang:
                  </Typography>
                  <div className="grid grid-cols-5 md:grid-cols-10 gap-2 max-h-32 overflow-y-auto p-2 bg-white dark:bg-gray-800 rounded">
                    {Array.from({ length: numPages }, (_, i) => i + 1).map((pageNum) => (
                      <div
                        key={pageNum}
                        className={`cursor-pointer text-center py-1 px-2 rounded text-xs font-semibold transition-all ${
                          selectedPages.has(pageNum)
                            ? 'bg-blue-500 text-white'
                            : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                        }`}
                        onClick={() => handlePageToggle(pageNum)}
                      >
                        {pageNum}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </CardBody>
          </Card>
        )}

        {/* PDF Viewer */}
        <Card className="border border-primary">
          <CardBody className="p-0">
            <div className="h-[500px] overflow-hidden rounded">
              {fileUrl ? (
                <Worker workerUrl="https://unpkg.com/pdfjs-dist@3.11.174/build/pdf.worker.min.js">
                  <Viewer
                    fileUrl={fileUrl}
                    plugins={[defaultLayoutPluginInstance]}
                    onDocumentLoad={handleDocumentLoad}
                  />
                </Worker>
              ) : (
                <div className="flex flex-col items-center justify-center h-full">
                  <Spinner className="h-10 w-10 mb-4" />
                  <Typography variant="small" className="text-secondary">
                    Đang tải PDF...
                  </Typography>
                </div>
              )}
            </div>
          </CardBody>
        </Card>
      </DialogBody>

      <DialogFooter className="border-t border-primary gap-3">
        <Button 
          variant="outlined" 
          onClick={handleClose} 
          disabled={loading}
          className="btn-secondary"
        >
          Hủy
        </Button>
        <Button
          color="blue"
          onClick={handleConfirm}
          disabled={loading || selectedPages.size === 0}
          loading={loading}
          className="flex items-center gap-2"
        >
          {loading ? 'Đang parse...' : `Parse ${selectedPages.size} trang`}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default PDFPageSelector;