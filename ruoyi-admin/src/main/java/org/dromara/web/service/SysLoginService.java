package org.dromara.web.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.*;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.PostDTO;
import org.dromara.system.api.domain.RoleDTO;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.*;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.*;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.support.SocialAccountNamer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginService {

    /**
     * 最大重试次数。
     */
    @Value("${user.password.maxRetryCount}")
    private Integer maxRetryCount;

    /**
     * 锁定时间。
     */
    @Value("${user.password.lockTime}")
    private Integer lockTime;

    private final ISysPermissionService permissionService;
    private final ISysSocialService sysSocialService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final ISysUserService userService;
    private final SysUserMapper userMapper;


    /**
     * 绑定第三方用户
     *
     * @param authUserData 授权响应实体
     */
    @Lock4j
    public void socialRegister(AuthUser authUserData) {
        String authId = authUserData.getSource() + authUserData.getUuid();
        // 第三方用户信息
        SysSocialBo bo = BeanUtil.toBean(authUserData, SysSocialBo.class);
        BeanUtil.copyProperties(authUserData.getToken(), bo);
        Long userId = LoginHelper.getUserId();
        bo.setUserId(userId);
        bo.setAuthId(authId);
        bo.setOpenId(authUserData.getUuid());
        bo.setUserName(authUserData.getUsername());
        bo.setNickName(authUserData.getNickname());
        List<SysSocialVo> checkList = sysSocialService.selectByAuthId(authId);
        if (CollUtil.isNotEmpty(checkList)) {
            throw new ServiceException("此三方账号已经被绑定!");
        }
        // 查询是否已经绑定用户
        SysSocialBo params = new SysSocialBo();
        params.setUserId(userId);
        params.setSource(bo.getSource());
        List<SysSocialVo> list = sysSocialService.queryList(params);
        if (CollUtil.isEmpty(list)) {
            // 没有绑定用户, 新增用户信息
            sysSocialService.insertByBo(bo);
        } else {
            // 更新用户信息
            bo.setId(list.getFirst().getId());
            sysSocialService.updateByBo(bo);
            // 如果要绑定的平台账号已经被绑定过了 是否抛异常自行决断
            // throw new ServiceException("此平台账号已经被绑定!");
        }
    }

    /**
     * 社交登录自动注册：首次 GitHub / Gitee 登录时创建系统账号并写入绑定关系（ROADMAP S14 / D34）。
     * <p>
     * 安全边界（AGENTS.md §9 红线，账号创建逻辑人工复核点）：
     * <ol>
     *     <li>密码为随机 UUID 的 BCrypt 哈希，明文永不下发、任何途径不可获知，自动创建的账号无法账密登录；</li>
     *     <li>不分配任何角色、不设部门，与框架 {@code /auth/register} 口径一致（所有注册用户天然可发文，不授予管理权限）；</li>
     *     <li>账号 / 昵称全部由授权响应在服务端派生，不信任前端传入的任何身份字段；</li>
     *     <li>{@code sys_user.user_name} 无唯一约束（仅普通索引），冲突在应用层消歧；并发首登按 authId 加分布式锁
     * （Lock4j advisor 为 HIGHEST_PRECEDENCE，锁包裹事务），锁内二次检查绑定关系后只有一个线程执行建号。</li>
     * </ol>
     *
     * @param authUserData 第三方授权响应
     * @return 新建用户的视图对象（并发下已被他人建好时返回既有绑定用户）
     */
    @Lock4j(keys = {"#authUserData.source + #authUserData.uuid"})
    @Transactional(rollbackFor = Exception.class)
    public SysUserVo socialAutoRegister(AuthUser authUserData) {
        String authId = authUserData.getSource() + authUserData.getUuid();
        // 锁内双重检查：并发首登下只有一个线程执行建号，其余线程直接复用已建绑定
        List<SysSocialVo> boundList = sysSocialService.selectByAuthId(authId);
        if (CollUtil.isNotEmpty(boundList)) {
            return userMapper.selectVoById(boundList.getFirst().getUserId());
        }
        // 1. 建号：随机不可用密码 + 应用层唯一账号 + 昵称消歧，不分配角色（registerUser 与 /auth/register 同口径）
        SysUserBo bo = new SysUserBo();
        String userName = resolveUniqueUserName(authUserData);
        bo.setUserName(userName);
        bo.setNickName(resolveUniqueNickName(authUserData.getNickname(), userName));
        bo.setPassword(BCrypt.hashpw(IdUtil.simpleUUID()));
        bo.setUserType(UserType.SYS_USER.getUserType());
        if (!userService.registerUser(bo)) {
            throw new ServiceException("社交登录自动创建账号失败");
        }
        // registerUser 不回填主键，按锁内唯一的账号名回查（并发已被锁排除，不会误取他人账号）
        SysUserVo user = userMapper.lambda().eq(SysUser::getUserName, userName).voOne();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException("社交登录自动创建账号失败");
        }
        // 2. 绑定：与 socialRegister 保持同一构建口径（AuthToken 属性拷贝 + authId/openId/展示名回填）
        SysSocialBo socialBo = BeanUtil.toBean(authUserData, SysSocialBo.class);
        BeanUtil.copyProperties(authUserData.getToken(), socialBo);
        socialBo.setUserId(user.getUserId());
        socialBo.setAuthId(authId);
        socialBo.setOpenId(authUserData.getUuid());
        socialBo.setUserName(authUserData.getUsername());
        socialBo.setNickName(authUserData.getNickname());
        sysSocialService.insertByBo(socialBo);
        log.info("社交登录自动注册成功, source={}, uuid={}, userId={}", authUserData.getSource(), authUserData.getUuid(), user.getUserId());
        return user;
    }

    /**
     * 注册即登录：为刚注册成功的账号建立会话并生成访问令牌（ROADMAP S22 / D35）。
     * <p>
     * 与 {@link #socialAutoRegister} 同一模式（建号后直接登录），区别在于账号凭证由用户本人在注册
     * 请求内提交、图形验证码已在注册时校验并消费，故本方法不做二次凭证校验。客户端与授权类型校验
     * 由 {@code AuthController#register} 按 {@code AuthController#login} 同口径完成，本方法只构建会话。
     *
     * @param username 注册用户名
     * @param client   客户端配置
     * @return 登录令牌信息
     */
    public LoginVo registerAutoLogin(String username, SysClientVo client) {
        SysUserVo user = userMapper.lambda()
            .eq(SysUser::getUserName, username)
            .voOne();
        if (ObjectUtil.isNull(user)) {
            throw new UserException("user.not.exists", username);
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            throw new UserException("user.blocked", username);
        }
        LoginUser loginUser = buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = IAuthStrategy.buildLoginParameter(client);
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 解析唯一系统账号：优先平台用户名（清洗为合法字符），冲突按序追加数字后缀，最终回退 source_uuid。
     *
     * @param authUserData 第三方授权响应
     * @return 唯一系统账号
     */
    private String resolveUniqueUserName(AuthUser authUserData) {
        String base = SocialAccountNamer.sanitizeUserName(authUserData.getUsername());
        if (StringUtils.isBlank(base)) {
            base = SocialAccountNamer.fallbackUserName(authUserData.getSource(), authUserData.getUuid());
        }
        if (!userNameExists(base)) {
            return base;
        }
        for (int i = 1; i <= SocialAccountNamer.INDEX_SUFFIX_RETRY; i++) {
            String candidate = SocialAccountNamer.withIndexSuffix(base, i);
            if (!userNameExists(candidate)) {
                return candidate;
            }
        }
        // 数字后缀全部冲突时回退确定性来源账号（平台+uuid，天然只与同平台同用户碰撞）
        String fallback = SocialAccountNamer.fallbackUserName(authUserData.getSource(), authUserData.getUuid());
        if (!userNameExists(fallback)) {
            return fallback;
        }
        throw new ServiceException("系统账号创建冲突，请稍后重试");
    }

    /**
     * 解析展示昵称：空值回退账号名、超长截断，与现有昵称冲突时追加随机后缀消歧。
     * <p>
     * 昵称无唯一约束、仅作展示，多次随机仍冲突时接受现值（不影响登录与数据正确性）。
     *
     * @param platformNickName 平台返回的昵称
     * @param userName         已解析的系统账号（兜底昵称）
     * @return 规范化昵称
     */
    private String resolveUniqueNickName(String platformNickName, String userName) {
        String nickName = SocialAccountNamer.normalizeNickName(platformNickName, userName);
        for (int i = 0; i < 3 && nickNameExists(nickName); i++) {
            nickName = SocialAccountNamer.withSuffix(nickName, SocialAccountNamer.randomSuffix(SocialAccountNamer.NICK_NAME_SUFFIX_LENGTH));
        }
        return nickName;
    }

    /**
     * 系统账号是否已存在（user_name 无唯一约束，存在性必须应用层校验）。
     */
    private boolean userNameExists(String userName) {
        return userMapper.lambda().eq(SysUser::getUserName, userName).exists();
    }

    /**
     * 展示昵称是否已被占用（无唯一约束，仅用于展示消歧）。
     */
    private boolean nickNameExists(String nickName) {
        return userMapper.lambda().eq(SysUser::getNickName, nickName).exists();
    }

    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            recordLoginInfo(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        } finally {
            try {
                StpUtil.logout();
            } catch (NotLoginException ignored) {
            }
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     */
    public void recordLoginInfo(String username, String status, String message) {
        LoginInfoEvent loginInfoEvent = new LoginInfoEvent();
        loginInfoEvent.setUsername(username);
        loginInfoEvent.setStatus(status);
        loginInfoEvent.setMessage(message);
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            loginInfoEvent.setIp(ServletUtils.getClientIP(request));
            loginInfoEvent.setUserAgent(request.getHeader("User-Agent"));
            loginInfoEvent.setClientId(request.getHeader(LoginHelper.CLIENT_KEY));
        }
        SpringUtils.context().publishEvent(loginInfoEvent);
    }

    /**
     * 根据用户视图对象组装登录态上下文。
     *
     * @param user 用户基础信息
     * @return 包含部门、角色、岗位与权限数据的登录用户
     */
    public LoginUser buildLoginUser(SysUserVo user) {
        LoginUser loginUser = new LoginUser();
        Long userId = user.getUserId();
        loginUser.setUserId(userId);
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(user.getUserType());
        if (ObjectUtil.isNotNull(user.getDeptId())) {
            Opt<SysDeptVo> deptOpt = Opt.of(user.getDeptId()).map(deptService::selectDeptById);
            loginUser.setDeptName(deptOpt.map(SysDeptVo::getDeptName).orElse(StringUtils.EMPTY));
            loginUser.setDeptCategory(deptOpt.map(SysDeptVo::getDeptCategory).orElse(StringUtils.EMPTY));
        }
        ThreadUtils.virtualInvokeAll(() -> {
            loginUser.setMenuPermission(permissionService.getMenuPermission(userId));
        }, () -> {
            loginUser.setRolePermission(permissionService.getRolePermission(userId));
        }, () -> {
            List<SysRoleVo> roles = roleService.selectRolesByUserId(userId);
            List<RoleDTO> roleDtos = BeanUtil.copyToList(roles, RoleDTO.class);
            loginUser.setRoles(roleDtos);
            loginUser.setDataScopeRoleMap(permissionService.getDataScopeRoleMap(roleDtos));
        }, () -> {
            List<SysPostVo> posts = postService.selectPostsByUserId(userId);
            loginUser.setPosts(BeanUtil.copyToList(posts, PostDTO.class));
        });
        return loginUser;
    }

    /**
     * 更新用户最近一次登录IP与登录时间。
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    public void updateLastLoginInfo(Long userId, String ip) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ip);
        sysUser.setLoginDate(LocalDateTime.now());
        sysUser.setUpdateBy(userId);
        DataPermissionHelper.ignore(() -> userMapper.updateById(sysUser));
    }

    /**
     * 执行登录失败次数校验，并在成功后清空失败计数。
     *
     * @param loginType 登录类型
     * @param username  登录标识
     * @param supplier  返回 {@code true} 表示本次认证失败
     */
    public void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = CacheNames.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;

        // 获取用户登录错误次数，默认为0 (可自定义限制策略 例如: key + username + ip)
        int errorNumber = ObjectUtil.defaultIfNull(RedisUtils.getCacheObject(errorKey), 0);
        // 锁定时间内登录 则踢出
        if (errorNumber >= maxRetryCount) {
            recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // 错误次数递增
            errorNumber++;
            RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
            // 达到规定错误次数 则锁定登录
            if (errorNumber >= maxRetryCount) {
                recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数
                recordLoginInfo(username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // 登录成功 清空错误次数
        RedisUtils.deleteObject(errorKey);
    }

}
