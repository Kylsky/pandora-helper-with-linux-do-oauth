package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties
@Component
@Data
public class AppProperties {
    private String fuclaudeProxy;
    private String adminName;
    private String chatSite;
    private String tokenProxy;
    private Task task;

    public static class Task {
        private String refresh;

        public String getRefresh() {
            return refresh;
        }

        public void setRefresh(String refresh) {
            this.refresh = refresh;
        }
    }
} 