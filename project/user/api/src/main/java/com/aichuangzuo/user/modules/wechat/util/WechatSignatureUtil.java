package com.aichuangzuo.user.modules.wechat.util;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 微信服务器请求签名校验工具。
 */
public final class WechatSignatureUtil {

    private WechatSignatureUtil() {
    }

    /**
     * 校验微信服务器签名。
     *
     * @param token     服务器配置 Token
     * @param signature 微信传来的 signature
     * @param timestamp 微信传来的 timestamp
     * @param nonce     微信传来的 nonce
     * @return 是否通过校验
     */
    public static boolean checkSignature(String token, String signature, String timestamp, String nonce) {
        if (token == null || signature == null || timestamp == null || nonce == null) {
            return false;
        }
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);
        String content = arr[0] + arr[1] + arr[2];
        String sha1 = sha1(content);
        return signature.equalsIgnoreCase(sha1);
    }

    private static String sha1(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 加密失败", e);
        }
    }
}
