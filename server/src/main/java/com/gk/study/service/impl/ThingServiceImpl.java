package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.Thing;
import com.gk.study.entity.ThingTag;
import com.gk.study.mapper.ThingMapper;
import com.gk.study.mapper.ThingTagMapper;
import com.gk.study.service.ThingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThingServiceImpl extends ServiceImpl<ThingMapper, Thing> implements ThingService {
    @Autowired
    ThingMapper mapper;

    @Autowired
    ThingTagMapper thingTagMapper;

    @Override
    public List<Thing> getThingList(String keyword, String sort, String c, String tag) {
        QueryWrapper<Thing> queryWrapper = new QueryWrapper<>();

        // 搜索
        queryWrapper.like(StringUtils.isNotBlank(keyword), "title", keyword)
                .or().like(StringUtils.isNotBlank(keyword), "description", keyword);

        // 排序
        if (StringUtils.isNotBlank(sort)) {
            if (sort.equals("recent")) {
                queryWrapper.orderBy(true, false, "create_time");
            } else if (sort.equals("hot") || sort.equals("recommend")) {
                queryWrapper.orderBy(true, false, "pv");
            }
        }else {
            queryWrapper.orderBy(true, false, "create_time");
        }

        // 根据分类筛选
        if (StringUtils.isNotBlank(c) && !c.equals("-1")) {
            queryWrapper.eq(true, "classification_id", c);
        }

        List<Thing> things = mapper.selectList(queryWrapper);

        // tag筛选
        if (StringUtils.isNotBlank(tag)) {
            List<Thing> tThings = new ArrayList<>();
            QueryWrapper<ThingTag> thingTagQueryWrapper = new QueryWrapper<>();
            thingTagQueryWrapper.eq("tag_id", tag);
            List<ThingTag> thingTagList = thingTagMapper.selectList(thingTagQueryWrapper);
            for (Thing thing : things) {
                for (ThingTag thingTag : thingTagList) {
                    if (thing.getId().equals(thingTag.getThingId())) {
                        tThings.add(thing);
                    }
                }
            }
            things.clear();
            things.addAll(tThings);
        }

        // 附加tag
        for (Thing thing : things) {
            QueryWrapper<ThingTag> thingTagQueryWrapper = new QueryWrapper<>();
            thingTagQueryWrapper.lambda().eq(ThingTag::getThingId, thing.getId());
            List<ThingTag> thingTags = thingTagMapper.selectList(thingTagQueryWrapper);
            List<Long> tags = thingTags.stream().map(ThingTag::getTagId).collect(Collectors.toList());
            thing.setTags(tags);
        }
        return things;
    }
    @Override
    public List<Thing> getThingListNew(String keyword, String sort, Long classificationId, Long tag,
                                       Double userLat, Double userLng, Double distanceKm,
                                       BigDecimal minPrice, BigDecimal maxPrice, Integer minScore) {
        QueryWrapper<Thing> queryWrapper = new QueryWrapper<>();

        // 1. 关键词模糊搜索 (title OR description)
        if(StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like("title", keyword).or().like("description", keyword)
            );
        }

        // 2. 分类筛选
        if(classificationId != null && classificationId != -1L) {
            queryWrapper.eq("classification_id", classificationId);
        }

        // 3. 标签筛选
        if(tag != null && tag != -1L) {
            // Join with b_thing_tag to filter by tag
            queryWrapper.inSql("id", "SELECT thing_id FROM b_thing_tag WHERE tag_id = " + tag);
        }

        // 4. 价格区间筛选
        if(minPrice != null) {
            queryWrapper.ge("price", minPrice);
        }
        if(maxPrice != null) {
            queryWrapper.le("price", maxPrice);
        }

        // 5. 评分筛选
        if(minScore != null) {
            queryWrapper.ge("score", minScore);
        }

        // 6. 排序
        if(StringUtils.isNotBlank(sort)) {
            switch(sort) {
                case "recent":
                    queryWrapper.orderByDesc("create_time");
                    break;
                case "hot":
                    queryWrapper.orderByDesc("pv");
                    break;
                case "recommend":
                    // Assuming AI recommendation logic is implemented in service
                    // For now, use recommend_count
                    queryWrapper.orderByDesc("recommend_count");
                    break;
                default:
                    queryWrapper.orderByDesc("create_time");
            }
        } else {
            queryWrapper.orderByDesc("create_time");
        }

        // 7. 执行查询
        List<Thing> things = mapper.selectList(queryWrapper);

        // 8. 地理位置筛选 (策略A: 后端Java层面计算距离并过滤)
        if(userLat != null && userLng != null && distanceKm != null) {
            things = things.stream()
                    .filter(thing -> {
                        double distance = calculateDistance(userLat, userLng, thing.getLatitude(), thing.getLongitude());
                        return distance <= distanceKm;
                    })
                    .sorted(Comparator.comparingDouble(thing ->
                            calculateDistance(userLat, userLng, thing.getLatitude(), thing.getLongitude())
                    ))
                    .collect(Collectors.toList());
        }

        // 9. 附加标签列表
        for (Thing thing : things) {
            QueryWrapper<ThingTag> thingTagQueryWrapper = new QueryWrapper<>();
            thingTagQueryWrapper.eq("thing_id", thing.getId());
            List<ThingTag> thingTags = thingTagMapper.selectList(thingTagQueryWrapper);
            List<Long> tags = thingTags.stream().map(ThingTag::getTagId).collect(Collectors.toList());
            thing.setTags(tags);
        }

        // 10. AI 推荐算法 (示例: 根据收藏数和心愿数简单加权)
        // 这里仅作为示例，实际AI推荐应基于更复杂的用户行为和偏好
        if("recommend".equals(sort)) {
            things = things.stream()
                    .sorted(Comparator.comparingDouble(this::calculateRecommendScore).reversed())
                    .collect(Collectors.toList());
        }

        return things;
    }

    /**
     * 示例推荐算法，实际应根据具体需求设计
     */
    private double calculateRecommendScore(Thing thing) {
        // 简单示例: 加权收藏数和心愿数
        return thing.getCollectCount() * 1.0 + thing.getWishCount() * 0.5;
    }


    @Override
    public void createThing(Thing thing) {
        System.out.println(thing);
        thing.setCreateTime(String.valueOf(System.currentTimeMillis()));

        if (thing.getPv() == null) {
            thing.setPv("0");
        }
        if (thing.getScore() == null) {
            thing.setScore("0");
        }
        mapper.insert(thing);
        // 更新tag
        setThingTags(thing);
    }

    @Override
    public void deleteThing(String id) {
        mapper.deleteById(id);
    }

    @Override
    public void updateThing(Thing thing) {

        // 更新tag
        setThingTags(thing);

        mapper.updateById(thing);
    }

    @Override
    public Thing getThingById(String id) {
        Thing thing = mapper.selectById(id);
        thing.setPv(String.valueOf(Integer.parseInt(thing.getPv()) + 1));
        mapper.updateById(thing);

        return thing;
    }

    // 心愿数加1
    @Override
    public void addWishCount(String thingId) {
        Thing thing = mapper.selectById(thingId);
        thing.setWishCount(thing.getWishCount() + 1);
        mapper.updateById(thing);
    }

    // 收藏数加1
    @Override
    public void addCollectCount(String thingId) {
        Thing thing = mapper.selectById(thingId);
        thing.setCollectCount(thing.getCollectCount() + 1);
        mapper.updateById(thing);
    }

    @Override
    public List<Thing> getUserThing(String userId) {
        QueryWrapper<Thing> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return mapper.selectList(queryWrapper);
    }

    public void setThingTags(Thing thing) {
        // 删除tag
        Map<String, Object> map = new HashMap<>();
        map.put("thing_id", thing.getId());
        thingTagMapper.deleteByMap(map);
        // 新增tag
        if (thing.getTags() != null) {
            for (Long tag : thing.getTags()) {
                ThingTag thingTag = new ThingTag();
                thingTag.setThingId(thing.getId());
                thingTag.setTagId(tag);
                thingTagMapper.insert(thingTag);
            }
        }
    }
    /**
     * 计算两点之间的距离（公里）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371; // 地球半径，单位公里
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}
