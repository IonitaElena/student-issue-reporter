package com.studentissue.complaint_service.controller;

import com.studentissue.complaint_service.dto.ImageResponse;
import com.studentissue.complaint_service.service.ImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/complaints/{complaintId}/images")
public class ImageController {

    private final ImageService service;

    public ImageController(ImageService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse upload(@PathVariable Long complaintId,
                                @RequestHeader("X-User-Id") Long userId,
                                @RequestHeader("X-User-Role") String role,
                                @RequestParam("file") MultipartFile file) {
        return service.upload(complaintId, userId, role, file);
    }

    @GetMapping
    public List<ImageResponse> list(@PathVariable Long complaintId) {
        return service.list(complaintId);
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long complaintId,
                                            @PathVariable Long imageId) {
        ImageService.ImageContent content = service.load(complaintId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long complaintId,
                       @PathVariable Long imageId,
                       @RequestHeader("X-User-Id") Long userId,
                       @RequestHeader("X-User-Role") String role) {
        service.delete(complaintId, imageId, userId, role);
    }
}