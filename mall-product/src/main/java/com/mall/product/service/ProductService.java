package com.mall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.entity.Product;
import com.mall.product.model.es.ProductDocument;
import com.mall.product.model.req.ProductReq;

import java.util.List;

public interface ProductService {
    Page<Product> pageProducts(int pageNum, int pageSize, Long categoryId, String keyword);
    Product getProductDetail(Long id);
    Product addProduct(ProductReq req);
    Product updateProduct(Long id, ProductReq req);
    void deleteProduct(Long id);
    void updateStock(Long id, int quantity);
    void incrementSales(Long id, int quantity);
    Page<ProductDocument> searchFromEs(String keyword, Long categoryId, int pageNum, int pageSize);
    void syncAllToEs();
}
