package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.mail")
@Data
public class MailProperties {
    
    private boolean enable;
    private String host;
    private int port;
    private String username;
    private String password;
    private Map<String, Object> properties;
} 