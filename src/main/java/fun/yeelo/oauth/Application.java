package fun.yeelo.oauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

@SpringBootApplication
@MapperScan("fun.yeelo.oauth.dao")  // Adjust the package to match where your mappers are located
public class Application {
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET);

        // set biz to use resource loader.
        ResourceLoader resourceLoader = new DefaultResourceLoader(SpringApplicationBuilder.class.getClassLoader());
        builder.resourceLoader(resourceLoader);
        ConfigurableApplicationContext context = builder.run(args);

    }
}
