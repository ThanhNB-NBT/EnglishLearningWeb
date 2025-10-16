// utils/grammarAdminUtils.js
// ⚠️ FILE NÀY SẼ BỊ XÓA SAU KHI REFACTOR XONG
// Tạm thời giữ lại để backward compatibility

import { topicService } from '../services/grammarService';

// Deprecated: Dùng topicService.fetchAll() thay thế
export const fetchTopics = async () => {
  console.warn('⚠️ fetchTopics() is deprecated. Use topicService.fetchAll() instead.');
  return topicService.fetchAll();
};

// Deprecated: Dùng topicService.delete() thay thế
export const deleteTopic = async (id) => {
  console.warn('⚠️ deleteTopic() is deprecated. Use topicService.delete() instead.');
  return topicService.delete(id);
};