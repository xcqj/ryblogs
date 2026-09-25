package org.dromara.blog.controller;


import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/blog")
@RestController
public class BlogHelloController {

    /**
     *  测试
     * @param title 测试
     * @return
     */
    @GetMapping("/hello")
    public Object select(String title) {

        return R.ok();
    }
}
