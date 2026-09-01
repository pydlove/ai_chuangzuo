package com.aichuangzuo.user.modules.article.controller;

import com.aichuangzuo.user.modules.article.service.ArticleService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 作品公开导出接口（免登录）。
 *
 * <p>通过临时 token 校验后返回 Word 文件流，用于微信等场景跳转到系统浏览器下载。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/articles")
@RequiredArgsConstructor
public class ArticleExportController {

    private final ArticleService articleService;

    @GetMapping("/export/{token}")
    public void exportWord(@PathVariable("token") String token, HttpServletResponse response) throws IOException {
        String bizNo = articleService.parseExportToken(token);
        log.info("公开导出作品, bizNo={}", bizNo);
        byte[] bytes = articleService.exportAsWord(bizNo);

        String filename = URLEncoder.encode(bizNo + ".doc", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/msword");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
