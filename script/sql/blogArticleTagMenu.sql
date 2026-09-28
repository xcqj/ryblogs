-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266690, '博客文章标签关系', 2103854919354421249, 1, 'blogArticleTag', 'blog/blogArticleTag/index', 'N', 'Y', 'C', '0', '0', 'blog:blogArticleTag:list', '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '博客文章标签关系菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266691, '博客文章标签关系查询', 2104273198897266690, 1,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleTag:query',        '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266692, '博客文章标签关系新增', 2104273198897266690, 2,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleTag:add',          '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266693, '博客文章标签关系修改', 2104273198897266690, 3,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleTag:edit',         '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266694, '博客文章标签关系删除', 2104273198897266690, 4,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleTag:remove',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(2104273198897266695, '博客文章标签关系导出', 2104273198897266690, 5,  '#', '', 'N', 'Y', 'F', '0', '0', 'blog:blogArticleTag:export',       '#', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
