package fun.yeelo.oauth.config;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Component
@Slf4j
public class Handler {

    @ExceptionHandler(ExpiredJwtException.class)
    public HttpResult<String> handleException(Exception ex) {
        log.error("ExpiredJwtException:", ex);

        return HttpResult.error(-1,"登录状态过期，请重新登录");
    }
}
