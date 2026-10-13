package org.dromara.web.service.support;

import cn.hutool.core.util.StrUtil;

import java.security.SecureRandom;

/**
 * 社交登录自动建号的账号信息派生纯逻辑（无 Spring / DB 依赖，可单测）。
 * <p>
 * 背景（ROADMAP S14 / D34）：首次 GitHub / Gitee 登录时按授权响应自动创建系统账号。
 * 库表约束实证（{@code script/sql/ry_vue.sql}）：{@code sys_user.user_name} 为 varchar(30)
 * 且**只有普通索引、无唯一约束**，{@code sys_social.auth_id} 同样无唯一约束，
 * 因此账号 / 昵称的清洗、截断、冲突消歧候选全部在此以纯函数给出，
 * 冲突的存在性检查由 Service 持锁执行（见 {@code SysLoginService#socialAutoRegister}）。
 *
 * @author Xu
 * @date 2026-10-13
 */
public final class SocialAccountNamer {

    /**
     * 系统账号最大长度（sys_user.user_name varchar(30)）
     */
    public static final int USER_NAME_MAX = 30;

    /**
     * 昵称最大长度（sys_user.nick_name varchar(30)）
     */
    public static final int NICK_NAME_MAX = 30;

    /**
     * 昵称冲突时随机后缀长度
     */
    public static final int NICK_NAME_SUFFIX_LENGTH = 4;

    /**
     * 账号冲突时数字后缀的最大尝试次数（base_1 ~ base_9）
     */
    public static final int INDEX_SUFFIX_RETRY = 9;

    private static final String RANDOM_SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private SocialAccountNamer() {
    }

    /**
     * 把平台用户名清洗为合法系统账号：仅保留字母 / 数字 / _ / -，统一小写。
     * <p>
     * 平台用户名可能含点号、中文等非法字符（Gitee / 各平台规范不一），
     * 直接入库会得到难以辨识的账号；清洗后为空（如纯中文用户名）时返回 {@code null}，
     * 由调用方回退到 {@link #fallbackUserName(String, String)}。
     *
     * @param raw 平台返回的用户名（可为空）
     * @return 清洗后的账号候选；无合法字符时返回 {@code null}
     */
    public static String sanitizeUserName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        raw.chars().forEach(ch -> {
            if (isLegalUserNameChar(ch)) {
                sb.appendCodePoint(ch);
            }
        });
        String cleaned = sb.toString().toLowerCase();
        return StrUtil.isEmpty(cleaned) ? null : cleaned;
    }

    /**
     * 兜底账号：{@code source + "_" + uuid}，截断至账号最大长度。
     * <p>
     * 平台账号在单一平台内由 uuid 唯一决定，因此该形态天然只与「同一平台同一用户」碰撞，
     * 不会误占他人账号；仅在极少数重复场景下由存在性检查兜底。
     *
     * @param source 平台标识（github / gitee）
     * @param uuid   平台内唯一用户ID
     * @return 兜底账号
     */
    public static String fallbackUserName(String source, String uuid) {
        String safeSource = StrUtil.isBlank(source) ? "social" : source.trim().toLowerCase();
        return truncate(safeSource + "_" + StrUtil.blankToDefault(uuid, ""), USER_NAME_MAX);
    }

    /**
     * 昵称规范化：空值回退为账号名（昵称不可为空，not null 列），超长截断。
     * <p>
     * 与账号不同，昵称无唯一约束、仅作展示，冲突消歧（随机后缀）由调用方追加，
     * 本方法只保证「非空 + 不超长」两个入库硬条件。
     *
     * @param nickName          平台返回的昵称（可为空）
     * @param fallbackUserName 兜底账号名（已保证非空）
     * @return 规范化昵称
     */
    public static String normalizeNickName(String nickName, String fallbackUserName) {
        String base = StrUtil.isBlank(nickName) ? fallbackUserName : nickName.trim();
        if (StrUtil.isBlank(base)) {
            base = StrUtil.blankToDefault(fallbackUserName, "blogger");
        }
        return truncate(base, NICK_NAME_MAX);
    }

    /**
     * 在基名后追加分隔后缀，并保证总长不超账号上限（基名按需截断）。
     * <p>
     * 用于昵称冲突消歧（随机后缀）与账号冲突消歧（数字后缀）两类场景。
     *
     * @param base   基名（不可为空）
     * @param suffix 后缀（不可含下划线以外的连接符，调用方负责）
     * @return base + "_" + suffix，总长 ≤ 上限
     */
    public static String withSuffix(String base, String suffix) {
        String safeSuffix = StrUtil.blankToDefault(suffix, "");
        String connector = safeSuffix.isEmpty() ? "" : "_";
        int keep = USER_NAME_MAX - connector.length() - safeSuffix.length();
        if (keep < 1) {
            keep = 1;
        }
        return truncate(base, keep) + connector + safeSuffix;
    }

    /**
     * 账号冲突的数字后缀候选：{@code base_1} ~ {@code base_9}（截断保长）。
     *
     * @param base 账号基名
     * @param index 尝试序号（从 1 开始）
     * @return 候选账号
     */
    public static String withIndexSuffix(String base, int index) {
        return withSuffix(base, String.valueOf(index));
    }

    /**
     * 生成指定长度的小写字母数字随机后缀（SecureRandom）。
     *
     * @param length 长度（≥1）
     * @return 随机后缀
     */
    public static String randomSuffix(int length) {
        int len = Math.max(length, 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(RANDOM_SUFFIX_CHARS.charAt(RANDOM.nextInt(RANDOM_SUFFIX_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 按字符截断（MySQL utf8mb4 的 varchar(N) 按字符计，与 Java char 计数一致）。
     *
     * @param value 原值
     * @param max   最大字符数
     * @return 截断结果；空值返回空串
     */
    public static String truncate(String value, int max) {
        if (value == null) {
            return StrUtil.EMPTY;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isLegalUserNameChar(int codePoint) {
        return (codePoint >= 'a' && codePoint <= 'z')
            || (codePoint >= 'A' && codePoint <= 'Z')
            || (codePoint >= '0' && codePoint <= '9')
            || codePoint == '_' || codePoint == '-';
    }

}
