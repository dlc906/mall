package com.mall.distribution.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.distribution.entity.CommissionRecord;
import com.mall.distribution.entity.DistributionRelationship;

import java.math.BigDecimal;

public interface DistributionService {
    String getInviteCode(Long userId);
    void bindRelationship(Long newUserId, String inviteCode);
    void calculateCommission(Long orderId, String orderNo, Long buyerUserId);
    Page<CommissionRecord> pageCommissions(Long userId, int pageNum, int pageSize);
    BigDecimal getTotalCommission(Long userId);
    void settleMonthlyCommission();
    DistributionRelationship getRelationship(Long userId);
}
