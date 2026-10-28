-- 第二周沿用的四表结构；仅手动导入，不由MySQL模式自动执行。
-- 适用 MySQL 8.0.16+。不要对已有生产库运行；先核对数据库名。
CREATE DATABASE IF NOT EXISTS campus_news
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campus_news;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL COMMENT '认证模块生成BCrypt哈希，禁止存明文',
  display_name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  CONSTRAINT chk_user_role CHECK (role IN ('PUBLISHER', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS news_category (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS news (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(200) NOT NULL,
  summary VARCHAR(500) NOT NULL DEFAULT '',
  body MEDIUMTEXT NOT NULL,
  author_name VARCHAR(50) NOT NULL,
  creator_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_news_public (status, published_at, id),
  KEY idx_news_category_public (category_id, status, published_at, id),
  KEY idx_news_creator (creator_id),
  CONSTRAINT fk_news_category FOREIGN KEY (category_id) REFERENCES news_category(id),
  CONSTRAINT fk_news_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id),
  CONSTRAINT chk_news_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN')),
  CONSTRAINT chk_news_published_time CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL),
  CONSTRAINT chk_news_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
  CONSTRAINT chk_news_body CHECK (CHAR_LENGTH(TRIM(body)) > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notice (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  body MEDIUMTEXT NOT NULL,
  publisher_unit VARCHAR(100) NOT NULL COMMENT '发布单位，如教务处',
  creator_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notice_public (status, published_at, id),
  KEY idx_notice_creator (creator_id),
  CONSTRAINT fk_notice_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id),
  CONSTRAINT chk_notice_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN')),
  CONSTRAINT chk_notice_published_time CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL),
  CONSTRAINT chk_notice_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
  CONSTRAINT chk_notice_body CHECK (CHAR_LENGTH(TRIM(body)) > 0)
) ENGINE=InnoDB;

-- SQL不预建登录账号；应用通过显式BOOTSTRAP初始化管理员并生成BCrypt哈希。
-- IF NOT EXISTS 只负责初建，不会把已有表自动迁移成新结构。

