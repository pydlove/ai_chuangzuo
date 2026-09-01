package com.aichuangzuo.admin.modules.settings.upgrademanagement.service;

import com.aichuangzuo.admin.modules.settings.upgrademanagement.dto.request.UpgradeConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.entity.UpgradeConfig;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.entity.UpgradeJobLog;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.mapper.UpgradeConfigMapper;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.mapper.UpgradeJobLogMapper;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeConfigVO;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeJobLogVO;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeScriptVO;
import com.aichuangzuo.shared.enums.error.AdminUpgradeErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 升级管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradeManagementService {

    private static final long CONFIG_ID = 1L;
    private static final Pattern MASKED_SECRET = Pattern.compile("^\\*+$");

    private final UpgradeConfigMapper configMapper;
    private final UpgradeJobLogMapper jobLogMapper;
    private final StringEncryptor encryptor;

    /**
     * 查询升级配置。
     */
    public UpgradeConfigVO detail() {
        UpgradeConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return toConfigVo(config, true);
    }

    /**
     * 更新升级配置。
     */
    @Transactional
    public UpgradeConfigVO update(UpgradeConfigUpdateRequest request, Long adminUserId) {
        UpgradeConfig exist = configMapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = defaultConfig();
        }

        exist.setScriptRootDir(request.getScriptRootDir());
        exist.setServerIp(request.getServerIp());
        exist.setServerUser(request.getServerUser());
        exist.setSshKeyPath(request.getSshKeyPath());
        exist.setCommandTimeoutSeconds(request.getCommandTimeoutSeconds());

        String password = request.getServerPassword();
        if (StringUtils.hasText(password) && !MASKED_SECRET.matcher(password).matches()) {
            exist.setServerPassword(encryptor.encrypt(password));
        } else if (isNew) {
            exist.setServerPassword(null);
        }

        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            exist.setCreatedBy(adminUserId == null ? 0L : adminUserId);
            configMapper.insert(exist);
        } else {
            configMapper.updateById(exist);
        }

        log.info("admin={} 更新升级配置, rootDir={}, serverIp={}", adminUserId,
                exist.getScriptRootDir(), exist.getServerIp());
        return toConfigVo(exist, true);
    }

    /**
     * 列出脚本根目录下所有 .sh 脚本。
     */
    public List<UpgradeScriptVO> listScripts() {
        UpgradeConfig config = requireConfig();
        String rootDir = config.getScriptRootDir();
        Path rootPath = Paths.get(rootDir);
        if (!Files.isDirectory(rootPath)) {
            throw new BusinessException(AdminUpgradeErrorCode.ROOT_DIR_INVALID);
        }

        List<UpgradeScriptVO> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".sh"))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return !name.startsWith("_") && !name.startsWith(".");
                    })
                    .sorted()
                    .forEach(p -> result.add(toScriptVo(rootPath, p)));
        } catch (IOException e) {
            log.error("扫描脚本目录失败: {}", rootDir, e);
            throw new BusinessException(AdminUpgradeErrorCode.ROOT_DIR_INVALID);
        }
        return result;
    }

    /**
     * 分页查询执行记录。
     */
    public IPage<UpgradeJobLogVO> listJobs(int pageNum, int pageSize) {
        Page<UpgradeJobLog> pageParam = new Page<>(pageNum, pageSize);
        QueryWrapper<UpgradeJobLog> wrapper = new QueryWrapper<UpgradeJobLog>()
                .orderByDesc("started_at")
                .orderByDesc("id");
        Page<UpgradeJobLog> result = jobLogMapper.selectPage(pageParam, wrapper);
        List<UpgradeJobLogVO> records = result.getRecords().stream()
                .map(this::toJobLogVo)
                .toList();
        Page<UpgradeJobLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 查询单条执行记录。
     */
    public UpgradeJobLogVO getJob(Long id) {
        UpgradeJobLog job = jobLogMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(AdminUpgradeErrorCode.JOB_NOT_FOUND);
        }
        return toJobLogVo(job);
    }

    UpgradeConfig requireConfig() {
        UpgradeConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return config;
    }

    private UpgradeConfig defaultConfig() {
        UpgradeConfig config = new UpgradeConfig();
        config.setId(CONFIG_ID);
        config.setScriptRootDir("/Users/panyong/aio_project/ai_chuangzuo/scripts");
        config.setCommandTimeoutSeconds(600);
        return config;
    }

    private UpgradeConfigVO toConfigVo(UpgradeConfig config, boolean decryptPassword) {
        UpgradeConfigVO vo = new UpgradeConfigVO();
        BeanUtils.copyProperties(config, vo);
        if (decryptPassword && StringUtils.hasText(config.getServerPassword())) {
            try {
                vo.setServerPassword(encryptor.decrypt(config.getServerPassword()));
            } catch (Exception e) {
                log.warn("升级管理密码解密失败，返回空");
                vo.setServerPassword("");
            }
        }
        return vo;
    }

    private UpgradeScriptVO toScriptVo(Path rootPath, Path scriptPath) {
        UpgradeScriptVO vo = new UpgradeScriptVO();
        String relative = rootPath.relativize(scriptPath).toString();
        vo.setRelativePath(relative);
        vo.setName(scriptPath.getFileName().toString());
        Path parent = scriptPath.getParent();
        vo.setCategory(parent == null ? "" : rootPath.relativize(parent).toString());
        vo.setDescription(extractDescription(scriptPath));
        return vo;
    }

    private String extractDescription(Path scriptPath) {
        try (BufferedReader reader = Files.newBufferedReader(scriptPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#!/")) {
                    continue;
                }
                if (line.startsWith("#")) {
                    return line.replaceFirst("^#+", "").trim();
                }
                break;
            }
        } catch (IOException e) {
            log.warn("读取脚本描述失败: {}", scriptPath, e);
        }
        return "";
    }

    private UpgradeJobLogVO toJobLogVo(UpgradeJobLog job) {
        UpgradeJobLogVO vo = new UpgradeJobLogVO();
        BeanUtils.copyProperties(job, vo);
        vo.setOutputTruncated(Integer.valueOf(1).equals(job.getOutputTruncated()));
        return vo;
    }
}
