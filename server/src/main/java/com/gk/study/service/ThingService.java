package com.gk.study.service;


import com.gk.study.entity.Thing;

import java.math.BigDecimal;
import java.util.List;

public interface ThingService {
    List<Thing> getThingList(String keyword, String sort, String c, String tag);

    List<Thing> getThingListNew(String keyword, String sort, Long classificationId, Long tag,
                                Double userLat, Double userLng, Double distanceKm,
                                BigDecimal minPrice, BigDecimal maxPrice, Integer minScore);

    void createThing(Thing thing);
    void deleteThing(String id);

    void updateThing(Thing thing);

    Thing getThingById(String id);

    void addWishCount(String thingId);

    void addCollectCount(String thingId);

    List<Thing> getUserThing(String userId);
}
