INSERT INTO sys_permission (id, permission_code, permission_name, resource_type, path, parent_id, order_num, created_by, created_at, updated_by, updated_at)
VALUES
    (1,  'project',           '项目管理',   'MENU', '/projects',  0,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (2,  'project:create',    '创建项目',   'BTN',  NULL,         1,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (3,  'project:edit',      '编辑项目',   'BTN',  NULL,         1,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (4,  'project:delete',    '删除项目',   'BTN',  NULL,         1,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (5,  'document',          '文档管理',   'MENU', '/documents', 0,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (6,  'document:create',   '创建文档',   'BTN',  NULL,         5,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (7,  'document:edit',     '编辑文档',   'BTN',  NULL,         5,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (8,  'document:delete',   '删除文档',   'BTN',  NULL,         5,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (9,  'catalog',           '文档目录',   'MENU', '/catalogs',  0,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (10, 'catalog:create',    '创建目录',   'BTN',  NULL,         9,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (11, 'catalog:edit',      '编辑目录',   'BTN',  NULL,         9,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (12, 'catalog:delete',    '删除目录',   'BTN',  NULL,         9,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (13, 'meeting',           '评审会议',   'MENU', '/meetings',  0,  4, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (14, 'meeting:create',    '创建会议',   'BTN',  NULL,         13, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (15, 'meeting:edit',      '编辑会议',   'BTN',  NULL,         13, 2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (16, 'meeting:delete',    '删除会议',   'BTN',  NULL,         13, 3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (17, 'system:user',       '用户管理',   'MENU', '/users',     0,  5, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (18, 'system:user:crud',  '用户CRUD',   'BTN',  NULL,         17, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (19, 'system:role',       '角色管理',   'MENU', '/roles',     0,  6, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (20, 'system:role:crud',  '角色CRUD',   'BTN',  NULL,         19, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (21, 'system:dict',       '字典配置',   'MENU', '/dicts',     0,  7, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (22, 'system:dict:crud',  '字典CRUD',   'BTN',  NULL,         21, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (23, 'system:perm',       '权限管理',   'MENU', '/permissions',0, 8, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (24, 'system:perm:crud',  '权限CRUD',   'BTN',  NULL,         23, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (25, 'template',          '模版管理',   'MENU', '/templates',  0,  9, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (26, 'template:crud',     '模版CRUD',   'BTN',  NULL,         25, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (27, 'standard',          '标准库',     'MENU', '/standards',  0, 10, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (28, 'standard:crud',     '标准CRUD',   'BTN',  NULL,         27, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT 1, id, 1, CURRENT_TIMESTAMP FROM sys_permission
ON CONFLICT DO NOTHING;
