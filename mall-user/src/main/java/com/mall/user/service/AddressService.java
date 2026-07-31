package com.mall.user.service;

import com.mall.user.entity.Address;
import com.mall.user.model.req.AddressReq;
import java.util.List;

public interface AddressService {
    List<Address> listAddress(Long userId);
    Address getAddress(Long id);
    Address addAddress(Long userId, AddressReq req);
    Address updateAddress(Long id, AddressReq req);
    void deleteAddress(Long id);
    void setDefault(Long userId, Long id);
}
