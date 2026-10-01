package org.dromara.blog.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.dromara.blog.domain.vo.BlogUserProfileVo;
import org.dromara.blog.domain.SdBlogUserProfile;
import org.dromara.blog.mapper.SdBlogUserProfileMapper;
import org.dromara.blog.service.ISdBlogUserProfileService;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;

import java.util.ArrayList;
import java.util.Map;

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
