import React from 'react';
import { IconButton, Typography, Button } from '@material-tailwind/react';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronDoubleLeftIcon,
  ChevronDoubleRightIcon,
} from '@heroicons/react/24/outline';

const PaginationControls = ({
  currentPage = 0,
  totalPages = 0,
  totalElements = 0,
  pageSize = 10,
  onPageChange,
  hasNext = false,
  hasPrevious = false,
}) => {
  const startItem = totalElements === 0 ? 0 : currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  const handlePageClick = (page) => {
    if (page >= 0 && page < totalPages) {
      onPageChange(page);
    }
  };

  // Generate visible page numbers (max 5 buttons)
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

    if (endPage - startPage < maxVisible - 1) {
      startPage = Math.max(0, endPage - maxVisible + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  };

  if (totalPages <= 1) return null;

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 border-t border-primary">
      {/* Info Text */}
      <Typography variant="small" className="text-secondary font-medium">
        Hiển thị <span className="font-bold text-primary">{startItem}</span> -{' '}
        <span className="font-bold text-primary">{endItem}</span> trong tổng số{' '}
        <span className="font-bold text-primary">{totalElements}</span> mục
      </Typography>

      {/* Pagination Buttons */}
      <div className="flex items-center gap-2">
        {/* First Page */}
        <IconButton
          size="sm"
          variant="outlined"
          onClick={() => handlePageClick(0)}
          disabled={!hasPrevious}
          className="border-primary hover:bg-tertiary disabled:opacity-50"
        >
          <ChevronDoubleLeftIcon className="h-4 w-4 dark:text-slate-50" />
        </IconButton>

        {/* Previous Page */}
        <IconButton
          size="sm"
          variant="outlined"
          onClick={() => handlePageClick(currentPage - 1)}
          disabled={!hasPrevious}
          className="border-primary hover:bg-tertiary disabled:opacity-50"
        >
          <ChevronLeftIcon className="h-4 w-4 dark:text-slate-200" />
        </IconButton>

        {/* Page Numbers */}
        <div className="flex items-center gap-1">
          {getPageNumbers().map((page) => (
            <Button
              key={page}
              size="sm"
              variant={page === currentPage ? 'filled' : 'outlined'}
              onClick={() => handlePageClick(page)}
              className={
                page === currentPage
                  ? 'bg-blue-500 text-white shadow-md'
                  : 'border-primary hover:bg-tertiary text-primary'
              }
            >
              {page + 1}
            </Button>
          ))}
        </div>

        {/* Next Page */}
        <IconButton
          size="sm"
          variant="outlined"
          onClick={() => handlePageClick(currentPage + 1)}
          disabled={!hasNext}
          className="border-primary hover:bg-tertiary disabled:opacity-50"
        >
          <ChevronRightIcon className="h-4 w-4 dark:text-slate-200" />
        </IconButton>

        {/* Last Page */}
        <IconButton
          size="sm"
          variant="outlined"
          onClick={() => handlePageClick(totalPages - 1)}
          disabled={!hasNext}
          className="border-primary hover:bg-tertiary disabled:opacity-50"
        >
          <ChevronDoubleRightIcon className="h-4 w-4 dark:text-slate-50" />
        </IconButton>
      </div>
    </div>
  );
};

export default PaginationControls;