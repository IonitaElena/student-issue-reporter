package com.studentissue.complaint_service.service;

import com.studentissue.complaint_service.dto.MapMarkerResponse;
import com.studentissue.complaint_service.model.Category;
import com.studentissue.complaint_service.model.Status;
import com.studentissue.complaint_service.repository.ComplaintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MapService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;
    private static final double MAX_RADIUS_METERS = 50_000;

    private final ComplaintRepository repository;

    public MapService(ComplaintRepository repository) {
        this.repository = repository;
    }

    public List<MapMarkerResponse> inBounds(double minLat, double minLng, double maxLat, double maxLng,
                                            Category category, Status status, Integer limit) {
        checkLatitude(minLat);
        checkLatitude(maxLat);
        checkLongitude(minLng);
        checkLongitude(maxLng);
        if (minLat > maxLat || minLng > maxLng) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limitele zonei sunt invalide (min trebuie să fie mai mic decât max)");
        }
        return repository.findInBounds(minLat, minLng, maxLat, maxLng,
                        name(category), name(status), clamp(limit))
                .stream().map(MapMarkerResponse::from).toList();
    }

    public List<MapMarkerResponse> nearby(double lat, double lng, double radiusMeters,
                                          Category category, Status status, Integer limit) {
        checkLatitude(lat);
        checkLongitude(lng);
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Raza trebuie să fie între 1 și 50000 de metri");
        }
        return repository.findNearby(lat, lng, radiusMeters, name(category), name(status), clamp(limit))
                .stream().map(MapMarkerResponse::from).toList();
    }

    private void checkLatitude(double v) {
        if (v < -90 || v > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitudinea trebuie să fie între -90 și 90");
        }
    }

    private void checkLongitude(double v) {
        if (v < -180 || v > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitudinea trebuie să fie între -180 și 180");
        }
    }

    private int clamp(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private String name(Enum<?> e) {
        return e == null ? null : e.name();
    }
}