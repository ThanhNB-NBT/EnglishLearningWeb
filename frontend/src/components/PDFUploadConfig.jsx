import React, { useState } from 'react';
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
  Input,
  Switch,
  Card,
  CardBody,
} from '@material-tailwind/react';
import {
  AdjustmentsHorizontalIcon,
  InformationCircleIcon,
} from '@heroicons/react/24/outline';

const PDFUploadConfig = ({ open, onClose, onConfirm }) => {
  const [config, setConfig] = useState({
    skipFirstPages: 2,
    skipLastPages: 0,
    removeHeaders: true,
    removeFooters: true,
    minSectionLength: 50,
    minQuestionLength: 20,
  });

  const handleChange = (field, value) => {
    setConfig(prev => ({ ...prev, [field]: value }));
  };

  const handleConfirm = () => {
    onConfirm(config);
    onClose();
  };

  const handleReset = () => {
    setConfig({
      skipFirstPages: 2,
      skipLastPages: 0,
      removeHeaders: true,
      removeFooters: true,
      minSectionLength: 50,
      minQuestionLength: 20,
    });
  };

  return (
    <Dialog open={open} handler={onClose} size="md">
      <DialogHeader className="flex items-center space-x-2">
        <AdjustmentsHorizontalIcon className="h-6 w-6 text-blue-500" />
        <span>Cài đặt xử lý PDF</span>
      </DialogHeader>

      <DialogBody className="space-y-4 overflow-y-auto max-h-[60vh]">
        {/* Info Alert */}
        <Card className="bg-blue-50 border border-blue-200">
          <CardBody className="p-3">
            <div className="flex items-start space-x-2">
              <InformationCircleIcon className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <Typography variant="small" color="blue-gray">
                Cấu hình này giúp loại bỏ các phần không cần thiết như header, footer, 
                trang giới thiệu để có nội dung chất lượng tốt hơn.
              </Typography>
            </div>
          </CardBody>
        </Card>

        {/* Skip Pages Section */}
        <div className="space-y-3">
          <Typography variant="h6" color="blue-gray" className="font-semibold">
            Bỏ qua trang
          </Typography>

          <div>
            <Typography variant="small" color="blue-gray" className="mb-2">
              Số trang đầu cần bỏ qua
            </Typography>
            <Input
              type="number"
              min="0"
              max="10"
              value={config.skipFirstPages}
              onChange={(e) => handleChange('skipFirstPages', parseInt(e.target.value) || 0)}
              className="!border-blue-gray-200"
            />
            <Typography variant="small" color="gray" className="mt-1">
              Thường là trang bìa, mục lục (mặc định: 2)
            </Typography>
          </div>

          <div>
            <Typography variant="small" color="blue-gray" className="mb-2">
              Số trang cuối cần bỏ qua
            </Typography>
            <Input
              type="number"
              min="0"
              max="10"
              value={config.skipLastPages}
              onChange={(e) => handleChange('skipLastPages', parseInt(e.target.value) || 0)}
              className="!border-blue-gray-200"
            />
            <Typography variant="small" color="gray" className="mt-1">
              Trang tài liệu tham khảo, quảng cáo (mặc định: 0)
            </Typography>
          </div>
        </div>

        {/* Auto Remove Section */}
        <div className="space-y-3">
          <Typography variant="h6" color="blue-gray" className="font-semibold">
            Tự động loại bỏ
          </Typography>

          <div className="flex items-center justify-between">
            <div>
              <Typography variant="small" color="blue-gray" className="font-medium">
                Loại bỏ header lặp lại
              </Typography>
              <Typography variant="small" color="gray">
                Tự động phát hiện và xóa header trên mỗi trang
              </Typography>
            </div>
            <Switch
              checked={config.removeHeaders}
              onChange={(e) => handleChange('removeHeaders', e.target.checked)}
              color="blue"
            />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <Typography variant="small" color="blue-gray" className="font-medium">
                Loại bỏ footer lặp lại
              </Typography>
              <Typography variant="small" color="gray">
                Tự động phát hiện và xóa footer (số trang, thông tin liên hệ)
              </Typography>
            </div>
            <Switch
              checked={config.removeFooters}
              onChange={(e) => handleChange('removeFooters', e.target.checked)}
              color="blue"
            />
          </div>
        </div>

        {/* Filter Section */}
        <div className="space-y-3">
          <Typography variant="h6" color="blue-gray" className="font-semibold">
            Lọc nội dung
          </Typography>

          <div>
            <Typography variant="small" color="blue-gray" className="mb-2">
              Độ dài tối thiểu của phần (ký tự)
            </Typography>
            <Input
              type="number"
              min="10"
              max="200"
              value={config.minSectionLength}
              onChange={(e) => handleChange('minSectionLength', parseInt(e.target.value) || 50)}
              className="!border-blue-gray-200"
            />
            <Typography variant="small" color="gray" className="mt-1">
              Phần ngắn hơn sẽ bị bỏ qua (mặc định: 50)
            </Typography>
          </div>

          <div>
            <Typography variant="small" color="blue-gray" className="mb-2">
              Độ dài tối thiểu của câu hỏi (ký tự)
            </Typography>
            <Input
              type="number"
              min="5"
              max="100"
              value={config.minQuestionLength}
              onChange={(e) => handleChange('minQuestionLength', parseInt(e.target.value) || 20)}
              className="!border-blue-gray-200"
            />
            <Typography variant="small" color="gray" className="mt-1">
              Câu hỏi ngắn hơn sẽ bị bỏ qua (mặc định: 20)
            </Typography>
          </div>
        </div>
      </DialogBody>

      <DialogFooter className="space-x-2">
        <Button variant="outlined" onClick={handleReset} size="sm">
          Đặt lại mặc định
        </Button>
        <Button variant="outlined" onClick={onClose}>
          Hủy
        </Button>
        <Button color="blue" onClick={handleConfirm}>
          Áp dụng
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default PDFUploadConfig;