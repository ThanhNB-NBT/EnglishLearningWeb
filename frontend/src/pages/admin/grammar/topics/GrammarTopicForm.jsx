import React from "react";
import { useParams } from "react-router-dom";
import { useTopicForm } from "../../../../hook/grammar/useGrammarTopics";
import { ADMIN_ROUTES } from "../../../../constants/routes";
import {
  Card,
  CardBody,
  CardHeader,
  Input,
  Textarea,
  Button,
  Typography,
  Select,
  Option,
  Switch,
  Progress,
  Spinner,
  Alert,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  BookOpenIcon,
  CheckCircleIcon,
  XMarkIcon,
  InformationCircleIcon,
  PencilIcon,
} from "@heroicons/react/24/outline";

const GrammarTopicForm = () => {
  const { id } = useParams();
  const {
    formData,
    loading,
    saving,
    errors,
    hasChanges,
    isEdit,
    handleInputChange,
    handleSubmit,
    handleCancel,
    getCompletionPercentage
  } = useTopicForm(id);

  const levels = [
    { value: "BEGINNER", label: "Cơ bản", color: "green" },
    { value: "INTERMEDIATE", label: "Trung bình", color: "orange" },
    { value: "ADVANCED", label: "Nâng cao", color: "red" },
  ];

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" color="blue-gray">
          Đang tải thông tin chủ đề...
        </Typography>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button
                variant="outlined"
                size="sm"
                onClick={handleCancel}
                className="flex items-center border-gray-300"
              >
                <ArrowLeftIcon className="h-4 w-4 mr-2" />
                Quay lại
              </Button>
              <div className="flex items-center space-x-3">
                <div className={`p-2 ${isEdit ? 'bg-orange-50' : 'bg-blue-50'} rounded-lg`}>
                  {isEdit ? (
                    <PencilIcon className="h-6 w-6 text-orange-600" />
                  ) : (
                    <BookOpenIcon className="h-6 w-6 text-blue-600" />
                  )}
                </div>
                <div>
                  <Typography
                    variant="h4"
                    color="blue-gray"
                    className="font-bold"
                  >
                    {isEdit ? "Chỉnh sửa Chủ đề Ngữ pháp" : "Tạo Chủ đề Ngữ pháp Mới"}
                  </Typography>
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="opacity-70"
                  >
                    {isEdit ? "Cập nhật thông tin chủ đề ngữ pháp" : "Thêm chủ đề ngữ pháp mới vào hệ thống"}
                  </Typography>
                </div>
              </div>
            </div>
            {isEdit && hasChanges && (
              <Alert color="orange" className="py-2 px-3">
                <Typography variant="small" className="font-medium">
                  Có thay đổi chưa lưu
                </Typography>
              </Alert>
            )}
          </div>
        </CardBody>
      </Card>

      {/* Progress */}
      <Card className="border border-blue-gray-100">
        <CardBody className="p-4">
          <div className="flex items-center justify-between mb-2">
            <Typography
              variant="small"
              color="blue-gray"
              className="font-medium"
            >
              Hoàn thành form
            </Typography>
            <Typography variant="small" color="blue-gray">
              {getCompletionPercentage()}%
            </Typography>
          </div>
          <Progress 
            value={getCompletionPercentage()} 
            color={isEdit ? "orange" : "blue"} 
            size="sm" 
          />
        </CardBody>
      </Card>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Information */}
            <Card className="border border-blue-gray-100">
              <CardHeader
                floated={false}
                shadow={false}
                className="m-0 p-4 border-b border-blue-gray-100"
              >
                <Typography
                  variant="h6"
                  color="blue-gray"
                  className="font-semibold"
                >
                  Thông tin cơ bản
                </Typography>
              </CardHeader>
              <CardBody className="p-6 space-y-6">
                <div>
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mb-2 font-medium"
                  >
                    Tên chủ đề <span className="text-red-500">*</span>
                  </Typography>
                  <Input
                    size="lg"
                    value={formData.name}
                    onChange={(e) => handleInputChange("name", e.target.value)}
                    placeholder="Ví dụ: Present Simple Tense"
                    error={!!errors.name}
                    className={`!border-blue-gray-200 focus:!border-${isEdit ? 'orange' : 'blue'}-500`}
                    labelProps={{
                      className: "before:content-none after:content-none",
                    }}
                  />
                  {errors.name && (
                    <Typography variant="small" color="red" className="mt-1">
                      {errors.name}
                    </Typography>
                  )}
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mt-1 opacity-60"
                  >
                    {formData.name.length}/100 ký tự
                  </Typography>
                </div>

                <div>
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mb-2 font-medium"
                  >
                    Mô tả chi tiết
                  </Typography>
                  <Textarea
                    size="lg"
                    value={formData.description}
                    onChange={(e) =>
                      handleInputChange("description", e.target.value)
                    }
                    placeholder="Mô tả về chủ đề ngữ pháp này, bao gồm mục tiêu học tập và nội dung chính..."
                    rows={4}
                    error={!!errors.description}
                    className={`!border-blue-gray-200 focus:!border-${isEdit ? 'orange' : 'blue'}-500`}
                    labelProps={{
                      className: "before:content-none after:content-none",
                    }}
                  />
                  {errors.description && (
                    <Typography variant="small" color="red" className="mt-1">
                      {errors.description}
                    </Typography>
                  )}
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mt-1 opacity-60"
                  >
                    {formData.description.length}/500 ký tự
                  </Typography>
                </div>
              </CardBody>
            </Card>
          </div>

          {/* Settings Sidebar */}
          <div className="space-y-6">
            {/* Level & Order */}
            <Card className="border border-blue-gray-100">
              <CardHeader
                floated={false}
                shadow={false}
                className="m-0 p-4 border-b border-blue-gray-100"
              >
                <Typography
                  variant="h6"
                  color="blue-gray"
                  className="font-semibold"
                >
                  Cài đặt
                </Typography>
              </CardHeader>
              <CardBody className="p-4 space-y-4">
                <div>
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mb-2 font-medium"
                  >
                    Cấp độ <span className="text-red-500">*</span>
                  </Typography>
                  <Select
                    size="lg"
                    value={formData.levelRequired}
                    onChange={(val) => handleInputChange("levelRequired", val)}
                    className={`!border-blue-gray-200 focus:!border-${isEdit ? 'orange' : 'blue'}-500`}
                  >
                    {levels.map((level) => (
                      <Option key={level.value} value={level.value}>
                        <div className="flex items-center space-x-2">
                          <div
                            className={`w-3 h-3 rounded-full bg-${level.color}-500`}
                          ></div>
                          <span>{level.label}</span>
                        </div>
                      </Option>
                    ))}
                  </Select>
                  {errors.levelRequired && (
                    <Typography variant="small" color="red" className="mt-1">
                      {errors.levelRequired}
                    </Typography>
                  )}
                </div>

                <div>
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="mb-2 font-medium"
                  >
                    Thứ tự hiển thị
                  </Typography>
                  <Input
                    type="number"
                    size="lg"
                    value={formData.orderIndex}
                    onChange={(e) =>
                      handleInputChange("orderIndex", e.target.value)
                    }
                    min="1"
                    error={!!errors.orderIndex}
                    className={`!border-blue-gray-200 focus:!border-${isEdit ? 'orange' : 'blue'}-500`}
                    labelProps={{
                      className: "before:content-none after:content-none",
                    }}
                  />
                  {errors.orderIndex && (
                    <Typography variant="small" color="red" className="mt-1">
                      {errors.orderIndex}
                    </Typography>
                  )}
                </div>

                <div className="flex items-center justify-between">
                  <Typography
                    variant="small"
                    color="blue-gray"
                    className="font-medium"
                  >
                    {isEdit ? "Trạng thái hoạt động" : "Kích hoạt ngay"}
                  </Typography>
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) =>
                      handleInputChange("isActive", e.target.checked)
                    }
                    color={isEdit ? "orange" : "blue"}
                  />
                </div>
              </CardBody>
            </Card>

            {/* Help Card */}
            <Card className={`border ${isEdit ? 'border-orange-200 bg-orange-50' : 'border-blue-200 bg-blue-50'}`}>
              <CardBody className="p-4">
                <div className="flex items-start space-x-2">
                  <InformationCircleIcon className={`h-5 w-5 ${isEdit ? 'text-orange-600' : 'text-blue-600'} mt-0.5 flex-shrink-0`} />
                  <div>
                    <Typography
                      variant="small"
                      color="blue-gray"
                      className="font-medium mb-2"
                    >
                      {isEdit ? "Lưu ý khi chỉnh sửa" : "Gợi ý"}
                    </Typography>
                    <ul className="text-sm text-blue-gray-700 space-y-1 opacity-80">
                      {isEdit ? (
                        <>
                          <li>• Thay đổi cấp độ có thể ảnh hưởng đến học viên</li>
                          <li>• Tắt trạng thái sẽ ẩn chủ đề khỏi danh sách</li>
                          <li>• Thứ tự quyết định vị trí hiển thị</li>
                        </>
                      ) : (
                        <>
                          <li>• Tên chủ đề nên ngắn gọn và dễ hiểu</li>
                          <li>• Mô tả chi tiết giúp học viên hiểu rõ nội dung</li>
                          <li>• Thứ tự hiển thị quyết định vị trí trong danh sách</li>
                        </>
                      )}
                    </ul>
                  </div>
                </div>
              </CardBody>
            </Card>
          </div>
        </div>

        {/* Action Buttons */}
        <Card className="border border-blue-gray-100">
          <CardBody className="p-6">
            <div className="flex items-center justify-between">
              <Button
                variant="outlined"
                size="lg"
                onClick={handleCancel}
                className="border-gray-300 flex items-center"
              >
                <XMarkIcon className="h-5 w-5 mr-2" />
                Hủy bỏ
              </Button>

              <Button
                type="submit"
                size="lg"
                disabled={saving || Object.keys(errors).length > 0 || (isEdit && !hasChanges)}
                loading={saving}
                className={`flex items-center bg-gradient-to-r ${isEdit ? 'from-orange-500 to-orange-600' : 'from-blue-500 to-blue-600'} shadow-lg min-w-[140px]`}
              >
                {saving ? (
                  isEdit ? "Đang lưu..." : "Đang tạo..."
                ) : (
                  <>
                    <CheckCircleIcon className="h-5 w-5 mr-2" />
                    {isEdit ? "Cập nhật" : "Tạo chủ đề"}
                  </>
                )}
              </Button>
            </div>
            {isEdit && !hasChanges && (
              <Typography variant="small" color="blue-gray" className="text-center mt-2 opacity-60">
                Không có thay đổi để lưu
              </Typography>
            )}
          </CardBody>
        </Card>
      </form>
    </div>
  );
};

export default GrammarTopicForm;