package com.aichuangzuo.admin.modules.simulation.client;

import com.aichuangzuo.admin.infrastructure.security.JwtUtil;
import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模拟运营-用户端 API 客户端。
 *
 * <p>内部接口用管理端 JWT + X-Internal-Key；机器人公开接口用机器人 token，
 * 其中订阅支付额外带 X-Internal-Key 走测试支付绿通。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationUserApiClient {

    private static final String TEST_PAY_CODE = "123456";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${simulation.user-api.base-url:http://localhost:25050}")
    private String baseUrl;

    @Value("${user.api.internal-key:}")
    private String internalKey;

    public record RobotCreated(Long userId, String email) {
    }

    public record MarketSkill(String bizNo, String skillName, Long publisherUserId) {
    }

    // ---------- 内部接口 ----------

    public RobotCreated createRobot(String email, String password, String inviteCode) {
        Map<String, Object> body = inviteCode == null
                ? Map.of("email", email, "password", password)
                : Map.of("email", email, "password", password, "inviteCode", inviteCode);
        JsonNode data = postInternal("/api/v1/user/internal/simulation/robots", body);
        return new RobotCreated(data.get("userId").asLong(), data.get("email").asText());
    }

    public List<String> randomRobotInviteCodes(int count) {
        JsonNode data = getInternal("/api/v1/user/internal/simulation/robot-invite-codes?count=" + count);
        List<String> codes = new ArrayList<>();
        if (data != null && data.isArray()) {
            data.forEach(n -> codes.add(n.asText()));
        }
        return codes;
    }

    // ---------- 机器人公开接口 ----------

    public String login(String email, String password) {
        JsonNode data = postRobot("/api/v1/user/auth/login",
                Map.of("email", email, "password", password), null);
        String token = data.get("accessToken").asText();
        log.info("模拟机器人登录成功 email={}", email);
        return token;
    }

    public void updateNickname(String token, String nickname) {
        putRobot("/api/v1/user/me/nickname", Map.of("nickname", nickname), token);
    }

    public void updateProfileBio(String token, String bio) {
        putRobot("/api/v1/user/me/profile", Map.of("bio", bio), token);
    }

    public void uploadAvatar(String token, byte[] imageBytes, String filename) {
        HttpHeaders headers = robotHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        ResponseEntity<Result> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/me/avatar", new HttpEntity<>(body, headers), Result.class);
        checkResult(response.getBody(), "upload avatar");
    }

    /** 当前抽奖活动 ID；无活动返回 null。 */
    public Long currentLotteryCampaignId(String token) {
        JsonNode data = getRobot("/api/v1/user/lottery/campaigns/current", token);
        return data == null || data.isNull() || !data.has("id") ? null : data.get("id").asLong();
    }

    public void drawLottery(String token, Long campaignId) {
        postRobot("/api/v1/user/lottery/draw", Map.of("campaignId", campaignId), token);
    }

    /** 订阅会员（测试支付绿通：payCode=123456 + X-Internal-Key）。 */
    public void subscribeMembership(String token, String planKey, String cycle) {
        JsonNode preview = postRobot("/api/v1/user/membership/subscribe-preview",
                Map.of("planKey", planKey, "cycle", cycle), token);
        BigDecimal finalPrice = preview.get("finalPrice").decimalValue();
        Map<String, Object> body = Map.of(
                "planKey", planKey,
                "cycle", cycle,
                "payCode", TEST_PAY_CODE,
                "amount", finalPrice);
        postRobot("/api/v1/user/membership/subscribe", body, token, true);
    }

    /**
     * 按发布者范围随机采样提示词列表（取前 sampleSize 条内随机）。
     */
    public List<MarketSkill> listMarketSkills(String token, Integer publisherType, int sampleSize) {
        StringBuilder url = new StringBuilder("/api/v1/user/market-skills/paged?page=1&pageSize=" + sampleSize);
        if (publisherType != null) {
            url.append("&publisherType=").append(publisherType);
        }
        JsonNode data = getRobot(url.toString(), token);
        List<MarketSkill> skills = new ArrayList<>();
        if (data != null && data.has("records")) {
            for (JsonNode r : data.get("records")) {
                skills.add(new MarketSkill(
                        r.get("bizNo").asText(),
                        r.has("skillName") ? r.get("skillName").asText() : "",
                        r.has("publisherUserId") && r.get("publisherUserId") != null ? r.get("publisherUserId").asLong() : null));
            }
        }
        return skills;
    }

    /**
     * 用指定市场提示词创建自由创作任务，返回任务 ID。
     */
    public Long createGenerationTask(String token, String marketSkillBizNo, String title) {
        Map<String, Object> body = Map.of(
                "title", title,
                "description", "模拟运营自动创作",
                "platform", "wechat",
                "wordCount", 800,
                "skillRef", marketSkillBizNo);
        JsonNode data = postRobot("/api/v1/user/generation-tasks", body, token);
        return data.get("id").asLong();
    }

    /**
     * 查询生成任务状态：2=完成 3=失败。
     */
    public int generationTaskStatus(String token, Long taskId) {
        JsonNode data = getRobot("/api/v1/user/generation-tasks/" + taskId, token);
        return data.get("status").asInt();
    }

    /** 生成完成后的文章 bizNo。 */
    public String generationTaskArticleBizNo(String token, Long taskId) {
        JsonNode data = getRobot("/api/v1/user/generation-tasks/" + taskId, token);
        return data.has("articleBizNo") && !data.get("articleBizNo").isNull()
                ? data.get("articleBizNo").asText() : null;
    }

    /** 随机取一个投稿中的约稿任务 ID；无则 null。 */
    public Long randomOpenCommissionTaskId(String token) {
        JsonNode data = getRobot("/api/v1/user/commission/tasks?status=submission&page=1&pageSize=20", token);
        if (data == null || !data.has("records") || data.get("records").isEmpty()) {
            return null;
        }
        JsonNode records = data.get("records");
        return records.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(records.size())).get("id").asLong();
    }

    public void submitCommission(String token, Long commissionTaskId, String articleBizNo) {
        postRobot("/api/v1/user/commission/tasks/" + commissionTaskId + "/submissions",
                Map.of("articleBizNo", articleBizNo), token);
    }

    // ---------- HTTP 基础 ----------

    private JsonNode postInternal(String path, Object body) {
        HttpHeaders headers = internalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Result> response = restTemplate.postForEntity(baseUrl + path, new HttpEntity<>(body, headers), Result.class);
        return checkedData(response.getBody(), path);
    }

    private JsonNode getInternal(String path) {
        ResponseEntity<Result> response = restTemplate.exchange(baseUrl + path, HttpMethod.GET,
                new HttpEntity<>(internalHeaders()), Result.class);
        return checkedData(response.getBody(), path);
    }

    private JsonNode postRobot(String path, Object body, String token) {
        return postRobot(path, body, token, false);
    }

    private JsonNode postRobot(String path, Object body, String token, boolean withInternalKey) {
        HttpHeaders headers = robotHeaders(token);
        if (withInternalKey) {
            headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Result> response = restTemplate.postForEntity(baseUrl + path, new HttpEntity<>(body, headers), Result.class);
        return checkedData(response.getBody(), path);
    }

    private void putRobot(String path, Object body, String token) {
        HttpHeaders headers = robotHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Result> response = restTemplate.exchange(baseUrl + path, HttpMethod.PUT,
                new HttpEntity<>(body, headers), Result.class);
        checkResult(response.getBody(), path);
    }

    private JsonNode getRobot(String path, String token) {
        ResponseEntity<Result> response = restTemplate.exchange(baseUrl + path, HttpMethod.GET,
                new HttpEntity<>(robotHeaders(token)), Result.class);
        return checkedData(response.getBody(), path);
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateAccessToken(0L));
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);
        return headers;
    }

    private HttpHeaders robotHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private JsonNode checkedData(Result result, String path) {
        checkResult(result, path);
        JsonNode root = objectMapper.valueToTree(result.getData());
        return root == null || root.isNull() ? null : root;
    }

    private void checkResult(Result result, String path) {
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            String msg = result == null ? "empty response" : String.valueOf(result.getMessage());
            int code = result == null || result.getCode() == null ? 500 : result.getCode();
            // 机器人 token 过期：映射为 401，由 RobotTokenHolder 重新登录后重试一次
            if (code == UserAuthErrorCode.TOKEN_EXPIRED.getCode()) {
                throw new BusinessException(401, "robot token expired: " + path);
            }
            throw new BusinessException(500, "模拟运营调用失败 " + path + ": " + msg);
        }
    }
}
