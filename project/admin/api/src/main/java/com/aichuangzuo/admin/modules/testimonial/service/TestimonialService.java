package com.aichuangzuo.admin.modules.testimonial.service;

import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialCreateRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialUpdateRequest;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialImportResultVO;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestimonialService {

    List<TestimonialVO> list();

    Long create(TestimonialCreateRequest req);

    void update(Long id, TestimonialUpdateRequest req);

    void delete(Long id);

    void updateStatus(Long id, TestimonialStatusRequest req);

    TestimonialImportResultVO importFromExcel(MultipartFile file);
}
