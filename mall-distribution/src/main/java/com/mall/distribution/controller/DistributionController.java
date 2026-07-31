package com.mall.distribution.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.entity.Result;
import com.mall.distribution.entity.CommissionRecord;
import com.mall.distribution.entity.DistributionRelationship;
import com.mall.distribution.service.DistributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "分销管理", description = "分销关系、佣金管理")
@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    @Resource
    private DistributionService distributionService;

    @Operation(summary = "获取我的邀请码")
    @GetMapping("/invite-code")
    public Result<Map<String, String>> getInviteCode(@RequestHeader("X-User-Id") Long userId) {
        String code = distributionService.getInviteCode(userId);
        Map<String, String> data = new HashMap<>();
        data.put("inviteCode", code);
        data.put("inviteUrl", "http://localhost:5173/register?inviteCode=" + code);
        return Result.success(data);
    }

    @Operation(summary = "我的佣金记录")
    @GetMapping("/commissions")
    public Result<Page<CommissionRecord>> commissions(@RequestHeader("X-User-Id") Long userId,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(distributionService.pageCommissions(userId, pageNum, pageSize));
    }

    @Operation(summary = "累计佣金")
    @GetMapping("/total-commission")
    public Result<BigDecimal> totalCommission(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(distributionService.getTotalCommission(userId));
    }

    @Operation(summary = "我的分销关系")
    @GetMapping("/relationship")
    public Result<DistributionRelationship> relationship(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(distributionService.getRelationship(userId));
    }

    @Operation(summary = "月度佣金结算(管理/XXL-Job)")
    @PostMapping("/settle")
    public Result<Void> settle() {
        distributionService.settleMonthlyCommission();
        return Result.success();
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
