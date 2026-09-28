-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323649, '博客标签历史别名', 2103854919354421249, 1, 'blogTagAlias', 'blog/blogTagAlias/index', 'N', 'Y', 'C', '0', '0', 'blog:blogTagAlias:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客标签历史别名菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323650, '博客标签历史别名查询', 2104273198855323649, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTagAlias:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323651, '博客标签历史别名新增', 2104273198855323649, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTagAlias:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323652, '博客标签历史别名修改', 2104273198855323649, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTagAlias:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323653, '博客标签历史别名删除', 2104273198855323649, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTagAlias:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198855323654, '博客标签历史别名导出', 2104273198855323649, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTagAlias:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
