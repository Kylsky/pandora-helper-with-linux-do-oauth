package fun.yeelo.oauth.aspect;

import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import fun.yeelo.oauth.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class LoginCheckAspect {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private ShareService shareService;
    
    @Pointcut("@annotation(fun.yeelo.oauth.annotation.RequireLogin)")
    public void loginCheck() {}
    
    @Before("loginCheck()")
    public void before() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            throw new RuntimeException("用户未登录，请尝试刷新页面");
        }
        
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            throw new RuntimeException("用户不存在，请联系管理员");
        }
        
        // 将用户信息存储在ThreadLocal中，供后续使用
        UserContext.setCurrentUser(user);
    }
} 