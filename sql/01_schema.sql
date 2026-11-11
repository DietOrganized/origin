-- =============================================================
-- 食物热量与菜谱推荐系统 - 建库建表脚本
-- MySQL 8.0+ / utf8mb4 / InnoDB
-- 执行方式：mysql -uroot -p < 01_schema.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `calorie_app`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `calorie_app`;

-- -------------------------------------------------------------
-- 1. 用户
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(50)  NOT NULL                COMMENT '登录名',
  `password`      VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密文',
  `nickname`      VARCHAR(50)      NULL                COMMENT '昵称',
  `email`         VARCHAR(100)     NULL                COMMENT '邮箱',
  `avatar`        VARCHAR(255)     NULL                COMMENT '头像 URL',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  `last_login_at` DATETIME         NULL                COMMENT '最后登录时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- -------------------------------------------------------------
-- 2. 用户资料与热量目标
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile` (
  `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`              BIGINT      NOT NULL COMMENT '用户 ID',
  `gender`               VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'MALE/FEMALE/UNKNOWN',
  `birth_date`           DATE            NULL COMMENT '出生日期，用于算年龄',
  `height_cm`            DECIMAL(5,1)    NULL COMMENT '身高 cm',
  `weight_kg`            DECIMAL(5,1)    NULL COMMENT '体重 kg',
  `activity_level`       VARCHAR(20) NOT NULL DEFAULT 'SEDENTARY'
                         COMMENT 'SEDENTARY/LIGHT/MODERATE/ACTIVE/VERY_ACTIVE',
  `goal_type`            VARCHAR(20) NOT NULL DEFAULT 'MAINTAIN'
                         COMMENT 'LOSE_FAT/MAINTAIN/GAIN_MUSCLE',
  `target_weight_kg`     DECIMAL(5,1)    NULL COMMENT '目标体重 kg',
  `bmr`                  INT             NULL COMMENT '基础代谢 kcal（计算值）',
  `tdee`                 INT             NULL COMMENT '每日总消耗 kcal（计算值）',
  `daily_calorie_target` INT             NULL COMMENT '每日热量目标 kcal',
  `target_manual`        TINYINT     NOT NULL DEFAULT 0 COMMENT '1 表示用户手动指定目标',
  `created_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料与热量目标';

-- -------------------------------------------------------------
-- 3. 食物营养库（同时作为 LLM 解析结果缓存）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `food`;
CREATE TABLE `food` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `name`               VARCHAR(100) NOT NULL COMMENT '食物名称（规范名）',
  `alias`              VARCHAR(255)     NULL COMMENT '别名，逗号分隔',
  `category`           VARCHAR(50)      NULL COMMENT '主食/肉蛋/水产/豆制品/乳制品/蔬菜/水果/饮品/零食/菜肴',
  `calories_per_100g`  DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每 100g 热量 kcal',
  `protein_per_100g`   DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '蛋白质 g',
  `fat_per_100g`       DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '脂肪 g',
  `carbs_per_100g`     DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '碳水 g',
  `fiber_per_100g`     DECIMAL(8,2)     NULL COMMENT '膳食纤维 g',
  `sodium_per_100g`    DECIMAL(8,2)     NULL COMMENT '钠 mg',
  `default_unit`       VARCHAR(20)      NULL COMMENT '常用单位：碗/个/杯/份',
  `default_unit_grams` DECIMAL(8,2)     NULL COMMENT '一个常用单位约多少克',
  `source`             VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/LLM/USER',
  `verified`           TINYINT      NOT NULL DEFAULT 0 COMMENT '1 已人工校验',
  `created_by`         BIGINT           NULL COMMENT '创建者（用户录入时）',
  `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_name` (`name`),
  KEY `idx_food_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物营养库';

-- -------------------------------------------------------------
-- 4. 餐次主表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `meal_record`;
CREATE TABLE `meal_record` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT        NOT NULL,
  `meal_type`      VARCHAR(20)   NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER/SNACK',
  `eaten_at`       DATETIME      NOT NULL COMMENT '进食时间',
  `stat_date`      DATE          NOT NULL COMMENT '统计日期（由 eaten_at 截取）',
  `total_calories` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总热量 kcal',
  `total_protein`  DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_fat`      DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_carbs`    DECIMAL(10,2) NOT NULL DEFAULT 0,
  `raw_input`      VARCHAR(500)      NULL COMMENT '用户原始自然语言输入',
  `note`           VARCHAR(255)      NULL,
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_meal_user_date` (`user_id`, `stat_date`),
  KEY `idx_meal_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐次记录';

-- -------------------------------------------------------------
-- 5. 餐次明细
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `meal_item`;
CREATE TABLE `meal_item` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `meal_record_id` BIGINT        NOT NULL,
  `food_id`        BIGINT            NULL COMMENT '关联食物库，可为空',
  `food_name`      VARCHAR(100)  NOT NULL COMMENT '冗余名称，食物改名不影响历史',
  `quantity`       DECIMAL(8,2)  NOT NULL DEFAULT 1 COMMENT '数量',
  `unit`           VARCHAR(20)       NULL COMMENT '单位：碗/个/g',
  `grams`          DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '折算克重',
  `calories`       DECIMAL(10,2) NOT NULL DEFAULT 0,
  `protein`        DECIMAL(10,2) NOT NULL DEFAULT 0,
  `fat`            DECIMAL(10,2) NOT NULL DEFAULT 0,
  `carbs`          DECIMAL(10,2) NOT NULL DEFAULT 0,
  `confidence`     DECIMAL(3,2)      NULL COMMENT 'LLM 置信度 0~1',
  `source`         VARCHAR(10)   NOT NULL DEFAULT 'LLM' COMMENT 'LLM/MANUAL',
  `sort_order`     INT           NOT NULL DEFAULT 0,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_meal` (`meal_record_id`),
  KEY `idx_item_food` (`food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐次明细';

-- -------------------------------------------------------------
-- 6. 每日汇总（派生表，写入时按天重算）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `daily_summary`;
CREATE TABLE `daily_summary` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT        NOT NULL,
  `stat_date`       DATE          NOT NULL,
  `total_calories`  DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_protein`   DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_fat`       DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_carbs`     DECIMAL(10,2) NOT NULL DEFAULT 0,
  `meal_count`      INT           NOT NULL DEFAULT 0,
  `target_calories` INT               NULL COMMENT '当日目标快照',
  `is_achieved`     TINYINT       NOT NULL DEFAULT 0,
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary_user_date` (`user_id`, `stat_date`),
  KEY `idx_summary_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇总';

-- -------------------------------------------------------------
-- 7. 菜谱
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `recipe`;
CREATE TABLE `recipe` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT            NULL COMMENT 'NULL 表示公共菜谱',
  `title`            VARCHAR(100)  NOT NULL,
  `description`      VARCHAR(500)      NULL,
  `servings`         INT           NOT NULL DEFAULT 1 COMMENT '份数',
  `cooking_time_min` INT               NULL COMMENT '烹饪时间（分钟）',
  `difficulty`       VARCHAR(10)       NULL COMMENT 'EASY/MEDIUM/HARD',
  `ingredients`      JSON              NULL COMMENT '[{name,amount,unit,grams,calories}]',
  `steps`            JSON              NULL COMMENT '["步骤1","步骤2"]',
  `total_calories`   DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_protein`    DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_fat`        DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_carbs`      DECIMAL(10,2) NOT NULL DEFAULT 0,
  `tags`             VARCHAR(255)      NULL COMMENT '标签，逗号分隔',
  `source_prompt`    VARCHAR(500)      NULL COMMENT '生成时的用户条件',
  `model`            VARCHAR(50)       NULL COMMENT '生成所用模型',
  `is_public`        TINYINT       NOT NULL DEFAULT 0,
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_user` (`user_id`),
  KEY `idx_recipe_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱';

-- -------------------------------------------------------------
-- 8. 菜谱收藏
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `recipe_favorite`;
CREATE TABLE `recipe_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL,
  `recipe_id`  BIGINT   NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_recipe` (`user_id`, `recipe_id`),
  KEY `idx_fav_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱收藏';

-- -------------------------------------------------------------
-- 9. 智能建议
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `nutrition_advice`;
CREATE TABLE `nutrition_advice` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT      NOT NULL,
  `period_type`  VARCHAR(10) NOT NULL COMMENT 'WEEK/MONTH',
  `period_start` DATE        NOT NULL,
  `period_end`   DATE        NOT NULL,
  `content`      TEXT        NOT NULL COMMENT 'Markdown 建议正文',
  `metrics`      JSON            NULL COMMENT '规则计算出的指标快照',
  `model`        VARCHAR(50)     NULL,
  `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_advice_user_period` (`user_id`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能建议';

-- -------------------------------------------------------------
-- 10. LLM 调用日志
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `llm_call_log`;
CREATE TABLE `llm_call_log` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT          NULL,
  `scene`             VARCHAR(30) NOT NULL COMMENT 'FOOD_PARSE/RECIPE/ADVICE',
  `model`             VARCHAR(50) NOT NULL,
  `request_summary`   VARCHAR(500)    NULL COMMENT '输入摘要（截断）',
  `prompt_tokens`     INT         NOT NULL DEFAULT 0,
  `completion_tokens` INT         NOT NULL DEFAULT 0,
  `total_tokens`      INT         NOT NULL DEFAULT 0,
  `latency_ms`        INT         NOT NULL DEFAULT 0,
  `success`           TINYINT     NOT NULL DEFAULT 1,
  `error_msg`         VARCHAR(500)    NULL,
  `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_user_time` (`user_id`, `created_at`),
  KEY `idx_log_scene` (`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM 调用日志';

-- -------------------------------------------------------------
-- 11. 登出黑名单（配合 30 天免登录）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `token_blacklist`;
CREATE TABLE `token_blacklist` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `jti`        VARCHAR(64) NOT NULL COMMENT 'JWT 唯一标识',
  `user_id`    BIGINT      NOT NULL,
  `expire_at`  DATETIME    NOT NULL COMMENT 'token 原始过期时间，到期可清理',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blacklist_jti` (`jti`),
  KEY `idx_blacklist_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登出 token 黑名单';
