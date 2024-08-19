package fun.yeelo.oauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("fun.yeelo.oauth.dao")  // Adjust the package to match where your mappers are located
public class Application {
    public static void main(String[] args) {
        System.setProperty("https.protocols", "TLSv1.3,TLSv1.2,TLSv1.1,TLSv1");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET);

        // set biz to use resource loader.
        ResourceLoader resourceLoader = new DefaultResourceLoader(SpringApplicationBuilder.class.getClassLoader());
        builder.resourceLoader(resourceLoader);
        ConfigurableApplicationContext context = builder.run(args);

    }
}
