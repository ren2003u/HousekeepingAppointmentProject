package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Thing;
import com.gk.study.entity.ThingCollect;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.ThingCollectService;
import com.gk.study.service.ThingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/thingCollect")
public class ThingCollectController {

    private final static Logger logger = LoggerFactory.getLogger(ThingCollectController.class);

    @Autowired
    ThingCollectService thingCollectService;

    @Autowired
    ThingService thingService;

    /**
     * 收藏家政服务
     */
    @Access(level = AccessLevel.LOGIN)
    @RequestMapping(value = "/collect", method = RequestMethod.POST)
    @Transactional
    public APIResponse collect(@RequestBody ThingCollect thingCollect) throws IOException {
        if(thingCollectService.getThingCollect(thingCollect.getUserId(), thingCollect.getThingId()) != null){
            return new APIResponse(ResponeCode.SUCCESS, "您已收藏过了");
        } else {
            thingCollectService.createThingCollect(thingCollect);
            thingService.addCollectCount(thingCollect.getThingId());
            return new APIResponse(ResponeCode.SUCCESS, "收藏成功");
        }
    }

    /**
     * 取消收藏家政服务
     */
    @Access(level = AccessLevel.LOGIN)
    @RequestMapping(value = "/unCollect", method = RequestMethod.POST)
    @Transactional
    public APIResponse unCollect(@RequestParam Long id) throws IOException {
        thingCollectService.deleteThingCollect(String.valueOf(id));
        return new APIResponse(ResponeCode.SUCCESS, "取消收藏成功");
    }

    /**
     * 获取用户收藏的家政服务列表
     */
    @Access(level = AccessLevel.LOGIN)
    @RequestMapping(value = "/getUserCollectList", method = RequestMethod.GET)
    public APIResponse getUserCollectList(@RequestParam Long userId) throws IOException {
        List<Map> collectList = thingCollectService.getThingCollectList(String.valueOf(userId));

        // 转换为详细的 Thing 信息列表
        List<Thing> things = collectList.stream()
                .map(collect -> thingService.getThingById(collect.get("thing_id").toString()))
                .collect(Collectors.toList());
        return new APIResponse(ResponeCode.SUCCESS, "获取成功", things);
    }
}