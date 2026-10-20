# 企业员工信息管理系统

> 课程设计 · 全栈独立开发（后端为主）｜ SpringBoot + Vue 前后端分离架构

基于 SpringBoot 3.3 + MyBatis + MySQL + Redis 的企业级员工信息管理系统，实现**员工管理、部门管理、权限控制**三大核心模块，覆盖完整企业组织管理业务流程。

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.3.4（JDK 17） |
| 持久层 | MyBatis 3.0.3 + PageHelper 2.1.0 |
| 数据库 | MySQL 8.x（Druid 连接池） |
| 缓存 | Redis（Spring Data Redis + Lettuce 池） |
| 权限 | 自定义注解 + AOP 切面 + HandlerInterceptor |
| 前端 | Vue 3 + Vite 5 + Element Plus + Vue Router + Pinia + Axios |
| 工具 | Maven、Git、Postman |

## 二、快速启动

### 1. 环境准备

- JDK **17**（重要：Lombok 与 JDK 23+ 不兼容，IDEA 请将 Project SDK 设为 17）
- MySQL 8.x，本地启动，账号 root
- Redis，本地启动（默认 6379 端口，无密码）
- Node.js 18+（前端）

### 2. 数据库

无需手动建表：Spring Boot 启动时自动执行 `src/main/resources/db/schema.sql`（四张表）和 `data.sql`（种子数据）。

数据库名固定为 `emp_manage`，连接信息在 `application.yml`：

```yaml
url: jdbc:mysql://localhost:3306/emp_manage?...
username: root
password: 你的MySQL密码   # ← 改成自己的
```

如需手动执行：先建库 `CREATE DATABASE emp_manage DEFAULT CHARSET utf8mb4;`，其余交给 SQL 脚本。

### 3. 启动后端

```bash
# 在项目根目录
mvn spring-boot:run
```

启动成功后可访问 Druid 监控台：http://localhost:8080/druid

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 ，Vite 已将 `/api` 代理到后端 8080。

### 5. 演示账号

| 账号 | 密码 | 角色 | 权限 |
|---|---|---|---|
| admin | admin123 | 管理员 | 全部（增删改查） |
| zhangsan | 123456 | 普通用户 | 只读（emp:view / dept:view） |

用 zhangsan 登录时，界面上的新增/编辑/删除按钮自动隐藏，直接调接口也会被 AOP 切面拦下返回 403——**前后端双重权限控制**。

## 三、架构设计

### 3.1 后端分层（Controller - Service - Mapper）

```
com.example.demo3
├── common/            # 统一返回 Result / 分页 PageResult / 全局异常处理 / 错误码
├── config/            # Redis 序列化配置 / WebMvc 拦截器注册
├── controller/        # EmpController / DeptController / AuthController / PermissionController
├── service/           # 业务接口 + impl（业务校验、分页、缓存策略都在这层）
├── mapper/            # MyBatis 接口
├── entity/            # 实体类（带 jakarta.validation 校验注解）
├── dto/               # EmpQueryDTO（条件分页参数）/ LoginDTO
├── annotation/        # @RequirePermission 权限注解
├── aspect/            # PermissionAspect 权限切面
└── interceptor/       # AuthInterceptor 登录态拦截器
```

职责纪律：Controller 只收参数和包装返回；业务校验全部写在 Service；Mapper 只写 SQL。

### 3.2 数据模型（四张核心表）

| 表 | 说明 | 关键设计 |
|---|---|---|
| `dept` | 部门表 | `uk_dept_name` 部门名唯一 |
| `emp` | 员工表 | `dept_id` 外键关联 dept；联合索引 `idx_emp_name_dept(name, dept_id)` 覆盖最高频查询 |
| `permission` | 角色权限表 | `(role, perm_code)` 联合唯一，一角色一权限一行 |
| `sys_user` | 系统用户（角色）表 | 账号唯一索引 + 角色索引 |

建表语句见 `resources/db/schema.sql`，含完整注释。

### 3.3 Redis 缓存策略

热点部门数据三级保障：

1. **读**：先查缓存，命中直接返回不打 MySQL；未命中查库后回写，TTL 30 分钟
2. **写**：增/改/删后**主动失效**相关缓存（列表 key + 精确 key），下次读自然回源重建
3. **兜底**：TTL 到期自动过期 + Redis 故障时 try-catch **降级走库**——缓存是加速件，不是单点

缓存键规范：`dept:list`、`dept:{id}`、`perm:role:{role}`、`auth:token:{token}`

### 3.4 权限控制链路

```
请求 → AuthInterceptor（校验登录态 Token → Redis）
     → @RequirePermission 注解 → PermissionAspect 切面（查角色权限码）
     → Controller 业务执行
```

- 登录：`POST /auth/login` 校验账号密码，生成 UUID Token 存 Redis（2 小时滑动过期）
- 鉴权：切面从请求上下文拿用户角色 → 查 `permission` 表（带缓存）→ 不含所需权限码抛 403
- 前端：Pinia 存权限码列表，按钮级 `v-if` 控制，越权操作根本不出现在界面上

## 四、接口文档

统一返回格式：`{"code":200,"msg":"操作成功","data":{...}}`；错误码：400 参数错误 / 401 未登录 / 403 无权限 / 404 不存在 / 500 系统错误。

除登录接口外，所有请求需带请求头：`Authorization: Bearer {token}`

### 认证

| 方法 | 路径 | 说明 | 所需权限 |
|---|---|---|---|
| POST | /auth/login | 登录，返回 token + 权限列表 | 无 |
| POST | /auth/logout | 注销，删除 Redis 登录态 | 登录 |
| GET | /auth/me | 当前用户信息 + 权限列表 | 登录 |

### 员工管理

| 方法 | 路径 | 说明 | 所需权限 |
|---|---|---|---|
| GET | /emp/page | **条件分页查询** `?pageNum=1&pageSize=10&name=张&deptId=1&job=工程师&gender=男` | emp:view |
| GET | /emp/list | 查询全部（联查部门名） | emp:view |
| GET | /emp/{id} | 按 id 查询 | emp:view |
| POST | /emp/add | 新增（@Valid 字段校验） | emp:add |
| PUT | /emp/{id} | 修改 | emp:edit |
| DELETE | /emp/{id} | 删除 | emp:delete |
| DELETE | /emp/batch | 批量删除，请求体 `[1,2,3]` | emp:delete |

### 部门管理

| 方法 | 路径 | 说明 | 所需权限 |
|---|---|---|---|
| GET | /dept/list | 查询全部（带员工数，走缓存） | dept:view |
| GET | /dept/{id} | 按 id 查询（走缓存） | dept:view |
| POST | /dept/add | 新增（部门名唯一校验） | dept:add |
| PUT | /dept/{id} | 修改 | dept:edit |
| DELETE | /dept/{id} | 删除（有员工的部门返回业务错误） | dept:delete |

### 权限管理

| 方法 | 路径 | 说明 | 所需权限 |
|---|---|---|---|
| GET | /perm/list?role=admin | 查询角色权限列表 | perm:view |

### Postman 测试要点

- 参数缺失：`POST /emp/add` 不带 name → 400「员工姓名不能为空」
- 非法值：age 传 10 → 400「年龄必须在18~60之间」；手机号传 123 → 400「手机号格式不正确」
- 权限不足：zhangsan 调 `POST /emp/add` → 403「没有权限执行该操作」
- 未登录：不带 Authorization 头 → 401「未登录，请先登录」
- 业务一致性：删除有员工的部门 → 500「该部门下还有 N 名员工，不能删除」

## 五、工程化实践

- **Maven**：依赖版本统一由 spring-boot-starter-parent 管理；Lombok 显式锁 1.18.48（父工程版本不支持 JDK 23+）
- **Git**：`.gitignore` 已忽略 target/、node_modules/、本地备份目录
- **统一结果封装**：所有接口返回 `Result<T>`，前端 axios 拦截器统一解包
- **全局异常处理**：`@RestControllerAdvice` 捕获业务异常、参数校验异常、系统异常，堆栈只进日志不泄露前端
- **SQL 优化**：员工/部门联查用一次 JOIN + resultMap 映射，替代循环单查的 N+1 问题；批量删除用单条 `IN` 语句；PageHelper 物理分页
- **AI 工具协作**：样板代码生成、逻辑梳理用 Cursor/Codex 辅助，核心业务逻辑（缓存策略、权限链路、异常体系）人工设计与把控

## 六、目录结构

```
demo3/
├── pom.xml                        # Maven 配置
├── src/main/
│   ├── java/com/example/demo3/    # 后端源码（见 3.1）
│   └── resources/
│       ├── application.yml        # 应用配置（数据源/Redis/分页/SQL初始化）
│       ├── db/schema.sql          # 四张核心表 + 索引 + 外键
│       ├── db/data.sql            # 种子数据
│       └── mapper/                # MyBatis XML（EmpMapper/DeptMapper/SysUserMapper）
└── frontend/                      # Vue3 前端工程
    ├── src/views/                 # Login / EmpList / DeptList / PermList
    ├── src/api/                   # axios 封装 + 各模块接口
    ├── src/store/user.js          # Pinia 用户状态（token/权限码）
    └── src/router/                # 路由 + 登录守卫
```

## 七、本地验证记录

以下链路已在本机（MySQL 真实连接 + Redis 协议桩）完整跑通：

| # | 验证项 | 结果 |
|---|---|---|
| 1 | 应用启动（Bean 装配 / MyBatis 扫描 / AOP / 拦截器） | ✅ 4.3s 启动，Tomcat 就绪 |
| 2 | 登录（admin/admin123）→ 签发 Token 存 Redis | ✅ |
| 3 | 条件分页查询 `?pageNum=1&pageSize=3` | ✅ total/pages 正确，JOIN 联查 deptName |
| 4 | 部门列表联查员工数 | ✅ 子查询统计正确 |
| 5 | **缓存命中**：连续两次 /dept/list | ✅ 首次查库，后两次「缓存命中」零 SQL |
| 6 | **缓存联动失效**：新增员工后再查部门 | ✅ 研发部 3→4 人，DEL dept:list 实证 |
| 7 | 新增员工 + 部门存在性校验 | ✅「所选部门不存在」人话错误 |
| 8 | 批量删除（单条 IN 语句） | ✅ |
| 9 | zhangsan 越权访问 /perm/list、POST /emp/add | ✅ 403「没有权限执行该操作」 |
| 10 | zhangsan 正常访问 /emp/page（有 view 权限） | ✅ 200 |
| 11 | 非法参数 age=10 | ✅ 400「年龄必须在18~60之间」 |
| 12 | 注销后原 Token 再访问 | ✅ 401「登录已过期」 |
| 13 | 无 Token 访问业务接口 | ✅ 401「未登录，请先登录」 |

## 八、已知限制（课程设计边界）

- 密码明文存储：生产环境应使用 BCrypt + 盐值，并加登录失败次数限制
- 无注册/找回密码：账号由管理员在数据库预置
- 权限粒度到「角色 + 权限码」：更细可扩展到数据行级权限
