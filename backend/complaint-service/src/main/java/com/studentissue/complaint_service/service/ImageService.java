package com.studentissue.complaint_service.service;

import com.studentissue.complaint_service.dto.ImageResponse;
import com.studentissue.complaint_service.model.Complaint;
import com.studentissue.complaint_service.model.ComplaintImage;
import com.studentissue.complaint_service.repository.ComplaintImageRepository;
import com.studentissue.complaint_service.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGES_PER_COMPLAINT = 5;

    public record ImageContent(Resource resource, String contentType) {
    }

    private record ImageType(String extension, String contentType) {
    }

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository imageRepository;
    private final Path root;

    public ImageService(ComplaintRepository complaintRepository,
                        ComplaintImageRepository imageRepository,
                        @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.complaintRepository = complaintRepository;
        this.imageRepository = imageRepository;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Transactional
    public ImageResponse upload(Long complaintId, Long userId, String role, MultipartFile file) {
        Complaint complaint = findComplaint(complaintId);
        requireOwnerOrAdmin(complaint, userId, role);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fișierul lipsește sau este gol");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Imaginea depășește 5 MB");
        }
        if (imageRepository.countByComplaintId(complaintId) >= MAX_IMAGES_PER_COMPLAINT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O sesizare poate avea cel mult " + MAX_IMAGES_PER_COMPLAINT + " imagini");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fișierul nu a putut fi citit");
        }

        ImageType type = detectType(data);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sunt acceptate doar imagini JPEG, PNG sau WebP");
        }

        String storedName = UUID.randomUUID() + "." + type.extension();
        Path target = root.resolve(storedName).normalize();
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Imaginea nu a putut fi salvată");
        }

        try {
            ComplaintImage saved = imageRepository.save(ComplaintImage.builder()
                    .complaintId(complaintId)
                    .originalFilename(cleanOriginalName(file.getOriginalFilename()))
                    .storedFilename(storedName)
                    .contentType(type.contentType())
                    .sizeBytes(data.length)
                    .uploadedBy(userId)
                    .build());
            return ImageResponse.from(saved);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    public List<ImageResponse> list(Long complaintId) {
        findComplaint(complaintId);
        return imageRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(ImageResponse::from).toList();
    }

    public ImageContent load(Long complaintId, Long imageId) {
        ComplaintImage image = findImage(complaintId, imageId);
        Path path = root.resolve(image.getStoredFilename()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fișierul imaginii nu există");
        }
        return new ImageContent(new FileSystemResource(path), image.getContentType());
    }

    @Transactional
    public void delete(Long complaintId, Long imageId, Long userId, String role) {
        Complaint complaint = findComplaint(complaintId);
        requireOwnerOrAdmin(complaint, userId, role);

        ComplaintImage image = findImage(complaintId, imageId);
        imageRepository.delete(image);
        deleteQuietly(root.resolve(image.getStoredFilename()).normalize());
    }

    // ---------- ajutătoare ----------

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesizare inexistentă"));
    }

    private ComplaintImage findImage(Long complaintId, Long imageId) {
        return imageRepository.findById(imageId)
                .filter(i -> i.getComplaintId().equals(complaintId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagine inexistentă"));
    }

    private void requireOwnerOrAdmin(Complaint complaint, Long userId, String role) {
        if (!complaint.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Doar autorul sesizării sau un administrator poate modifica imaginile");
        }
    }

    // verificare după conținut (primii octeți), nu după extensie sau Content-Type
    private ImageType detectType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return new ImageType("jpg", "image/jpeg");
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return new ImageType("png", "image/png");
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return new ImageType("webp", "image/webp");
        }
        return null;
    }

    private String cleanOriginalName(String original) {
        String name = original == null ? null : StringUtils.getFilename(StringUtils.cleanPath(original));
        if (name == null || name.isBlank()) {
            return "imagine";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // fișier rămas orfan; nu blocăm operația
        }
    }
}