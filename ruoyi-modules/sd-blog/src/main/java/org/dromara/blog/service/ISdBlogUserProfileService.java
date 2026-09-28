package org.dromara.blog.service;

import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.domain.vo.SdBlogUserProfileVo;
import org.dromara.blog.domain.bo.SdBlogUserProfileBo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 博客用户资料Service接口
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
public interface ISdBlogUserProfileService {

    /**
     * 查询博客用户资料
     *
     * @param userId 主键
     * @return 博客用户资料
     */
    SdBlogUserProfileVo queryById(Long userId);

    /**
     * 分页查询博客用户资料列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客用户资料分页列表
     */
    PageResult<SdBlogUserProfileVo> queryPageList(SdBlogUserProfileBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的博客用户资料列表
     *
     * @param bo 查询条件
     * @return 博客用户资料列表
     */
    List<SdBlogUserProfileVo> queryList(SdBlogUserProfileBo bo);


    /**
     * 新增博客用户资料
     *
     * @param bo 博客用户资料
     * @return 是否新增成功
     */
    Boolean insertByBo(SdBlogUserProfileBo bo);

    /**
     * 修改博客用户资料
     *
     * @param bo 博客用户资料
     * @return 是否修改成功
     */
    Boolean updateByBo(SdBlogUserProfileBo bo);



    /**
     * 校验并批量删除博客用户资料信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询用户博客资料（聚合 sys_user 实时昵称头像，契约 §8.1 / §9.1）
     *
     * @param userId 用户ID
     * @return 博客资料视图；用户不存在或已停用时返回 null
     */
    BlogUserProfileVo queryProfile(Long userId);

    /**
     * 保存用户博客资料：首次填写时创建记录，之后更新简介（契约 §8.2）
     *
     * @param userId 用户ID（必须由调用方从登录上下文取得）
     * @param bio    个人简介，允许为空字符串（清空）
     * @return 是否保存成功
     */
    Boolean saveProfile(Long userId, String bio);
}
