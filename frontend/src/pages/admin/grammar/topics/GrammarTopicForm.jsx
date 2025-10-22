import React from "react";
import { useParams } from "react-router-dom";
import { useTopicForm } from "../../../../hook/grammar/useGrammarTopics";
import {
  Card,
  CardBody,
  CardHeader,
  Input,
  Textarea,
  Button,
  IconButton,
  Typography,
  Select,
  Option,
  Switch,
  Progress,
  Spinner,
  Alert,
  Chip,
} from "@material-tailwind/react";
import {
  ArrowLeftIcon,
  BookOpenIcon,
  CheckCircleIcon,
  XMarkIcon,
  PencilIcon,
  ExclamationTriangleIcon,
  LightBulbIcon,
  HashtagIcon,
  XCircleIcon,
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
    getCompletionPercentage,
  } = useTopicForm(id);

  const levels = [
    { value: "BEGINNER", label: "Cơ bản", icon: "🟢" },
    { value: "INTERMEDIATE", label: "Trung bình", icon: "🟠" },
    { value: "ADVANCED", label: "Nâng cao", icon: "🔴" },
  ];

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-96 space-y-4">
        <Spinner className="h-12 w-12 text-blue-500" />
        <Typography variant="paragraph" className="text-secondary">
          Đang tải thông tin chủ đề...
        </Typography>
      </div>
    );
  }

  const completionPercentage = getCompletionPercentage();
  const hasErrors = Object.keys(errors).length > 0;

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6 p-4 md:p-6">
      {/* Header */}
      <Card className="bg-gradient-to-r from-slate-800 to-slate-900 dark:from-slate-900 dark:to-black border border-slate-700 shadow-xl">
        <CardBody className="p-4 md:p-6">
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
            <div className="flex items-center gap-3 flex-1">
              <IconButton
                variant="text"
                size="lg"
                onClick={handleCancel}
                className="text-slate-300 hover:bg-slate-700 hidden md:flex shrink-0"
              >
                <ArrowLeftIcon className="h-5 w-5" />
              </IconButton>

              <div
                className={`p-3 rounded-xl border ${
                  isEdit
                    ? "bg-orange-500/20 border-orange-500/30"
                    : "bg-blue-500/20 border-blue-500/30"
                }`}
              >
                {isEdit ? (
                  <PencilIcon className="h-6 w-6 text-orange-400" />
                ) : (
                  <BookOpenIcon className="h-6 w-6 text-blue-400" />
                )}
              </div>

              <div>
                <Typography variant="h4" className="text-slate-100 font-bold mb-1">
                  {isEdit ? "Chỉnh sửa Chủ đề" : "Tạo Chủ đề Mới"}
                </Typography>
                <Typography variant="small" className="text-slate-400">
                  {isEdit
                    ? "Cập nhật thông tin chủ đề ngữ pháp"
                    : "Thêm chủ đề ngữ pháp mới vào hệ thống"}
                </Typography>
              </div>
            </div>

            {isEdit && hasChanges && (
              <Alert
                color="orange"
                className="py-2 px-4 bg-orange-500/20 border border-orange-500/30"
                icon={<ExclamationTriangleIcon className="h-5 w-5" />}
              >
                <Typography variant="small" className="font-medium text-orange-400">
                  Có thay đổi chưa lưu
                </Typography>
              </Alert>
            )}
          </div>
        </CardBody>
      </Card>

      {/* Progress */}
      <Card className="card-base border-primary">
        <CardBody className="p-4 md:p-6">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Typography variant="small" className="text-primary font-semibold">
                Hoàn thành form
              </Typography>
              {completionPercentage === 100 && (
                <CheckCircleIcon className="h-5 w-5 text-green-500" />
              )}
            </div>
            <Typography
              variant="small"
              className={`font-bold ${
                completionPercentage === 100 ? "text-green-500" : "text-secondary"
              }`}
            >
              {completionPercentage}%
            </Typography>
          </div>
          <Progress
            value={completionPercentage}
            color={isEdit ? "orange" : "blue"}
            size="lg"
            className="bg-tertiary"
          />
          <Typography variant="small" className="text-tertiary mt-2">
            {completionPercentage < 100
              ? "Vui lòng điền đầy đủ thông tin bắt buộc"
              : "✅ Form đã hoàn thiện, sẵn sàng lưu"}
          </Typography>
        </CardBody>
      </Card>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            <Card className="card-base border-primary">
              <CardHeader
                floated={false}
                shadow={false}
                className="m-0 p-4 border-b border-primary bg-tertiary rounded-t-xl rounded-b-none"
              >
                <div className="flex items-center gap-2">
                  <div className="w-1 h-6 bg-blue-500 rounded-full"></div>
                  <Typography variant="h6" className="text-primary font-bold">
                    Thông tin cơ bản
                  </Typography>
                </div>
              </CardHeader>
              <CardBody className="p-4 md:p-6 space-y-6">
                {/* Name */}
                <div>
                  <Typography variant="small" className="mb-2 font-semibold text-primary">
                    Tên chủ đề <span className="text-red-500">*</span>
                  </Typography>
                  <Input
                    size="lg"
                    value={formData.name}
                    onChange={(e) => handleInputChange("name", e.target.value)}
                    placeholder="Ví dụ: 12 thì cơ bản trong tiếng Anh"
                    error={!!errors.name}
                    className="bg-secondary"
                    color="blue"
                  />
                  {errors.name && (
                    <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
                      <span>⚠️</span> {errors.name}
                    </Typography>
                  )}
                  <div className="flex items-center justify-between mt-2">
                    <Typography variant="small" className="text-tertiary">
                      {formData.name.length}/100 ký tự
                    </Typography>
                    {formData.name.length > 0 && formData.name.length < 5 && (
                      <Typography variant="small" className="text-amber-500">
                        💡 Tên nên dài hơn 5 ký tự
                      </Typography>
                    )}
                  </div>
                </div>

                {/* Description */}
                <div>
                  <Typography variant="small" className="mb-2 font-semibold text-primary">
                    Mô tả chi tiết
                  </Typography>
                  <Textarea
                    size="lg"
                    value={formData.description}
                    onChange={(e) => handleInputChange("description", e.target.value)}
                    placeholder="Mô tả về chủ đề ngữ pháp này, bao gồm mục tiêu học tập và nội dung chính..."
                    rows={10}
                    error={!!errors.description}
                    className="bg-secondary"
                    color="blue"
                  />
                  {errors.description && (
                    <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
                      <span>⚠️</span> {errors.description}
                    </Typography>
                  )}
                  <Typography variant="small" className="text-tertiary mt-2">
                    {formData.description.length}/500 ký tự
                  </Typography>
                </div>
              </CardBody>
            </Card>
          </div>

          {/* Settings Sidebar */}
          <div className="space-y-6">
            {/* Settings Card */}
            <Card className="card-base border-primary">
              <CardHeader
                floated={false}
                shadow={false}
                className="m-0 p-4 border-b border-primary bg-tertiary rounded-t-xl rounded-b-none"
              >
                <div className="flex items-center gap-2">
                  <div className="w-1 h-6 bg-green-500 rounded-full"></div>
                  <Typography variant="h6" className="text-primary font-bold">
                    Cài đặt
                  </Typography>
                </div>
              </CardHeader>
              <CardBody className="p-4 space-y-5">
                {/* Level */}
                <div>
                  <Typography variant="small" className="mb-2 font-semibold text-primary">
                    Cấp độ <span className="text-red-500">*</span>
                  </Typography>
                  <Select
                    size="lg"
                    value={formData.levelRequired}
                    onChange={(val) => handleInputChange("levelRequired", val)}
                    className="bg-secondary"
                    color="blue"
                    containerProps={{ className: "!min-w-full" }}
                    menuProps={{ className: "bg-secondary border-primary" }}
                  >
                    {levels.map((level) => (
                      <Option key={level.value} value={level.value}>
                        <div className="flex items-center gap-2">
                          <span className="text-base">{level.icon}</span>
                          <span className="font-medium">{level.label}</span>
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

                {/* Order Index */}
                <div>
                  <Typography variant="small" className="mb-2 font-semibold text-primary flex items-center gap-2">
                    <HashtagIcon className="h-4 w-4 text-blue-500" />
                    Thứ tự hiển thị
                  </Typography>
                  <div className="relative">
                    <Input
                      type="number"
                      size="lg"
                      value={formData.orderIndex}
                      onChange={(e) =>
                        handleInputChange("orderIndex", parseInt(e.target.value) || 1)
                      }
                      min="1"
                      error={!!errors.orderIndex}
                      className="bg-secondary"
                      color="blue"
                      disabled={!isEdit}
                    />
                    {!isEdit && (
                      <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                        <CheckCircleIcon className="h-5 w-5 text-green-500" />
                      </div>
                    )}
                  </div>
                  {errors.orderIndex && (
                    <Typography variant="small" color="red" className="mt-1">
                      {errors.orderIndex}
                    </Typography>
                  )}
                  <Typography variant="small" className="text-tertiary mt-1">
                    {isEdit 
                      ? "Quyết định vị trí trong danh sách" 
                      : `✅ Tự động: Vị trí thứ ${formData.orderIndex}`}
                  </Typography>
                </div>

                {/* Active Status */}
                <div>
                  <div className="flex items-center justify-between p-3 bg-tertiary rounded-lg border border-primary">
                    <div>
                      <Typography variant="small" className="text-primary font-medium">
                        {formData.isActive ? "Hiển thị cho học viên" : "Ẩn khỏi danh sách"}
                      </Typography>
                    </div>
                    <Switch
                      checked={formData.isActive}
                      onChange={(e) => handleInputChange("isActive", e.target.checked)}
                      color={formData.isActive ? "blue" : "gray"}
                      className="checked:bg-blue-500"
                    />
                  </div>
                </div>
              </CardBody>
            </Card>

            {/* Action Buttons */}
            <Card className="card-base border-primary">
              <CardBody className="p-4">
                <div className="flex flex-col gap-3">
                  <Button
                    type="submit"
                    size="lg"
                    disabled={saving || hasErrors || (isEdit && !hasChanges)}
                    loading={saving}
                    color={isEdit ? "orange" : "blue"}
                    className="flex items-center justify-center shadow-lg hover:shadow-xl transition-all w-full"
                  >
                    {!saving && <CheckCircleIcon className="h-5 w-5 mr-2" />}
                    {saving ? (isEdit ? "Đang lưu..." : "Đang tạo...") : (isEdit ? "Cập nhật" : "Tạo chủ đề")}
                  </Button>

                  <Button
                    variant="outlined"
                    size="lg"
                    onClick={handleCancel}
                    disabled={saving}
                    className="btn-secondary flex items-center justify-center w-full"
                  >
                    <XMarkIcon className="h-5 w-5 mr-2" />
                    Hủy bỏ
                  </Button>
                </div>

                {/* Status Messages */}
                {isEdit && !hasChanges && !saving && (
                  <Typography variant="small" className="text-secondary text-center mt-3">
                    ℹ️ Không có thay đổi để lưu
                  </Typography>
                )}
                {hasErrors && (
                  <Typography variant="small" className="text-red-500 text-center mt-3 flex items-center justify-center gap-1">
                    <ExclamationTriangleIcon className="h-4 w-4" />
                    Vui lòng sửa các lỗi trước khi lưu
                  </Typography>
                )}
              </CardBody>
            </Card>
          </div>
        </div>
      </form>
    </div>
  );
};

export default GrammarTopicForm;