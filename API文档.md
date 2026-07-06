# API 接口文档

## 基础信息

- **Base URL**: `http://127.0.0.1:5000/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

---

## 账号管理 API

### 1. 获取所有账号

**接口**: `GET /api/accounts`

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "张三",
      "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
      "area_id": "22",
      "date_preference": "tomorrow",
      "strategy": "first",
      "created_at": "2026-07-05T10:30:00",
      "updated_at": "2026-07-05T10:30:00",
      "token_status": {
        "is_valid": true,
        "message": "有效，剩余 95 分钟",
        "info": {
          "exp": 1720155000,
          "exp_formatted": "2026-07-05 12:30:00",
          "is_expired": false,
          "remaining_minutes": 95,
          "user_name": "张三",
          "user_id": "2023413383"
        }
      }
    }
  ]
}
```

### 2. 获取单个账号

**接口**: `GET /api/accounts/{id}`

**路径参数**:
- `id`: 账号 ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "张三",
    "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
    "area_id": "22",
    "date_preference": "tomorrow",
    "strategy": "first",
    "token_status": { ... }
  }
}
```

### 3. 添加账号

**接口**: `POST /api/accounts`

**请求体**:
```json
{
  "name": "张三",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "area_id": "22",
  "date_preference": "tomorrow",
  "strategy": "first"
}
```

**字段说明**:
- `name`: 姓名（必填）
- `token`: JWT Token（必填，会自动移除 bearer 前缀）
- `area_id`: 区域 ID（可选，默认 "22"）
- `date_preference`: 预约日期（可选，"today" 或 "tomorrow"，默认 "tomorrow"）
- `strategy`: 座位策略（可选，"first" 或 "random"，默认 "first"）

**响应示例**:
```json
{
  "success": true,
  "message": "账号添加成功",
  "data": {
    "id": 1,
    "name": "张三",
    ...
  }
}
```

### 4. 更新账号

**接口**: `PUT /api/accounts/{id}`

**路径参数**:
- `id`: 账号 ID

**请求体**:
```json
{
  "name": "李四",
  "token": "新的token...",
  "area_id": "23",
  "date_preference": "today",
  "strategy": "random"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "账号更新成功",
  "data": { ... }
}
```

### 5. 删除账号

**接口**: `DELETE /api/accounts/{id}`

**路径参数**:
- `id`: 账号 ID

**响应示例**:
```json
{
  "success": true,
  "message": "账号删除成功"
}
```

---

## 预约管理 API

### 1. 测试预约

**接口**: `POST /api/reservations/test`

**说明**: 模拟预约流程，不实际提交，用于检查是否有可用座位

**请求体**:
```json
{
  "account_id": 1
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "测试成功",
  "data": {
    "date": "2026-07-06",
    "segment": {
      "id": "1234",
      "start": "08:00",
      "end": "12:00"
    },
    "seat": {
      "id": "9185",
      "no": "233",
      "name": "233"
    },
    "available_seats_count": 45,
    "total_seats_count": 120
  }
}
```

### 2. 执行预约

**接口**: `POST /api/reservations/execute`

**说明**: 正式提交预约请求

**请求体**:
```json
{
  "account_id": 1
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "预约成功",
  "data": {
    "reservation": {
      "id": 1,
      "account_id": 1,
      "reservation_id": "56789",
      "seat_no": "233",
      "seat_id": "9185",
      "area_name": "区域 22",
      "segment_id": "1234",
      "reserve_date": "2026-07-06",
      "start_time": "08:00",
      "end_time": "12:00",
      "status": "confirmed",
      "message": "预约成功",
      "created_at": "2026-07-05T10:35:00"
    },
    "api_response": {
      "code": 1,
      "msg": "预约成功",
      "data": { ... }
    }
  }
}
```

### 3. 获取当前预约

**接口**: `POST /api/reservations/current`

**请求体**:
```json
{
  "account_id": 1
}
```

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": "56789",
      "spaceName": "233",
      "areaName": "东校区图书馆-三层自习室",
      "beginTime": "2026-07-06 08:00",
      "endTime": "2026-07-06 12:00"
    }
  ]
}
```

### 4. 取消预约

**接口**: `POST /api/reservations/cancel`

**请求体**:
```json
{
  "account_id": 1,
  "reservation_id": "56789"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "取消成功",
  "data": {
    "code": 1,
    "msg": "取消成功"
  }
}
```

### 5. 签到

**接口**: `POST /api/reservations/check-in`

**请求体**:
```json
{
  "account_id": 1
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "签到成功",
  "data": {
    "code": 1,
    "msg": "签到成功"
  }
}
```

### 6. 签退

**接口**: `POST /api/reservations/sign-out`

**请求体**:
```json
{
  "account_id": 1
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "签退成功",
  "data": {
    "code": 1,
    "msg": "签退成功"
  }
}
```

### 7. 获取历史记录

**接口**: `GET /api/reservations/history?account_id={id}`

**查询参数**:
- `account_id`: 账号 ID（可选，不填则返回所有记录）

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "account_id": 1,
      "reservation_id": "56789",
      "seat_no": "233",
      "reserve_date": "2026-07-06",
      "start_time": "08:00",
      "end_time": "12:00",
      "status": "confirmed",
      "created_at": "2026-07-05T10:35:00"
    }
  ]
}
```

---

## 错误响应

所有 API 在失败时返回统一格式：

```json
{
  "success": false,
  "message": "错误描述信息"
}
```

**HTTP 状态码**:
- `200`: 成功
- `201`: 创建成功
- `400`: 请求参数错误
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 常见错误

### Token 相关

```json
{
  "success": false,
  "message": "Token 无效: Token 已过期（2026-07-05 10:00:00）"
}
```

### 预约相关

```json
{
  "success": false,
  "message": "预约失败: 当前没有可用座位"
}
```

```json
{
  "success": false,
  "message": "获取时间段失败: 该区域不存在"
}
```

---

## 测试工具

### cURL 示例

```bash
# 获取所有账号
curl http://127.0.0.1:5000/api/accounts

# 添加账号
curl -X POST http://127.0.0.1:5000/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "token": "your_token_here",
    "area_id": "22"
  }'

# 测试预约
curl -X POST http://127.0.0.1:5000/api/reservations/test \
  -H "Content-Type: application/json" \
  -d '{"account_id": 1}'
```

### Postman 导入

可以将以上接口导入 Postman 进行测试。

---

**版本**: 1.0  
**更新时间**: 2026-07-05
