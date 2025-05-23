package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mirror")
@Data
public class MirrorProperties {
    private Boolean enable;

    private String host;

    private String password;
} 