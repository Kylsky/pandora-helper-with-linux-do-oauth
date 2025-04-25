package fun.yeelo.oauth.aspect;

import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 登录验证切面
 */
@Aspect
@Component
public class LoginCheckAspect {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ShareService shareService;

    /**
     * 环绕通知，处理需要登录验证的方法
     */
    @Around("@annotation(fun.yeelo.oauth.annotation.RequireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireLogin requireLogin = method.getAnnotation(RequireLogin.class);
        
        // 从请求中获取 token
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error(requireLogin.notLoginMessage());
        }
        
        // 检查用户是否存在
        if (requireLogin.checkUserExists()) {
            String username = jwtTokenUtil.extractUsername(token);
            Share user = shareService.getByUserName(username);
            if (user == null) {
                return HttpResult.error(requireLogin.notExistMessage());
            }
            
            // 将用户信息存储到请求属性中，方便后续使用
            request.setAttribute("currentUser", user);
        }
        
        // 继续执行原方法
        return joinPoint.proceed();
    }
} 