package com.mall.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mall.common.constant.CommonConstants;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = CommonConstants.SUCCESS;
        result.message = "success";
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = success(data);
        result.message = message;
        return result;
    }

    public static <T> Result<T> error() {
        return error(CommonConstants.ERROR, "error");
    }

    public static <T> Result<T> error(String message) {
        return error(CommonConstants.ERROR, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = null;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public boolean isSuccess() {
        return CommonConstants.SUCCESS.equals(this.code);
    }
}
