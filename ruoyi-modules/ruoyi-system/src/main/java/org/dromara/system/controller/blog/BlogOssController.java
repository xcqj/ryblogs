package org.dromara.system.controller.blog;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 博客域文件上传。
 * <p>
 * 契约 §2.4（D42）：博客作者上传封面与正文插图，**只要求登录**，
 * 不要求后台「文件管理」的按钮权限 {@code system:oss:upload}。
 * <p>
 * 为什么单独开接口而不复用 {@code /resource/oss/upload}：
 * ① 注册流程（{@code SysRegisterService}）只建用户、不写 {@code sys_user_role}，博客用户默认零权限，
 *    复用后台接口会让每个新注册用户都无法传图；
 * ② 给普通读者授予后台菜单权限在语义上不成立；
 * ③ 本接口只做「登录校验 + 转调 {@code ISysOssService}」，不重复实现对象存储
 *    （符合 AGENTS.md「复用 RuoYi OSS 能力」）。
 * <p>
 * 鉴权：路径落在 Sa-Token 拦截范围内（{@code security.excludes} 只放行 {@code /blog/public/**}），
 * 与 {@code /blog/article/**}、{@code /blog/user/**} 一致——不加按钮权限注解，登录即通过。
 *
 * @author Xu
 * @date 2026-10-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/oss")
public class BlogOssController extends BaseController {

    private final ISysOssService ossService;

    /**
     * 上传图片（封面 / 正文插图）
     *
     * @param file 文件
     * @return OSS 访问地址、原始文件名与 ossId
     */
    @Log(title = "博客图片上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<BlogOssUploadVo> upload(@RequestPart("file") MultipartFile file) {
        SysOssVo oss = ossService.upload(file, null);
        return R.ok(new BlogOssUploadVo(oss.getUrl(), oss.getOriginalName(), oss.getOssId().toString()));
    }

    /**
     * 上传响应对象。
     * <p>
     * 字段与 RuoYi 原生 {@code SysOssController.SysOssUploadVo} 保持一致（url / fileName / ossId），
     * 前端 blog-web 的 {@code OssUploadResult} 无需区分来源。
     *
     * @param url      文件访问地址
     * @param fileName 原始文件名
     * @param ossId    OSS 对象 ID
     */
    public record BlogOssUploadVo(String url, String fileName, String ossId) {
    }

}
