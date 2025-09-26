// GrammarTopicEdit.jsx
import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { grammarAdminAPI } from "../../../api";
import toast from "react-hot-toast";

const AdminGrammarTopicEdit = () => {
  const { id } = useParams();
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchTopic = async () => {
      try {
        // nếu backend có endpoint getTopicById thì gọi, còn không thì lấy từ getAllTopics rồi lọc
        const res = await grammarAdminAPI.getAllTopics();
        const topic = res.data.data.find(t => t.id.toString() === id);
        if (!topic) {
          toast.error("Không tìm thấy chủ đề");
          navigate("/admin/grammar");
          return;
        }
        setName(topic.name);
      } catch (err) {
        toast.error(err.response?.data?.message || "Lấy thông tin thất bại");
        navigate("/admin/grammar");
      } finally {
        setLoading(false);
      }
    };
    fetchTopic();
  }, [id, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await grammarAdminAPI.updateTopic(id, { name });
      toast.success("Cập nhật thành công");
      navigate("/admin/grammar");
    } catch {
      toast.error("Cập nhật thất bại");
    }
  };

  if (loading) return <div className="p-6">Đang tải dữ liệu...</div>;

  return (
    <form onSubmit={handleSubmit} className="p-6 space-y-4">
      <h1 className="text-xl font-bold">Chỉnh sửa Grammar Topic</h1>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Tên chủ đề"
        className="border p-2 w-full"
      />
      <div className="space-x-2">
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white rounded"
        >
          Lưu
        </button>
        <button
          type="button"
          onClick={() => navigate("/admin/grammar")}
          className="px-4 py-2 bg-gray-400 text-white rounded"
        >
          Hủy
        </button>
      </div>
    </form>
  );
};

export default AdminGrammarTopicEdit;
