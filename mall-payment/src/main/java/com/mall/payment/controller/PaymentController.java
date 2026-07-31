package com.mall.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.entity.Result;
import com.mall.payment.entity.PaymentRecord;
import com.mall.payment.model.req.PayReq;
import com.mall.payment.model.resp.PayResp;
import com.mall.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "支付管理", description = "模拟支付、退款")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Operation(summary = "支付")
    @PostMapping("/pay")
    public Result<PayResp> pay(@RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody PayReq req) {
        return Result.success(paymentService.pay(userId, req));
    }

    @Operation(summary = "退款")
    @PostMapping("/refund/{orderNo}")
    public Result<PayResp> refund(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable String orderNo) {
        return Result.success(paymentService.refund(userId, orderNo));
    }

    @Operation(summary = "支付记录列表")
    @GetMapping("/records")
    public Result<Page<PaymentRecord>> listRecords(@RequestHeader("X-User-Id") Long userId,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(paymentService.pagePayments(userId, pageNum, pageSize));
    }

    @Operation(summary = "支付记录详情")
    @GetMapping("/record/{paymentNo}")
    public Result<PaymentRecord> recordDetail(@PathVariable String paymentNo) {
        return Result.success(paymentService.getPayment(paymentNo));
    }

    @Operation(summary = "支付回调(模拟)")
    @PostMapping("/callback")
    public Result<String> callback(@RequestParam String orderNo,
                                    @RequestParam(defaultValue = "success") String result) {
        // Simulated payment gateway callback
        return Result.success("回调处理成功: " + orderNo);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
