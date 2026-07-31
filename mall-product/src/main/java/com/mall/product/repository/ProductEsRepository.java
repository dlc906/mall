package com.mall.product.repository;

import com.mall.product.model.es.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductEsRepository extends ElasticsearchRepository<ProductDocument, Long> {

    Page<ProductDocument> findByNameOrDescriptionOrKeywords(String name, String desc, String keywords, Pageable pageable);

    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    void deleteByCategoryId(Long categoryId);
}
