package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.entity.Thing;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.ServiceProviderService;
import com.gk.study.service.ThingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/thing")
public class ThingController {

    private final static Logger logger = LoggerFactory.getLogger(ThingController.class);

    @Autowired
    ThingService service;

    @Autowired
    ServiceProviderService serviceProviderService;

    @Value("${File.uploadPath}")
    private String uploadPath;

    /**
     * 更新后的 list 方法，支持多种筛选条件
     */
    @GetMapping("/list")
    public APIResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort, // 如: recent, hot, recommend
            @RequestParam(required = false) Long classificationId,
            @RequestParam(required = false) Long tag,
            @RequestParam(required = false) Double latitude, // 用户纬度
            @RequestParam(required = false) Double longitude, // 用户经度
            @RequestParam(required = false, defaultValue = "10") Double distanceKm, // 过滤距离，默认10公里
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minScore
    ){
        logger.info("Listing things with filters - keyword: {}, sort: {}, classificationId: {}, tag: {}, latitude: {}, longitude: {}, distanceKm: {}, minPrice: {}, maxPrice: {}, minScore: {}",
                keyword, sort, classificationId, tag, latitude, longitude, distanceKm, minPrice, maxPrice, minScore);
        List<Thing> list = service.getThingListNew(keyword, sort, classificationId, tag, latitude, longitude, distanceKm, minPrice, maxPrice, minScore);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @GetMapping("/detail")
    public APIResponse detail(@RequestParam Long id){
        logger.info("Fetching detail for thing ID: {}", id);
        Thing thing = service.getThingById(id.toString());

        // 获取服务提供者信息
        ServiceProvider provider = serviceProviderService.getServiceProviderByUserId(thing.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("thing", thing);
        result.put("provider", provider);

        return new APIResponse(ResponeCode.SUCCESS, "查询成功", result);
    }

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/create")
    @Transactional
    public APIResponse create(@ModelAttribute Thing thing) throws IOException {
        logger.info("Creating thing: {}", thing);
        String url = saveThing(thing);
        if(!StringUtils.isEmpty(url)) {
            thing.setCover(url);
        }

        service.createThing(thing);
        return new APIResponse(ResponeCode.SUCCESS, "创建成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/delete")
    public APIResponse delete(@RequestParam String ids){
        logger.info("Deleting things with IDs: {}", ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            service.deleteThing(id);
        }
        return new APIResponse(ResponeCode.SUCCESS, "删除成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/update")
    @Transactional
    public APIResponse update(@ModelAttribute Thing thing) throws IOException {
        logger.info("Updating thing: {}", thing);
        String url = saveThing(thing);
        if(!StringUtils.isEmpty(url)) {
            thing.setCover(url);
        }

        service.updateThing(thing);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }

    /**
     * 保存家政服务的封面图片，并返回文件名
     */
    public String saveThing(Thing thing) throws IOException {
        MultipartFile file = thing.getImageFile();
        String newFileName = null;
        if(file != null && !file.isEmpty()) {
            // 存文件
            String oldFileName = file.getOriginalFilename();
            String randomStr = UUID.randomUUID().toString();
            newFileName = randomStr + oldFileName.substring(oldFileName.lastIndexOf("."));
            String filePath = uploadPath + File.separator + "image" + File.separator + newFileName;
            File destFile = new File(filePath);
            if(!destFile.getParentFile().exists()){
                destFile.getParentFile().mkdirs();
            }
            file.transferTo(destFile);
        }
        if(!StringUtils.isEmpty(newFileName)) {
            thing.setCover(newFileName);
        }
        return newFileName;
    }

    @GetMapping("/listUserThing")
    public APIResponse listUserThing(@RequestParam Long userId){
        logger.info("Listing things for user ID: {}", userId);
        List<Thing> list = service.getUserThing(String.valueOf(userId));
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }
}
