# EnglishLearningWeb

EnglishLearningWeb là một ứng dụng web hỗ trợ học tiếng Anh, phát triển chủ yếu bằng Java (Spring Boot) và JavaScript. Dự án nhằm cung cấp các tính năng giúp người dùng cải thiện kỹ năng tiếng Anh một cách hiệu quả, phù hợp cho học sinh, sinh viên hoặc bất kỳ ai muốn nâng cao trình độ tiếng Anh.

## Tính năng chính

- **Quản lý và luyện tập từ vựng:** Thêm, sửa, xóa, ôn tập từ vựng theo chủ đề.
- **Học ngữ pháp:** Học các ngữ pháp tiếng anh
- **Luyện nghe:** Tích hợp các bài nghe, có thể chấm điểm hoặc hiển thị phụ đề.
- **Luyện đọc hiểu:** Các đoạn văn, bài đọc với câu hỏi kiểm tra mức độ hiểu bài.
- **Kiểm tra và thống kê kết quả học tập:** Theo dõi tiến trình, thành tích học tập.
- **Giao diện thân thiện:** Thiết kế hiện đại, dễ sử dụng trên cả desktop và mobile.

## Công nghệ sử dụng

- **Backend:** Java (Spring Boot)
- **Frontend:** JavaScript, HTML, CSS, React Native
- **Cơ sở dữ liệu:** PostgreSQL
- **Cache/Message Broker:** Redis (chạy trên Docker)

## Cài đặt và chạy dự án

### Yêu cầu

- Java 11 trở lên
- Maven
- Node.js (nếu build frontend riêng)
- PostgreSQL
- Docker (để chạy Redis)

### Hướng dẫn cài đặt

1. **Clone repository**
   ```bash
   git clone https://github.com/ThanhNB-NBT/EnglishLearningWeb.git
   cd EnglishLearningWeb
   ```

2. **Cấu hình cơ sở dữ liệu PostgreSQL**
   - Tạo database mới trên PostgreSQL, ví dụ: `english_learning`
   - Mở file `application.properties` hoặc `application.yml` trong thư mục `src/main/resources`
   - Cập nhật thông tin kết nối:
     ```
     spring.datasource.url=jdbc:postgresql://localhost:5432/english_learning
     spring.datasource.username=your_postgres_username
     spring.datasource.password=your_postgres_password
     ```

3. **Chạy Redis trên Docker**
   ```bash
   docker run -d --name redis -p 6379:6379 redis
   ```
   - Redis sẽ chạy trên `localhost:6379`. Nếu cần cấu hình thêm, cập nhật thông tin redis trong file cấu hình Spring Boot.

4. **Build và chạy backend**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **(Tùy chọn) Build frontend**
   - Nếu frontend tách riêng, di chuyển vào thư mục frontend và chạy:
     ```bash
     npm install
     npm start
     ```

6. **Truy cập ứng dụng**
   - Mặc định tại `http://localhost:8080` (hoặc cổng bạn đã cấu hình)

## Đóng góp

Chào mừng mọi ý kiến đóng góp! Vui lòng tạo Pull Request hoặc Issue để thảo luận thêm.

## License

Dự án sử dụng giấy phép MIT. Xem thêm trong file [LICENSE](LICENSE).

---

**Tác giả:** [ThanhNB-NBT](https://github.com/ThanhNB-NBT)
