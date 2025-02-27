package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.*;
import com.gk.study.jwt.JwtUtil;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.UserService;
import com.gk.study.userenum.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/userAuth")
public class UserAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${File.uploadPath}")
    private String uploadPath;

    // ================== 用户列表、详情（后台/通用接口） ==================
    @Operation(
            summary = "获取用户列表",
            description = "查询用户列表，可以根据关键词进行模糊搜索。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "400", description = "查询参数不合法")
            }
    )
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> list( @Parameter(description = "查询的关键词，可选") @RequestParam(required=false) String keyword){
        List<User> list = userService.getUserList(keyword);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", list)
        );
    }

    @Operation(
            summary = "获取用户详情",
            description = "根据用户ID查询用户详情。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "404", description = "用户不存在")
            }
    )
    @GetMapping("/detail")
    public ResponseEntity<APIResponse<?>> detail(@Parameter(description = "用户ID", required = true)  @RequestParam String userId){
        User user = userService.getUserDetail(userId);
        if(user == null){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", user)
        );
    }

    // ================== 用户注册（普通 / 微信 / 其他） ==================

    /**
     * 普通用户注册 (结合了之前 userRegister + handleNormalRegister 的逻辑)
     */
    @Operation(
            summary = "用户注册",
            description = "支持普通注册和微信注册。根据提供的微信代码或普通注册信息进行用户注册。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "注册成功"),
                    @ApiResponse(responseCode = "400", description = "注册失败")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<APIResponse<?>> register(@Parameter(description = "注册请求数据", required = true) @RequestBody RegisterRequest request) {
        try {
            // 如果有 wechatCode，就优先走微信注册，否则走普通注册
            if (!StringUtils.isEmpty(request.getWechatCode())) {
                return ResponseEntity.ok(handleWeChatRegister(request));
            } else {
                return ResponseEntity.ok(handleNormalRegister(request));
            }
        } catch (Exception e) {
            logger.error("注册失败:", e);
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "注册失败: " + e.getMessage())
            );
        }
    }

    /**
     * [A] 普通注册逻辑
     */
    private APIResponse<?> handleNormalRegister(RegisterRequest request) {
        logger.info("handleNormalRegister : {}", request);
        // 1. 校验必填项
        if (StringUtils.isEmpty(request.getUsername()) ||
                StringUtils.isEmpty(request.getPassword()) ||
                StringUtils.isEmpty(request.getRePassword())) {
            return new APIResponse<>(ResponeCode.FAIL, "缺少必要字段");
        }
        // 2. 校验用户名是否重复
        User userInDb = userService.getUserByUserName(request.getUsername());
        if (userInDb != null) {
            return new APIResponse<>(ResponeCode.FAIL, "用户名已被占用");
        }
        // 3. 验证两次密码是否一致
        if (!request.getPassword().equals(request.getRePassword())) {
            return new APIResponse<>(ResponeCode.FAIL, "两次输入的密码不一致");
        }
        // 4. 使用BCrypt加密
        String bcryptPwd = passwordEncoder.encode(request.getPassword());

        // 5. 组装User对象
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(bcryptPwd);
        // 这里设定普通用户角色
        newUser.setRole(UserRole.NORMAL_USER.getCode());
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));
        newUser.setMobile(request.getPhone());
        newUser.setEmail(request.getEmail());
        // 生成一个 token (可选)
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        userService.createUser(newUser);
        return new APIResponse<>(ResponeCode.SUCCESS, "普通用户注册成功", newUser);
    }

    /**
     * [B] 微信注册逻辑
     */
    private APIResponse<?> handleWeChatRegister(RegisterRequest request) throws IOException {
        // 1. 根据wechatCode换取openid
        String openid = getWeChatOpenId(request.getWechatCode());
        // 2. 查数据库，看是否已经绑定
        User existUser = userService.getUserByWeChatOpenId(openid);
        if(existUser != null){
            return new APIResponse<>(ResponeCode.FAIL, "该微信已绑定过账号，请直接登录");
        }
        // 3. 自动注册
        User newUser = new User();
        String autoUsername = "wx_" + System.currentTimeMillis();
        newUser.setUsername(autoUsername);
        // 可以设置随机密码
        newUser.setPassword(passwordEncoder.encode("WX_" + UUID.randomUUID()));

        newUser.setWechatOpenid(openid);
        newUser.setRole(User.NormalUser);
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));
        // 其他信息 ...
        userService.createUser(newUser);

        return new APIResponse<>(ResponeCode.SUCCESS, "微信注册成功", newUser);
    }

    private String getWeChatOpenId(String code) throws IOException {
        // TODO: 与微信服务器交互获取 openid
        // 示例直接返回一个假 openid
        return "fake_openid_" + code;
    }

    // ================== 登录接口（来自原 AuthController） ==================

    /**
     * 用户名+密码登录
     */
    @Operation(
            summary = "用户名和密码登录",
            description = "通过用户名和密码进行登录，成功后返回JWT令牌。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "401", description = "用户名或密码错误")
            }
    )
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
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名或密码错误")
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "登录失败: " + e.getMessage())
            );
        }

        // 3. 若认证成功, 生成JWT并返回
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(request.getUsername());

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", request.getUsername());
        dataMap.put("message", "登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "登录成功", dataMap)
        );
    }

    /**
     * 手机号验证码登录
     */
    @Operation(
            summary = "手机号验证码登录",
            description = "通过手机号和验证码登录，成功后返回JWT令牌。(暂时未完成)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "验证码错误")
            }
    )
    @PostMapping("/loginByPhone")
    public ResponseEntity<APIResponse<?>> loginByPhone(@Parameter(description = "手机号登录请求数据", required = true) @RequestBody PhoneLoginRequest request) {
        boolean pass = checkSmsCode(request.getPhone(), request.getSmsCode());
        if (!pass) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "验证码错误")
            );
        }
        // DB查询或自动注册
        String username = loadUsernameByPhone(request.getPhone());
        String jwt = jwtUtil.generateToken(username);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("phone", request.getPhone());
        dataMap.put("message", "手机登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "手机登录成功", dataMap)
        );
    }

    /**
     * 微信登录
     */
    @Operation(
            summary = "微信登录",
            description = "通过微信登录，提供微信code获取openId，若用户不存在则自动注册。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "微信登录失败")
            }
    )
    @PostMapping("/loginByWeChat")
    public ResponseEntity<APIResponse<?>> loginByWeChat(@Parameter(description = "微信登录请求数据", required = true)  @RequestBody WeChatLoginRequest request) {
        String openId;
        try {
            openId = getWeChatOpenId(request.getCode());
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "微信登录失败: " + e.getMessage())
            );
        }
        // 若不存在则自动注册
        String username = loadOrCreateUserByWeChatOpenId(openId);
        String jwt = jwtUtil.generateToken(username);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", username);
        dataMap.put("message", "微信登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "微信登录成功", dataMap)
        );
    }

    private boolean checkSmsCode(String phone, String smsCode) {
        // TODO: 调用你的短信服务检查
        return true;
    }
    private String loadUsernameByPhone(String phone) {
        // TODO: 查数据库, 若无则自动新建
        return "userOf_" + phone;
    }
    private String loadOrCreateUserByWeChatOpenId(String openid) {
        // TODO: 查数据库, 若不存在则注册
        return "userOf_" + openid;
    }

    // ================== 其他用户管理接口（后台 / 普通） ==================

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/create")
    @Transactional
    @Operation(
            summary = "管理员创建用户",
            description = "管理员通过接口创建用户，创建过程中会进行用户名校验。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "创建成功"),
                    @ApiResponse(responseCode = "400", description = "创建失败")
            }
    )
    public ResponseEntity<APIResponse<?>> create(@Parameter(description = "用户信息", required = true) @RequestBody User user) throws IOException {
        // 管理员创建用户
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名或密码不能为空")
            );
        }
        // 查重
        if(userService.getUserByUserName(user.getUsername()) != null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名重复")
            );
        }
        String bcryptPwd = passwordEncoder.encode(user.getPassword());
        user.setPassword(bcryptPwd);
        user.setCreateTime(String.valueOf(System.currentTimeMillis()));
        user.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        // 这里可以处理头像
        // ...

        userService.createUser(user);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "创建成功")
        );
    }

    @Access(level = AccessLevel.ADMIN)
    @Operation(
            summary = "删除用户",
            description = "管理员删除指定用户。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功"),
                    @ApiResponse(responseCode = "404", description = "用户不存在")
            }
    )
    @PostMapping("/delete")
    public ResponseEntity<APIResponse<?>> delete(@Parameter(description = "用户ID列表", required = true) @RequestParam String ids){
        String[] arr = ids.split(",");
        for (String id : arr) {
            userService.deleteUser(id);
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "删除成功")
        );
    }

    @Access(level = AccessLevel.ADMIN)
    @Operation(
            summary = "更新用户信息",
            description = "管理员更新用户信息，密码不允许直接修改。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "更新失败")
            }
    )
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<APIResponse<?>> update(@Parameter(description = "用户信息", required = true) @RequestBody User user) throws IOException {
        // 不允许直接改密码
        user.setPassword(null);
        // 处理头像
        // ...
        userService.updateUser(user);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
        );
    }

    @Access(level = AccessLevel.LOGIN)
    @Operation(
            summary = "更新用户信息",
            description = "普通用户更新自己的个人信息，不能修改用户名和密码。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "更新失败")
            }
    )
    @PostMapping("/updateUserInfo")
    @Transactional
    public ResponseEntity<APIResponse<?>> updateUserInfo(@Parameter(description = "用户信息", required = true) @RequestBody User user) throws IOException {
        User tmpUser = userService.getUserDetail(String.valueOf(user.getId()));
        if(tmpUser == null){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }
        if(tmpUser.getRole() == User.NormalUser){
            user.setUsername(null);
            user.setPassword(null);
            user.setRole(User.NormalUser);
            // 处理头像
            // ...
            userService.updateUser(user);
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
            );
        } else {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "非法操作")
            );
        }
    }

    @Access(level = AccessLevel.LOGIN)
    @Operation(
            summary = "更新用户密码",
            description = "普通用户修改自己的密码，要求提供原密码和新密码。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "密码修改失败")
            }
    )
    @PostMapping("/updatePwd")
    @Transactional
    public ResponseEntity<APIResponse<?>> updatePwd(@Parameter(description = "用户ID", required = true) @RequestParam String userId,
                                                    @Parameter(description = "原密码", required = true) @RequestParam String password,
                                                    @Parameter(description = "新密码", required = true) @RequestParam String newPassword) throws IOException {
        User user = userService.getUserDetail(userId);
        if(user == null){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }
        if(user.getRole() == User.NormalUser) {
            // 校验旧密码
            if(!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.ok(
                        new APIResponse<>(ResponeCode.FAIL, "原密码错误")
                );
            }
            // 更新为新密码
            String bcryptPwd = passwordEncoder.encode(newPassword);
            user.setPassword(bcryptPwd);
            userService.updateUser(user);
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
            );
        } else {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "非法操作")
            );
        }
    }

    // 如果需要保存头像
    private String saveAvatar(User user) throws IOException {
        MultipartFile file = user.getAvatarFile();
        String newFileName = null;
        if(file != null && !file.isEmpty()) {
            String oldFileName = file.getOriginalFilename();
            String randomStr = UUID.randomUUID().toString();
            newFileName = randomStr + oldFileName.substring(oldFileName.lastIndexOf("."));
            String filePath = uploadPath + File.separator + "avatar" + File.separator + newFileName;
            File destFile = new File(filePath);
            if(!destFile.getParentFile().exists()){
                destFile.getParentFile().mkdirs();
            }
            file.transferTo(destFile);
        }
        if(!StringUtils.isEmpty(newFileName)) {
            user.setAvatar(newFileName);
        }
        return newFileName;
    }
}

