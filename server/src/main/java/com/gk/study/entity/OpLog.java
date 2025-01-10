package com.gk.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("b_op_log")
public class OpLog implements Serializable {
    @TableId(value = "id",type = IdType.AUTO)
    public Long id;
    @TableField
    public String reIp;
    @TableField
    public String reTime;
    @TableField
    public String reUa;
    @TableField
    public String reUrl;
    @TableField
    public String reMethod;
    @TableField
    public String reContent;
    @TableField
    public String accessTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReIp() {
        return reIp;
    }

    public void setReIp(String reIp) {
        this.reIp = reIp;
    }

    public String getReTime() {
        return reTime;
    }

    public void setReTime(String reTime) {
        this.reTime = reTime;
    }

    public String getReUa() {
        return reUa;
    }

    public void setReUa(String reUa) {
        this.reUa = reUa;
    }

    public String getReUrl() {
        return reUrl;
    }

    public void setReUrl(String reUrl) {
        this.reUrl = reUrl;
    }

    public String getReMethod() {
        return reMethod;
    }

    public void setReMethod(String reMethod) {
        this.reMethod = reMethod;
    }

    public String getReContent() {
        return reContent;
    }

    public void setReContent(String reContent) {
        this.reContent = reContent;
    }

    public String getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(String accessTime) {
        this.accessTime = accessTime;
    }
}
