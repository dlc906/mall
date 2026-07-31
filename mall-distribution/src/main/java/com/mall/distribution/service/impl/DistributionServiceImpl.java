package com.mall.distribution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BizException;
import com.mall.distribution.entity.CommissionRecord;
import com.mall.distribution.entity.DistributionRelationship;
import com.mall.distribution.mapper.CommissionRecordMapper;
import com.mall.distribution.mapper.DistributionRelationshipMapper;
import com.mall.distribution.service.DistributionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class DistributionServiceImpl implements DistributionService {

    @Resource
    private DistributionRelationshipMapper relationshipMapper;
    @Resource
    private CommissionRecordMapper commissionRecordMapper;

    /** 一级分销佣金比例: 10% */
    private static final BigDecimal LEVEL1_RATIO = new BigDecimal("10");
    /** 二级分销佣金比例: 5% */
    private static final BigDecimal LEVEL2_RATIO = new BigDecimal("5");

    @Override
    public String getInviteCode(Long userId) {
        // Invite code is stored in user service; here we just return
        // In production: call Feign to user service
        return "INV-" + userId + "-" + System.currentTimeMillis() % 10000;
    }

    @Override
    @Transactional
    public void bindRelationship(Long newUserId, String inviteCode) {
        // The relationship binding happens during user registration
        // This method is for explicit binding after registration if needed
        log.info("Binding distribution relationship: userId={}, inviteCode={}", newUserId, inviteCode);
    }

    @Override
    @Transactional
    public void calculateCommission(Long orderId, String orderNo, Long buyerUserId) {
        // Check if buyer has a parent distributor (inviter)
        DistributionRelationship rel = relationshipMapper.selectOne(
                new LambdaQueryWrapper<DistributionRelationship>()
                        .eq(DistributionRelationship::getUserId, buyerUserId));

        if (rel == null) {
            log.debug("No distribution relationship for user {}", buyerUserId);
            return;
        }

        // Simplified: using mock order amount
        BigDecimal orderAmount = new BigDecimal("100.00");

        String settleMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Calculate Level 1 commission
        if (rel.getParentId() != null) {
            CommissionRecord comm = new CommissionRecord();
            comm.setUserId(rel.getParentId());
            comm.setOrderNo(orderNo);
            comm.setOrderId(orderId);
            comm.setBuyerUserId(buyerUserId);
            comm.setOrderAmount(orderAmount);
            comm.setCommissionRatio(LEVEL1_RATIO);
            comm.setCommissionAmount(orderAmount.multiply(LEVEL1_RATIO).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            comm.setLevel(1);
            comm.setStatus(0);
            comm.setSettleMonth(settleMonth);
            commissionRecordMapper.insert(comm);

            log.info("Level 1 commission: userId={}, amount={}", rel.getParentId(), comm.getCommissionAmount());
        }

        // Calculate Level 2 commission
        if (rel.getGrandparentId() != null) {
            CommissionRecord comm = new CommissionRecord();
            comm.setUserId(rel.getGrandparentId());
            comm.setOrderNo(orderNo);
            comm.setOrderId(orderId);
            comm.setBuyerUserId(buyerUserId);
            comm.setOrderAmount(orderAmount);
            comm.setCommissionRatio(LEVEL2_RATIO);
            comm.setCommissionAmount(orderAmount.multiply(LEVEL2_RATIO).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            comm.setLevel(2);
            comm.setStatus(0);
            comm.setSettleMonth(settleMonth);
            commissionRecordMapper.insert(comm);

            log.info("Level 2 commission: userId={}, amount={}", rel.getGrandparentId(), comm.getCommissionAmount());
        }
    }

    @Override
    public Page<CommissionRecord> pageCommissions(Long userId, int pageNum, int pageSize) {
        return commissionRecordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getUserId, userId)
                        .orderByDesc(CommissionRecord::getCreateTime));
    }

    @Override
    public BigDecimal getTotalCommission(Long userId) {
        return commissionRecordMapper.selectList(
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getUserId, userId)
                        .eq(CommissionRecord::getStatus, 1)) // Only settled
                .stream()
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void settleMonthlyCommission() {
        String settleMonth = LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));

        int count = 0;
        // Settle all pending commissions for last month
        LambdaQueryWrapper<CommissionRecord> wrapper = new LambdaQueryWrapper<>();
        // In production: filter by settleMonth
        wrapper.eq(CommissionRecord::getStatus, 0);

        for (CommissionRecord record : commissionRecordMapper.selectList(wrapper)) {
            record.setStatus(1);
            commissionRecordMapper.updateById(record);
            count++;
        }

        log.info("Settled {} commission records for month {}", count, settleMonth);
    }

    @Override
    public DistributionRelationship getRelationship(Long userId) {
        return relationshipMapper.selectOne(
                new LambdaQueryWrapper<DistributionRelationship>()
                        .eq(DistributionRelationship::getUserId, userId));
    }
}
