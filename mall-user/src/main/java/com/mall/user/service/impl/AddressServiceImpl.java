package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.exception.BizException;
import com.mall.user.entity.Address;
import com.mall.user.mapper.AddressMapper;
import com.mall.user.model.req.AddressReq;
import com.mall.user.service.AddressService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Resource
    private AddressMapper addressMapper;

    @Override
    public List<Address> listAddress(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime));
    }

    @Override
    public Address getAddress(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    @Transactional
    public Address addAddress(Long userId, AddressReq req) {
        Address address = new Address();
        BeanUtils.copyProperties(req, address);
        address.setUserId(userId);

        // Set as default if it's the first address or explicitly set
        if (req.getIsDefault() != null && req.getIsDefault() == 1) {
            clearDefault(userId);
        }
        if (req.getIsDefault() == null) {
            long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId));
            if (count == 0) {
                address.setIsDefault(1);
            } else {
                address.setIsDefault(0);
            }
        }

        addressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public Address updateAddress(Long id, AddressReq req) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BizException("地址不存在");
        }
        BeanUtils.copyProperties(req, address, "id", "userId");
        if (req.getIsDefault() != null && req.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }
        addressMapper.updateById(address);
        return address;
    }

    @Override
    public void deleteAddress(Long id) {
        addressMapper.deleteById(id);
    }

    @Override
    public void setDefault(Long userId, Long id) {
        clearDefault(userId);
        Address address = addressMapper.selectById(id);
        if (address != null) {
            address.setIsDefault(1);
            addressMapper.updateById(address);
        }
    }

    private void clearDefault(Long userId) {
        Address temp = new Address();
        temp.setIsDefault(0);
        addressMapper.update(temp, new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
    }
}
