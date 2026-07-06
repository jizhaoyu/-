-- 图书馆预约管理系统 - MySQL 数据库初始化
-- 用法: mysql -u root -p < init_db.sql
-- 仅创建数据库；表结构由后端启动时 SQLAlchemy 的 db.create_all() 自动创建

CREATE DATABASE IF NOT EXISTS library_reservation
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 可选：创建专用账号并授权（按需取消注释并修改密码）
-- CREATE USER IF NOT EXISTS 'library'@'localhost' IDENTIFIED BY 'your_password';
-- GRANT ALL PRIVILEGES ON library_reservation.* TO 'library'@'localhost';
-- FLUSH PRIVILEGES;
