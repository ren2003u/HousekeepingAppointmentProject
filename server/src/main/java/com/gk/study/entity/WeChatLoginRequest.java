package com.gk.study.entity;

import lombok.Data;

@Data
public class WeChatLoginRequest {
    // 微信登录时前端会携带的code（用于后端去换取 openId、sessionKey 等）
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
