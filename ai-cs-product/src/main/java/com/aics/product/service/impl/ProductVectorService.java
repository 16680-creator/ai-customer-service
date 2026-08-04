package com.aics.product.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.product.entity.Product;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.ImageDescriptionService;
import com.aics.product.vo.ProductSimilarVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 商品向量服务：将商品名称/描述/图片描述向量化入向量库，提供相似商品检索。
 * 检索文本支持商品描述关键词（以文搜图），图片识别接入后自动增强（以图搜文）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVectorService {

    private static final String METADATA_PRODUCT_ID = "productId";

    private final VectorStore vectorStore;
    private final ProductMapper productMapper;
    private final ImageDescriptionService imageDescriptionService;

    /**
     * 索引商品（创建/更新时调用；相同商品 ID 重复索引会覆盖）
     */
    public void indexProduct(Product product) {
        String text = buildSearchText(product);
        if (!StringUtils.hasText(text)) {
            log.warn("商品无可索引文本，跳过: id={}", product.getId());
            return;
        }
        Document document = new Document(
                String.valueOf(product.getId()),
                text,
                Map.of(METADATA_PRODUCT_ID, product.getId(), "name", product.getName()));
        vectorStore.add(List.of(document));
        log.info("商品向量索引成功: id={}, text={}", product.getId(), text);
    }

    /**
     * 删除商品索引
     */
    public void removeProduct(Long productId) {
        vectorStore.delete(List.of(String.valueOf(productId)));
        log.info("商品向量索引已删除: id={}", productId);
    }

    /**
     * 按文本检索相似商品（以文搜图）
     */
    public List<ProductSimilarVO> searchByText(String text, int topK) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "检索文本不能为空");
        }
        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder().query(text).topK(topK).build());
            return documents.stream()
                    .map(this::toVO)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("相似商品检索失败: text={}", text, e);
            throw new BusinessException(ResultCode.PRODUCT_SIMILAR_SEARCH_FAIL, "相似商品检索失败: " + e.getMessage());
        }
    }

    /**
     * 按商品查找相似商品（以商品自身的索引文本作为查询）
     */
    public List<ProductSimilarVO> searchByProduct(Long productId, int topK) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }
        String text = buildSearchText(product);
        return searchByText(text, topK + 1).stream()
                .filter(vo -> !vo.getProductId().equals(productId))
                .limit(topK)
                .toList();
    }

    private String buildSearchText(Product product) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(product.getName())) {
            sb.append(product.getName());
        }
        if (StringUtils.hasText(product.getDescription())) {
            sb.append(" ").append(product.getDescription());
        }
        if (StringUtils.hasText(product.getImage())) {
            String imageDescription = imageDescriptionService.describe(product.getImage());
            if (StringUtils.hasText(imageDescription)) {
                sb.append(" ").append(imageDescription);
            }
        }
        return sb.toString();
    }

    private ProductSimilarVO toVO(Document document) {
        Object idValue = document.getMetadata().get(METADATA_PRODUCT_ID);
        if (idValue == null) {
            return null;
        }
        Product product = productMapper.selectById(Long.valueOf(idValue.toString()));
        if (product == null) {
            return null;
        }
        ProductSimilarVO vo = new ProductSimilarVO();
        vo.setProductId(product.getId());
        vo.setName(product.getName());
        vo.setImage(product.getImage());
        vo.setPrice(product.getPrice());
        Double score = document.getScore();
        vo.setScore(score == null ? 0.0 : score);
        return vo;
    }
}
