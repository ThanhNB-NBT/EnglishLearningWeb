import React from 'react';
import { Select, Option, Typography } from '@material-tailwind/react';

const PageSizeSelector = ({ pageSize, onPageSizeChange, options = [6, 10, 20, 50] }) => {
  return (
    <div className="flex items-center gap-2">
      <Typography variant="small" className="text-secondary font-medium whitespace-nowrap">
        Hiển thị:
      </Typography>
      <div className="w-32">
        <Select
          value={pageSize.toString()}
          onChange={(val) => onPageSizeChange(parseInt(val))}
          color='blue'
          size="md"
          className="bg-secondary"
          containerProps={{ className: "!min-w-0" }}
          menuProps={{ className: "bg-secondary border-primary" }}
        >
          {options.map((size) => (
            <Option key={size} value={size.toString()}>
              {size} / trang
            </Option>
          ))}
        </Select>
      </div>
    </div>
  );
};

export default PageSizeSelector;