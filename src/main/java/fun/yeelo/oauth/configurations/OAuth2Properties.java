package fun.yeelo.oauth.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "linux-do.oauth2")
@Data
public class OAuth2Properties {

    private Client client;
    private Provider provider;

    public static class Client {
        private Registration registration;

        public Registration getRegistration() {
            return registration;
        }

        public void setRegistration(Registration registration) {
            this.registration = registration;
        }

        @Data
        public static class Registration {
            private String clientId;
            private String clientSecret;
            private String redirectUri;
            private String authorizationGrantType;
            private String scope;
        }
    }

    @Data
    public static class Provider {
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;
    }
} 