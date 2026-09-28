-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020482, '博客文章版本历史', 2103854919354421249, 1, 'blogArticleVersion', 'blog/blogArticleVersion/index', 'N', 'Y', 'C', '0', '0', 'blog:blogArticleVersion:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客文章版本历史菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020483, '博客文章版本历史查询', 2104273198784020482, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleVersion:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020484, '博客文章版本历史新增', 2104273198784020482, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleVersion:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020485, '博客文章版本历史修改', 2104273198784020482, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleVersion:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020486, '博客文章版本历史删除', 2104273198784020482, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleVersion:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198784020487, '博客文章版本历史导出', 2104273198784020482, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleVersion:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
