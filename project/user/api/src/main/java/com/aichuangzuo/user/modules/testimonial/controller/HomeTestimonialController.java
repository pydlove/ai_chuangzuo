package com.aichuangzuo.user.modules.testimonial.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.user.modules.testimonial.vo.TestimonialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "首页用户评价")
@RestController
@RequestMapping("/api/v1/user/home/testimonials")
@RequiredArgsConstructor
@Slf4j
public class HomeTestimonialController {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final TestimonialService service;

    @Operation(summary = "首页评价列表")
    @GetMapping
    public Result<List<TestimonialVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        log.info("查询首页评价列表, page={}, size={}", safePage, safeSize);
        return Result.success(service.listEnabled(safePage, safeSize));
    }
}
