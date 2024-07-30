package fun.yeelo.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 将allowedOrigins 改成allowedOriginPatterns
                .allowedOriginPatterns("*")
                // 是否发送Cookie信息
                .allowCredentials(true)
                // 放行哪些原始域(请求方式)
                .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                // 放行哪些原始域(头部信息)
                .allowedHeaders("*")
                // 设置预检请求的缓存时间为 3600 秒
                .maxAge(3600);
    }
}
