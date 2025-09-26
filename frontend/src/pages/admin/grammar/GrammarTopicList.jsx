// src/pages/admin/grammar/GrammarTopicList.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchTopics, deleteTopic } from '../../../utils/grammarAdminUtils';
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Typography,
  Spinner,
  Alert,
  Table,
  TableHeaderCell,
  TableCell,
  TableRow,
  TableBody,
} from '@material-tailwind/react';

const GrammarTopicList = () => {
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState('success');
  const navigate = useNavigate();

  useEffect(() => {
    const loadTopics = async () => {
      setLoading(true);
      try {
        const data = await fetchTopics();
        setTopics(data);
      } catch {
        setSnackbarMessage('Lỗi khi lấy danh sách topic');
        setSnackbarSeverity('error');
        setSnackbarOpen(true);
      } finally {
        setLoading(false);
      }
    };
    loadTopics();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc muốn xóa topic này?')) return;
    try {
      await deleteTopic(id);
      setTopics(topics.filter((topic) => topic.id !== id));
      setSnackbarMessage('Xóa topic thành công!');
      setSnackbarSeverity('success');
      setSnackbarOpen(true);
    } catch {
      setSnackbarMessage('Lỗi khi xóa topic');
      setSnackbarSeverity('error');
      setSnackbarOpen(true);
    }
  };

  const handleSnackbarClose = () => setSnackbarOpen(false);

  return (
    <Card className="mx-auto max-w-screen-lg p-4">
      <CardHeader floated={false} shadow={false} className="mb-4 flex items-center justify-between">
        <Typography variant="h4" color="blue-gray" className="font-bold">
          Quản lý chủ đề ngữ pháp
        </Typography>
        <Button
          color="blue"
          onClick={() => navigate('/admin/grammar/create')}
          className="rounded-md"
        >
          Tạo topic mới
        </Button>
      </CardHeader>

      {loading ? (
        <div className="flex justify-center items-center h-64">
          <Spinner className="h-12 w-12 text-blue-500" />
        </div>
      ) : topics.length === 0 ? (
        <Typography variant="paragraph" className="text-center text-gray-600">
          Không có chủ đề nào.
        </Typography>
      ) : (
        <CardBody className="overflow-x-auto">
          <Table className="w-full min-w-max">
            <TableRow>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Tên</TableHeaderCell>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Mô tả</TableHeaderCell>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Level</TableHeaderCell>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Thứ tự</TableHeaderCell>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Trạng thái</TableHeaderCell>
              <TableHeaderCell className="bg-blue-gray-50 text-blue-gray-700">Hành động</TableHeaderCell>
            </TableRow>
            <TableBody>
              {topics.map((topic) => (
                <TableRow key={topic.id} className="even:bg-blue-gray-50/50">
                  <TableCell>{topic.name}</TableCell>
                  <TableCell>
                    {topic.description?.substring(0, 50) || 'Không có mô tả'}
                    {topic.description?.length > 50 && '...'}
                  </TableCell>
                  <TableCell>{topic.levelRequired}</TableCell>
                  <TableCell>{topic.orderIndex}</TableCell>
                  <TableCell>{topic.isActive ? 'Hoạt động' : 'Không hoạt động'}</TableCell>
                  <TableCell>
                    <Button
                      color="amber"
                      onClick={() => navigate(`/admin/grammar/edit/${topic.id}`)}
                      className="mr-2 rounded-md"
                    >
                      Sửa
                    </Button>
                    <Button
                      color="red"
                      onClick={() => handleDelete(topic.id)}
                      className="rounded-md"
                    >
                      Xóa
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardBody>
      )}
      <Alert
        open={snackbarOpen}
        onClose={handleSnackbarClose}
        color={snackbarSeverity === 'success' ? 'green' : 'red'}
        className="fixed top-4 right-4 w-80"
        animate={{
          mount: { y: 0 },
          unmount: { y: -100 },
        }}
      >
        {snackbarMessage}
      </Alert>
    </Card>
  );
};

export default GrammarTopicList;