package fun.yeelo.oauth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记需要登录验证的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
    /**
     * 是否需要检查用户存在性
     */
    boolean checkUserExists() default true;
    
    /**
     * 用户不存在时的错误消息
     */
    String notExistMessage() default "用户不存在，请联系管理员";
    
    /**
     * 未登录时的错误消息
     */
    String notLoginMessage() default "用户未登录，请尝试刷新页面";
} 