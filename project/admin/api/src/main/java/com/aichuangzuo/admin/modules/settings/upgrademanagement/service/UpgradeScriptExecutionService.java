package com.aichuangzuo.admin.modules.settings.upgrademanagement.service;

import com.aichuangzuo.admin.modules.settings.upgrademanagement.entity.UpgradeConfig;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.entity.UpgradeJobLog;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.mapper.UpgradeJobLogMapper;
import com.aichuangzuo.shared.enums.error.AdminUpgradeErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 升级脚本执行服务。
 *
 * <p>异步执行本地脚本，注入环境变量替换脚本中的服务器连接信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradeScriptExecutionService {

    private static final int MAX_OUTPUT_LENGTH = 1024 * 1024; // 1 MB

    private final UpgradeManagementService upgradeManagementService;
    private final UpgradeJobLogMapper jobLogMapper;
    @Qualifier("upgradeTaskExecutor")
    private final Executor upgradeTaskExecutor;

    /**
     * 校验并解析脚本路径，返回绝对路径。
     */
    public Path resolveScript(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException(AdminUpgradeErrorCode.SCRIPT_PATH_INVALID);
        }
        UpgradeConfig config = upgradeManagementService.requireConfig();
        Path rootPath = Paths.get(config.getScriptRootDir()).toAbsolutePath().normalize();
        Path scriptPath = rootPath.resolve(relativePath).toAbsolutePath().normalize();

        // 防止目录遍历
        if (!scriptPath.startsWith(rootPath)) {
            throw new BusinessException(AdminUpgradeErrorCode.SCRIPT_PATH_INVALID);
        }
        if (!Files.isRegularFile(scriptPath) || !scriptPath.toString().toLowerCase().endsWith(".sh")) {
            throw new BusinessException(AdminUpgradeErrorCode.SCRIPT_NOT_FOUND);
        }
        return scriptPath;
    }

    /**
     * 提交执行记录并触发异步执行。
     */
    public Long submit(String relativePath, List<String> arguments, Long adminUserId) {
        Path scriptPath = resolveScript(relativePath);
        Path fileName = scriptPath.getFileName();

        UpgradeJobLog job = new UpgradeJobLog();
        job.setScriptRelativePath(relativePath);
        job.setScriptName(fileName == null ? relativePath : fileName.toString());
        job.setTriggerType("manual");
        job.setRunStatus("running");
        job.setStartedAt(LocalDateTime.now());
        job.setCreatedBy(adminUserId == null ? 0L : adminUserId);
        job.setOutputTruncated(0);
        jobLogMapper.insert(job);

        upgradeTaskExecutor.execute(() -> executeAsync(job.getId(), scriptPath, arguments, adminUserId));
        return job.getId();
    }

    public void executeAsync(Long jobId, Path scriptPath, List<String> arguments, Long adminUserId) {
        UpgradeConfig config = upgradeManagementService.requireConfig();
        Integer timeoutSeconds = config.getCommandTimeoutSeconds();
        if (timeoutSeconds == null || timeoutSeconds < 10) {
            timeoutSeconds = 600;
        }

        List<String> command = new ArrayList<>();
        command.add("/bin/bash");
        command.add(scriptPath.toString());
        if (arguments != null) {
            command.addAll(arguments);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptPath.getParent().toFile());
        pb.redirectErrorStream(false);

        // 注入环境变量，替换脚本中的服务器连接信息
        pb.environment().put("SERVER_IP", defaultString(config.getServerIp()));
        pb.environment().put("SERVER_USER", defaultString(config.getServerUser()));
        pb.environment().put("SERVER_PASSWORD", defaultString(config.getServerPassword()));
        pb.environment().put("SSH_KEY_PATH", defaultString(config.getSshKeyPath()));

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Integer exitCode = null;
        boolean truncated = false;

        Process process = null;
        try {
            process = pb.start();
            final Process finalProcess = process;
            final StringBuilder finalStdout = stdout;
            final StringBuilder finalStderr = stderr;

            Thread stdoutReader = new Thread(() -> readStream(finalProcess.getInputStream(), finalStdout));
            Thread stderrReader = new Thread(() -> readStream(finalProcess.getErrorStream(), finalStderr));
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            stdoutReader.join(5000);
            stderrReader.join(5000);

            if (!finished) {
                process.destroyForcibly();
                exitCode = null;
                updateJob(jobId, "timeout", exitCode,
                        truncate(stdout.toString()), truncate(stderr.toString()), true);
                log.warn("升级脚本执行超时, jobId={}, script={}, timeout={}s", jobId, scriptPath, timeoutSeconds);
                return;
            }

            exitCode = process.exitValue();
            String status = exitCode == 0 ? "success" : "failed";
            String outStr = stdout.toString();
            String errStr = stderr.toString();
            if (outStr.length() > MAX_OUTPUT_LENGTH || errStr.length() > MAX_OUTPUT_LENGTH) {
                truncated = true;
            }
            updateJob(jobId, status, exitCode, truncate(outStr), truncate(errStr), truncated);
            log.info("升级脚本执行完成, jobId={}, script={}, exitCode={}", jobId, scriptPath, exitCode);
        } catch (IOException | InterruptedException e) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            log.error("升级脚本执行异常, jobId={}, script={}", jobId, scriptPath, e);
            updateJob(jobId, "failed", exitCode == null ? -1 : exitCode,
                    truncate(stdout.toString()), truncate(stderr.toString()) + "\n" + e.getMessage(), false);
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void onDestroy() {
        log.info("UpgradeScriptExecutionService 销毁");
    }

    private void readStream(InputStream inputStream, StringBuilder builder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
                if (builder.length() > MAX_OUTPUT_LENGTH * 2) {
                    // 超过两倍上限后丢弃后续输出，避免内存爆炸
                    while (reader.read(buffer) != -1) {
                        // drain
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("读取脚本输出流失败", e);
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() <= MAX_OUTPUT_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_OUTPUT_LENGTH)
                + "\n\n[输出过长，已截断。完整输出请查看服务器日志文件。]";
    }

    private void updateJob(Long jobId, String status, Integer exitCode,
                           String stdout, String stderr, boolean truncated) {
        UpgradeJobLog job = new UpgradeJobLog();
        job.setId(jobId);
        job.setRunStatus(status);
        job.setExitCode(exitCode);
        job.setStdout(stdout);
        job.setStderr(stderr);
        job.setOutputTruncated(truncated ? 1 : 0);
        job.setFinishedAt(LocalDateTime.now());
        jobLogMapper.updateById(job);
    }

    private String defaultString(String s) {
        return s == null ? "" : s;
    }
}
