package org.dromara.blog.service.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.blog.domain.SdBlogArticleVersion;

import java.util.Objects;

/**
 * 文章版本策略纯逻辑（契约 §3.4，ROADMAP 决策：四字段比对）
 * <p>
 * 内容变化判定：title、summary、coverUrl、content 任一变化即产生新版本；
 * summary/coverUrl 允许为空，null 与空串视为等价（避免前端传 "" 造成假变化）。
 *
 * @author Xu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VersionPolicy {

    /**
     * 判断待保存内容与当前生效版本是否存在实际变化
     *
     * @param current 当前生效版本（可为 null，表示尚无版本，此时视为有变化）
     * @param title   待保存标题
     * @param summary 待保存摘要
     * @param cover   待保存封面
     * @param content 待保存正文
     * @return true 需要创建新版本
     */
    public static boolean contentChanged(SdBlogArticleVersion current, String title, String summary, String cover, String content) {
        if (Objects.isNull(current)) {
            return true;
        }
        return !equalsText(current.getTitle(), title)
            || !equalsText(current.getSummary(), summary)
            || !equalsText(current.getCoverUrl(), cover)
            || !Objects.equals(current.getContent(), content);
    }

    /**
     * 计算下一版本号
     *
     * @param maxVersionNo 该文章当前最大版本号（无版本时传 null）
     * @return 下一版本号，从 1 开始
     */
    public static int nextVersionNo(Integer maxVersionNo) {
        return Objects.isNull(maxVersionNo) ? 1 : maxVersionNo + 1;
    }

    /**
     * 可空文本比较：null 与空串等价
     */
    private static boolean equalsText(String a, String b) {
        String x = (a == null) ? "" : a;
        String y = (b == null) ? "" : b;
        return x.equals(y);
    }
}
