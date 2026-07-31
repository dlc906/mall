package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.exception.BizException;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.model.req.CategoryReq;
import com.mall.product.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getCategoryTree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));

        // Build tree structure
        Map<Long, List<Category>> parentMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        // In-memory tree (not deeply nested in entity, just return flat with parentId
        // Frontend will build the tree. For actual tree, use recursion if needed.
        return all;
    }

    @Override
    @Transactional
    public Category addCategory(CategoryReq req) {
        Category category = new Category();
        BeanUtils.copyProperties(req, category);
        category.setStatus(1);

        // Auto-set level based on parent
        if (req.getParentId() != null && req.getParentId() > 0) {
            Category parent = categoryMapper.selectById(req.getParentId());
            if (parent != null) {
                category.setLevel(parent.getLevel() + 1);
            }
        } else {
            category.setLevel(1);
        }

        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategoryReq req) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BizException("分类不存在");
        BeanUtils.copyProperties(req, category, "id");
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        // Check for sub-categories
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (count > 0) throw new BizException("该分类下有子分类，无法删除");
        categoryMapper.deleteById(id);
    }
}
