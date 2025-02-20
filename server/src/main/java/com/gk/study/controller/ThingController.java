package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.entity.Thing;
import com.gk.study.entity.ThingCollect;
import com.gk.study.entity.User;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.ServiceProviderService;
import com.gk.study.service.ThingCollectService;
import com.gk.study.service.ThingService;
import com.gk.study.service.UserService;
import com.gk.study.userenum.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/thing")
public class ThingController {

    private final static Logger logger = LoggerFactory.getLogger(ThingController.class);

    @Autowired
    ThingService thingService;

    @Autowired
    ServiceProviderService serviceProviderService;

    @Autowired
    ThingCollectService thingCollectService;

    @Autowired
    UserService userService;

    @Value("${File.uploadPath}")
    private String uploadPath;

    /**
     * 更新后的 list 方法，支持多种筛选条件
     */
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort, // 如: recent, hot, recommend
            @RequestParam(required = false) Long classificationId,
            @RequestParam(required = false) Long tag,
            @RequestParam(required = false) Double latitude,  // 用户纬度
            @RequestParam(required = false) Double longitude, // 用户经度
            @RequestParam(required = false, defaultValue = "10") Double distanceKm, // 过滤距离，默认10公里
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minScore
    ) {
        logger.info("Listing things with filters - keyword: {}, sort: {}, classificationId: {}, tag: {}, latitude: {}, longitude: {}, distanceKm: {}, minPrice: {}, maxPrice: {}, minScore: {}",
                keyword, sort, classificationId, tag, latitude, longitude, distanceKm, minPrice, maxPrice, minScore);

        List<Thing> list = thingService.getThingListNew(keyword, sort, classificationId, tag,
                latitude, longitude, distanceKm,
                minPrice, maxPrice, minScore);

        // 如果查询结果为空
        if (list == null || list.isEmpty()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "暂无家政服务数据", list)
            );
        }

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", list)
        );
    }

    @GetMapping("/detail")
    public ResponseEntity<APIResponse<?>> detail(@RequestParam Long id) {
        logger.info("Fetching detail for thing ID: {}", id);
        Thing thing = thingService.getThingById(id.toString());

        // 如果查询不到对应服务
        if (thing == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "该家政服务不存在或已被删除")
            );
        }

        // 获取服务提供者信息
        ServiceProvider provider = serviceProviderService.getServiceProviderByUserId(thing.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("thing", thing);
        result.put("provider", provider);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", result)
        );
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<APIResponse<?>> create(@ModelAttribute Thing thing) throws IOException {
        logger.info("Creating thing: {}", thing);

        User currentUser = userService.getUserDetail(String.valueOf(thing.userId));
        if (currentUser == null || currentUser.getRole() != UserRole.NORMAL_USER.getCode() || currentUser.getRole() != UserRole.ADMIN.getCode() ) {
            // 如果当前用户不是服务提供者，返回权限不足的错误
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "权限不足，只有服务提供者才能发布家政服务")
            );
        }
        // 1. 校验必填字段及格式
        if (thing.getTitle() == null || thing.getTitle().trim().isEmpty() ||
                !thing.getTitle().matches(".*[a-zA-Z\u4e00-\u9fa5]+.*")) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "标题不能为空且必须包含汉字或英文字符")
            );
        }

        if (thing.getPrice() == null || thing.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "价格必须为正数")
            );
        }

        if (thing.getMobile() == null || thing.getMobile().trim().isEmpty() ||
                !thing.getMobile().matches("^[0-9]+$")) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "手机号码不能为空且必须全部为数字")
            );
        }

        if (thing.getAge() == null || thing.getAge().trim().isEmpty() ||
                !thing.getAge().matches("^[1-9][0-9]*$")) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "年龄不能为空且必须为正整数")
            );
        }

        if (thing.getSex() == null || !(thing.getSex().equals("男") || thing.getSex().equals("女"))) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "性别必须为'男'或'女'")
            );
        }

        if (thing.getLocation() == null || thing.getLocation().trim().isEmpty()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "服务地址(location)不能为空")
            );
        }

        if (thing.getLatitude() == null || thing.getLatitude() <= 0) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "Latitude不能为空且必须为正数")
            );
        }

        if (thing.getLongitude() == null || thing.getLongitude() <= 0) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "Longitude不能为空且必须为正数")
            );
        }

        if (thing.getUserId() == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "发布服务的用户ID不能为空")
            );
        }
        // 通过 userService 检查 userId 合法性（此处假设返回 null 则非法）
        User publisher = userService.getUserDetail(thing.getUserId().toString());
        if (publisher == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "发布服务的用户不存在")
            );
        }

        // 2. 设置默认值
        // status 默认为 "0" 表示新发布服务可用
        if (thing.getStatus() == null || thing.getStatus().trim().isEmpty()) {
            thing.setStatus("0");
        }
        // score 初始为 "0"
        if (thing.getScore() == null || thing.getScore().trim().isEmpty()) {
            thing.setScore("0");
        }
        // wishCount 废弃字段，统一设为 0
        thing.setWishCount(0);
        // collectCount 初始为 0
        thing.setCollectCount(0);

        // 3. 处理封面图片上传 (可选)
        String url = saveThing(thing);
        if (!StringUtils.isEmpty(url)) {
            thing.setCover(url);
        }

        // 4. 调用 Service 创建家政服务
        thingService.createThing(thing);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "创建成功")
        );
    }


    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/delete")
    public ResponseEntity<APIResponse<?>> delete(@RequestParam String ids) {
        logger.info("Deleting things with IDs: {}", ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            thingService.deleteThing(id);
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "删除成功")
        );
    }

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<APIResponse<?>> update(@ModelAttribute Thing thing) throws IOException {
        logger.info("Updating thing: {}", thing);
        String url = saveThing(thing);
        if (!StringUtils.isEmpty(url)) {
            thing.setCover(url);
        }

        thingService.updateThing(thing);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
        );
    }

    /**
     * 保存家政服务的封面图片，并返回文件名
     */
    public String saveThing(Thing thing) throws IOException {
        MultipartFile file = thing.getImageFile();
        String newFileName = null;
        if (file != null && !file.isEmpty()) {
            // 存文件
            String oldFileName = file.getOriginalFilename();
            String randomStr = UUID.randomUUID().toString();
            newFileName = randomStr + oldFileName.substring(oldFileName.lastIndexOf("."));
            String filePath = uploadPath + File.separator + "image" + File.separator + newFileName;
            File destFile = new File(filePath);
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            file.transferTo(destFile);
        }
        if (!StringUtils.isEmpty(newFileName)) {
            thing.setCover(newFileName);
        }
        return newFileName;
    }

    /**
     * 收藏家政服务
     */
    @Access(level = AccessLevel.LOGIN)
    @PostMapping("/collect")
    @Transactional
    public ResponseEntity<APIResponse<?>> collect(@RequestBody ThingCollect thingCollect) throws IOException {
        // 判断是否已收藏
        if (thingCollectService.getThingCollect(thingCollect.getUserId(), thingCollect.getThingId()) != null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "您已收藏过了")
            );
        }
        // 未收藏则进行收藏
        thingCollectService.createThingCollect(thingCollect);
        thingService.addCollectCount(thingCollect.getThingId());

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "收藏成功")
        );
    }

    /**
     * 取消收藏家政服务
     */
    @Access(level = AccessLevel.LOGIN)
    @PostMapping("/unCollect")
    @Transactional
    public ResponseEntity<APIResponse<?>> unCollect(@RequestParam Long id) throws IOException {
        thingCollectService.deleteThingCollect(String.valueOf(id));
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "取消收藏成功")
        );
    }

    /**
     * 获取用户收藏的家政服务列表
     */
    @Access(level = AccessLevel.LOGIN)
    @GetMapping("/getUserCollectList")
    public ResponseEntity<APIResponse<?>> getUserCollectList(@RequestParam Long userId) throws IOException {
        List<Map> collectList = thingCollectService.getThingCollectList(String.valueOf(userId));

        // 如果收藏列表为空
        if (collectList == null || collectList.isEmpty()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "您还没有收藏任何家政服务", Collections.emptyList())
            );
        }

        // 转换为详细的 Thing 信息列表
        List<Thing> things = collectList.stream()
                .map(collect -> thingService.getThingById(collect.get("thing_id").toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 如果things依旧为空，可能所有已收藏的id都无效或已被删除
        if (things.isEmpty()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "暂无可展示的已收藏家政服务", things)
            );
        }

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "获取成功", things)
        );
    }

    /**
     * 获取用户发布的家政服务列表
     */
    @GetMapping("/listUserThing")
    public ResponseEntity<APIResponse<?>> listUserThing(@RequestParam Long userId) {
        logger.info("Listing things for user ID: {}", userId);
        List<Thing> list = thingService.getUserThing(String.valueOf(userId));

        if (list == null || list.isEmpty()) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "该用户尚未发布家政服务", list)
            );
        }

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", list)
        );
    }
}
