package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "midjourney")
@Data
public class MidjourneyProperties {
    private String url;

    private String key;

    private Boolean enable;
} 