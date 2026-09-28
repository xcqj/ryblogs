-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940098, '博客文章', 2103854919354421249, 1, 'blogArticle', 'blog/blogArticle/index', 'N', 'Y', 'C', '0', '0', 'blog:blogArticle:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客文章菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940099, '博客文章查询', 2104273198695940098, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticle:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940100, '博客文章新增', 2104273198695940098, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticle:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940101, '博客文章修改', 2104273198695940098, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticle:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940102, '博客文章删除', 2104273198695940098, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticle:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198695940103, '博客文章导出', 2104273198695940098, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticle:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
