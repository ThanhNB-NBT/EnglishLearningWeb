import React from 'react';
import { Typography } from '@material-tailwind/react';
import '../../../styles/lessonPreview.css';

/**
 * Component: Preview nội dung bài học với styling đầy đủ cho table
 */
const LessonContentPreview = ({ content }) => {
  if (!content) return null;

  return (
    <div className="border-t pt-4">
      <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
        📄 Preview nội dung
      </Typography>
      
      <div className="lesson-preview-wrapper">
        <div 
          className="lesson-preview-content"
          dangerouslySetInnerHTML={{ __html: content }}
        />
      </div>
    </div>
  );
};

export default LessonContentPreview;