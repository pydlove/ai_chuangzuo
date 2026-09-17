package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationAvatar;
import com.aichuangzuo.admin.modules.simulation.entity.SimulationNickname;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationAvatarMapper;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationNicknameMapper;
import com.aichuangzuo.admin.modules.simulation.util.SimulationNicknameExcelUtil;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationAvatarVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationNicknameVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 模拟运营-昵称/头像库：管理端上传维护，PROFILE 阶段随机取用，用后即删。
 *
 * <p>头像上传时服务端统一压缩：等比缩到最长边 256px、白底 JPEG q0.8。
 * 取用时先随机选一条再按 id 删除，删除成功即归属当前机器人（单消费者下基本无竞争）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationLibraryService {

    /** 单次批量添加/导入昵称上限 */
    private static final int MAX_NICKNAME_BATCH = 2000;
    private static final int MAX_NICKNAME_LENGTH = 30;
    private static final long MAX_AVATAR_FILE_SIZE = 20 * 1024 * 1024;
    private static final int AVATAR_MAX_EDGE = 256;
    private static final float AVATAR_JPEG_QUALITY = 0.8f;
    private static final int MAX_AVATAR_BATCH = 50;
    /** 随机取用与删除之间的冲突重试次数 */
    private static final int CLAIM_TRIES = 3;

    private final SimulationNicknameMapper nicknameMapper;
    private final SimulationAvatarMapper avatarMapper;

    // ---------- 昵称库 ----------

    public PageResult<SimulationNicknameVO> listNicknames(String keyword, long page, long size) {
        LambdaQueryWrapper<SimulationNickname> wrapper = new LambdaQueryWrapper<SimulationNickname>()
                .and(StringUtils.hasText(keyword), w -> w.like(SimulationNickname::getNickname, keyword))
                .orderByDesc(SimulationNickname::getId);
        Page<SimulationNickname> result = nicknameMapper.selectPage(new Page<>(page, size), wrapper);
        List<SimulationNicknameVO> items = result.getRecords().stream()
                .map(n -> new SimulationNicknameVO(n.getId(), n.getNickname(), n.getCreatedAt()))
                .toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long countNicknames() {
        return nicknameMapper.selectCount(null);
    }

    /** 批量添加昵称（去重、去空白），返回新增条数。 */
    public int addNicknames(List<String> nicknames) {
        Set<String> normalized = normalizeNicknames(nicknames);
        if (normalized.size() > MAX_NICKNAME_BATCH) {
            throw new BusinessException(400, "单次最多添加 " + MAX_NICKNAME_BATCH + " 个昵称");
        }
        int inserted = 0;
        for (String nickname : normalized) {
            inserted += insertNicknameIgnore(nickname);
        }
        return inserted;
    }

    /** Excel 导入昵称，返回新增条数。 */
    public int importNicknames(MultipartFile file) {
        List<String> rows = SimulationNicknameExcelUtil.readNicknames(file);
        return addNicknames(rows);
    }

    public void deleteNickname(Long id) {
        nicknameMapper.deleteById(id);
    }

    /** 批量删除昵称，返回删除条数。 */
    public int batchDeleteNicknames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的昵称");
        }
        return nicknameMapper.deleteBatchIds(ids);
    }

    /** PROFILE 阶段取用：随机取一条并删除；库空或重试均失败返回 null。 */
    public String claimNickname() {
        for (int i = 0; i < CLAIM_TRIES; i++) {
            SimulationNickname row = nicknameMapper.pickRandom();
            if (row == null) {
                return null;
            }
            if (nicknameMapper.deleteById(row.getId()) > 0) {
                log.info("模拟运营昵称取用 id={} nickname={}", row.getId(), row.getNickname());
                return row.getNickname();
            }
        }
        return null;
    }

    // ---------- 头像库 ----------

    public PageResult<SimulationAvatarVO> listAvatars(long page, long size) {
        Page<SimulationAvatar> result = avatarMapper.selectPage(
                new Page<>(page, size), new LambdaQueryWrapper<SimulationAvatar>().orderByDesc(SimulationAvatar::getId));
        List<SimulationAvatarVO> items = result.getRecords().stream()
                .map(a -> new SimulationAvatarVO(a.getId(), toDataUrl(a.getAvatar()), a.getCreatedAt()))
                .toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long countAvatars() {
        return avatarMapper.selectCount(null);
    }

    /** 批量上传头像：逐张压缩为 JPEG 后入库，返回成功条数。 */
    public int uploadAvatars(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(400, "请选择要上传的头像文件");
        }
        if (files.length > MAX_AVATAR_BATCH) {
            throw new BusinessException(400, "单次最多上传 " + MAX_AVATAR_BATCH + " 张头像");
        }
        int saved = 0;
        for (MultipartFile file : files) {
            byte[] compressed = compressAvatar(file);
            SimulationAvatar entity = new SimulationAvatar();
            entity.setAvatar(compressed);
            entity.setCreatedAt(LocalDateTime.now());
            avatarMapper.insert(entity);
            saved++;
        }
        return saved;
    }

    public void deleteAvatar(Long id) {
        avatarMapper.deleteById(id);
    }

    /** 批量删除头像，返回删除条数。 */
    public int batchDeleteAvatars(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的头像");
        }
        return avatarMapper.deleteBatchIds(ids);
    }

    /** PROFILE 阶段取用：随机取一条并删除；库空或重试均失败返回 null。 */
    public AvatarPick claimAvatar() {
        for (int i = 0; i < CLAIM_TRIES; i++) {
            SimulationAvatar row = avatarMapper.pickRandom();
            if (row == null) {
                return null;
            }
            if (avatarMapper.deleteById(row.getId()) > 0) {
                log.info("模拟运营头像取用 id={} size={}", row.getId(), row.getAvatar().length);
                return new AvatarPick(row.getId(), row.getAvatar());
            }
        }
        return null;
    }

    /** 取用的头像：库 id + JPEG 字节。 */
    public record AvatarPick(Long id, byte[] bytes) {
    }

    // ---------- 内部 ----------

    private int insertNicknameIgnore(String nickname) {
        // 依赖 uk_simulation_nickname 唯一索引去重，INSERT IGNORE 避免先查后插的竞争
        return nicknameMapper.insertIgnore(nickname);
    }

    private Set<String> normalizeNicknames(List<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            throw new BusinessException(400, "昵称列表不能为空");
        }
        Set<String> set = new LinkedHashSet<>();
        for (String raw : nicknames) {
            if (raw == null) {
                continue;
            }
            String nickname = raw.trim();
            if (nickname.isEmpty()) {
                continue;
            }
            if (nickname.length() > MAX_NICKNAME_LENGTH) {
                nickname = nickname.substring(0, MAX_NICKNAME_LENGTH);
            }
            set.add(nickname);
        }
        if (set.isEmpty()) {
            throw new BusinessException(400, "昵称列表不能为空");
        }
        return set;
    }

    /** 压缩头像：等比缩到最长边 256px，白底 JPEG q0.8。 */
    private byte[] compressAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "头像文件为空");
        }
        if (file.getSize() > MAX_AVATAR_FILE_SIZE) {
            throw new BusinessException(400, "单张头像不能超过 20MB");
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (source == null) {
                throw new BusinessException(400, "头像文件不是有效的图片");
            }
            double scale = Math.min(1.0,
                    (double) AVATAR_MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(AVATAR_JPEG_QUALITY);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(target, null, null), param);
            } finally {
                writer.dispose();
            }
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new BusinessException(500, "头像压缩失败");
            }
            return bytes;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(400, "头像文件不是有效的图片");
        }
    }

    private static String toDataUrl(byte[] bytes) {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
