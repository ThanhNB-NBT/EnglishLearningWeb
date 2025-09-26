// src/utils/grammarAdminUtils.js
import { grammarAdminAPI } from '../api/grammarAdminAPI';
import toast from 'react-hot-toast';

export const fetchTopics = async () => {
  try {
    const response = await grammarAdminAPI.getAllTopics();
    return response.data.data || [];
  } catch (error) {
    toast.error('Lỗi khi lấy danh sách topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
    throw error;
  }
};

export const deleteTopic = async (id) => {
  try {
    await grammarAdminAPI.deleteTopic(id);
    toast.success('Xóa topic thành công!');
  } catch (error) {
    toast.error('Lỗi khi xóa topic: ' + (error.response?.data?.message || 'Vui lòng thử lại.'));
    throw error;
  }
};