package com.mall.product.model.req;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CategoryReq {
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;
}
