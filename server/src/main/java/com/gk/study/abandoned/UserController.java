package com.gk.study.abandoned;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.RegisterRequest;
import com.gk.study.entity.User;
import com.gk.study.entity.WeChatOpenInfo;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    String salt = "abcd1234";

    @Autowired
    UserService userService;

    // 注入 BCryptPasswordEncoder
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${File.uploadPath}")
    private String uploadPath;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public APIResponse list(String keyword){
        List<User> list =  userService.getUserList(keyword);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public APIResponse detail(String userId){
        User user =  userService.getUserDetail(userId);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", user);
    }

    // 后台用户登录
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public APIResponse login(User user){
        user.setPassword(DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes()));
        User responseUser =  userService.getAdminUser(user);
        if(responseUser != null) {
            return new APIResponse(ResponeCode.SUCCESS, "查询成功", responseUser);
        }else {
            return new APIResponse(ResponeCode.FAIL, "用户名或密码错误");
        }
    }

    // 普通用户登录
    @RequestMapping(value = "/userLogin", method = RequestMethod.POST)
    public APIResponse userLogin(User user){
        user.setPassword(DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes()));
        User responseUser =  userService.getNormalUser(user);
        if(responseUser != null) {
            return new APIResponse(ResponeCode.SUCCESS, "查询成功", responseUser);
        }else {
            return new APIResponse(ResponeCode.FAIL, "用户名或密码错误");
        }
    }

    // 普通用户注册
    @RequestMapping(value = "/userRegister", method = RequestMethod.POST)
    @Transactional
    public APIResponse userRegister(User user) throws IOException {

        if (!StringUtils.isEmpty(user.getUsername())
                && !StringUtils.isEmpty(user.getPassword())
                && !StringUtils.isEmpty(user.getRePassword())) {
            // 查重
            if(userService.getUserByUserName(user.getUsername()) != null) {
                return new APIResponse(ResponeCode.FAIL, "用户名重复");
            }
            // 验证密码
            if(!user.getPassword().equals(user.getRePassword())) {
                return new APIResponse(ResponeCode.FAIL, "密码不一致");
            }
            String md5Str = DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes());
            // 设置密码
            user.setPassword(md5Str);
            md5Str = DigestUtils.md5DigestAsHex((user.getUsername() + salt).getBytes());
            // 设置token
            user.setToken(md5Str);

            String avatar = saveAvatar(user);
            if(!StringUtils.isEmpty(avatar)) {
                user.avatar = avatar;
            }
            // 设置角色
            user.setRole(User.NormalUser);
            // 设置状态
            user.setStatus("0");
            user.setCreateTime(String.valueOf(System.currentTimeMillis()));

            userService.createUser(user);
            return new APIResponse(ResponeCode.SUCCESS, "创建成功");
        }
        return new APIResponse(ResponeCode.FAIL, "创建失败");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @Transactional
    public APIResponse create(User user) throws IOException {

        if (!StringUtils.isEmpty(user.getUsername()) || !StringUtils.isEmpty(user.getPassword())) {
            // 查重
            if(userService.getUserByUserName(user.getUsername()) != null) {
                return new APIResponse(ResponeCode.FAIL, "用户名重复");
            }
            String md5Str = DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes());
            // 设置密码
            user.setPassword(md5Str);
            md5Str = DigestUtils.md5DigestAsHex((user.getUsername() + salt).getBytes());
            // 设置token
            user.setToken(md5Str);
            user.setCreateTime(String.valueOf(System.currentTimeMillis()));

            String avatar = saveAvatar(user);
            if(!StringUtils.isEmpty(avatar)) {
                user.avatar = avatar;
            }
            userService.createUser(user);
            return new APIResponse(ResponeCode.SUCCESS, "创建成功");
        }
        return new APIResponse(ResponeCode.FAIL, "创建失败");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public APIResponse delete(String ids){
        System.out.println("ids===" + ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            userService.deleteUser(id);
        }
        return new APIResponse(ResponeCode.SUCCESS, "删除成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @Transactional
    public APIResponse update(User user) throws IOException {
        // update不能修改密码，故置空
        user.setPassword(null);
        String avatar = saveAvatar(user);
        if(!StringUtils.isEmpty(avatar)) {
            user.avatar = avatar;
        }
        userService.updateUser(user);
        System.out.println(user);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }


    @Access(level = AccessLevel.LOGIN)
    @RequestMapping(value = "/updateUserInfo", method = RequestMethod.POST)
    @Transactional
    public APIResponse updateUserInfo(User user) throws IOException {
        User tmpUser =  userService.getUserDetail(String.valueOf(user.getId()));
        if(tmpUser.getRole() == User.NormalUser){
            // username和password不能改，故置空
            user.setUsername(null);
            user.setPassword(null);
            user.setRole(User.NormalUser);
            String avatar = saveAvatar(user);
            if(!StringUtils.isEmpty(avatar)) {
                user.avatar = avatar;
            }
            userService.updateUser(user);
            return new APIResponse(ResponeCode.SUCCESS, "更新成功");
        }else {
            return new APIResponse(ResponeCode.FAIL, "非法操作");
        }
    }

    @Access(level = AccessLevel.LOGIN)
    @RequestMapping(value = "/updatePwd", method = RequestMethod.POST)
    @Transactional
    public APIResponse updatePwd(String userId, String password, String newPassword) throws IOException {
        User user =  userService.getUserDetail(userId);
        if(user.getRole() == User.NormalUser) {
            String md5Pwd = DigestUtils.md5DigestAsHex((password + salt).getBytes());
            if(user.getPassword().equals(md5Pwd)){
                user.setPassword(DigestUtils.md5DigestAsHex((newPassword + salt).getBytes()));
                userService.updateUser(user);
            }else {
                return new APIResponse(ResponeCode.FAIL, "原密码错误");
            }
            return new APIResponse(ResponeCode.SUCCESS, "更新成功");
        }else {
            return new APIResponse(ResponeCode.FAIL, "非法操作");
        }

    }

    /**
     * 兼容“普通注册”和“微信注册”的示例接口
     * - 如果 request.wechatCode 不为空，则尝试微信注册
     * - 否则就是普通注册
     */
    @PostMapping("/register")
    public APIResponse userRegister(@RequestBody RegisterRequest request) {
        try {
            // 1. 如果前端有传 wechatCode，就走“微信注册逻辑”
            if (!StringUtils.isEmpty(request.getWechatCode())) {
                return handleWeChatRegister(request);
            } else {
                // 2. 否则走“普通注册逻辑”
                return handleNormalRegister(request);
            }
        } catch (Exception e) {
            // 捕获异常并返回你们自定义的APIResponse
            return new APIResponse(ResponeCode.FAIL, "注册失败: " + e.getMessage());
        }
    }

    /**
     * [A] 普通注册逻辑
     */
    private APIResponse handleNormalRegister(RegisterRequest request) {
        // 1. 校验必填项
        if (StringUtils.isEmpty(request.getUsername()) ||
                StringUtils.isEmpty(request.getPassword()) ||
                StringUtils.isEmpty(request.getRePassword())) {
            return new APIResponse(ResponeCode.FAIL, "缺少必要字段");
        }
        // 2. 校验用户名是否重复
        User userInDb = userService.getUserByUserName(request.getUsername());
        if (userInDb != null) {
            return new APIResponse(ResponeCode.FAIL, "用户名已被占用");
        }
        // 3. 验证两次密码是否一致
        if (!request.getPassword().equals(request.getRePassword())) {
            return new APIResponse(ResponeCode.FAIL, "两次输入的密码不一致");
        }
        // 4. 使用 BCrypt 进行加密
        String bcryptPwd = passwordEncoder.encode(request.getPassword());

        // 5. 组装 User 对象，插入 DB
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(bcryptPwd);
        newUser.setRole(0); // 假设0=普通用户; 也可存 request 传来的值
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));

        if (!StringUtils.isEmpty(request.getPhone())) {
            newUser.setMobile(request.getPhone());
        }
        if (!StringUtils.isEmpty(request.getEmail())) {
            newUser.setEmail(request.getEmail());
        }

        // 如果需要生成一个 token
        // 这里可以用 UUID 或者自己的一套逻辑
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        userService.createUser(newUser);

        return new APIResponse(ResponeCode.SUCCESS, "普通用户注册成功");
    }

    /**
     * [B] 微信注册逻辑
     */
    private APIResponse handleWeChatRegister(RegisterRequest request) {
        // 1. 根据 wechatCode 调用微信接口，拿到 openid 和 unionid (如果有)
        String openid;
        String unionid = null; // 有些场景会返回 unionid，也可能没返回
        try {
            // 这里是示例方法，你需要自行实现
            WeChatOpenInfo weChatOpenInfo = getWeChatOpenInfo(request.getWechatCode());
            openid = weChatOpenInfo.getOpenid();
            unionid = weChatOpenInfo.getUnionid();
        } catch (Exception e) {
            return new APIResponse(ResponeCode.FAIL, "微信注册失败: " + e.getMessage());
        }

        // 2. 查询本地数据库，看是否已经存在该微信 openid
        User existUser = userService.getUserByWeChatOpenId(openid);
        if (existUser != null) {
            return new APIResponse(ResponeCode.FAIL, "该微信已绑定过账号，请直接登录");
        }

        // 3. 如果不存在，则“自动注册”一个新用户
        User newUser = new User();
        // 3.1 你可以让用户在前端填写 username / nickname，或自动生成一个
        String autoUsername = "wx_" + System.currentTimeMillis();
        newUser.setUsername(autoUsername);

        // 3.2 微信登录不一定有密码，但有时为了兼容账号体系，可以设置一个随机密码
        //     或者干脆留空，视你的业务场景而定
        String randomPwd = "WX_" + UUID.randomUUID(); // 仅示例
        newUser.setPassword(passwordEncoder.encode(randomPwd));

        // 3.3 存储 openid, unionid 到对应字段
        newUser.setWechatOpenid(openid);
        newUser.setWechatUnionid(unionid);

        // 3.4 其他常规字段
        newUser.setRole(0); // 普通用户
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));
        // 例如取微信昵称/头像，如果你能从微信接口拿到
        newUser.setNickname("微信用户");
        // newUser.setAvatar(...);

        // 3.5 设置 token (若需要)
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        userService.createUser(newUser);

        return new APIResponse(ResponeCode.SUCCESS, "微信注册成功", newUser);
    }

    /**
     * 调用微信API拿到 openid (和 unionid)
     * 下面演示一个返回值对象，你需根据实际接口调试。
     */
    private WeChatOpenInfo getWeChatOpenInfo(String code) throws IOException {
        // 真实情况：调用微信服务器，例如:
        // GET https://api.weixin.qq.com/sns/jscode2session
        //      ?appid=xxx&secret=xxx&js_code={code}&grant_type=authorization_code
        //
        // 解析JSON得到 openid, unionid...
        // 这里只是举例返回假数据
        WeChatOpenInfo info = new WeChatOpenInfo();
        if ("fake_code".equals(code)) {
            info.setOpenid("fake_wechat_openid_123");
            info.setUnionid("fake_wechat_unionid_456");
        } else {
            info.setOpenid("real_openid_" + code);
            info.setUnionid("real_unionid_" + code);
        }
        return info;
    }

    public String saveAvatar(User user) throws IOException {
        MultipartFile file = user.getAvatarFile();
        String newFileName = null;
        if(file !=null && !file.isEmpty()) {

            // 存文件
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
            user.avatar = newFileName;
        }
        return newFileName;
    }
}
