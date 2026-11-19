package com.studentissue.complaint_service.controller;

import com.studentissue.complaint_service.dto.MapMarkerResponse;
import com.studentissue.complaint_service.model.Category;
import com.studentissue.complaint_service.model.Status;
import com.studentissue.complaint_service.service.MapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/complaints")
public class MapController {

    private final MapService service;

    public MapController(MapService service) {
        this.service = service;
    }

    @GetMapping("/map")
    public List<MapMarkerResponse> map(@RequestParam double minLat,
                                       @RequestParam double minLng,
                                       @RequestParam double maxLat,
                                       @RequestParam double maxLng,
                                       @RequestParam(required = false) Category category,
                                       @RequestParam(required = false) Status status,
                                       @RequestParam(required = false) Integer limit) {
        return service.inBounds(minLat, minLng, maxLat, maxLng, category, status, limit);
    }

    @GetMapping("/nearby")
    public List<MapMarkerResponse> nearby(@RequestParam double lat,
                                          @RequestParam double lng,
                                          @RequestParam(defaultValue = "500") double radius,
                                          @RequestParam(required = false) Category category,
                                          @RequestParam(required = false) Status status,
                                          @RequestParam(required = false) Integer limit) {
        return service.nearby(lat, lng, radius, category, status, limit);
    }
}