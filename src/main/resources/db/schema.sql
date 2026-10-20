-- ============================================================
-- 企业员工信息管理系统 · 建表脚本
-- 四张核心表：dept（部门）、emp（员工）、permission（权限）、sys_user（用户角色）
-- 设计要点：
--   1. 主键统一自增 INT；时间字段用 DATETIME + ON UPDATE 自动维护
--   2. emp.dept_id 外键关联 dept.id，保证部门必须存在才能挂员工
--   3. 联合索引 idx_emp_name_dept 覆盖「姓名+部门」最高频的条件查询组合
--   4. permission 以 (role, perm_code) 唯一约束，一个角色一种权限只存一行
-- 执行方式：Spring Boot 启动时自动执行（application.yml: spring.sql.init）
-- ============================================================

CREATE TABLE IF NOT EXISTS dept (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门主键',
    dept_name   VARCHAR(50)  NOT NULL COMMENT '部门名称',
    dept_desc   VARCHAR(255) DEFAULT NULL COMMENT '部门描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dept_name (dept_name) COMMENT '部门名唯一，防止重复建部门'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

CREATE TABLE IF NOT EXISTS emp (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工主键',
    name        VARCHAR(50) NOT NULL COMMENT '员工姓名',
    age         INT         DEFAULT NULL COMMENT '年龄',
    gender      VARCHAR(4)  DEFAULT NULL COMMENT '性别：男/女',
    phone       VARCHAR(11) DEFAULT NULL COMMENT '手机号（11位）',
    job         VARCHAR(50) DEFAULT NULL COMMENT '岗位',
    dept_id     INT         DEFAULT NULL COMMENT '所属部门id（外键）',
    entry_date  DATE        DEFAULT NULL COMMENT '入职日期',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 联合索引：条件查询最高频组合（按姓名+部门筛选），覆盖查询与排序
    KEY idx_emp_name_dept (name, dept_id),
    -- 单列索引：按部门查员工列表、联查部门
    KEY idx_emp_dept (dept_id),
    -- 外键约束：员工必须挂在已存在的部门下，保证数据一致性
    CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES dept (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工表';

CREATE TABLE IF NOT EXISTS permission (
    id        INT PRIMARY KEY AUTO_INCREMENT COMMENT '权限主键',
    role      VARCHAR(20) NOT NULL COMMENT '角色：admin 管理员 / user 普通用户',
    perm_code VARCHAR(50) NOT NULL COMMENT '权限码，如 emp:view / dept:add',
    perm_name VARCHAR(50) NOT NULL COMMENT '权限名称，如 查看员工 / 新增部门',
    -- 同一角色的同一权限只存一行，重复灌数据不会插出两份
    UNIQUE KEY uk_role_perm (role, perm_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限表';

CREATE TABLE IF NOT EXISTS sys_user (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（课程设计阶段明文，生产应BCrypt加密）',
    name        VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：admin / user',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_name (username) COMMENT '账号唯一',
    KEY idx_sys_user_role (role) COMMENT '按角色查用户'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户（角色）表';
