-- ============================================================
-- 种子数据：部门 / 员工 / 用户 / 角色权限
-- 每次启动都会执行，INSERT IGNORE 保证不重复插入
-- （dept/emp 无唯一业务键，仅在全空库首次启动时生效，
   已有数据的库不会重复插入——emp 按 id 主键冲突自动忽略）
-- ============================================================

-- ---------- 部门 ----------
INSERT IGNORE INTO dept (id, dept_name, dept_desc) VALUES
(1, '研发部', '负责产品研发与技术攻关'),
(2, '市场部', '负责市场推广与品牌运营'),
(3, '人事部', '负责招聘与员工关系');

-- ---------- 员工（先插部门再插员工，满足外键依赖） ----------
INSERT IGNORE INTO emp (id, name, age, gender, phone, job, dept_id, entry_date) VALUES
(1, '张伟', 28, '男', '13800000001', 'Java开发工程师', 1, '2023-03-01'),
(2, '李娜', 26, '女', '13800000002', '前端开发工程师', 1, '2023-07-15'),
(3, '王强', 35, '男', '13800000003', '产品经理',     1, '2021-05-20'),
(4, '赵敏', 24, '女', '13800000004', '市场专员',     2, '2024-02-01'),
(5, '陈杰', 31, '男', '13800000005', 'HRBP',        3, '2022-11-11');

-- ---------- 系统用户（角色） ----------
-- admin：管理员，拥有全部权限；zhangsan：普通用户，只有查看权限
INSERT IGNORE INTO sys_user (id, username, password, name, role) VALUES
(1, 'admin',    'admin123', '系统管理员', 'admin'),
(2, 'zhangsan', '123456',   '张三',      'user');

-- ---------- 角色权限 ----------
-- admin：员工/部门/权限三大模块全量操作
INSERT IGNORE INTO permission (role, perm_code, perm_name) VALUES
('admin', 'emp:view',   '查看员工'),
('admin', 'emp:add',    '新增员工'),
('admin', 'emp:edit',   '修改员工'),
('admin', 'emp:delete', '删除员工'),
('admin', 'dept:view',  '查看部门'),
('admin', 'dept:add',   '新增部门'),
('admin', 'dept:edit',  '修改部门'),
('admin', 'dept:delete','删除部门'),
('admin', 'perm:view',  '查看权限');
-- user：只读
INSERT IGNORE INTO permission (role, perm_code, perm_name) VALUES
('user', 'emp:view',  '查看员工'),
('user', 'dept:view', '查看部门');
