-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150465, '博客评论', 2103854919354421249, 1, 'blogComment', 'blog/blogComment/index', 'N', 'Y', 'C', '0', '0', 'blog:blogComment:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客评论菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150466, '博客评论查询', 2104275755145150465, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogComment:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150467, '博客评论新增', 2104275755145150465, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogComment:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150468, '博客评论修改', 2104275755145150465, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogComment:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150469, '博客评论删除', 2104275755145150465, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogComment:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104275755145150470, '博客评论导出', 2104275755145150465, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogComment:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
