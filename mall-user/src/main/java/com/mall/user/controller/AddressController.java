package com.mall.user.controller;

import com.mall.common.entity.Result;
import com.mall.user.entity.Address;
import com.mall.user.model.req.AddressReq;
import com.mall.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "地址管理", description = "收货地址CRUD")
@RestController
@RequestMapping("/api/user/address")
public class AddressController {

    @Resource
    private AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public Result<List<Address>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(addressService.listAddress(userId));
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public Result<Address> detail(@PathVariable Long id) {
        return Result.success(addressService.getAddress(id));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Address> add(@RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody AddressReq req) {
        return Result.success(addressService.addAddress(userId, req));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @Valid @RequestBody AddressReq req) {
        return Result.success(addressService.updateAddress(id, req));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return Result.success();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long id) {
        addressService.setDefault(userId, id);
        return Result.success();
    }
}
