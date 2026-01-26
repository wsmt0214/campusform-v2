package com.campusform.server.global.infrastructure;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 파일 업로드/삭제 서비스
 *
 * 모든 컨텍스트에서 공유하는 파일 업로드 인프라 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 프로필 이미지 업로드
     *
     * @param file 업로드할 이미지 파일
     * @param userId 사용자 ID
     * @return S3에 저장된 파일의 URL
     */
    public String uploadProfileImage(MultipartFile file, Long userId) {
        validateImageFile(file);

        String fileName = generateProfileImageFileName(userId, file.getOriginalFilename());
        String key = "profile-images/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String imageUrl = getFileUrl(key);
            log.info("프로필 이미지 업로드 성공: userId={}, url={}", userId, imageUrl);

            return imageUrl;

        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패: userId={}", userId, e);
            throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 파일 삭제
     *
     * @param fileUrl S3 파일 URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.contains(bucketName)) {
            return; // Google 프로필 이미지 등 외부 URL은 삭제하지 않음
        }

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: key={}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: url={}", fileUrl, e);
        }
    }

    /**
     * 파일이 유효한 이미지인지 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("파일이 비어있습니다.");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");

        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize)
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
    }

    /**
     * 프로필 이미지 파일명 생성
     */
    private String generateProfileImageFileName(Long userId, String originalFilename) {
        String extension = extractExtension(originalFilename);
        return userId + "_" + UUID.randomUUID() + extension;
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExtension(String filename) {
        if (filename == null)
            return "";
        int lastDotIndex = filename.lastIndexOf(".");
        return (lastDotIndex == -1) ? "" : filename.substring(lastDotIndex);
    }

    /**
     * S3 파일 URL 생성
     */
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * S3 URL에서 key 추출
     */
    private String extractKeyFromUrl(String fileUrl) {
        String[] parts = fileUrl.split(bucketName + ".s3." + region + ".amazonaws.com/");
        if (parts.length < 2)
            throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + fileUrl);
        return parts[1];
    }
}
