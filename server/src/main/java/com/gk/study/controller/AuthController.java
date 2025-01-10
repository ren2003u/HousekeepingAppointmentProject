package com.gk.study.controller;

import com.gk.study.entity.LoginRequest;
import com.gk.study.entity.PhoneLoginRequest;
import com.gk.study.entity.RegisterRequest;
import com.gk.study.entity.WeChatLoginRequest;
import com.gk.study.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. 构造用户名密码登录token
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        // 2. 执行认证
        Authentication authentication = authenticationManager.authenticate(authToken);
        // 3. 若认证成功, 生成JWT并返回
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(request.getUsername());

        Map<String, String> result = new HashMap<>();
        result.put("token", jwt);
        return ResponseEntity.ok(result);
    }

    // ================ 2. 手机验证码登录 =================
    @PostMapping("/loginByPhone")
    public ResponseEntity<?> loginByPhone(@RequestBody PhoneLoginRequest request) {
        // 假设已在前端拿到短信验证码, 并后端对验证码进行校验(需自己实现)
        boolean pass = checkSmsCode(request.getPhone(), request.getSmsCode());
        if (!pass) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("验证码错误");
        }
        // 如果校验通过, 根据phone加载DB里的User
        // 并生成Token或自动注册
        String username = loadUsernameByPhone(request.getPhone());
        String jwt = jwtUtil.generateToken(username);

        return ResponseEntity.ok(Collections.singletonMap("token", jwt));
    }

    // ================ 3. 微信登录 (OAuth2) =================
    @PostMapping("/loginByWeChat")
    public ResponseEntity<?> loginByWeChat(@RequestBody WeChatLoginRequest request) {
        // 1. 前端拿到微信code, 后端通过微信API获取openId
        String openId = getWeChatOpenId(request.getCode());
        // 2. DB查找用户，若不存在则自动注册(或提示绑定)
        String username = loadOrCreateUserByWeChatOpenId(openId);
        // 3. 生成token并返回
        String jwt = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Collections.singletonMap("token", jwt));
    }

    // ================ 4. 注册接口(可选) =================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // 校验用户名是否已被占用，手机号、邮箱等
        // 生成BCrypt哈希密码等
        // userMapper.insert(...)
        // 处理成功后返回
        return ResponseEntity.ok("注册成功");
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
