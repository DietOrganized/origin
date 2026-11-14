# 智能饮食助手

> 用户可通过手动输入记录每日饮食，系统自动识别食物并估算热量与营养成分，支持按日期查看和管理记录；同时提供热量趋势、营养素占比、餐次分布等数据统计，并根据历史饮食数据生成分析与建议，也可根据现有食材生成菜谱。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 | JDK 21 |
| 后端 | Spring Boot 3.5、MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 容器 | Tomcat 11（war 部署） |
| 大模型 | DeepSeek API |
| 前端 | 原生 JavaScript + JSP |
| 构建 | Maven |

## 更新记录

## 2026-11-14

- 完成今日概览、饮食记录、营养统计、灵感菜谱、智能建议和个人目标六个 JSP 页面。
- 提供公共侧边导航、页头页尾、统一配色及响应式布局。
- 当前页面使用占位内容展示结构，按钮会提示“第一阶段页面设计”；本阶段不调用业务 API。
- 沿用现有页面路由：应用根路径跳转至 /jsp/overview，其他模块通过导航进入。
- 验证：JavaScript 语法检查通过；两项 JSP 路由测试通过（mvn test）。

### 2026-11-14

- 新增统计接口：今日概览、周报、月报、自定义日期区间趋势
- 新增菜谱推荐：按现有食材或热量上限生成，支持保存与收藏
- 新增智能建议：先由后端计算指标，再交给大模型生成周 / 月饮食建议
- 补充页面路由，数据库不可用时给出更明确的提示
- 数据库连接支持 `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` 环境变量
- 新增非破坏性建表脚本 `sql/03_init_schema_safe.sql`

### 2026-11-13

- 接入 DeepSeek API，支持用一句话描述一餐并解析出食物、份量与热量
- 食物库匹配优先，未命中才调用大模型，解析结果回写食物库
- 餐次增删改查，支持直接提交自然语言记账
- 每日汇总按天重算，为统计接口做准备
- 新增接口调试页面，可逐项验证接口返回

### 2026-11-12

- 实现用户注册、登录、退出登录
- 登录签发 30 天有效 token，登出后写入黑名单立即失效
- JWT 拦截器统一鉴权，业务接口按用户隔离
- 个人资料维护：性别、生日、身高、体重、活动水平、目标类型
- 按 Mifflin-St Jeor 公式计算 BMR / TDEE，并按减脂 / 维持 / 增肌给出每日热量目标，支持手动覆盖

### 2026-11-11

- 搭建 Maven 工程（JDK 21 + Spring Boot 3.5 + MyBatis-Plus）
- 设计数据库表结构，共 11 张表：
  用户、用户资料、食物库、餐次、餐次明细、每日汇总、菜谱、菜谱收藏、饮食建议、LLM 调用日志、token 黑名单
- 编写建表脚本 `sql/01_schema.sql` 与常见食物初始化脚本 `sql/02_init_food.sql`（77 条数据）
- 实现统一响应体、业务码与全局异常处理
- 增加健康检查接口 `GET /api/v1/health`，用于验证服务与数据库连通

## 目录结构

```
├─ pom.xml
├─ sql/
│  ├─ 01_schema.sql              建库建表
│  ├─ 02_init_food.sql           常见食物初始化
│  └─ 03_init_schema_safe.sql    非破坏性建表（已存在则跳过）
└─ src/main/
   ├─ java/com/example/calorie/
   │  ├─ CalorieApplication.java
   │  ├─ client/      DeepSeek 接口客户端
   │  ├─ common/      统一响应、异常、业务码、分页
   │  ├─ config/      框架配置
   │  ├─ context/     当前登录用户上下文
   │  ├─ controller/  接口与页面路由
   │  ├─ dto/         请求参数
   │  ├─ entity/      数据库实体
   │  ├─ interceptor/ JWT 鉴权拦截器
   │  ├─ mapper/      MyBatis-Plus Mapper
   │  ├─ prompt/      Prompt 模板加载
   │  ├─ service/     业务逻辑
   │  ├─ task/        定时任务
   │  ├─ util/        工具类
   │  └─ vo/          响应对象
   ├─ resources/
   │  ├─ application.yml
   │  ├─ application-secret.example.yml
   │  ├─ prompts/     Prompt 模板
   │  └─ static/      单页版页面与接口调试页
   └─ webapp/WEB-INF/
      ├─ web.xml
      └─ jsp/         JSP 多页版页面
```

## 运行

### 1. 初始化数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/01_schema.sql
mysql -uroot -p --default-character-set=utf8mb4 < sql/02_init_food.sql
```

已有数据的环境改用 `sql/03_init_schema_safe.sql`，只创建缺失的表，不会清空数据。

### 2. 配置本地参数

在 `src/main/resources/` 下新建 `application-secret.yml`（已在 `.gitignore` 中排除）：

```yaml
spring:
  datasource:
    username: root
    password: 你的数据库密码
deepseek:
  api-key: 你的 DeepSeek API Key
```

### 3. 构建部署

```bash
mvn clean package -DskipTests
```

将 `target/calorie-app.war` 部署到 Tomcat 11，或在 IDEA 中配置 Tomcat 11 运行 `calorie-app:war exploded`。

### 4. 页面入口

| 地址 | 说明 |
| --- | --- |
| `/` | JSP 多页版（默认入口，自动跳转到今日概览） |
| `/index.html` | 保留的后端接口调试页 |
| `/swagger-ui.html` | 接口文档 |

### 5. 验证

```
GET http://localhost:8080/api/v1/health
```

```json
{"code":0,"data":{"app":"calorie-app","status":"UP","dbTime":"2026-11-14 10:00:00"}}
```

## 约定

- 统一响应：`{ code, message, data, timestamp }`，`code=0` 表示成功
- 接口前缀：`/api/v1`
- 时间格式：`yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`
