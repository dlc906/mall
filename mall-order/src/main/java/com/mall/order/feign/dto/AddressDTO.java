package com.mall.order.feign.dto;

import lombok.Data;

/**
 * 用户服务返回的收货地址 DTO（Feign 传输对象）
 */
@Data
public class AddressDTO {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
}
