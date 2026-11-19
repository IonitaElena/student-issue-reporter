package com.studentissue.complaint_service.repository;

import com.studentissue.complaint_service.dto.MapMarker;
import com.studentissue.complaint_service.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query(value = """
            SELECT c.id AS "id", c.title AS "title", c.category AS "category",
                   c.status AS "status", c.priority AS "priority",
                   c.latitude AS "latitude", c.longitude AS "longitude",
                   (SELECT COUNT(*) FROM complaint_votes v WHERE v.complaint_id = c.id) AS "voteCount",
                   CAST(NULL AS double precision) AS "distanceMeters"
            FROM complaints c
            WHERE c.location IS NOT NULL
              AND c.location && CAST(ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326) AS geography)
              AND (CAST(:category AS text) IS NULL OR c.category = CAST(:category AS text))
              AND (CAST(:status AS text) IS NULL OR c.status = CAST(:status AS text))
            ORDER BY c.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<MapMarker> findInBounds(@Param("minLat") double minLat, @Param("minLng") double minLng,
                                 @Param("maxLat") double maxLat, @Param("maxLng") double maxLng,
                                 @Param("category") String category, @Param("status") String status,
                                 @Param("limit") int limit);

    @Query(value = """
            SELECT c.id AS "id", c.title AS "title", c.category AS "category",
                   c.status AS "status", c.priority AS "priority",
                   c.latitude AS "latitude", c.longitude AS "longitude",
                   (SELECT COUNT(*) FROM complaint_votes v WHERE v.complaint_id = c.id) AS "voteCount",
                   ST_Distance(c.location,
                       CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)) AS "distanceMeters"
            FROM complaints c
            WHERE c.location IS NOT NULL
              AND ST_DWithin(c.location,
                       CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography), :radius)
              AND (CAST(:category AS text) IS NULL OR c.category = CAST(:category AS text))
              AND (CAST(:status AS text) IS NULL OR c.status = CAST(:status AS text))
            ORDER BY "distanceMeters" ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<MapMarker> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                               @Param("radius") double radiusMeters,
                               @Param("category") String category, @Param("status") String status,
                               @Param("limit") int limit);
}