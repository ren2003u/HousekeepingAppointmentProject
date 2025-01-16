package com.gk.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
@TableName("b_user")
public class User implements Serializable {
    public static final int NormalUser = 1;
    public static final int DemoUser = 2;
    public static final int AdminUser = 3;

    @TableId(value = "id",type = IdType.AUTO)
    public Long id;
    @TableField
    public String username;
    @TableField
    public String password;
    @TableField(exist = false)
    public String rePassword;
    @TableField
    public String nickname;
    @TableField
    public String mobile;
    @TableField
    public String email;
    @TableField
    public String description;
    @TableField
    public int role;
    @TableField
    public String status;
    @TableField
    public String score;
    @TableField
    public String avatar;
    @TableField(exist = false)
    public MultipartFile avatarFile;
    @TableField
    public String token;
    @TableField
    public String createTime;
    @TableField
    public String pushEmail;
    @TableField
    public String pushSwitch;

    // 新增的
    @TableField
    private String wechatOpenid;
    @TableField
    private String wechatUnionid;

    public String getWechatOpenid() {
        return wechatOpenid;
    }

    public void setWechatOpenid(String wechatOpenid) {
        this.wechatOpenid = wechatOpenid;
    }

    public String getWechatUnionid() {
        return wechatUnionid;
    }

    public void setWechatUnionid(String wechatUnionid) {
        this.wechatUnionid = wechatUnionid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRePassword() {
        return rePassword;
    }

    public void setRePassword(String rePassword) {
        this.rePassword = rePassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public MultipartFile getAvatarFile() {
        return avatarFile;
    }

    public void setAvatarFile(MultipartFile avatarFile) {
        this.avatarFile = avatarFile;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPushEmail() {
        return pushEmail;
    }

    public void setPushEmail(String pushEmail) {
        this.pushEmail = pushEmail;
    }

    public String getPushSwitch() {
        return pushSwitch;
    }

    public void setPushSwitch(String pushSwitch) {
        this.pushSwitch = pushSwitch;
    }
}
