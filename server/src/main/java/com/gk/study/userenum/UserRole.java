package com.gk.study.userenum;

public enum UserRole {
    NORMAL_USER(0, "普通用户"),
    SERVICE_PROVIDER(1, "家政服务提供者"),
    ADMIN(2, "管理员");

    private final int code;
    private final String description;

    UserRole(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.getCode() == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
