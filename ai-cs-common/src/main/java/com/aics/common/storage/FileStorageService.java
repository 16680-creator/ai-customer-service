package com.aics.common.storage;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务：上传、删除、生成访问 URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /**
     * 上传文件
     *
     * @param file      文件
     * @param directory 目录（如 product/earphone），为空时按日期分目录
     * @return 可访问的文件 URL
     */
    /**
     * 上传文件（本地磁盘或 MinIO），返回可访问 URL。
     * <p><b>学习要点</b>：文件名做 UUID 化避免冲突与路径穿越，目录按业务隔离。</p>
     */
    public String upload(MultipartFile file, String directory) {
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex).toLowerCase();
        }
        String objectName = buildObjectName(directory, ext);
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
            log.info("文件上传成功: bucket={}, object={}, size={}", properties.getBucket(), objectName, file.getSize());
        } catch (Exception e) {
            log.error("文件上传失败: object={}", objectName, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL, "文件上传失败: " + e.getMessage());
        }
        return buildUrl(objectName);
    }

    /**
     * 删除文件
     *
     * @param url 文件 URL
     */
    /** 按 URL 删除文件（解析出存储路径后删除） */
    public void delete(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        String objectName = extractObjectName(url);
        if (objectName == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
            log.info("文件删除成功: object={}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: object={}", objectName, e);
            throw new BusinessException(ResultCode.FILE_DELETE_FAIL, "文件删除失败: " + e.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            log.info("桶不存在，已自动创建: {}", properties.getBucket());
        }
    }

    private String buildObjectName(String directory, String ext) {
        String dir = StringUtils.hasText(directory) ? directory
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return dir + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String buildUrl(String objectName) {
        String base = StringUtils.hasText(properties.getPublicEndpoint())
                ? properties.getPublicEndpoint() : properties.getEndpoint();
        return base + "/" + properties.getBucket() + "/" + objectName;
    }

    private String extractObjectName(String url) {
        String marker = "/" + properties.getBucket() + "/";
        int index = url.indexOf(marker);
        return index >= 0 ? url.substring(index + marker.length()) : null;
    }
}
