# 🌐 English Learning Web

Nền tảng học tiếng Anh trực tuyến toàn diện, giúp người học phát triển các kỹ năng tiếng Anh thông qua các bài học ngữ pháp, luyện nghe và luyện đọc.

## ✨ Tính năng chính

- 📚 **Học ngữ pháp**: Hệ thống bài học ngữ pháp từ cơ bản đến nâng cao
- 🎧 **Luyện nghe**: Các bài tập nghe hiểu với nhiều cấp độ khác nhau
- 📖 **Luyện đọc**: Bài tập đọc hiểu với đa dạng chủ đề
- 👤 **Quản lý tài khoản**: Đăng ký, đăng nhập, quản lý tiến độ học tập

## 🛠️ Công nghệ sử dụng

### Backend
- **Java 21** - Ngôn ngữ lập trình chính
- **Spring Boot 3.5.5** - Framework backend
- **Spring Security** - Bảo mật và xác thực
- **JWT** - Token-based authentication
- **PostgreSQL** - Cơ sở dữ liệu
- **Redis** - Session management và caching
- **Maven** - Quản lý dependencies

### Frontend
- **Vue.js 3** - Framework JavaScript
- **Vite** - Build tool
- **Element Plus** - UI component library
- **Pinia** - State management
- **Axios** - HTTP client
- **Vue Router** - Routing

### DevOps
- **Docker & Docker Compose** - Containerization
- **Swagger/OpenAPI** - API documentation

## 📋 Yêu cầu hệ thống

- Docker Desktop (phiên bản mới nhất)
- Docker Compose V2
- Git

## 🚀 Hướng dẫn cài đặt và chạy (Docker)

### Bước 1: Clone repository

```bash
git clone https://github.com/ThanhNB-NBT/EnglishLearningWeb.git
cd EnglishLearningWeb
```

### Bước 2: Cấu hình biến môi trường

Tạo file `.env` từ file mẫu:

```bash
cp .env.example .env
```

Chỉnh sửa file `.env` với thông tin của bạn:

```env
# ========== DATABASE ==========
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_strong_password
POSTGRES_DB=EL_test

# ========== EMAIL (Optional - cho tính năng gửi email) ==========
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your_16_char_app_password

# ========== JWT ==========
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_at_least_64_characters
JWT_EXPIRATION=86400000

# ========== AI API (Optional - cho tính năng AI) ==========
AI_GEMINI_API_KEY=your_gemini_api_key
AI_GROQ_API_KEY=your_groq_api_key
```

**Lưu ý quan trọng:**
- `JWT_SECRET`: Nên là chuỗi ngẫu nhiên dài ít nhất 64 ký tự
- `SPRING_MAIL_PASSWORD`: Phải là App Password của Gmail (không phải mật khẩu thông thường)
- Các API key AI là tùy chọn, nếu không có có thể để giá trị mặc định

### Bước 3: Khởi chạy ứng dụng

```bash
docker-compose up -d
```

Lệnh này sẽ:
- Tải và khởi chạy PostgreSQL database
- Tải và khởi chạy Redis server
- Build và khởi chạy Backend API
- Build và khởi chạy Frontend Vue.js

### Bước 4: Kiểm tra trạng thái

```bash
docker-compose ps
```

Đảm bảo tất cả services đang chạy (status: `Up`):
- `english-db-test` (PostgreSQL)
- `english-app-redis` (Redis)
- `english-app-test` (Backend)
- `english-app-frontend` (Frontend)

### Bước 5: Truy cập ứng dụng

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8980
- **API Documentation**: http://localhost:8980/swagger-ui.html
- **Health Check**: http://localhost:8980/actuator/health

## 🎯 Các lệnh hữu ích

### Xem logs

```bash
# Xem logs tất cả services
docker-compose logs -f

# Xem logs của một service cụ thể
docker-compose logs -f app        # Backend
docker-compose logs -f frontend   # Frontend
docker-compose logs -f db         # Database
```

### Dừng ứng dụng

```bash
docker-compose down
```

### Dừng và xóa dữ liệu

```bash
docker-compose down -v
```

### Rebuild images

```bash
docker-compose up -d --build
```

### Restart một service cụ thể

```bash
docker-compose restart app        # Restart backend
docker-compose restart frontend   # Restart frontend
```

## 📁 Cấu trúc dự án

```
EnglishLearningWeb/
├── backend/                    # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java source code
│   │   │   └── resources/     # Application properties
│   │   └── test/              # Test files
│   ├── pom.xml                # Maven dependencies
│   └── Dockerfile.dev         # Docker configuration
│
├── frontend-vue/              # Vue.js frontend
│   ├── src/
│   │   ├── components/        # Vue components
│   │   ├── views/             # Page views
│   │   ├── router/            # Router configuration
│   │   ├── stores/            # Pinia stores
│   │   └── assets/            # Static assets
│   ├── package.json           # NPM dependencies
│   └── Dockerfile             # Docker configuration
│
├── docker-compose.yml         # Docker compose configuration
├── .env.example               # Environment variables template
└── README.md                  # Documentation
```

## 🐛 Xử lý sự cố

### Backend không khởi động được

1. Kiểm tra logs: `docker-compose logs app`
2. Đảm bảo PostgreSQL đã khởi động: `docker-compose ps db`
3. Kiểm tra biến môi trường trong file `.env`

### Frontend không kết nối được Backend

1. Kiểm tra Backend đã chạy: http://localhost:8980/actuator/health
2. Xem logs frontend: `docker-compose logs frontend`
3. Kiểm tra CORS configuration trong backend

### Database connection error

1. Kiểm tra PostgreSQL đã chạy: `docker-compose ps db`
2. Verify database credentials trong `.env`
3. Xem logs database: `docker-compose logs db`

### Port đã được sử dụng

Nếu port bị conflict, chỉnh sửa ports trong `docker-compose.yml`:

```yaml
ports:
  - "8980:8980"  # Thay 8980 thành port khác nếu cần
```

## 📞 Liên hệ

- **GitHub**: [@ThanhNB-NBT](https://github.com/ThanhNB-NBT)
- **Repository**: [EnglishLearningWeb](https://github.com/ThanhNB-NBT/EnglishLearningWeb)

---

⭐ Nếu dự án hữu ích, đừng quên cho một star nhé!
