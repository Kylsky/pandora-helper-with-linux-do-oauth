package fun.yeelo.oauth.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/account").setViewName("forward:/index.html");
        registry.addViewController("/share").setViewName("forward:/index.html");
        registry.addViewController("/redemption").setViewName("forward:/index.html");
        registry.addViewController("/car").setViewName("forward:/index.html");
        registry.addViewController("/reset").setViewName("forward:/index.html");
        registry.addViewController("/pandora").setViewName("forward:/index.html");
        registry.addViewController("/loading").setViewName("forward:/index.html");
        registry.addViewController("/claude").setViewName("forward:/index.html");
        registry.addViewController("/navi").setViewName("forward:/index.html");
        WebMvcConfigurer.super.addViewControllers(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 添加资源处理器，用于映射静态资源路径
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        WebMvcConfigurer.super.addResourceHandlers(registry);
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer(){
        return factory -> {
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/static/index.html");
            factory.addErrorPages(error404Page);
        };
    }
}
