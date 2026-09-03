package com.aichuangzuo.admin.infrastructure.storage;

import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * 本地磁盘文件存储（管理端上传）。
 */
@Slf4j
@Component
public class LocalFileStorage {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final List<String> USER_AVATAR_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    private final Path basePath;

    public LocalFileStorage(@Value("${storage.local.base-path:data/uploads}") String basePath) {
        this.basePath = Paths.get(basePath);
    }

    /**
     * 存储用户头像，返回 API 前缀访问路径。
     *
     * @param file 头像文件
     * @return /api/v1/admin/uploads/user/avatar/{filename}.{ext}
     */
    public String storeUserAvatar(MultipartFile file) {
        validateUserAvatar(file);

        Path dir = basePath.resolve("user").resolve("avatar");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException(AdminUserErrorCode.AVATAR_UPLOAD_FAILED.getCode(), "创建头像目录失败");
        }

        String ext = extension(file.getOriginalFilename());
        String filename = System.nanoTime() + "." + ext;
        Path target = dir.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(AdminUserErrorCode.AVATAR_UPLOAD_FAILED.getCode(), "头像保存失败");
        }

        return "/api/v1/admin/uploads/user/avatar/" + filename;
    }

    /**
     * 存储首页评价头像，返回 API 前缀访问路径。
     * <p>使用 /api/v1/admin/uploads 前缀，避免线上 /uploads 未代理导致头像裂图。
     *
     * @param file 头像文件
     * @return /api/v1/admin/uploads/testimonial/avatar/{filename}.jpg
     */
    public String storeTestimonialAvatar(MultipartFile file) {
        validateAvatar(file);

        Path dir = basePath.resolve("testimonial").resolve("avatar");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException(TestimonialErrorCode.AVATAR_UPLOAD_FAILED.getCode(), "创建头像目录失败");
        }

        String filename = System.nanoTime() + ".jpg";
        Path target = dir.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(TestimonialErrorCode.AVATAR_UPLOAD_FAILED.getCode(), "头像保存失败");
        }

        return "/api/v1/admin/uploads/testimonial/avatar/" + filename;
    }

    private void validateUserAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AdminUserErrorCode.AVATAR_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(AdminUserErrorCode.AVATAR_FILE_INVALID);
        }
        String ext = extension(file.getOriginalFilename());
        if (!USER_AVATAR_EXTENSIONS.contains(ext)) {
            throw new BusinessException(AdminUserErrorCode.AVATAR_FILE_INVALID);
        }
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(TestimonialErrorCode.AVATAR_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(TestimonialErrorCode.AVATAR_FILE_INVALID);
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(TestimonialErrorCode.AVATAR_FILE_INVALID);
        }
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
