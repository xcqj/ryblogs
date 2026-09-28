-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157825, '博客标签', 2103854919354421249, 1, 'blogTag', 'blog/blogTag/index', 'N', 'Y', 'C', '0', '0', 'blog:blogTag:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客标签菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157826, '博客标签查询', 2104273198830157825, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTag:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157827, '博客标签新增', 2104273198830157825, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTag:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157828, '博客标签修改', 2104273198830157825, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTag:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157829, '博客标签删除', 2104273198830157825, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTag:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198830157830, '博客标签导出', 2104273198830157825, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogTag:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
