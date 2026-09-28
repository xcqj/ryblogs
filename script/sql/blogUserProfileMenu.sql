-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295170, '博客用户资料', 2103854919354421249, 1, 'blogUserProfile', 'blog/blogUserProfile/index', 'N', 'Y', 'C', '0', '0', 'blog:blogUserProfile:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客用户资料菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295171, '博客用户资料查询', 2104273198876295170, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogUserProfile:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295172, '博客用户资料新增', 2104273198876295170, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogUserProfile:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295173, '博客用户资料修改', 2104273198876295170, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogUserProfile:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295174, '博客用户资料删除', 2104273198876295170, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogUserProfile:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198876295175, '博客用户资料导出', 2104273198876295170, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogUserProfile:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
