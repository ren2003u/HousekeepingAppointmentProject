package com.gk.study.entity;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String rePassword;   // 再次输入密码，用于校验
    private String phone;
    private String email;

    // 若你想一次性兼容“普通注册” & “微信登录注册”:
    // 若前端传了 wechatCode，表示走微信注册逻辑；若没传，则走普通注册
    private String wechatCode;

    // 其他你需要的字段...
    // private String nickname;
    // private String avatar;
    // ...

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRePassword() {
        return rePassword;
    }

    public void setRePassword(String rePassword) {
        this.rePassword = rePassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWechatCode() {
        return wechatCode;
    }

    public void setWechatCode(String wechatCode) {
        this.wechatCode = wechatCode;
    }
}
