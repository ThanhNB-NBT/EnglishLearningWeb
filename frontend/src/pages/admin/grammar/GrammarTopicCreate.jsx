import React, { useState } from "react";
import { grammarAdminAPI } from "../../../api";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";

const AdminGrammarTopicCreate = () => {
  const [name, setName] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await grammarAdminAPI.createTopic({ name });
      toast.success("Thêm thành công");
      navigate("/admin/grammar");
    } catch {
      toast.error("Thêm thất bại");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="p-6 space-y-4">
        <h1 className="text-xl font-bold">Thêm Grammar Topic</h1>
        <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Tên chủ đề"
            className="border p-2 w-full"
        />
        <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded">
            Lưu
        </button>
    </form>
    
  );
};
export default AdminGrammarTopicCreate;
