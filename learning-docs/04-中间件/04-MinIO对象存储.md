# MinIO 对象存储

> 本项目使用 **MinIO** 存储知识库文档（PDF、TXT）、用户头像、商品图片等文件。
> 对应项目文件：`docker-compose.yml`（MinIO 容器）、`ai-cs-knowledge`（文档上传）

---

## 一、什么是对象存储？

```
【文件系统】树形目录结构
  /home/user/docs/report.pdf
  → 适合本地，不适合分布式

【对象存储】扁平的 Key-Value
  Bucket: aics-knowledge
    ├── docs/product-manual.pdf
    ├── docs/faq.txt
    └── images/avatar-100.png
  → 海量文件、HTTP 访问、无限扩展
```

### 为什么不用 MySQL 存文件？

| 对比 | MySQL | MinIO |
|------|-------|-------|
| 存储大小 | 不适合大文件 | TB 级 |
| 访问方式 | 需要查库再读 | 直接 HTTP URL |
| 成本 | 贵（SSD） | 便宜（HDD） |
| 扩展 | 困难 | 分布式扩展 |

---

## 二、Docker 部署

```yaml
# docker-compose.yml
minio:
  image: minio/minio:latest
  container_name: aics-minio
  ports:
    - "9000:9000"     # API 端口
    - "9001:9001"     # 管理控制台
  environment:
    MINIO_ROOT_USER: minioadmin       # 用户名
    MINIO_ROOT_PASSWORD: minioadmin   # 密码
  volumes:
    - minio-data:/data
  command: server /data --console-address ":9001"
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
    interval: 10s
    timeout: 5s
    retries: 5
```

启动后访问：
- API：`http://localhost:9000`
- 控制台：`http://localhost:9001`（账号 minioadmin/minioadmin）

---

## 三、核心概念

```
MinIO
├── Bucket（桶）= 顶级容器，类似文件夹
│   ├── aics-knowledge    ← 知识库文档
│   ├── aics-avatar       ← 用户头像
│   └── aics-product-img  ← 商品图片
│
└── Object（对象）= 一个文件
    ├── key: "docs/2024/product-manual.pdf"
    ├── data: 文件二进制内容
    └── metadata: Content-Type, Size, 自定义标签
```

---

## 四、Java 集成

### 4.1 依赖

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 4.2 配置

```yaml
# application.yml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket:
    knowledge: aics-knowledge
    avatar: aics-avatar
```

### 4.3 配置类

```java
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}
```

### 4.4 文件上传服务

```java
@Service
public class FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.knowledge}")
    private String knowledgeBucket;

    /**
     * 上传知识库文档
     */
    public String uploadDocument(MultipartFile file, String category) {
        try {
            // 1. 确保 Bucket 存在
            ensureBucketExists(knowledgeBucket);

            // 2. 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String objectName = category + "/" + UUID.randomUUID() + extension;

            // 3. 上传
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(knowledgeBucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            return objectName;  // 返回存储路径
        } catch (Exception e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件下载 URL（预签名，有效期 7 天）
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(knowledgeBucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
        } catch (Exception e) {
            throw new BusinessException("获取文件URL失败");
        }
    }

    /**
     * 下载文件（用于 RAG 文档加载）
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(knowledgeBucket)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new BusinessException("文件下载失败");
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucket).build()
            );
        }
    }
}
```

---

## 五、与 RAG 的结合

```
用户上传 PDF → MinIO 存储 → 异步触发向量化 → 存入向量数据库
                                                    ↓
用户提问 → 向量检索相关文档 → 从 MinIO 读取原文 → 组装 Prompt → AI 回答
```

```java
// 知识库文档处理流程
@Service
public class KnowledgeService {

    @Autowired
    private FileStorageService fileStorage;
    @Autowired
    private DocumentLoader documentLoader;  // ai-cs-chat 中的 RAG 组件

    public void processDocument(MultipartFile file) {
        // 1. 上传到 MinIO
        String objectName = fileStorage.uploadDocument(file, "raw");

        // 2. 下载并解析（调用 ai-cs-chat 的 DocumentLoader）
        InputStream is = fileStorage.downloadFile(objectName);
        Resource resource = new InputStreamResource(is);
        List<Document> documents = documentLoader.loadPdf(resource);

        // 3. 分块 + 向量化 + 存储
        // vectorStore.add(splitAndEmbed(documents));
    }
}
```

---

## 六、控制台操作

访问 `http://localhost:9001`：

1. **创建 Bucket**：Buckets → Create Bucket → 名称 `aics-knowledge`
2. **上传文件**：进入 Bucket → Upload → 选择文件
3. **设置访问策略**：Bucket → Access Policy → 设为 `public`（开发用）或 `private`
4. **获取 URL**：点击文件 → Share → 生成预签名链接

---

## 七、动手练习

1. 启动 MinIO：`docker-compose up -d minio`
2. 访问控制台 `http://localhost:9001`，创建 Bucket
3. 手动上传一个 PDF 文件
4. 在 Java 中写上传/下载接口
5. 测试预签名 URL 的有效期

---

## 学习检查清单

- [ ] 理解对象存储 vs 文件系统的区别
- [ ] 理解 Bucket 和 Object 的概念
- [ ] 会用 MinIO 控制台操作
- [ ] 会在 Java 中上传/下载文件
- [ ] 理解预签名 URL 的用途和安全性
- [ ] 理解 MinIO 在 RAG 流程中的角色

---

## 下一步

→ [05-AI集成/01-SpringAI入门](../05-AI集成/01-SpringAI入门.md)
