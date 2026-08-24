package com.aichuangzuo.user.infrastructure.storage;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地磁盘文件存储（收益截图等）。
 */
@Slf4j
@Component
public class LocalFileStorage {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_FILES_PER_UPLOAD = 20;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    private final Path basePath;

    public LocalFileStorage(@Value("${storage.local.base-path:data/uploads}") String basePath) {
        this.basePath = Paths.get(basePath);
    }

    /**
     * 存储文件，返回相对于 data/uploads 的访问路径。
     *
     * @param userId  用户ID
     * @param subDir  子目录（如 batchId / bizNo）
     * @param files   文件列表
     * @return 相对路径列表
     */
    public List<String> store(Long userId, String subDir, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_FILE_INVALID);
        }

        Path dir = basePath.resolve("leaderboard").resolve(userId.toString()).resolve(subDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("创建上传目录失败: " + dir, e);
        }

        List<String> relativePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            validate(file);
            String filename = System.nanoTime() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path target = dir.resolve(filename);
            try {
                file.transferTo(target);
            } catch (IOException e) {
                throw new IllegalStateException("文件保存失败: " + target, e);
            }
            relativePaths.add("leaderboard/" + userId + "/" + subDir + "/" + filename);
        }
        return relativePaths;
    }

    /**
     * 存储头像，返回 root-relative 访问路径。
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return /uploads/avatar/{userId}/{filename}.jpg
     */
    public String storeAvatar(Long userId, MultipartFile file) {
        validateAvatar(file);

        Path dir = basePath.resolve("avatar").resolve(userId.toString());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("创建头像目录失败: " + dir, e);
        }

        String filename = System.nanoTime() + ".jpg";
        Path target = dir.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("头像保存失败: " + target, e);
        }

        return "/uploads/avatar/" + userId + "/" + filename;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(UserAuthErrorCode.AVATAR_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(UserAuthErrorCode.AVATAR_FILE_INVALID);
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(UserAuthErrorCode.AVATAR_FILE_INVALID);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_FILE_INVALID);
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_FILE_INVALID);
        }
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        String name = filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        int idx = name.lastIndexOf('.');
        return idx < 0 ? name : name.substring(idx);
    }
}
