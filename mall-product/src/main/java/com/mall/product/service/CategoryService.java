package com.mall.product.service;

import com.mall.product.entity.Category;
import com.mall.product.model.req.CategoryReq;
import java.util.List;

public interface CategoryService {
    List<Category> getCategoryTree();
    Category addCategory(CategoryReq req);
    Category updateCategory(Long id, CategoryReq req);
    void deleteCategory(Long id);
}
