# Spring Boot JWT 认证系统使用指南

## 项目概述

这是一个基于Spring Boot的JWT认证系统，实现了用户注册、登录、token验证等功能。

## 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+

## 项目结构

```
src/main/java/com/example/jwtauth/
├── config/
│   └── WebSecurityConfig.java          # Spring Security配置
├── controller/
│   ├── AuthController.java             # 认证控制器
│   └── TestController.java             # 测试控制器
├── dto/
│   ├── JwtResponse.java                # JWT响应DTO
│   ├── LoginRequest.java               # 登录请求DTO
│   ├── MessageResponse.java            # 消息响应DTO
│   └── SignupRequest.java              # 注册请求DTO
├── entity/
│   └── User.java                       # 用户实体
├── repository/
│   └── UserRepository.java             # 用户数据访问层
├── security/
│   ├── AuthEntryPointJwt.java          # 认证入口点
│   └── AuthTokenFilter.java            # JWT认证过滤器
├── service/
│   ├── AuthService.java                # 认证服务
│   └── UserDetailsServiceImpl.java     # 用户详情服务
├── util/
│   └── JwtUtils.java                   # JWT工具类
└── JwtAuthApplication.java             # 主应用程序类
```

## 配置步骤

### 1. 数据库配置

创建MySQL数据库：
```sql
CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `application.yml` 中的数据库连接配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 2. JWT密钥配置

修改 `application.yml` 中的JWT配置：
```yaml
jwt:
  secret: your_secret_key_here_at_least_32_characters
  expiration: 86400000  # 24小时
```

## 启动应用

```bash
mvn clean install
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## API接口说明

### 认证接口

#### 1. 用户注册
- **URL**: `POST /api/auth/signup`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
    "username": "testuser",
    "email": "test@example.com",
    "password": "123456"
}
```
- **响应**:
```json
{
    "message": "User registered successfully!"
}
```

#### 2. 用户登录
- **URL**: `POST /api/auth/login`
- **Content-Type**: `application/json`
- **请求体**:
```json
{
    "username": "testuser",
    "password": "123456"
}
```
- **响应**:
```json
{
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "roles": ["ROLE_USER"]
}
```

#### 3. 验证Token
- **URL**: `POST /api/auth/validate?token=your_jwt_token`
- **响应**:
```json
{
    "message": "Token is valid for user: testuser"
}
```

#### 4. 获取当前用户信息
- **URL**: `GET /api/auth/me`
- **Header**: `Authorization: Bearer your_jwt_token`
- **响应**:
```json
{
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "roles": ["ROLE_USER"]
}
```

### 测试接口

#### 1. 公开接口（无需认证）
- **URL**: `GET /api/test/public`
- **响应**: `"Public Content."`

#### 2. 用户接口（需要认证）
- **URL**: `GET /api/test/user`
- **Header**: `Authorization: Bearer your_jwt_token`
- **响应**: `"User Content."`

#### 3. 管理员接口（需要管理员权限）
- **URL**: `GET /api/test/admin`
- **Header**: `Authorization: Bearer your_jwt_token`
- **响应**: `"Admin Board."`

## 测试示例

### 使用curl测试

1. **注册用户**:
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "123456"
  }'
```

2. **用户登录**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

3. **访问受保护的接口**:
```bash
curl -X GET http://localhost:8080/api/test/user \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 使用Postman测试

1. 导入以下请求到Postman
2. 先调用注册接口创建用户
3. 调用登录接口获取JWT token
4. 在需要认证的请求中添加Header: `Authorization: Bearer YOUR_JWT_TOKEN`

## 重要配置说明

### JWT配置
- `jwt.secret`: JWT签名密钥，建议使用至少32位的随机字符串
- `jwt.expiration`: Token过期时间（毫秒），默认24小时

### 数据库配置
- 使用JPA自动创建表结构
- 密码使用BCrypt加密存储
- 支持用户名和邮箱唯一性验证

### 安全配置
- 使用JWT进行无状态认证
- 支持CORS跨域请求
- 实现了认证失败的统一异常处理

## 扩展功能

可以基于此项目添加以下功能：
- 刷新Token机制
- 用户角色管理
- 密码重置功能
- 用户信息修改
- 登录日志记录
- 多因素认证

## 注意事项

1. 生产环境中请修改JWT密钥
2. 根据需要调整Token过期时间
3. 考虑添加限流和防暴力破解机制
4. 定期更新依赖包版本以确保安全性