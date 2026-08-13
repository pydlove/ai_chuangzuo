package com.aichuangzuo.admin.modules.user.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.plan.entity.Plan;
import com.aichuangzuo.admin.modules.plan.mapper.PlanMapper;
import com.aichuangzuo.admin.modules.benefit.entity.BenefitUsageAggregate;
import com.aichuangzuo.admin.modules.benefit.mapper.BenefitUsageAdminMapper;
import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.entity.UserMarketFavorite;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.UserMarketFavoriteMapper;
import com.aichuangzuo.admin.modules.skill.review.mapper.SkillReviewMapper;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserUpdateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.ResetCustomSkillQuotaRequest;
import com.aichuangzuo.admin.modules.user.dto.excel.UserImportExcelRowData;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.entity.UserInviteRelation;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.mapper.UserInviteRelationMapper;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.util.UserExcelImportUtil;
import com.aichuangzuo.admin.modules.user.vo.AdminLearnedSkillMonthVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserFavoriteSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserImportResultVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserImportRowErrorVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserInviteDetailVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserInviteeVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserOptionVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPublishedSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserLoginLogMapper platformUserLoginLogMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final PasswordEncoder passwordEncoder;
    private final SkillReviewMapper skillReviewMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final UserMarketFavoriteMapper userMarketFavoriteMapper;
    private final BenefitUsageAdminMapper benefitUsageAdminMapper;
    private final PlanMapper planMapper;
    private final AdminMembershipMapper adminMembershipMapper;

    private static final String RESET_PASSWORD = "Aichuangzuo@123";
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String BENEFIT_CODE_LEARN_ANALYZE = "skill_learn_analyze";
    private static final String BENEFIT_CODE_CUSTOM_SKILL = "skill_custom";
    private static final String BENEFIT_CODE_SKILL_MARKET_PUBLISH = "skill_market_publish";
    private static final String LIFETIME_PERIOD = "lifetime";
    private static final int SOURCE_TYPE_CUSTOM = 1;
    private static final int SOURCE_TYPE_LEARN = 2;
    private static final int AUDIT_STATUS_PENDING = 0;
    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final String PERIOD_PATTERN = "^\\d{4}-\\d{2}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminUserCreateRequest request) {
        String email = request.getEmail().trim();
        if (!StringUtils.hasText(request.getNickname())) {
            throw new BusinessException(AdminUserErrorCode.NICKNAME_FORMAT_ERROR);
        }
        if (request.getUserType() == null || (request.getUserType() != 0 && request.getUserType() != 1)) {
            throw new BusinessException(AdminUserErrorCode.USER_TYPE_INVALID);
        }

        LambdaQueryWrapper<PlatformUser> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(PlatformUser::getEmail, email).eq(PlatformUser::getIsDeleted, 0);
        if (platformUserMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(AdminUserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : RESET_PASSWORD;
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(AdminUserErrorCode.PASSWORD_FORMAT_ERROR);
        }

        PlatformUser user = new PlatformUser();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setInviteCode(generateInviteCode());
        user.setUserStatus(1);
        user.setUserType(request.getUserType());
        user.setEmailVerified(1);
        user.setTenantId(0L);
        user.setIsDeleted(0);
        platformUserMapper.insert(user);

        log.info("管理员创建用户成功, adminUserId={}, userId={}, email={}",
                SecurityAdminContext.getCurrentAdminUserId(), user.getId(), email);

        return toAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserImportResultVO importUsersFromExcel(MultipartFile file) {
        List<UserImportExcelRowData> rows = UserExcelImportUtil.readRows(file);
        List<AdminUserImportRowErrorVO> errors = new ArrayList<>();
        List<PlatformUser> users = new ArrayList<>(rows.size());
        List<String> emailsInFile = new ArrayList<>(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            UserImportExcelRowData row = rows.get(i);
            int rowIndex = i + 2;
            List<String> rowErrors = new ArrayList<>();
            PlatformUser user = validateAndBuildUser(row, rowIndex, rowErrors, emailsInFile);
            if (!rowErrors.isEmpty()) {
                errors.add(new AdminUserImportRowErrorVO(rowIndex, trim(row.getEmail()), rowErrors));
            } else {
                users.add(user);
                emailsInFile.add(user.getEmail());
            }
        }

        if (!errors.isEmpty()) {
            return new AdminUserImportResultVO(false, rows.size(), 0, errors);
        }

        for (PlatformUser user : users) {
            platformUserMapper.insert(user);
        }
        log.info("管理员批量导入用户成功, adminUserId={}, total={}, success={}",
                SecurityAdminContext.getCurrentAdminUserId(), rows.size(), users.size());
        return new AdminUserImportResultVO(true, rows.size(), users.size(), List.of());
    }

    private PlatformUser validateAndBuildUser(UserImportExcelRowData row, int rowIndex,
                                              List<String> errors, List<String> emailsInFile) {
        String email = trim(row.getEmail());
        String nickname = trim(row.getNickname());
        String password = trim(row.getPassword());
        String userTypeText = trim(row.getUserType());

        if (email == null || email.isEmpty()) {
            errors.add("【邮箱】未填写");
        } else if (email.length() > 128) {
            errors.add("【邮箱】长度超过 128 字符，当前 " + email.length() + " 字符");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("【邮箱】格式不正确");
        }

        if (nickname == null || nickname.isEmpty()) {
            errors.add("【昵称】未填写");
        } else if (nickname.length() > 64) {
            errors.add("【昵称】长度超过 64 字符，当前 " + nickname.length() + " 字符");
        }

        String finalPassword = StringUtils.hasText(password) ? password : RESET_PASSWORD;
        if (finalPassword.length() < 6 || finalPassword.length() > 32) {
            errors.add("【密码】长度需在 6-32 字符之间");
        }

        Integer userType = parseUserType(userTypeText, errors);

        if (email != null && !email.isEmpty() && emailsInFile.contains(email)) {
            errors.add("【邮箱】在 Excel 中重复");
        }

        if (email != null && !email.isEmpty() && !errors.stream().anyMatch(e -> e.contains("邮箱"))) {
            LambdaQueryWrapper<PlatformUser> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(PlatformUser::getEmail, email).eq(PlatformUser::getIsDeleted, 0);
            if (platformUserMapper.selectCount(existsWrapper) > 0) {
                errors.add("【邮箱】已注册");
            }
        }

        if (!errors.isEmpty()) {
            return null;
        }

        PlatformUser user = new PlatformUser();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(finalPassword));
        user.setInviteCode(generateInviteCode());
        user.setUserStatus(1);
        user.setUserType(userType);
        user.setEmailVerified(1);
        user.setTenantId(0L);
        user.setIsDeleted(0);
        return user;
    }

    private Integer parseUserType(String value, List<String> errors) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        String text = value.trim();
        if ("0".equals(text) || "机器人".equals(text)) {
            return 0;
        }
        if ("1".equals(text) || "真实用户".equals(text)) {
            return 1;
        }
        errors.add("【用户类型】格式不正确，请填写 0/机器人 或 1/真实用户");
        return null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlatformUser::getInviteCode, code).eq(PlatformUser::getIsDeleted, 0);
            if (platformUserMapper.selectCount(wrapper) == 0) {
                return code;
            }
        }
        throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
    }

    @Override
    public AdminUserPageVO listUsers(String keyword, String inviteCode, int page, int pageSize) {
        Page<PlatformUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformUser::getIsDeleted, 0);
        if (StringUtils.hasText(inviteCode)) {
            wrapper.eq(PlatformUser::getInviteCode, inviteCode.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlatformUser::getEmail, kw)
                    .or()
                    .like(PlatformUser::getNickname, kw)
                    .or()
                    .like(PlatformUser::getInviteCode, kw));
        }
        wrapper.orderByDesc(PlatformUser::getCreatedAt);
        Page<PlatformUser> result = platformUserMapper.selectPage(pageParam, wrapper);
        List<AdminUserVO> list = result.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
        AdminUserPageVO vo = new AdminUserPageVO();
        vo.setList(list);
        vo.setTotal(result.getTotal());
        return vo;
    }

    @Override
    public AdminUserVO getUser(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        return toAdminUserVO(user);
    }

    @Override
    public AdminUserInviteDetailVO getUserInviteDetail(Long id, int page, int pageSize) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }

        AdminUserInviteDetailVO detail = new AdminUserInviteDetailVO();
        detail.setUserId(user.getId());
        detail.setInviteCode(user.getInviteCode());
        detail.setPage(page);
        detail.setPageSize(pageSize);

        UserInviteRelation inviterRelation = userInviteRelationMapper.selectByInviteeId(id);
        if (inviterRelation != null) {
            PlatformUser inviter = platformUserMapper.selectById(inviterRelation.getInviterId());
            if (inviter != null && inviter.getIsDeleted() == 0) {
                detail.setInviter(toAdminUserInviteeVO(inviter, inviterRelation.getCreatedAt()));
            }
        }

        int total = userInviteRelationMapper.countEffectiveByInviterId(id);
        detail.setTotal(total);
        if (total > 0 && pageSize > 0) {
            int offset = (page - 1) * pageSize;
            List<Long> inviteeIds = userInviteRelationMapper.selectEffectiveInviteeIdsByInviterId(id, offset, pageSize);
            if (!inviteeIds.isEmpty()) {
                List<PlatformUser> invitees = platformUserMapper.selectBatchIds(inviteeIds);
                Map<Long, PlatformUser> inviteeMap = invitees.stream()
                        .filter(u -> u.getIsDeleted() == 0)
                        .collect(Collectors.toMap(PlatformUser::getId, u -> u));
                List<AdminUserInviteeVO> inviteeVOs = inviteeIds.stream()
                        .map(inviteeMap::get)
                        .filter(Objects::nonNull)
                        .map(u -> toAdminUserInviteeVO(u, null))
                        .collect(Collectors.toList());
                detail.setInvitees(inviteeVOs);
            }
        }

        return detail;
    }

    private AdminUserInviteeVO toAdminUserInviteeVO(PlatformUser user, LocalDateTime relationCreatedAt) {
        AdminUserInviteeVO vo = new AdminUserInviteeVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setCreatedAt(relationCreatedAt != null ? relationCreatedAt : user.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, AdminUserStatusRequest request) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        int status = "enabled".equals(request.getStatus()) ? 1 : 0;
        user.setUserStatus(status);
        platformUserMapper.updateById(user);
        log.info("管理员修改用户状态成功, adminUserId={}, userId={}, status={}",
                SecurityAdminContext.getCurrentAdminUserId(), id, request.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResetPasswordVO resetPassword(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        user.setPasswordHash(passwordEncoder.encode(RESET_PASSWORD));
        platformUserMapper.updateById(user);
        log.info("管理员重置用户密码成功, adminUserId={}, userId={}",
                SecurityAdminContext.getCurrentAdminUserId(), id);
        AdminUserResetPasswordVO vo = new AdminUserResetPasswordVO();
        vo.setNewPassword(RESET_PASSWORD);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateUser(Long id, AdminUserUpdateRequest request) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        if (request.getUserType() == null || (request.getUserType() != 0 && request.getUserType() != 1)) {
            throw new BusinessException(AdminUserErrorCode.USER_TYPE_INVALID);
        }

        String email = request.getEmail().trim();
        LambdaQueryWrapper<PlatformUser> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(PlatformUser::getEmail, email)
                .eq(PlatformUser::getIsDeleted, 0)
                .ne(PlatformUser::getId, id);
        if (platformUserMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(AdminUserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setUserStatus("enabled".equals(request.getStatus()) ? 1 : 0);
        user.setUserType(request.getUserType());
        user.setMembershipExpireAt(request.getExpireDate() == null ? null : request.getExpireDate().plusDays(1).atStartOfDay());

        String membershipPlan = request.getMembershipPlan();
        if (StringUtils.hasText(membershipPlan)) {
            LambdaQueryWrapper<Plan> planWrapper = new LambdaQueryWrapper<>();
            planWrapper.eq(Plan::getPlanKey, membershipPlan.trim());
            if (planMapper.selectCount(planWrapper) == 0) {
                throw new BusinessException(AdminUserErrorCode.MEMBERSHIP_PLAN_INVALID);
            }
            user.setMembershipPlan(membershipPlan.trim());
        } else {
            user.setMembershipPlan(null);
        }

        platformUserMapper.updateById(user);

        // 同步 u_user_membership：用户端权益校验读的是这张表，不能只更新 u_user 缓存列
        syncUserMembership(user.getId(), user.getMembershipPlan(), user.getMembershipExpireAt());

        log.info("管理员更新用户成功, adminUserId={}, userId={}, email={}",
                SecurityAdminContext.getCurrentAdminUserId(), id, email);
        return toAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        platformUserMapper.deleteById(id);
        log.info("管理员删除用户成功, adminUserId={}, userId={}",
                SecurityAdminContext.getCurrentAdminUserId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        int deleted = platformUserMapper.deleteBatchIds(ids);
        log.info("管理员批量删除用户成功, adminUserId={}, count={}, ids={}",
                SecurityAdminContext.getCurrentAdminUserId(), deleted, ids);
        return deleted;
    }

    @Override
    public List<AdminUserOptionVO> listUserOptions(String keyword, int limit) {
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformUser::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlatformUser::getEmail, kw)
                    .or()
                    .like(PlatformUser::getNickname, kw));
        }
        wrapper.orderByDesc(PlatformUser::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        List<PlatformUser> users = platformUserMapper.selectList(wrapper);
        return users.stream()
                .map(this::toAdminUserOptionVO)
                .collect(Collectors.toList());
    }

    private AdminUserVO toAdminUserVO(PlatformUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getEmail());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getUserStatus() == 1 ? "enabled" : "disabled");
        vo.setUserType(user.getUserType() != null && user.getUserType() == 0 ? "robot" : "real");
        vo.setInviteCode(user.getInviteCode());
        vo.setInvitedCount(userInviteRelationMapper.countEffectiveByInviterId(user.getId()));

        UserInviteRelation inviterRelation = userInviteRelationMapper.selectByInviteeId(user.getId());
        if (inviterRelation != null) {
            PlatformUser inviter = platformUserMapper.selectById(inviterRelation.getInviterId());
            if (inviter != null && inviter.getIsDeleted() == 0) {
                vo.setInviterId(inviter.getId());
                vo.setInviterEmail(inviter.getEmail());
                vo.setInviterNickname(inviter.getNickname());
            }
        }

        vo.setMembershipExpireAt(user.getMembershipExpireAt());
        vo.setMembershipPlan(user.getMembershipPlan());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(platformUserLoginLogMapper.selectLastLoginAtByUserId(user.getId()));
        return vo;
    }

    private AdminUserOptionVO toAdminUserOptionVO(PlatformUser user) {
        AdminUserOptionVO vo = new AdminUserOptionVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        return vo;
    }

    @Override
    public List<AdminUserSkillVO> listUserSkills(Long userId, Integer sourceType) {
        LambdaQueryWrapper<UserSkillAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillAggregate::getUserId, userId);
        if (sourceType != null) {
            wrapper.eq(UserSkillAggregate::getSourceType, sourceType);
        }
        wrapper.orderByDesc(UserSkillAggregate::getCreatedAt);
        return skillReviewMapper.selectList(wrapper).stream()
                .map(this::toAdminUserSkillVO)
                .collect(Collectors.toList());
    }

    private AdminUserSkillVO toAdminUserSkillVO(UserSkillAggregate skill) {
        AdminUserSkillVO vo = new AdminUserSkillVO();
        vo.setBizNo(skill.getBizNo());
        vo.setSkillName(skill.getSkillName());
        vo.setPrompt(skill.getPrompt());
        vo.setScope(skill.getScope());
        vo.setSourceType(skill.getSourceType());
        vo.setUseCount(skill.getUseCount());
        vo.setAuditStatus(skill.getAuditStatus());
        vo.setCreatedAt(skill.getCreatedAt());
        return vo;
    }

    @Override
    public List<AdminUserPublishedSkillVO> listUserPublishedSkills(Long userId) {
        ensureUserExists(userId);

        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getPublisherUserId, userId)
                .eq(SkillMarket::getIsDeleted, 0)
                .and(w -> w.eq(SkillMarket::getAuditStatus, AUDIT_STATUS_PENDING)
                        .or()
                        .eq(SkillMarket::getAuditStatus, AUDIT_STATUS_APPROVED))
                .orderByDesc(SkillMarket::getCreatedAt);
        return skillMarketMapper.selectList(wrapper).stream()
                .map(this::toAdminUserPublishedSkillVO)
                .collect(Collectors.toList());
    }

    private AdminUserPublishedSkillVO toAdminUserPublishedSkillVO(SkillMarket market) {
        AdminUserPublishedSkillVO vo = new AdminUserPublishedSkillVO();
        vo.setBizNo(market.getBizNo());
        vo.setSkillName(market.getSkillName());
        vo.setPromptSummary(market.getPromptSummary());
        vo.setPrompt(market.getPrompt());
        vo.setScope(market.getScope());
        vo.setPrice(market.getPrice());
        vo.setTotalUses(market.getTotalUses());
        vo.setAuditStatus(market.getAuditStatus());
        vo.setCreatedAt(market.getCreatedAt());
        return vo;
    }

    @Override
    public List<AdminUserFavoriteSkillVO> listUserFavoriteSkills(Long userId) {
        ensureUserExists(userId);

        LambdaQueryWrapper<UserMarketFavorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(UserMarketFavorite::getUserId, userId)
                .orderByDesc(UserMarketFavorite::getCreatedAt);
        List<UserMarketFavorite> favorites = userMarketFavoriteMapper.selectList(favoriteWrapper);
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<String> skillBizNos = favorites.stream()
                .map(UserMarketFavorite::getMarketSkillId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        LambdaQueryWrapper<SkillMarket> marketWrapper = new LambdaQueryWrapper<>();
        marketWrapper.in(SkillMarket::getBizNo, skillBizNos);
        List<SkillMarket> markets = skillMarketMapper.selectList(marketWrapper);
        Map<String, SkillMarket> marketMap = markets.stream()
                .collect(Collectors.toMap(SkillMarket::getBizNo, m -> m, (a, b) -> a));

        List<Long> publisherIds = markets.stream()
                .map(SkillMarket::getPublisherUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PlatformUser> publisherMap = publisherIds.isEmpty()
                ? Map.of()
                : platformUserMapper.selectBatchIds(publisherIds).stream()
                        .filter(u -> u.getIsDeleted() == 0)
                        .collect(Collectors.toMap(PlatformUser::getId, u -> u));

        Map<String, LocalDateTime> favoriteAtMap = favorites.stream()
                .filter(f -> StringUtils.hasText(f.getMarketSkillId()))
                .collect(Collectors.toMap(
                        UserMarketFavorite::getMarketSkillId,
                        f -> f.getCreatedAt() != null ? f.getCreatedAt() : LocalDateTime.MIN,
                        (a, b) -> a));

        return skillBizNos.stream()
                .map(marketMap::get)
                .filter(Objects::nonNull)
                .map(market -> {
                    AdminUserFavoriteSkillVO vo = new AdminUserFavoriteSkillVO();
                    vo.setBizNo(market.getBizNo());
                    vo.setSkillName(market.getSkillName());
                    vo.setPromptSummary(market.getPromptSummary());
                    vo.setPrompt(market.getPrompt());
                    vo.setScope(market.getScope());
                    vo.setPrice(market.getPrice());
                    vo.setAuditStatus(market.getAuditStatus());
                    vo.setFavoriteAt(favoriteAtMap.get(market.getBizNo()));
                    PlatformUser publisher = publisherMap.get(market.getPublisherUserId());
                    if (publisher != null) {
                        vo.setPublisherEmail(publisher.getEmail());
                        vo.setPublisherNickname(publisher.getNickname());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminLearnedSkillMonthVO> listUserLearnedSkillsByMonth(Long userId) {
        ensureUserExists(userId);

        LambdaQueryWrapper<BenefitUsageAggregate> usageWrapper = new LambdaQueryWrapper<>();
        usageWrapper.eq(BenefitUsageAggregate::getUserId, userId)
                .eq(BenefitUsageAggregate::getBenefitCode, BENEFIT_CODE_LEARN_ANALYZE)
                .orderByDesc(BenefitUsageAggregate::getPeriod);
        List<BenefitUsageAggregate> usageRows = benefitUsageAdminMapper.selectList(usageWrapper);

        LambdaQueryWrapper<UserSkillAggregate> skillWrapper = new LambdaQueryWrapper<>();
        skillWrapper.eq(UserSkillAggregate::getUserId, userId)
                .eq(UserSkillAggregate::getSourceType, SOURCE_TYPE_LEARN);
        List<UserSkillAggregate> learnedSkills = skillReviewMapper.selectList(skillWrapper);

        Map<String, Long> skillCountByPeriod = learnedSkills.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getCreatedAt()).toString(),
                        Collectors.counting()));

        return usageRows.stream()
                .map(u -> {
                    AdminLearnedSkillMonthVO vo = new AdminLearnedSkillMonthVO();
                    vo.setPeriod(u.getPeriod());
                    vo.setUsedCount(u.getUsedCount());
                    vo.setPreUsedCount(u.getPreUsedCount());
                    vo.setSkillCount(skillCountByPeriod.getOrDefault(u.getPeriod(), 0L));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetLearnedSkillQuota(Long userId, String period) {
        ensureUserExists(userId);
        if (!Pattern.matches(PERIOD_PATTERN, period)) {
            throw new BusinessException(AdminUserErrorCode.PERIOD_FORMAT_ERROR);
        }
        benefitUsageAdminMapper.resetQuotaByPeriod(userId, BENEFIT_CODE_LEARN_ANALYZE, period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCustomSkillQuota(Long userId, ResetCustomSkillQuotaRequest request) {
        ensureUserExists(userId);
        String period = YearMonth.now().toString();
        benefitUsageAdminMapper.decreaseUsedCount(
                userId, BENEFIT_CODE_CUSTOM_SKILL, period, request.getCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releasePublishSkillQuota(Long userId, ResetCustomSkillQuotaRequest request) {
        ensureUserExists(userId);
        benefitUsageAdminMapper.decreaseUsedCount(
                userId, BENEFIT_CODE_SKILL_MARKET_PUBLISH, LIFETIME_PERIOD, request.getCount());
    }

    private void ensureUserExists(Long userId) {
        PlatformUser user = platformUserMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
    }

    private void syncUserMembership(Long userId, String planKey, LocalDateTime expireAt) {
        if (!StringUtils.hasText(planKey) || expireAt == null) {
            return;
        }
        // PlatformUser.membership_expire_at 存的是“到期日次日 00:00”，
        // u_user_membership.expires_at 存的是实际到期日，需要减一天。
        LocalDate expireDate = expireAt.toLocalDate().minusDays(1);
        if (expireDate.isBefore(LocalDate.now())) {
            return;
        }

        AdminMembership membership = adminMembershipMapper.selectByUserId(userId);
        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(userId);
            membership.setLevel(planKey);
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(expireDate);
            adminMembershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(planKey);
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(expireDate);
            adminMembershipMapper.updateMembership(membership);
        }
    }
}
