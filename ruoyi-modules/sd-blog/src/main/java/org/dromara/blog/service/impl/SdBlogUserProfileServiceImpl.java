package org.dromara.blog.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.dromara.blog.domain.bo.SdBlogUserProfileBo;
import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.domain.vo.SdBlogUserProfileVo;
import org.dromara.blog.domain.SdBlogUserProfile;
import org.dromara.blog.mapper.SdBlogUserProfileMapper;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 博客用户资料Service业务层处理
 *
 * @author Xu
 * @date 2026-09-28 02:13:20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SdBlogUserProfileServiceImpl implements ISdBlogUserProfileService {

    private final SdBlogUserProfileMapper sdBlogUserProfileMapper;
    private final UserService userService;

    /**
     * 查询博客用户资料
     *
     * @param userId 主键
     * @return 博客用户资料
     */
    @Override
    public SdBlogUserProfileVo queryById(Long userId) {
        return sdBlogUserProfileMapper.selectVoById(userId);
    }

    /**
     * 分页查询博客用户资料列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 博客用户资料分页列表
     */
    @Override
    public PageResult<SdBlogUserProfileVo> queryPageList(SdBlogUserProfileBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SdBlogUserProfile> lqw = buildQueryWrapper(bo);
        Page<SdBlogUserProfileVo> result = sdBlogUserProfileMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的博客用户资料列表
     *
     * @param bo 查询条件
     * @return 博客用户资料列表
     */
    @Override
    public List<SdBlogUserProfileVo> queryList(SdBlogUserProfileBo bo) {
        LambdaQueryWrapper<SdBlogUserProfile> lqw = buildQueryWrapper(bo);
        return sdBlogUserProfileMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SdBlogUserProfile> buildQueryWrapper(SdBlogUserProfileBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SdBlogUserProfile.class)
            .eqIfPresent(SdBlogUserProfile::getUserId, bo.getUserId())
            .eqIfText(SdBlogUserProfile::getBio, bo.getBio())
            .eqIfPresent(SdBlogUserProfile::getCreateBy, bo.getCreateBy())
            .betweenParams(SdBlogUserProfile::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .orderByAsc(SdBlogUserProfile::getUserId)
            .build();
    }

    /**
     * 新增博客用户资料
     *
     * @param bo 博客用户资料
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SdBlogUserProfileBo bo) {
        SdBlogUserProfile add = MapstructUtils.convert(bo, SdBlogUserProfile.class);
        validEntityBeforeSave(add);
        boolean flag = sdBlogUserProfileMapper.insert(add) > 0;
        if (flag) {
            bo.setUserId(add.getUserId());
        }
        return flag;
    }

    /**
     * 修改博客用户资料
     *
     * @param bo 博客用户资料
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SdBlogUserProfileBo bo) {
        SdBlogUserProfile update = MapstructUtils.convert(bo, SdBlogUserProfile.class);
        validEntityBeforeSave(update);
        return sdBlogUserProfileMapper.updateById(update) > 0;
    }



    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SdBlogUserProfile entity) {
        // 可在此扩展通用业务校验
    }


    /**
     * 校验并批量删除博客用户资料信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可在此扩展删除前业务校验
        }
        return sdBlogUserProfileMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查询用户博客资料（聚合 sys_user 实时昵称头像）
     *
     * @param userId 用户ID
     * @return 博客资料视图；用户不存在或已停用时返回 null
     */
    @Override
    public BlogUserProfileVo queryProfile(Long userId) {
        UserDTO user = userService.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            return null;
        }
        BlogUserProfileVo vo = new BlogUserProfileVo();
        vo.setUserId(user.getUserId());
        vo.setNickName(user.getNickName());
        vo.setAvatar(user.getAvatar());
        SdBlogUserProfile profile = sdBlogUserProfileMapper.selectById(userId);
        if (ObjectUtil.isNotNull(profile)) {
            vo.setBio(profile.getBio());
        }
        return vo;
    }

    /**
     * 保存用户博客资料：首次填写时创建记录，之后更新简介
     * <p>
     * 主键即 userId 无需发号；insertOrUpdate 按主键存在性自动选择插入或更新。
     * 并发首次保存时后到者会因主键冲突失败，MVP 接受该行为（用户重试即恢复）。
     *
     * @param userId 用户ID（必须由调用方从登录上下文取得）
     * @param bio    个人简介，允许为空字符串（清空）
     * @return 是否保存成功
     */
    @Override
    public Boolean saveProfile(Long userId, String bio) {
        SdBlogUserProfile profile = new SdBlogUserProfile();
        profile.setUserId(userId);
        profile.setBio(bio);
        return sdBlogUserProfileMapper.insertOrUpdate(profile);
    }

}
