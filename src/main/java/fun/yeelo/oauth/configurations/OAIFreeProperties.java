package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "linux-do.oaifree")
@Data
public class OAIFreeProperties {

    private String authApi;

    private String tokenApi;
} 