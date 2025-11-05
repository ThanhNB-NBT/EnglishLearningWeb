import React from 'react';
import { Typography } from '@material-tailwind/react';

/**
 * Component hiển thị lỗi validation cho một trường input
 * @param {object} props
 * @param {string} props.error - Nội dung lỗi
 */
const FormInputError = ({ error }) => {
  if (!error) {
    return null;
  }

  return (
    <Typography
      variant="small"
      color="red"
      className="mt-1 flex items-center gap-1"
    >
      <span>⚠️</span> {error}
    </Typography>
  );
};

export default FormInputError;