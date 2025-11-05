// frontend/src/components/reading/editors/ReadingContentPreview.jsx
import React from 'react';
import { Typography } from '@material-tailwind/react';
import '../../../styles/lessonPreview.css';

const ReadingContentPreview = ({ content, translation }) => {
  return (
    <div className="border-t pt-4 mt-6">
      <Typography variant="small" className="mb-3 font-medium text-primary">
        📄 Preview nội dung bài đọc
      </Typography>
      
      {/* English Content */}
      {content && (
        <div className="mb-6">
          <Typography variant="small" className="mb-2 font-semibold text-blue-600">
            🇬🇧 English
          </Typography>
          <div className="lesson-preview-wrapper">
            <div 
              className="lesson-preview-content"
              dangerouslySetInnerHTML={{ __html: content }}
            />
          </div>
        </div>
      )}

      {/* Vietnamese Translation */}
      {translation && (
        <div>
          <Typography variant="small" className="mb-2 font-semibold text-green-600">
            🇻🇳 Tiếng Việt
          </Typography>
          <div className="lesson-preview-wrapper">
            <div 
              className="lesson-preview-content"
              dangerouslySetInnerHTML={{ __html: translation }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ReadingContentPreview;