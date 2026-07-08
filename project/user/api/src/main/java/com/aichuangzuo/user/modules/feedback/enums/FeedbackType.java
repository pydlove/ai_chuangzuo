package com.aichuangzuo.user.modules.feedback.enums;

public enum FeedbackType {
    功能建议, 问题反馈, 其他;

    public static boolean isValid(String label) {
        for (FeedbackType t : values()) if (t.name().equals(label)) return true;
        return false;
    }
}
