package fun.yeelo.oauth.utils;

import fun.yeelo.oauth.domain.share.Share;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类，用于获取当前登录用户信息
 */
public class UserContextUtil {

    /**
     * 获取当前登录用户
     * @return 当前登录用户信息，如果未登录则返回 null
     */
    public static Share getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        
        HttpServletRequest request = attributes.getRequest();
        return (Share) request.getAttribute("currentUser");
    }
    
    /**
     * 获取当前登录用户名
     * @return 当前登录用户名，如果未登录则返回 null
     */
    public static String getCurrentUsername() {
        Share currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUniqueName() : null;
    }
} 