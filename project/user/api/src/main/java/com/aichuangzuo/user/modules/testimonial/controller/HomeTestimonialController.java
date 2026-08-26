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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "首页用户评价")
@RestController
@RequestMapping("/api/v1/user/home/testimonials")
@RequiredArgsConstructor
@Slf4j
public class HomeTestimonialController {

    private final TestimonialService service;

    @Operation(summary = "首页评价列表")
    @GetMapping
    public Result<List<TestimonialVO>> list() {
        log.info("查询首页评价列表");
        return Result.success(service.listEnabled());
    }
}
