package fun.yeelo.oauth.config;

import fun.yeelo.oauth.enums.ExceptionTypeEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class HttpResult<T> implements Serializable {
    /**
     * 状态码
     */
    private boolean status;
    /**
     * 返回信息
     */
    private String message;
    /**
     * 错误类型
     */
    private Integer errorType;
    /**
     * 返回数据
     */
    private T data;

    public HttpResult() {

    }

    public HttpResult(boolean status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public HttpResult(boolean status, String message, Integer errorType, T data) {
        this.status = status;
        this.message = message;
        this.errorType = errorType;
        this.data = data;
    }

    public static <T> HttpResult<T> success() {
        return new HttpResult<>(true, null, null);
    }

    public static <T> HttpResult<T> success(T data) {
        return new HttpResult<>(true, null, data);
    }

    public static <T> HttpResult<T> success(T data, String message) {
        return new HttpResult<>(true, message, data);
    }

    public static <T> HttpResult<T> success(T data, String message, Object... args) {
        return new HttpResult<>(true, String.format(message, args), data);
    }

    public static <T> HttpResult<T> error(String message) {
        return new HttpResult<>(false, message, ExceptionTypeEnum.BIZ_EXCEPTION.getType(), null);
    }

    public static <T> HttpResult<T> error(String message, Object... args) {
        return new HttpResult<>(false, String.format(message, args), ExceptionTypeEnum.BIZ_EXCEPTION.getType(), null);
    }

    public static <T> HttpResult<T> error(int errorType, String message) {
        return new HttpResult<>(false, message, errorType, null);
    }

    public static <T> HttpResult<T> error(int errorType, String message, Object... args) {
        return new HttpResult<>(false, String.format(message, args), errorType, null);
    }

    public static <T> HttpResult<T> error(int errorType, String message, T data) {
        return new HttpResult<>(false, message, errorType, data);
    }
}
