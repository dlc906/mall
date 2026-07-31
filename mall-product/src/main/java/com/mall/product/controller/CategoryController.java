package com.mall.product.controller;

import com.mall.common.entity.Result;
import com.mall.product.entity.Category;
import com.mall.product.model.req.CategoryReq;
import com.mall.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "商品分类", description = "商品分类管理")
@RestController
@RequestMapping("/api/product/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "分类树")
    @GetMapping
    public Result<List<Category>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Category> add(@Valid @RequestBody CategoryReq req) {
        return Result.success(categoryService.addCategory(req));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryReq req) {
        return Result.success(categoryService.updateCategory(id, req));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
