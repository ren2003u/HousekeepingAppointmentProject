package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.LoginRequest;
import com.gk.study.entity.PhoneLoginRequest;
import com.gk.study.entity.RegisterRequest;
import com.gk.study.entity.WeChatLoginRequest;
import com.gk.study.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    // ================ 1. 用户名密码登录 =================
    @PostMapping("/login")
    public ResponseEntity<APIResponse<?>> login(@RequestBody LoginRequest request) {
        // 1. 构造用户名密码登录token
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        // 2. 执行认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException e) {
            // 密码错误或用户名不存在时会抛出这个异常
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名或密码错误")
            );
        } catch (Exception e) {
            // 其他异常
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "登录失败: " + e.getMessage())
            );
        }

        // 3. 若认证成功, 生成JWT并返回
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(request.getUsername());

        // 4. 组织返回内容
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", request.getUsername());
        dataMap.put("message", "登录成功");

        // 5. 返回统一格式的APIResponse
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "登录成功", dataMap)
        );
    }

    // ================ 2. 手机验证码登录 =================
    @PostMapping("/loginByPhone")
    public ResponseEntity<APIResponse<?>> loginByPhone(@RequestBody PhoneLoginRequest request) {
        // 假设已在前端拿到短信验证码, 并后端对验证码进行校验(需自己实现)
        boolean pass = checkSmsCode(request.getPhone(), request.getSmsCode());
        if (!pass) {
            // 这里返回401或者其他状态都行，你项目里可以统一为400
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "验证码错误")
            );
        }
        // 如果校验通过, 根据phone加载DB里的User
        String username = loadUsernameByPhone(request.getPhone());
        String jwt = jwtUtil.generateToken(username);

        // 组织返回
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("phone", request.getPhone());
        dataMap.put("message", "手机验证码登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "手机登录成功", dataMap)
        );
    }

    // ================ 3. 微信登录 (OAuth2) =================
    @PostMapping("/loginByWeChat")
    public ResponseEntity<APIResponse<?>> loginByWeChat(@RequestBody WeChatLoginRequest request) {
        // 1. 前端拿到微信code, 后端通过微信API获取openId
        String openId;
        try {
            openId = getWeChatOpenId(request.getCode());
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "微信登录失败: " + e.getMessage())
            );
        }
        // 2. DB查找用户，若不存在则自动注册(或提示绑定)
        String username = loadOrCreateUserByWeChatOpenId(openId);
        // 3. 生成token并返回
        String jwt = jwtUtil.generateToken(username);

        // 组织返回
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", username);
        dataMap.put("message", "微信登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "微信登录成功", dataMap)
        );
    }

    // ==================== 其他辅助方法 ====================
    private boolean checkSmsCode(String phone, String smsCode) {
        // TODO: 调用你自己的短信服务/Redis校验，示例省略
        return true;
    }
    private String loadUsernameByPhone(String phone) {
        // TODO: DB查询，若不存在可以自动注册
        return "userOf_" + phone;
    }
    private String getWeChatOpenId(String code) {
        // TODO: 根据微信开放平台API，用appid/appsecret + code换取openId
        return "test_openid";
    }
    private String loadOrCreateUserByWeChatOpenId(String openid) {
        // TODO: DB查，没查到就插入一个新user
        return "userOf_" + openid;
    }
}
